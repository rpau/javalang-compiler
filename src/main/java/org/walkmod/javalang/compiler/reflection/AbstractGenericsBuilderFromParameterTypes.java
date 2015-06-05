/*
 Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 
Walkmod is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Walkmod is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;

public abstract class AbstractGenericsBuilderFromParameterTypes {
	private Map<String, SymbolType> typeMapping;

	private Map<String, SymbolType> typeMappingClasses;

	private List<Expression> args;

	private SymbolType[] typeArgs;

	private Type[] types;

	private SymbolTable symTable;

	public AbstractGenericsBuilderFromParameterTypes(
			Map<String, SymbolType> typeMapping, List<Expression> args,
			SymbolType[] typeArgs, SymbolTable symTable) {
		this.typeMapping = typeMapping;
		this.args = args;
		this.typeArgs = typeArgs;
		this.symTable = symTable;
	}

	public SymbolTable getSymbolTable() {
		return symTable;
	}

	public AbstractGenericsBuilderFromParameterTypes() {
	}

	public void setArgs(List<Expression> args) {
		this.args = args;
	}

	public List<Expression> getArgs() {
		return args;
	}

	public void setTypeArgs(SymbolType[] typeArgs) {
		this.typeArgs = typeArgs;
	}

	private SymbolType getType(ClassExpr classExpr) {
		String name = classExpr.getType().toString();
		SymbolType type = symTable.getType(name, ReferenceType.TYPE);
		if (type == null) {
			Class<?> clazz = null;
			try {
				clazz = TypesLoaderVisitor.getClassLoader().loadClass(name);

				String className = clazz.getName();
				type = new SymbolType();
				type.setName(className);

			} catch (ClassNotFoundException e) {
				// a name expression could be "org.walkmod.A" and this node
				// could be "org.walkmod"

			}
		}
		return type;
	}

	public void loadTypeMappingFromTypeArgs() throws Exception {
		typeMappingClasses = new HashMap<String, SymbolType>();
		java.lang.reflect.Type[] types = getTypes();

		int pos = 0;
		for (Type type : types) {
			if (type instanceof ParameterizedType) {

				Type aux = ((ParameterizedType) type).getRawType();
				if (aux instanceof Class) {
					if (((Class<?>) aux).getName().equals("java.lang.Class")) {
						Type[] targs = ((ParameterizedType) type)
								.getActualTypeArguments();
						for (Type targ : targs) {
							String letter = targ.toString();
							if (!"?".equals(letter)) {
								if (pos < args.size()) {
									Expression e = args.get(pos);
									// String className = "";
									if (e instanceof ClassExpr) {

										SymbolType eType = getType(((ClassExpr) e));

										typeMapping.put(letter, eType);
										typeMappingClasses.put(letter, eType);
									}
								} else {
									typeMapping.put(letter, new SymbolType(
											Object.class));
								}
							}
						}
					}
				}
			} 
			pos++;
		}

	}

	public void build() throws Exception {
		if (typeMappingClasses == null) {
			loadTypeMappingFromTypeArgs();
		}
		for (int i = 0; i < types.length && i < typeArgs.length; i++) {
			typeMappingUpdate(types[i], typeMappingClasses, typeArgs[i]);
		}

	}

	private void typeMappingUpdate(Type type,
			Map<String, SymbolType> typeMappingClasses, SymbolType typeArg) {
		if (type instanceof TypeVariable) {
			String name = ((TypeVariable<?>) type).getName();
			SymbolType st = typeMapping.get(name);
			if (st == null) {
				typeMapping.put(name, typeArg);
			} else {
				if (!typeMappingClasses.containsKey(name)) {
					if (st.getClazz().equals(Object.class)) {
						typeMapping.put(name, typeArg);
					} else {

						typeMapping.put(name, (SymbolType) st.merge(typeArg));

					}
				}
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType paramType = (ParameterizedType) type;
			Type[] args = paramType.getActualTypeArguments();
			if (typeArg != null) {
				List<SymbolType> paramsSymbol = typeArg.getParameterizedTypes();
				if (paramsSymbol != null) {
					for (int i = 0; i < args.length; i++) {
						typeMappingUpdate(args[i], typeMappingClasses,
								paramsSymbol.get(i));
					}
				}
			}
		} else if (type instanceof WildcardType) {
			if (typeArg != null) {
				WildcardType wildcardType = (WildcardType) type;
				Type[] upper = wildcardType.getUpperBounds();
				List<SymbolType> bounds = typeArg.getBounds();
				if (bounds == null) {
					bounds = new LinkedList<SymbolType>();
					bounds.add(typeArg);
				}

				for (int i = 0; i < upper.length; i++) {
					typeMappingUpdate(upper[i], typeMappingClasses,
							bounds.get(i));
				}

				Type[] lower = wildcardType.getLowerBounds();
				bounds = typeArg.getLowerBounds();
				if (bounds != null) {
					for (int i = 0; i < lower.length; i++) {
						typeMappingUpdate(lower[i], typeMappingClasses,
								bounds.get(i));
					}
				}
			}
		} else if (type instanceof GenericArrayType) {
			if (typeArg != null) {
				GenericArrayType arrayType = (GenericArrayType) type;
				SymbolType aux = typeArg.clone();
				aux.setArrayCount(typeArg.getArrayCount() - 1);
				typeMappingUpdate(arrayType.getGenericComponentType(),
						typeMappingClasses, aux);
			}
		}
	}

	public void setTypeMapping(Map<String, SymbolType> typeMapping) {
		this.typeMapping = typeMapping;
	}

	public Map<String, SymbolType> getTypeMapping() {
		return typeMapping;
	}

	public void setTypes(Type[] types) {
		this.types = types;
	}

	public Type[] getTypes() {
		return types;
	}

}
