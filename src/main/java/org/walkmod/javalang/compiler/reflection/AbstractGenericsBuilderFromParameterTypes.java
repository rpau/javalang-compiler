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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;

public abstract class AbstractGenericsBuilderFromParameterTypes {
	private Map<String, SymbolType> typeMapping;

	private List<Expression> args;

	private SymbolType[] typeArgs;

	private Type[] types;

	public AbstractGenericsBuilderFromParameterTypes(
			Map<String, SymbolType> typeMapping, List<Expression> args,
			SymbolType[] typeArgs) {
		this.typeMapping = typeMapping;
		this.args = args;
		this.typeArgs = typeArgs;
	}

	public AbstractGenericsBuilderFromParameterTypes() {
	}

	public void setArgs(List<Expression> args) {
		this.args = args;
	}

	public void setTypeArgs(SymbolType[] typeArgs) {
		this.typeArgs = typeArgs;
	}

	public void build() throws Exception {
		int pos = 0;

		Map<String, SymbolType> typeMappingClasses = new HashMap<String, SymbolType>();
		for (Type type : types) {
			if (type instanceof ParameterizedType) {

				Type aux = ((ParameterizedType) type).getRawType();
				if (aux instanceof Class) {
					if (((Class<?>) aux).getName().equals("java.lang.Class")) {
						Type[] targs = ((ParameterizedType) type)
								.getActualTypeArguments();
						for (Type targ : targs) {
							String letter = targ.toString();
							if (!"?".equals(letter)
									&& !typeMapping.containsKey(letter)) {
								if (pos < args.size()) {
									Expression e = args.get(pos);
									String className = "";
									if (e instanceof ClassExpr) {
										className = ((ClassExpr) e).getType()
												.toString();
										Class<?> tclazz = TypesLoaderVisitor.getClassLoader().loadClass(
														className);
										typeMapping.put(letter, new SymbolType(
												tclazz.getName()));
										typeMappingClasses
												.put(letter, new SymbolType(
														tclazz.getName()));
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
		pos = 0;
		for (Type type : types) {

			if (type instanceof TypeVariable) {
				String name = ((TypeVariable<?>) type).getName();
				SymbolType st = typeMapping.get(name);
				if (st == null) {
					typeMapping.put(name, typeArgs[pos]);
				} else {
					if (!typeMappingClasses.containsKey(name)) {
						typeMapping.put(name,
								(SymbolType) st.merge(typeArgs[pos]));
					}
				}
			}
			pos++;
		}

	}

	public void setTypeMapping(Map<String, SymbolType> typeMapping) {
		this.typeMapping = typeMapping;
	}

	public void setTypes(Type[] types) {
		this.types = types;
	}

}
