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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.ArrayAccessExpr;
import org.walkmod.javalang.ast.expr.ArrayCreationExpr;
import org.walkmod.javalang.ast.expr.ArrayInitializerExpr;
import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr.Operator;
import org.walkmod.javalang.ast.expr.BooleanLiteralExpr;
import org.walkmod.javalang.ast.expr.CastExpr;
import org.walkmod.javalang.ast.expr.CharLiteralExpr;
import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.ConditionalExpr;
import org.walkmod.javalang.ast.expr.DoubleLiteralExpr;
import org.walkmod.javalang.ast.expr.EnclosedExpr;
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
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.QualifiedNameExpr;
import org.walkmod.javalang.ast.expr.SingleMemberAnnotationExpr;
import org.walkmod.javalang.ast.expr.StringLiteralExpr;
import org.walkmod.javalang.ast.expr.SuperExpr;
import org.walkmod.javalang.ast.expr.ThisExpr;
import org.walkmod.javalang.ast.expr.TypeExpr;
import org.walkmod.javalang.ast.expr.UnaryExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.PrimitiveType.Primitive;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.VoidType;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.CompositeBuilder;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.reflection.CompatibleArgsPredicate;
import org.walkmod.javalang.compiler.reflection.CompatibleFunctionalPredicate;
import org.walkmod.javalang.compiler.reflection.FieldInspector;
import org.walkmod.javalang.compiler.reflection.GenericsBuilderFromParameterTypes;
import org.walkmod.javalang.compiler.reflection.InvokableMethodsPredicate;
import org.walkmod.javalang.compiler.reflection.MethodInspector;
import org.walkmod.javalang.compiler.reflection.MethodsByNamePredicate;
import org.walkmod.javalang.compiler.reflection.SymbolDataOfMethodReferenceBuilder;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class ExpressionTypeAnalyzer<A extends Map<String, Object>> extends
		VoidVisitorAdapter<A> {

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
		SymbolType arrayType = (SymbolType) n.getName().getSymbolData();
		SymbolType newType = new SymbolType();
		newType.setName(arrayType.getName());
		newType.setParameterizedTypes(arrayType.getParameterizedTypes());
		newType.setArrayCount(arrayType.getArrayCount() - 1);
		n.setSymbolData(newType);
	}

	@Override
	public void visit(ArrayCreationExpr n, A arg) {
		SymbolType arrayType = typeTable.valueOf(n.getType());
		arrayType.setArrayCount(1);
		n.setSymbolData(arrayType);
	}

	@Override
	public void visit(ArrayInitializerExpr n, A arg) {

		if (n.getValues() != null) {
			int arrayCount = 1;
			List<Class<?>> classes = new LinkedList<Class<?>>();
			Integer minArrayCount = null;
			List<Expression> values = n.getValues();
			for (Expression expr : values) {
				expr.accept(this, arg);
				SymbolData sd = expr.getSymbolData();

				if (sd != null) {
					if (minArrayCount == null
							|| sd.getArrayCount() < minArrayCount) {
						minArrayCount = sd.getArrayCount();
					}
					classes.add(sd.getClazz());
				}
			}
			if (values != null && !values.isEmpty() && minArrayCount != null) {
				arrayCount = minArrayCount + 1;
			}
			Class<?> superClass = ClassInspector
					.getTheNearestSuperClass(classes);
			SymbolType st = new SymbolType(superClass);
			st.setArrayCount(arrayCount);
			n.setSymbolData(st);
		}
	}

	@Override
	public void visit(AssignExpr n, A arg) {
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	@Override
	public void visit(BinaryExpr n, A arg) {

		n.getLeft().accept(this, arg);
		SymbolType leftType = (SymbolType) n.getLeft().getSymbolData();

		n.getRight().accept(this, arg);
		SymbolType rightType = (SymbolType) n.getRight().getSymbolData();

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

		n.setSymbolData(resultType);

	}

	public void visit(UnaryExpr n, A arg) {
		super.visit(n, arg);
		SymbolData sd = n.getExpr().getSymbolData();
		n.setSymbolData(sd);
	}

	@Override
	public void visit(BooleanLiteralExpr n, A arg) {
		n.setSymbolData(new SymbolType("boolean"));
	}

	@Override
	public void visit(CastExpr n, A arg) {
		super.visit(n, arg);
		n.setSymbolData(n.getType().getSymbolData());
	}

	@Override
	public void visit(CharLiteralExpr n, A arg) {
		n.setSymbolData(new SymbolType("char"));
	}

	@Override
	public void visit(ClassExpr n, A arg) {
		n.setSymbolData(new SymbolType("java.lang.Class"));
	}

	@Override
	public void visit(ConditionalExpr n, A arg) {

		super.visit(n, arg);
		SymbolData thenData = n.getThenExpr().getSymbolData();
		SymbolData elseData = n.getElseExpr().getSymbolData();

		if (elseData == null || thenData == null) {
			if (elseData == null) {
				n.setSymbolData(thenData);
			} else {
				n.setSymbolData(elseData);
			}
		} else {
			Class<?> thenClass = thenData.getClazz();
			Class<?> elseClass = elseData.getClazz();
			Class<?> superClass = ClassInspector.getTheNearestSuperClass(
					thenClass, elseClass);
			n.setSymbolData(new SymbolType(superClass));
		}

	}

	@Override
	public void visit(EnclosedExpr n, A arg) {
		super.visit(n, arg);
		n.setSymbolData(n.getInner().getSymbolData());
	}

	@Override
	public void visit(DoubleLiteralExpr n, A arg) {
		n.setSymbolData(new SymbolType("double"));
	}

	@Override
	public void visit(FieldAccessExpr n, A arg) {

		n.getScope().accept(this, arg);

		SymbolType scopeType = (SymbolType) n.getScope().getSymbolData();

		Class<?> c = null;

		try {
			if (scopeType == null) {
				try {
					c = typeTable.loadClass(n.toString());
					if (c != null) {
						scopeType = new SymbolType(c);
						symbolTable.lookUpSymbolForRead(
								typeTable.getSimpleName(c.getName()),
								ReferenceType.TYPE);
						n.setSymbolData(scopeType);
					}
				} catch (ClassNotFoundException e) {

				}
			} else {

				SymbolType fieldType = FieldInspector.findFieldType(scopeType,
						n.getField());
				n.setSymbolData(fieldType);

			}

		} catch (Exception e) {
			throw new NoSuchExpressionTypeException(e);

		}
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	@Override
	public void visit(InstanceOfExpr n, A arg) {
		super.visit(n, arg);
		n.setSymbolData(new SymbolType("boolean"));
	}

	@Override
	public void visit(IntegerLiteralExpr n, A arg) {
		n.setSymbolData(new SymbolType("int"));
	}

	@Override
	public void visit(IntegerLiteralMinValueExpr n, A arg) {
		n.setSymbolData(new SymbolType("int"));
	}

	@Override
	public void visit(LongLiteralExpr n, A arg) {
		n.setSymbolData(new SymbolType("long"));
	}

	@Override
	public void visit(LongLiteralMinValueExpr n, A arg) {
		n.setSymbolData(new SymbolType("long"));
	}

	@Override
	public void visit(MethodCallExpr n, A arg) {
		try {
			SymbolType scope;

			if (n.getScope() != null) {

				n.getScope().accept(this, arg);

				scope = (SymbolType) n.getScope().getSymbolData();

				LOG.debug("scope: (" + n.getScope().toString() + ")"
						+ scope.getName() + " method " + n.toString());

			} else {
				scope = symbolTable.getType("this", ReferenceType.VARIABLE);
				LOG.debug("scope (this): " + scope.getName() + " method "
						+ n.toString());
			}

			SymbolType[] symbolTypes = null;
			boolean hasFunctionalExpressions = false;
			if (n.getArgs() != null) {

				symbolTypes = new SymbolType[n.getArgs().size()];
				int i = 0;

				for (Expression e : n.getArgs()) {
					if (!(e instanceof LambdaExpr)
							&& !(e instanceof MethodReferenceExpr)) {
						e.accept(this, arg);
						SymbolType argType = (SymbolType) e.getSymbolData();
						symbolTypes[i] = argType;

					} else {
						hasFunctionalExpressions = true;
					}
					i++;
				}
			}

			// for static imports
			Symbol s = symbolTable.findSymbol(n.getName(),
					ReferenceType.METHOD, scope, symbolTypes);

			if (s != null) {
				n.setSymbolData(s.getType());
			} else {
				Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
				// it should be initialized after resolving the method

				ArrayFilter<Method> filter = new ArrayFilter<Method>(null);

				filter.appendPredicate(new MethodsByNamePredicate(n.getName()))
						.appendPredicate(new InvokableMethodsPredicate())
						.appendPredicate(
								new CompatibleArgsPredicate(symbolTypes));
				if (hasFunctionalExpressions) {
					filter.appendPredicate(new CompatibleFunctionalPredicate<A>(
							scope, this, n.getArgs(), arg));
				}
				CompositeBuilder<Method> builder = new CompositeBuilder<Method>();
				builder.appendBuilder(new GenericsBuilderFromParameterTypes(
						typeMapping, n.getArgs(), symbolTypes));

				SymbolType st = MethodInspector.findMethodType(scope, filter,
						builder, typeMapping);
				n.setSymbolData(st);

				SymbolDataOfMethodReferenceBuilder<A> typeBuilder = new SymbolDataOfMethodReferenceBuilder<A>(
						typeMapping, this, arg);
				typeBuilder.build(n);

			}
			if (semanticVisitor != null) {
				n.accept(semanticVisitor, arg);
			}

		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);

		} catch (Exception e) {
			throw new NoSuchExpressionTypeException(e);
		}

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
				// a name expression could be "org.walkmod.A" and this node
				// could be "org.walkmod"
			}
		}
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
		n.setSymbolData(type);

	}

	@Override
	public void visit(ObjectCreationExpr n, A arg) {
		boolean isAnnonymousClass = n.getAnonymousClassBody() != null
				&& !n.getAnonymousClassBody().isEmpty();
		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
		}
		if (n.getTypeArgs() != null) {
			for (Type t : n.getTypeArgs()) {
				t.accept(this, arg);
			}
		}
		n.getType().accept(this, arg);
		if (n.getArgs() != null) {
			for (Expression e : n.getArgs()) {
				e.accept(this, arg);
			}
		}
		n.setSymbolData(n.getType().getSymbolData());
		if (isAnnonymousClass) {
			// we need to update the symbol table
			if (semanticVisitor != null) {
				n.accept(semanticVisitor, arg);
			}
		}

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
		n.setSymbolData(type);
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	@Override
	public void visit(StringLiteralExpr n, A arg) {
		n.setSymbolData(new SymbolType("java.lang.String"));
	}

	@Override
	public void visit(SuperExpr n, A arg) {
		n.setSymbolData(symbolTable.getType("super", ReferenceType.VARIABLE));
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	@Override
	public void visit(ThisExpr n, A arg) {
		n.setSymbolData(symbolTable.getType("this", ReferenceType.VARIABLE));
		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
	}

	@Override
	public void visit(TypeExpr n, A arg) {
		super.visit(n, arg);
		n.setSymbolData(n.getType().getSymbolData());
	}

	@Override
	public void visit(MethodReferenceExpr n, A arg) {

		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
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

	@Override
	public void visit(LambdaExpr n, A arg) {
		if (semanticVisitor != null) {
			symbolTable.pushScope();
			List<Parameter> params = n.getParameters();
			if (params != null) {
				for (Parameter p : params) {
					p.accept(semanticVisitor, arg);
				}
			}
			Statement stmt = n.getBody();
			stmt.accept(semanticVisitor, arg);
			if (stmt instanceof ExpressionStmt) {
				stmt.setSymbolData(((ExpressionStmt) stmt).getExpression()
						.getSymbolData());
			}
			symbolTable.popScope();
		}
	}

	@Override
	public void visit(ClassOrInterfaceType n, A arg) {
		super.visit(n, arg);
		String typeName = n.getName();
		ClassOrInterfaceType scope = n.getScope();
		Class<?> clazz = null;
		SymbolType type = null;
		if (scope != null) {
			SymbolData data = scope.getSymbolData();
			if (data == null) {
				typeName = scope.toString() + "." + typeName;
				try {
					clazz = typeTable.loadClass(typeName);
				} catch (ClassNotFoundException e) {
					// we are the parents of another class or interface type
				}
			} else {
				typeName = data.getName() + "$" + typeName;
				try {
					clazz = typeTable.loadClass(typeName);
				} catch (ClassNotFoundException e) {
					throw new NoSuchExpressionTypeException("Ops! The class "
							+ n.toString() + " can't be resolved", null);
				}
			}
			if (clazz != null) {
				type = new SymbolType(clazz);
			}
		} else {
			Symbol s = symbolTable.lookUpSymbolForRead(typeName,
					ReferenceType.TYPE);
			if (s != null) {
				type = s.getType().clone();
			} else {
				String fullName = typeTable.getFullName(n);
				if (fullName != null) {
					type = new SymbolType(fullName);
				}
			}
		}
		if (type != null) {
			List<Type> args = n.getTypeArgs();

			if (args != null) {
				List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();
				for (Type currentArg : args) {
					parameterizedTypes.add((SymbolType) currentArg
							.getSymbolData());
				}
				type.setParameterizedTypes(parameterizedTypes);
			}
			n.setSymbolData(type);
		}
	}

	@Override
	public void visit(PrimitiveType n, A arg) {
		super.visit(n, arg);
		Primitive type = n.getType();
		if (type.equals(Primitive.Boolean)) {
			n.setSymbolData(new SymbolType("boolean"));
		} else if (type.equals(Primitive.Byte)) {
			n.setSymbolData(new SymbolType("byte"));
		} else if (type.equals(Primitive.Char)) {
			n.setSymbolData(new SymbolType("char"));
		} else if (type.equals(Primitive.Double)) {
			n.setSymbolData(new SymbolType("double"));
		} else if (type.equals(Primitive.Float)) {
			n.setSymbolData(new SymbolType("float"));
		} else if (type.equals(Primitive.Int)) {
			n.setSymbolData(new SymbolType("int"));
		} else if (type.equals(Primitive.Long)) {
			n.setSymbolData(new SymbolType("long"));
		} else if (type.equals(Primitive.Short)) {
			n.setSymbolData(new SymbolType("short"));
		}
	}

	@Override
	public void visit(org.walkmod.javalang.ast.type.ReferenceType n, A arg) {
		super.visit(n, arg);
		SymbolType newType = null;
		SymbolType st = (SymbolType) n.getType().getSymbolData();
		if (st != null) {
			newType = st.clone();
			newType.setArrayCount(n.getArrayCount());
		}
		n.setSymbolData(newType);
	}

	@Override
	public void visit(VoidType n, A arg) {
		super.visit(n, arg);
		n.setSymbolData(new SymbolType(void.class));
	}

	@Override
	public void visit(WildcardType n, A arg) {
		super.visit(n, arg);
		Type superType = n.getSuper();
		if (superType != null) {
			n.setSymbolData(superType.getSymbolData());
		} else {
			Type extendsType = n.getExtends();
			if (extendsType != null) {
				n.setSymbolData(extendsType.getSymbolData());
			}
		}
	}

	public void visit(VariableDeclarationExpr n, A arg) {

		if (semanticVisitor != null) {
			n.accept(semanticVisitor, arg);
		}
		super.visit(n, arg);
	}

	@Override
	public void visit(VariableDeclarator n, A arg) {
		n.getId().accept(this, arg);
		Expression init = n.getInit();
		if (init != null) {
			if (init instanceof LambdaExpr
					|| init instanceof MethodReferenceExpr) {
				ArrayFilter<Method> filter = new ArrayFilter<Method>(null);
				SymbolType scope = symbolTable.getType(n.getId().getName(),
						ReferenceType.VARIABLE);
				filter.appendPredicate(new CompatibleFunctionalPredicate<A>(
						scope, this, null, arg));
				SymbolData sd = null;
				try {
					sd = MethodInspector.findMethodType(scope, filter, null,
							null);
				} catch (Exception e) {
					throw new NoSuchExpressionTypeException(e);
				}
				if (init instanceof LambdaExpr) {
					init.setSymbolData(sd);
				} else {
					init.setSymbolData(scope);
					MethodReferenceExpr methodRef = (MethodReferenceExpr) init;
					methodRef.accept(this, arg);
				}
			} else {
				init.accept(this, arg);
			}
		}
	}

}