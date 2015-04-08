package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;

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
		boolean hasGenerics = false;

		for (Type type : types) {
			if (type instanceof ParameterizedType) {
				if (!hasGenerics) {
					hasGenerics = true;
				}
				Type aux = ((ParameterizedType) type).getRawType();
				if (aux instanceof Class) {
					if (((Class<?>) aux).getName().equals("java.lang.Class")) {
						Type[] targs = ((ParameterizedType) type)
								.getActualTypeArguments();
						for (Type targ : targs) {
							String letter = targ.toString();
							if (!"?".equals(letter)
									&& !typeMapping.containsKey(letter)) {
								Expression e = args.get(pos);
								String className = "";
								if (e instanceof ClassExpr) {
									className = ((ClassExpr) e).getType()
											.toString();
									Class<?> tclazz = TypeTable.getInstance()
											.loadClass(className);
									typeMapping.put(letter, new SymbolType(
											tclazz.getName()));
								}
							}
						}
					}
				}
			} else if (type instanceof TypeVariable) {
				String name = ((TypeVariable<?>) type).getName();
				SymbolType st = typeMapping.get(name);
				if (st == null) {
					typeMapping.put(name, typeArgs[pos]);
				} else {
					typeMapping.put(name, (SymbolType) st.merge(typeArgs[pos]));
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
