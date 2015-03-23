package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;

/**
 * For a given set of expressions, which some of them could reference an
 * specific class (e.g A.class), when the parameter is generic (e.g. T), then
 * the map that T corresponds to A.class
 * 
 * @author rpau
 *
 */
public class GenericsBuilderFromArgs implements
		Builder<Map<String, SymbolType>> {

	private Method method;

	private List<Expression> argumentValues;

	public GenericsBuilderFromArgs() {
	}

	public GenericsBuilderFromArgs(Method method,
			List<Expression> argumentValues, TypeTable<?> typeTable) {
		this.method = method;
		this.argumentValues = argumentValues;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public void setArgumentValues(List<Expression> argumentValues) {
		this.argumentValues = argumentValues;
	}

	@Override
	public Map<String, SymbolType> build(Map<String, SymbolType> obj) {
		if (obj == null) {
			obj = new HashMap<String, SymbolType>();
		}
		TypeVariable<?>[] typeVariables = method.getTypeParameters();

		if (typeVariables != null) {

			for (int i = 0; i < typeVariables.length; i++) {

				Type[] parameterTypes = method.getGenericParameterTypes();

				if (parameterTypes != null && argumentValues != null) {

					for (int j = 0; j < parameterTypes.length
							&& j < argumentValues.size(); j++) {

						if (parameterTypes[j] instanceof ParameterizedType) {

							String variableName = ((ParameterizedType) parameterTypes[j])
									.getActualTypeArguments()[0].toString();

							if (variableName.length() == 1) {
								if (argumentValues.get(j) instanceof ClassExpr) {
									Class<?> paramClass;
									try {
										paramClass = TypeTable
												.getInstance()
												.loadClass(
														((ClassExpr) argumentValues
																.get(j))
																.getType());
									} catch (ClassNotFoundException e) {
										throw new NoSuchExpressionTypeException(
												"Invalid class into the generics resolution",
												e);
									}

									SymbolType auxType = new SymbolType();
									auxType.setName(paramClass.getName());
									obj.put(variableName, auxType);
								}
							}
						}
					}
				}
			}
		}

		return obj;
	}

}
