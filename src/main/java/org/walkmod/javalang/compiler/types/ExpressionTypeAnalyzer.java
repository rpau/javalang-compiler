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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
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
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.CompositeBuilder;
import org.walkmod.javalang.compiler.reflection.CompatibleArgsPredicate;
import org.walkmod.javalang.compiler.reflection.GenericsBuilderFromParameterTypes;
import org.walkmod.javalang.compiler.reflection.InvokableMethodsPredicate;
import org.walkmod.javalang.compiler.reflection.MethodInspector;
import org.walkmod.javalang.compiler.reflection.MethodsByNamePredicate;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class ExpressionTypeAnalyzer<A extends Map<String, Object>> extends
		VoidVisitorAdapter<A> {

	public static final String TYPE_KEY = "type_key";

	public static final String IMPLICIT_PARAM_TYPE = "implicit_param_type";

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

					arg.put(TYPE_KEY, SymbolType.valueOf(
							field.getGenericType(), typeMapping));
				}
			}

		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);

		} catch (Exception e) {
			throw new NoSuchExpressionTypeException(e);

		}
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
			Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
			// it should be initialized after resolving the method

			ArrayFilter<Method> filter = new ArrayFilter<Method>(null);

			filter.appendPredicate(new MethodsByNamePredicate(n.getName()))
					.appendPredicate(new InvokableMethodsPredicate())
					.appendPredicate(new CompatibleArgsPredicate(typeArgs));

			CompositeBuilder<Method> builder = new CompositeBuilder<Method>();
			builder.appendBuilder(new GenericsBuilderFromParameterTypes(
					typeMapping, n.getArgs(), typeArgs));

			SymbolType st = MethodInspector.findMethodType(scope, filter,
					builder, typeMapping);

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
		} else {
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
		if (semanticVisitor != null) {
			symbolTable.pushScope();
			List<Parameter> params = n.getParameters();
			if (params != null) {
				for (Parameter p : params) {
					p.accept(semanticVisitor, arg);
				}
			}
			n.getBody().accept(semanticVisitor, arg);
			symbolTable.popScope();
		}
	}
}