/* 
Copyright (C) 2013 Raquel Pau and Albert Coroleu.

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
package org.walkmod.javalang.compiler.types;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.expr.ArrayAccessExpr;
import org.walkmod.javalang.ast.expr.ArrayCreationExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr.Operator;
import org.walkmod.javalang.ast.expr.BooleanLiteralExpr;
import org.walkmod.javalang.ast.expr.CastExpr;
import org.walkmod.javalang.ast.expr.CharLiteralExpr;
import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.ConditionalExpr;
import org.walkmod.javalang.ast.expr.DoubleLiteralExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.InstanceOfExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralMinValueExpr;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.expr.LongLiteralExpr;
import org.walkmod.javalang.ast.expr.LongLiteralMinValueExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.NormalAnnotationExpr;
import org.walkmod.javalang.ast.expr.NullLiteralExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.QualifiedNameExpr;
import org.walkmod.javalang.ast.expr.SingleMemberAnnotationExpr;
import org.walkmod.javalang.ast.expr.StringLiteralExpr;
import org.walkmod.javalang.ast.expr.SuperExpr;
import org.walkmod.javalang.ast.expr.ThisExpr;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.InvalidTypeException;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class ExpressionTypeAnalyzer<A extends Map<String, Object>> extends
		VoidVisitorAdapter<A> {

	public static final String TYPE_KEY = "type_key";

	private TypeTable<?> typeTable;

	private SymbolTable symbolTable;

	private static Logger LOG = Logger.getLogger(ExpressionTypeAnalyzer.class);

	private VoidVisitorAdapter<A> semanticVisitor;

	public ExpressionTypeAnalyzer(TypeTable<?> typeTable,
			SymbolTable symbolTable) {
		this(typeTable, symbolTable, null);
	}

	public ExpressionTypeAnalyzer(TypeTable<?> typeTable,
			SymbolTable symbolTable, VoidVisitorAdapter<A> semanticVisitor) {
		this.typeTable = typeTable;
		this.symbolTable = symbolTable;
		this.semanticVisitor = semanticVisitor;
	}

	@Override
	public void visit(ArrayAccessExpr n, A arg) {
		n.getName().accept(this, arg);
		SymbolType arrayType = (SymbolType) arg.remove(TYPE_KEY);
		SymbolType newType = new SymbolType();
		newType.setName(arrayType.getName());
		newType.setParameterizedTypes(arrayType.getParameterizedTypes());
		newType.setArrayCount(arrayType.getArrayCount() - 1);
		arg.put(TYPE_KEY, newType);
	}

	@Override
	public void visit(ArrayCreationExpr n, A arg) {
		SymbolType arrayType = typeTable.valueOf(n.getType());
		arrayType.setArrayCount(1);
		arg.put(TYPE_KEY, arrayType);
	}

	@Override
	public void visit(BinaryExpr n, A arg) {

		n.getLeft().accept(this, arg);
		SymbolType leftType = (SymbolType) arg.remove(TYPE_KEY);

		n.getRight().accept(this, arg);
		SymbolType rightType = (SymbolType) arg.remove(TYPE_KEY);

		SymbolType resultType = leftType;

		try {
			if (Types.isCompatible(typeTable.loadClass(leftType),
					typeTable.loadClass(rightType))) {
				resultType = rightType;
			}
		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);
		}

		if (n.getOperator().equals(Operator.plus)) {

			if (leftType.getName().equals("java.lang.String")) {
				resultType = leftType;
			} else if (rightType.getName().equals("java.lang.String")) {
				resultType = rightType;
			}
		}

		arg.put(TYPE_KEY, resultType);

	}

	@Override
	public void visit(BooleanLiteralExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("boolean"));
	}

	@Override
	public void visit(CastExpr n, A arg) {
		arg.put(TYPE_KEY, typeTable.valueOf(n.getType()));
	}

	@Override
	public void visit(CharLiteralExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("char"));
	}

	@Override
	public void visit(ClassExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("java.lang.Class"));
	}

	@Override
	public void visit(ConditionalExpr n, A arg) {
		// then and else expression must have the same type
		n.getThenExpr().accept(this, arg);
	}

	@Override
	public void visit(DoubleLiteralExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("double"));
	}

	@Override
	public void visit(FieldAccessExpr n, A arg) {

		n.getScope().accept(this, arg);

		SymbolType scopeType = (SymbolType) arg.remove(TYPE_KEY);

		Class<?> c = null;

		try {
			if (scopeType == null) {
				try {
					c = typeTable.loadClass(n.toString());
					if (c != null) {
						String className = c.getName();
						scopeType = new SymbolType();
						scopeType.setName(className);
						symbolTable.lookUpSymbolForRead(
								typeTable.getSimpleName(className),
								ReferenceType.TYPE);
						arg.put(TYPE_KEY, scopeType);
					} else {
						arg.put(TYPE_KEY, null);
					}
				} catch (ClassNotFoundException e) {
					arg.put(TYPE_KEY, null);
				}
			} else {
				c = typeTable.loadClass(scopeType);

				Field field = null;
				if (c.isArray() && n.getField().equals("length")) {

					arg.put(TYPE_KEY, new SymbolType("int"));
				} else {
					try {

						field = c.getDeclaredField(n.getField());

					} catch (NoSuchFieldException fe) {

						try {
							field = c.getField(n.getField());

						} catch (NoSuchFieldException fe2) {
							// it is an inner class parsed as a field
							// declaration
							c = typeTable.loadClass(c.getName() + "$"
									+ n.getField());
							scopeType.setName(c.getName());
							arg.put(TYPE_KEY, scopeType);
							return;
						}

					}

					Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();

					TypeVariable<?>[] typeParams = c.getTypeParameters();

					if (typeParams != null) {

						for (int i = 0; i < typeParams.length; i++) {
							if (scopeType != null
									&& scopeType.getParameterizedTypes() != null) {
								typeMapping.put(typeParams[i].getName(),
										scopeType.getParameterizedTypes()
												.get(i));
							} else {
								typeMapping.put(typeParams[i].getName(),
										new SymbolType("java.lang.Object"));
							}
						}

					}
					SymbolType thisType = symbolTable.getType("this",
							ReferenceType.VARIABLE);
					if (thisType != null && scopeType.isCompatible(thisType)) {
						symbolTable.lookUpSymbolForRead(n.getField(),
								ReferenceType.VARIABLE);
					}

					arg.put(TYPE_KEY,
							valueOf(field.getGenericType(), typeMapping));
				}
			}

		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);

		} catch (Exception e) {
			throw new NoSuchExpressionTypeException(e);

		}
	}

	private SymbolType valueOf(Type type, Map<String, SymbolType> typeMapping)
			throws InvalidTypeException {

		SymbolType returnType = null;

		if (type instanceof Class<?>) {
			Class<?> aux = ((Class<?>) type);
			returnType = new SymbolType(aux.getName());
			if (aux.isArray()) {
				returnType.setArrayCount(1);
				returnType.setName(aux.getComponentType().getName());
			}

		} else if (type instanceof TypeVariable) {

			String variableName = ((TypeVariable<?>) type).getName();
			SymbolType aux = typeMapping.get(variableName);

			if (aux == null) {
				aux = new SymbolType(Object.class.getName());
				return aux;
			} else {
				return aux;
			}

		} else if (type instanceof ParameterizedType) {
			Class<?> auxClass = (Class<?>) ((ParameterizedType) type)
					.getRawType();

			Type[] types = ((ParameterizedType) type).getActualTypeArguments();

			returnType = new SymbolType(auxClass.getName());

			if (types != null) {
				List<SymbolType> params = new LinkedList<SymbolType>();
				returnType.setParameterizedTypes(params);
				for (Type t : types) {
					SymbolType param = typeMapping.get(t.toString());
					if (param != null) {
						params.add(param);
					} else {
						try {
							SymbolType st = valueOf(t, typeMapping);
							if (st != null) {
								params.add(st);
							}
						} catch (InvalidTypeException e) {
							LOG.warn("Unmappeable type " + t.toString());
						}
					}
				}
				if (params.isEmpty()) {
					returnType.setParameterizedTypes(null);
				}
			}

		} else if (type instanceof GenericArrayType) {
			// method.getReturnType();(
			returnType = new SymbolType(valueOf(
					((GenericArrayType) type).getGenericComponentType(),
					typeMapping).getName());

			returnType.setArrayCount(1);

		} else {
			throw new InvalidTypeException(type);
		}
		return returnType;

	}

	@Override
	public void visit(InstanceOfExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("boolean"));
	}

	@Override
	public void visit(IntegerLiteralExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("int"));
	}

	@Override
	public void visit(IntegerLiteralMinValueExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("int"));
	}

	@Override
	public void visit(LongLiteralExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("long"));
	}

	@Override
	public void visit(LongLiteralMinValueExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("long"));
	}

	@Override
	public void visit(MethodCallExpr n, A arg) {
		try {
			SymbolType scope;

			if (n.getScope() != null) {

				n.getScope().accept(this, arg);

				scope = (SymbolType) arg.remove(TYPE_KEY);

				LOG.debug("scope: (" + n.getScope().toString() + ")"
						+ scope.getName() + " method " + n.toString());

			} else {
				scope = symbolTable.getType("this", ReferenceType.VARIABLE);
				LOG.debug("scope (this): " + scope.getName() + " method "
						+ n.toString());
			}

			Class<?>[] typeArgs = null;
			SymbolType[] symbolTypes = null;
			if (n.getArgs() != null) {
				typeArgs = new Class[n.getArgs().size()];
				symbolTypes = new SymbolType[n.getArgs().size()];
				int i = 0;
				for (Expression e : n.getArgs()) {
					e.accept(this, arg);
					SymbolType argType = (SymbolType) arg.remove(TYPE_KEY);
					symbolTypes[i] = argType;
					typeArgs[i] = typeTable.loadClass(argType);
					i++;
				}
			}

			SymbolType thisType = symbolTable.getType("this",
					ReferenceType.VARIABLE);
			if (thisType != null && scope.isCompatible(thisType)) {
				symbolTable.lookUpSymbolForRead(n.getName(),
						ReferenceType.METHOD, scope, symbolTypes);
			}
			Method method = null;
			Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
			// it should be initialized after resolving the method

			method = getMethod(scope, n.getName(), typeArgs, n.getArgs(), arg,
					typeMapping);
			Type[] types = method.getGenericParameterTypes();
			int pos = 0;
			boolean hasGenerics = false;

			for (Type type : types) {
				if (type instanceof ParameterizedType) {
					if (!hasGenerics) {
						LOG.debug(n + " is a call with generics ");
						hasGenerics = true;
					}
					Type aux = ((ParameterizedType) type).getRawType();
					if (aux instanceof Class) {
						if (((Class<?>) aux).getName()
								.equals("java.lang.Class")) {
							Type[] targs = ((ParameterizedType) type)
									.getActualTypeArguments();
							for (Type targ : targs) {
								String letter = targ.toString();
								if (!"?".equals(letter)
										&& !typeMapping.containsKey(letter)) {
									Expression e = (Expression) n.getArgs()
											.get(pos);
									String className = "";
									if (e instanceof ClassExpr) {
										className = ((ClassExpr) e).getType()
												.toString();
										Class<?> tclazz = typeTable
												.loadClass(className);
										typeMapping.put(letter, new SymbolType(
												tclazz.getName()));
									}
								}
							}
						}
					}
				} else if (type instanceof TypeVariable) {
					SymbolType st = typeMapping.get(type.getTypeName());
					if (st == null) {
						typeMapping.put(type.getTypeName(), new SymbolType(
								typeArgs[pos]));
					} else {
						Class<?> common = ClassInspector
								.getTheNearestSuperClass(st.getClazz(),
										typeArgs[pos]);
						typeMapping.put(type.getTypeName(), new SymbolType(
								common));
					}
				}
				pos++;
			}

			SymbolType st = getMethodType(method, typeMapping);

			// Generics exception it.next() -> returns Object instead of the it
			// parametrized type
			if (st.getName().equals("java.lang.Object")
					&& scope.getParameterizedTypes() != null) {
				if (!scope.getParameterizedTypes().isEmpty()) {
					st.setName(scope.getParameterizedTypes().get(0).getName());
				}
			}

			arg.put(TYPE_KEY, st);

		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);

		} catch (Exception e) {
			throw new NoSuchExpressionTypeException(e);
		}

	}

	public Map<String, SymbolType> getSymbolsOfGenericParameterTypes(
			Method method, List<Expression> argumentValues) {
		Map<String, SymbolType> symbols = new HashMap<String, SymbolType>();

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
										paramClass = typeTable
												.loadClass(((ClassExpr) argumentValues
														.get(j)).getType());
									} catch (ClassNotFoundException e) {
										throw new NoSuchExpressionTypeException(
												"Invalid class into the generics resolution",
												e);
									}

									SymbolType auxType = new SymbolType();
									auxType.setName(paramClass.getName());

									symbols.put(variableName, auxType);
								}
							}
						}
					}
				}
			}
		}
		return symbols;
	}

	public Method findMethod(Method[] methods, String methodName,
			Class<?>[] typeArgs, List<Expression> argumentValues,
			Map<String, SymbolType> typeMapping, MethodCallExpr requiredMethod,
			FieldAccessExpr requiredField, A arg) throws NoSuchMethodException,
			ClassNotFoundException, InvalidTypeException {

		Method result = null;
		if (methods == null) {
			throw new NoSuchMethodException("There are not methods to select");
		}

		for (int i = 0; i < methods.length && result == null; i++) {

			Method method = methods[i];
			if (!method.isBridge() && !method.isSynthetic()) {
				if (method.getName().equals(methodName)) {
					LOG.debug("Method " + method.getDeclaringClass().getName()
							+ "#" + methodName + ":"
							+ method.getReturnType().getName() + " found");
					Map<String, SymbolType> symbols = getSymbolsOfGenericParameterTypes(
							method, argumentValues);

					symbols.putAll(typeMapping);
					SymbolType returnType = getMethodType(method, symbols);

					if (isCompatible(method, methodName, typeArgs, returnType,
							requiredMethod, requiredField, arg)) {
						arg.put(TYPE_KEY, returnType);
						typeMapping.putAll(typeMapping);
						result = method;
						LOG.debug("compatible?  [OK] - result: "
								+ returnType.getName());
					} else {
						LOG.debug("compatible?  [NO]");
					}
				}
			}
		}

		return result;
	}

	public Method getMethod(Class<?> clazz, String methodName,
			Class<?>[] typeArgs, List<Expression> argumentValues, A arg,
			Map<String, SymbolType> typeMapping, boolean throwException)
			throws SecurityException, NoSuchMethodException,
			ClassNotFoundException, InvalidTypeException {

		LOG.debug("Looking for " + clazz.getName() + "#" + methodName);
		Method result = findMethod(clazz.getDeclaredMethods(), methodName,
				typeArgs, argumentValues, typeMapping, null, null, arg);

		if (result == null) {

			if (clazz.isMemberClass()) {

				result = getMethod(clazz.getDeclaringClass(), methodName,
						typeArgs, argumentValues, arg, typeMapping, false);

			} else if (clazz.isAnonymousClass()) {

				result = getMethod(clazz.getEnclosingClass(), methodName,
						typeArgs, argumentValues, arg, typeMapping, false);
			}
			if (result == null) {
				Class<?> superClass = clazz.getSuperclass();
				if (superClass != null) {
					result = getMethod(superClass, methodName, typeArgs,
							argumentValues, arg, typeMapping, false);
				}

				if (result == null) {
					Type[] types = clazz.getGenericInterfaces();
					if (types.length > 0) {

						for (int i = 0; i < types.length && result == null; i++) {

							Class<?> type = typeTable.loadClass(valueOf(
									types[i], typeMapping));

							result = getMethod(type, methodName, typeArgs,
									argumentValues, arg, typeMapping, false);
						}

					}
					if (result == null && clazz.isInterface()) {
						result = getMethod(Object.class, methodName, typeArgs,
								argumentValues, arg, typeMapping, false);
					}
				}
			}

		}
		if (result == null && throwException) {
			throw new NoSuchMethodException("The method " + clazz.getName()
					+ "#" + methodName + " cannot be found");
		}
		return result;
	}

	// TODO: Test
	public Method getMethod(SymbolType scope, // scope
												// to
												// find
			// the method
			String methodName, // method name to look for.
								// Multiple methods with the same name can exist
								// in a taxonomy.
								// Methods are not included into the
								// symbolTable. These are found by Java
								// introspection.
			Class<?>[] typeArgs, // java types of the argument expressions
			List<Expression> argumentValues, A arg, // context
			Map<String, SymbolType> typeMapping // mapping for Java Generics
												// applied
	// into the scope
	) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
			InvalidTypeException {
		List<Class<?>> candidates = new LinkedList<Class<?>>();
		if (scope.hasBounds()) {
			for (SymbolType bound : scope.getBounds()) {
				candidates.add(bound.getClazz());
			}
		} else {
			Class<?> clazz = scope.getClazz();
			if (clazz == null) {
				clazz = typeTable.loadClass(scope);
			}
			candidates.add(clazz);
		}
		Method result = null;
		Iterator<Class<?>> it = candidates.iterator();
		HashMap<String, SymbolType> mapping = null;
		while (it.hasNext() && result == null) {
			Class<?> clazz = it.next();
			TypeVariable<?>[] typeParams = clazz.getTypeParameters();
			mapping = new HashMap<String, SymbolType>();
			if (typeParams != null) {

				for (int i = 0; i < typeParams.length; i++) {
					if (scope != null && scope.getParameterizedTypes() != null) {
						mapping.put(typeParams[i].getName(), scope
								.getParameterizedTypes().get(i));
					} else {
						mapping.put(typeParams[i].getName(), new SymbolType(
								"java.lang.Object"));
					}
				}
			}

			try {

				result = getMethod(clazz, methodName, typeArgs, argumentValues,
						arg, mapping, true);
			} catch (NoSuchMethodException e1) {
				if (!it.hasNext()) {
					throw e1;
				}
			}
		}
		if (result != null) {
			typeMapping.putAll(mapping);
		}
		return result;

	}

	private SymbolType getMethodType(Method method,
			Map<String, SymbolType> typeMapping) throws ClassNotFoundException,
			InvalidTypeException {

		TypeVariable<Method>[] tvs = method.getTypeParameters();
		java.lang.reflect.Type type = method.getGenericReturnType();
		if (tvs.length > 0) {
			for (TypeVariable<Method> tv : tvs) {
				Type[] bounds = tv.getBounds();
				List<SymbolType> boundsList = new LinkedList<SymbolType>();
				for (int i = 0; i < bounds.length; i++) {
					boundsList.add(new SymbolType((Class<?>) bounds[i]));
				}
				SymbolType st = typeMapping.get(tv.getName());
				if (st == null) {
					if (boundsList.size() == 1) {
						typeMapping.put(tv.getName(), boundsList.get(0));

					} else {
						typeMapping.put(tv.getName(),
								new SymbolType(boundsList));
					}
				}
			}
		}

		return valueOf(type, typeMapping);
	}

	private boolean isCompatible(Method method, String name,
			Class<?>[] typeArgs, SymbolType returnType,
			MethodCallExpr requiredMethod, FieldAccessExpr requiredField, A arg)
			throws ClassNotFoundException {

		Class<?> lastVariableTypeArg = null;

		if (method.getName().equals(name)) {

			int numParams = typeArgs == null ? 0 : typeArgs.length;

			if ((method.getParameterTypes().length == numParams)
					|| method.isVarArgs()) {
				LOG.debug("The method [" + name
						+ "] is found with the same number of params: "
						+ numParams);
				if (method.isVarArgs()) {

					if (method.getParameterTypes().length < numParams) {

						lastVariableTypeArg = method.getParameterTypes()[method
								.getParameterTypes().length - 1];

						numParams = method.getParameterTypes().length;
					}
					if (method.getParameterTypes().length <= numParams) {
						// changing the last argument to an array
						Class<?>[] newTypeArgs = new Class<?>[method
								.getParameterTypes().length];

						for (int i = 0; i < newTypeArgs.length - 1; i++) {
							newTypeArgs[i] = typeArgs[i];
						}

						newTypeArgs[newTypeArgs.length - 1] = method
								.getParameterTypes()[method.getParameterTypes().length - 1];

						typeArgs = newTypeArgs;
					}
				}

				boolean isCompatible = true;
				Class<?>[] methodParameterTypes = method.getParameterTypes();
				int i = 0;
				for (i = 0; i < numParams && isCompatible; i++) {

					isCompatible = Types.isCompatible(typeArgs[i],
							methodParameterTypes[i]);
				}
				if (!isCompatible && numParams > 0) {
					LOG.debug("The parameter of " + (i - 1) + " is an "
							+ methodParameterTypes[i - 1].getName()
							+ ", but expected " + typeArgs[i - 1].getName());
				}

				if (isCompatible && lastVariableTypeArg != null) {
					int j = numParams;
					for (; j < typeArgs.length && isCompatible; j++) {
						isCompatible = Types.isCompatible(typeArgs[j],
								lastVariableTypeArg);

					}
					if (!isCompatible && numParams > 0) {
						LOG.debug("The parameter of " + (j - 1) + " is an "
								+ lastVariableTypeArg.getName()
								+ ", but expected " + typeArgs[j - 1].getName());
					}

				}

				if (isCompatible) {

					List<Class<?>> compatibleClasses = new LinkedList<Class<?>>();
					if (returnType.hasBounds()) {
						List<SymbolType> bounds = returnType.getBounds();
						for (SymbolType bound : bounds) {
							compatibleClasses.add(bound.getClass());
						}

					} else {
						Class<?> clazz = returnType.getClazz();
						if (clazz == null) {
							clazz = typeTable.loadClass(returnType);
						}
						compatibleClasses.add(clazz);
					}
					if (requiredMethod != null) {

						List<Method> methods = new LinkedList<Method>();

						Iterator<Class<?>> itC = compatibleClasses.iterator();

						boolean returnTypeCompatible = false;
						while (itC.hasNext() && !returnTypeCompatible) {
							Class<?> candidate = itC.next();
							methods.addAll(Arrays.asList(candidate
									.getDeclaredMethods()));

							methods.addAll(Arrays.asList(candidate.getMethods()));
							Iterator<Method> it = methods.iterator();
							while (it.hasNext() && !returnTypeCompatible) {

								Method currentMethod = it.next();

								// checking method name
								if (currentMethod.getName().equals(
										requiredMethod.getName())) {
									List<Expression> args = requiredMethod
											.getArgs();
									Class<?>[] parameterTypes = currentMethod
											.getParameterTypes();
									if (args != null) {
										boolean compatibleArgs = true;
										int k = 0;
										for (Expression argExpr : args) {
											argExpr.accept(this, arg);
											SymbolType typeArg = (SymbolType) arg
													.remove(TYPE_KEY);
											if (!Types.isCompatible(typeTable
													.loadClass(typeArg),
													parameterTypes[k])) {
												compatibleArgs = false;
											}
											k++;
										}
										returnTypeCompatible = compatibleArgs;
									} else {
										returnTypeCompatible = true;
									}

								}
							}
						}
						isCompatible = returnTypeCompatible;
					} else if (requiredField != null) {

						Iterator<Class<?>> itC = compatibleClasses.iterator();
						boolean fieldCompatible = false;

						while (itC.hasNext() && !fieldCompatible) {
							Class<?> candidate = itC.next();
							if (candidate.isArray()
									&& requiredField.getField()
											.equals("length")) {
								return true;
							}
							try {
								// the return type has the required field as
								// public?
								candidate.getField(requiredField.getField());
								fieldCompatible = true;
							} catch (NoSuchFieldException e) {
								// searching in all fields
								Field[] fields = candidate.getDeclaredFields();
								String fieldName = requiredField.getField();
								fieldCompatible = false;
								i = 0;
								for (i = 0; i < fields.length
										&& !fieldCompatible; i++) {
									fieldCompatible = (fields[i].getName()
											.equals(fieldName));
								}
							}
						}
						isCompatible = fieldCompatible;
						// the field has been found. Then, the method is
						// compatible
						return true;
					}
				}

				if (isCompatible) {

					return true;
				}

			}
		}
		return false;
	}

	@Override
	public void visit(NameExpr n, A arg) {

		SymbolType type = symbolTable.getType(n.getName(), null);

		if (type == null) {
			try {
				Class<?> clazz = typeTable.loadClass(n.getName());
				if (clazz != null) {
					String className = clazz.getName();
					type = new SymbolType();
					type.setName(className);
				}
			} catch (ClassNotFoundException e) {
				throw new NoSuchExpressionTypeException(e);
			}
		}
		else{
			symbolTable.lookUpSymbolForRead(n.getName(), null);
		}
		arg.put(TYPE_KEY, type);

	}

	@Override
	public void visit(NullLiteralExpr n, A arg) {
		arg.put(TYPE_KEY, null);
	}

	@Override
	public void visit(ObjectCreationExpr n, A arg) {

		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
		arg.put(TYPE_KEY, typeTable.valueOf(n.getType()));

	}

	@Override
	public void visit(QualifiedNameExpr n, A arg) {
		SymbolType type = new SymbolType(n.getName());
		NameExpr aux = n.getQualifier();
		while (aux != null) {
			type.setName(type.getName() + "." + aux.getName());
			if (aux instanceof QualifiedNameExpr) {
				aux = ((QualifiedNameExpr) aux).getQualifier();
			} else {
				aux = null;
			}
		}
		arg.put(TYPE_KEY, type);
	}

	@Override
	public void visit(StringLiteralExpr n, A arg) {
		arg.put(TYPE_KEY, new SymbolType("java.lang.String"));
	}

	@Override
	public void visit(SuperExpr n, A arg) {
		arg.put(TYPE_KEY,
				symbolTable
						.lookUpSymbolForRead("super", ReferenceType.VARIABLE)
						.getType());
	}

	@Override
	public void visit(ThisExpr n, A arg) {
		arg.put(TYPE_KEY,
				symbolTable.lookUpSymbolForRead("this", ReferenceType.VARIABLE)
						.getType());
	}

	@Override
	public void visit(MethodReferenceExpr n, A arg) {

		SymbolType scopeType = null;

		if (n.getScope() == null) {
			scopeType = symbolTable.getType("this", ReferenceType.VARIABLE);
		} else {
			n.getScope().accept(this, arg);
			scopeType = (SymbolType) arg
					.remove(ExpressionTypeAnalyzer.TYPE_KEY);
			try {
				Class<?> clazz = typeTable.loadClass(scopeType);
				scopeType.setClazz(clazz);
			} catch (ClassNotFoundException e) {
				new NoSuchExpressionTypeException("Error resolving "
						+ scopeType + " as scope type of " + n.toString(), e);
			}
		}
		try {
			Method m = scopeType.getClazz().getMethod(n.getIdentifier());
			SymbolType st = new SymbolType(m.getReturnType());
			arg.put(TYPE_KEY, st);

		} catch (Exception e) {
			throw new NoSuchExpressionTypeException("Error resolving "
					+ n.toString(), e);
		}
		List<TypeParameter> args = n.getTypeParameters();

		if (args != null) {
			Iterator<TypeParameter> it = args.iterator();
			while (it.hasNext()) {
				it.next().accept(this, arg);
			}
		}

	}

	public void visit(NormalAnnotationExpr n, A arg) {
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	public void visit(MarkerAnnotationExpr n, A arg) {
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	public void visit(SingleMemberAnnotationExpr n, A arg) {
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	@Override
	public void visit(TypeParameter n, A arg) {
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
		super.visit(n, arg);
	}

	public void visit(LambdaExpr n, A arg) {
		// TODO

		super.visit(n, arg);
	}

}