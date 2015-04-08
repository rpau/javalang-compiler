package org.walkmod.javalang.compiler.symbols;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.JavadocManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.AnnotationMemberDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.JavadocComment;
import org.walkmod.javalang.ast.body.JavadocTag;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.MultiTypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.NormalAnnotationExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.SingleMemberAnnotationExpr;
import org.walkmod.javalang.ast.expr.SuperExpr;
import org.walkmod.javalang.ast.expr.ThisExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.AssertStmt;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.CatchClause;
import org.walkmod.javalang.ast.stmt.DoStmt;
import org.walkmod.javalang.ast.stmt.ExplicitConstructorInvocationStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.ForStmt;
import org.walkmod.javalang.ast.stmt.ForeachStmt;
import org.walkmod.javalang.ast.stmt.IfStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.stmt.SwitchEntryStmt;
import org.walkmod.javalang.ast.stmt.SwitchStmt;
import org.walkmod.javalang.ast.stmt.SynchronizedStmt;
import org.walkmod.javalang.ast.stmt.ThrowStmt;
import org.walkmod.javalang.ast.stmt.WhileStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.VoidType;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.compiler.actions.LoadEnumConstantLiteralsAction;
import org.walkmod.javalang.compiler.actions.LoadFieldDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadMethodDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadStaticImportsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeParamsAction;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.providers.SymbolActionProviderAware;
import org.walkmod.javalang.compiler.types.TypeVisitorAdapter;
import org.walkmod.javalang.compiler.types.TypeTable;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class SymbolVisitorAdapter<A extends Map<String, Object>> extends
		VoidVisitorAdapter<A> implements SymbolActionProviderAware {

	private SymbolTable symbolTable;

	private ClassLoader classLoader;

	private TypeTable<Map<String, Object>> typeTable;

	private TypeVisitorAdapter<A> expressionTypeAnalyzer;

	private List<SymbolAction> actions = null;

	private SymbolActionProvider actionProvider = null;

	private static final String ORIGINAL_LOCATION = "SemanticVisitorAdapter_original_location";

	private int innerAnonymousClassCounter = 1;

	public SymbolTable getSymbolTable() {
		return symbolTable;
	}

	public void setSymbolTable(SymbolTable symbolTable) {
		this.symbolTable = symbolTable;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public void setSymbolActions(List<SymbolAction> actions) {
		this.actions = actions;
	}

	public void setActionProvider(SymbolActionProvider actionProvider) {
		this.actionProvider = actionProvider;
	}

	public void setExpressionTypeAnalyzer(
			TypeVisitorAdapter<A> expressionTypeAnalyzer) {
		this.expressionTypeAnalyzer = expressionTypeAnalyzer;
	}

	@Override
	public void visit(CompilationUnit unit, A arg) {
		if (actionProvider != null) {
			actions = actionProvider.getActions(unit);
		}
		symbolTable = new SymbolTable();
		symbolTable.setActions(actions);
		typeTable = TypeTable.getInstance();
		typeTable.clear();
		typeTable.setClassLoader(classLoader);
		typeTable.visit(unit, arg);
		symbolTable.pushScope();
		innerAnonymousClassCounter = 1;
		expressionTypeAnalyzer = new TypeVisitorAdapter<A>(typeTable,
				symbolTable, this);
		Set<String> langImports = typeTable.findTypesByPrefix("java.lang");
		if (langImports != null) {
			for (String type : langImports) {
				SymbolType stype = new SymbolType();
				stype.setName(type);

				if (Modifier.isPublic(stype.getClazz().getModifiers())) {
					symbolTable.pushSymbol(typeTable.getSimpleName(type),
							ReferenceType.TYPE, stype, null, actions);
				}

			}
		}
		super.visit(unit, arg);
		symbolTable.popScope();
	}

	public void visit(NormalAnnotationExpr n, A arg) {

		Symbol s = symbolTable.lookUpSymbolForRead(n.getName().toString(),
				ReferenceType.TYPE);
		n.setSymbolData(s.getType());
		super.visit(n, arg);
	}

	public void visit(MarkerAnnotationExpr n, A arg) {
		Symbol s = symbolTable.lookUpSymbolForRead(n.getName().toString(),
				ReferenceType.TYPE);
		n.setSymbolData(s.getType());
		super.visit(n, arg);
	}

	public void visit(SingleMemberAnnotationExpr n, A arg) {
		Symbol s = symbolTable.lookUpSymbolForRead(n.getName().toString(),
				ReferenceType.TYPE);
		n.setSymbolData(s.getType());
		super.visit(n, arg);
	}

	private void processJavadocTypeReference(String type) {
		if (type != null) {
			String[] split = type.split("#");
			String typeName = split[0];
			if (!"".equals(typeName)) {
				symbolTable.lookUpSymbolForRead(typeName, ReferenceType.TYPE);
			}
			if (split.length == 2) {
				String signature = split[1];
				int start = signature.indexOf("(");
				if (start > 0 && signature.endsWith(")")) {
					signature = signature.substring(start + 1,
							signature.length() - 1);
					String[] params = signature.split(",");
					if (params != null) {
						for (String param : params) {
							if (!"".equals(param)) {
								if (param.endsWith("[]")) {
									param = param.substring(0,
											param.length() - 2);
								}
								symbolTable.lookUpSymbolForRead(param.trim(),
										ReferenceType.TYPE);
							}
						}
					}
				}
			}
		}
	}

	public void visit(JavadocComment n, A arg) {
		List<JavadocTag> tags = null;
		try {
			tags = JavadocManager.parse(n.getContent());
		} catch (Exception e) {
			// nothing, javadoc can be bad writen and the code compiles
		}
		if (tags != null) {
			for (JavadocTag tag : tags) {
				String name = tag.getName();
				if ("@link".equals(name) || "@linkplain".equals(name)
						|| "@throws".equals(name)) {
					List<String> values = tag.getValues();
					if (values != null) {
						String type = values.get(0);
						processJavadocTypeReference(type);

					}
				} else if ("@see".equals(name)) {
					List<String> values = tag.getValues();
					if (values != null) {
						String type = values.get(0);
						if (type != null && !type.startsWith("<")
								&& !type.startsWith("\"")) {
							processJavadocTypeReference(type);

						}
					}
				}
			}
		}
	}

	public void visit(EnumDeclaration n, A arg) {
		try {
			symbolTable.pushScope();
			loadThisSymbol(n, arg);
			super.visit(n, arg);
			symbolTable.popScope();
		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);
		}
	}

	public void visit(ImportDeclaration id, A arg) {

		String name = id.getName().toString();
		Set<String> types = null;
		if (!id.isStatic() || id.isAsterisk()) {
			types = typeTable.findTypesByPrefix(name);
		} else {
			int classNameEnding = name.lastIndexOf('.');
			if (classNameEnding != -1) {
				name = name.substring(0, classNameEnding);
				types = new HashSet<String>();
				types.add(name);
			} else {
				throw new NoSuchExpressionTypeException("Ops! The import "
						+ id.toString() + " can't be resolved", null);
			}
		}
		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		if (id.isStatic()) {
			actions.add(new LoadStaticImportsAction());
		}
		if (actionProvider != null) {
			actions.addAll(actionProvider.getActions(id));
		}
		for (String type : types) {
			SymbolType stype = new SymbolType();
			stype.setName(type);
			symbolTable.pushSymbol(typeTable.getSimpleName(type),
					ReferenceType.TYPE, stype, id, actions);
		}

	}

	private void loadThisSymbol(TypeDeclaration declaration, A arg)
			throws ClassNotFoundException {
		SymbolType lastScope = symbolTable.getType("this",
				ReferenceType.VARIABLE);

		String className = typeTable.getFullName(declaration);
		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		actions.add(new LoadTypeParamsAction());
		actions.add(new LoadFieldDeclarationsAction(typeTable, actionProvider));
		actions.add(new LoadMethodDeclarationsAction(typeTable, actionProvider));
		actions.add(new LoadTypeDeclarationsAction(typeTable, actionProvider));
		actions.add(new LoadEnumConstantLiteralsAction());

		if (actionProvider != null) {
			actions.addAll(actionProvider.getActions(declaration));
		}

		SymbolType type = null;
		if (lastScope == null) {
			type = new SymbolType(className);
			symbolTable.pushSymbol(typeTable.getSimpleName(className),
					ReferenceType.TYPE, type, declaration);
			symbolTable.pushSymbol("this", ReferenceType.VARIABLE, type,
					declaration, actions);
			symbolTable.pushSymbol("this", ReferenceType.TYPE, type,
					declaration, (List<SymbolAction>) null);

		} else {
			type = new SymbolType(lastScope.getName() + "$"
					+ declaration.getName());
			symbolTable.pushSymbol(typeTable.getSimpleName(className),
					ReferenceType.TYPE, type, declaration);
			symbolTable.pushSymbol("this", ReferenceType.VARIABLE, type,
					declaration, actions);
			symbolTable.pushSymbol("this", ReferenceType.TYPE, type,
					declaration, (List<SymbolAction>) null);
		}
		if (declaration instanceof ClassOrInterfaceDeclaration) {
			if (!((ClassOrInterfaceDeclaration) declaration).isInterface()) {
				symbolTable.pushSymbol("super", ReferenceType.VARIABLE,
						new SymbolType(type.getClazz().getSuperclass()), null,
						actions);
			}
		}

	}

	private void loadThisSymbol(ObjectCreationExpr n, A arg)
			throws ClassNotFoundException {

		boolean anonymousClass = n.getAnonymousClassBody() != null;
		if (anonymousClass) {
			String className = symbolTable
					.findSymbol("this", ReferenceType.TYPE).getType().getName();
			className = className.replace('$', '.');

			List<SymbolAction> actions = new LinkedList<SymbolAction>();
			actions.add(new LoadTypeParamsAction());
			actions.add(new LoadFieldDeclarationsAction(typeTable,
					actionProvider));
			actions.add(new LoadMethodDeclarationsAction(typeTable,
					actionProvider));
			actions.add(new LoadTypeDeclarationsAction(typeTable,
					actionProvider));
			actions.add(new LoadEnumConstantLiteralsAction());

			if (actionProvider != null) {
				actions.addAll(actionProvider.getActions(n));
			}

			SymbolType type = null;
			type = new SymbolType(className + "$" + innerAnonymousClassCounter);
			symbolTable.pushSymbol("this", ReferenceType.VARIABLE, type, n,
					actions);

			symbolTable
					.pushSymbol("super", ReferenceType.VARIABLE,
							new SymbolType(type.getClazz().getSuperclass()), n,
							actions);
			innerAnonymousClassCounter++;
		}
	}

	@Override
	public void visit(ConstructorDeclaration n, A arg) {
		symbolTable.pushScope();
		super.visit(n, arg);
		n.accept(expressionTypeAnalyzer, arg);
		symbolTable.popScope();
	}

	@Override
	public void visit(ObjectCreationExpr n, A arg) {
		List<BodyDeclaration> body = n.getAnonymousClassBody();
		if (body != null) {
			symbolTable.pushScope();
			try {
				loadThisSymbol(n, arg);
			} catch (ClassNotFoundException e) {
				throw new NoSuchExpressionTypeException(e);
			}
			super.visit(n, arg);
			symbolTable.popScope();
		}
	}

	@Override
	public void visit(ClassOrInterfaceDeclaration n, A arg) {

		try {
			symbolTable.pushScope();
			loadThisSymbol(n, arg);
			super.visit(n, arg);
			n.setSymbolData(symbolTable.getType("this",
					ReferenceType.VARIABLE));
			symbolTable.popScope();
		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);
		}
	}

	@Override
	public void visit(AnnotationDeclaration n, A arg) {
		try {
			symbolTable.pushScope();
			loadThisSymbol(n, arg);
			super.visit(n, arg);
			n.setSymbolData(symbolTable.getType("this",
					ReferenceType.VARIABLE));
			symbolTable.popScope();
		} catch (ClassNotFoundException e) {
			throw new NoSuchExpressionTypeException(e);
		}
	}

	@Override
	public void visit(EnumConstantDeclaration n, A arg) {
		if (n.getJavaDoc() != null) {
			n.getJavaDoc().accept(this, arg);
		}
		if (n.getAnnotations() != null) {
			for (AnnotationExpr a : n.getAnnotations()) {
				a.accept(this, arg);
			}
		}
		if (n.getArgs() != null) {
			for (Expression e : n.getArgs()) {
				e.accept(expressionTypeAnalyzer, arg);
			}
		}
		if (n.getClassBody() != null) {
			for (BodyDeclaration member : n.getClassBody()) {
				member.accept(this, arg);
			}
		}
	}

	public void visit(VariableDeclarator n, A arg) {
		n.getId().accept(this, arg);
		if (n.getInit() != null) {
			n.getInit().accept(expressionTypeAnalyzer, arg);
		}
	}

	@Override
	public void visit(AnnotationMemberDeclaration n, A arg) {

		if (n.getJavaDoc() != null) {
			n.getJavaDoc().accept(this, arg);
		}
		if (n.getAnnotations() != null) {
			for (AnnotationExpr a : n.getAnnotations()) {
				a.accept(this, arg);
			}
		}
		n.getType().accept(expressionTypeAnalyzer, arg);

		Expression expr = n.getDefaultValue();
		if (expr != null) {
			expr.accept(expressionTypeAnalyzer, arg);
		}
	}

	@Override
	public void visit(BlockStmt n, A arg) {
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(n);
		}
		symbolTable.addActionsToScope(actions);
		symbolTable.pushScope();
		super.visit(n, arg);
		List<Statement> stmts = n.getStmts();
		if (stmts != null) {
			SymbolData sd = null;
			for (Statement stmt : stmts) {
				SymbolData stmtSd = stmt.getSymbolData();
				if (stmtSd != null) {
					if (sd == null) {
						sd = stmtSd;
					} else {
						sd = sd.merge(stmtSd);
					}
				}
			}
			n.setSymbolData(sd);
		}
		symbolTable.popScope();
	}

	@Override
	public void visit(MethodDeclaration n, A arg) {
		symbolTable.pushScope();
		super.visit(n, arg);
		n.accept(expressionTypeAnalyzer, arg);
		symbolTable.popScope();
	}

	@Override
	public void visit(Parameter n, A arg) {
		super.visit(n, arg);
		Type ptype = n.getType();
		SymbolType type = null;
		if (ptype != null) {
			type = (SymbolType) ptype.getSymbolData();

		} else {
			type = (SymbolType) n.getSymbolData();
		}
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(n);
		}
		symbolTable.pushSymbol(n.getId().getName(), ReferenceType.VARIABLE,
				type, n, actions);
		n.setSymbolData(type);
	}

	@Override
	public void visit(CatchClause n, A arg) {
		symbolTable.pushScope();
		super.visit(n, arg);
		symbolTable.popScope();
	}

	@Override
	public void visit(MultiTypeParameter n, A arg) {
		super.visit(n, arg);
		List<Type> types = n.getTypes();
		List<SymbolType> symbolTypes = new LinkedList<SymbolType>();
		Iterator<Type> it = types.iterator();

		while (it.hasNext()) {
			Type aux = it.next();
			SymbolType type = typeTable.valueOf(aux);
			aux.setSymbolData(type);
			symbolTypes.add(type);
		}
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(n);
		}
		MultiTypeSymbol symbol = new MultiTypeSymbol(n.getId().getName(),
				symbolTypes, n, actions);
		symbolTable.pushSymbol(symbol);
		n.setSymbolData(new SymbolType(symbolTypes));

	}

	@Override
	public void visit(MethodCallExpr n, A arg) {

		SymbolType scopeType = null;

		if (n.getScope() == null) {
			scopeType = symbolTable.getType("this", ReferenceType.VARIABLE);
		} else {

			scopeType = (SymbolType) n.getScope().getSymbolData();

		}
		List<Expression> args = n.getArgs();
		SymbolType[] argsType = null;
		if (args != null) {
			Iterator<Expression> it = args.iterator();
			argsType = new SymbolType[args.size()];
			int i = 0;
			while (it.hasNext()) {
				Expression current = it.next();

				SymbolType argType = (SymbolType) current.getSymbolData();

				argsType[i] = argType;

				i++;

			}
		} else {
			argsType = new SymbolType[0];
		}

		symbolTable.lookUpSymbolForRead(n.getName(), ReferenceType.METHOD,
				scopeType, argsType);

	}

	@Override
	public void visit(FieldAccessExpr n, A arg) {

		if (n.getScope() != null) {
			SymbolType scopeType = (SymbolType) n.getScope().getSymbolData();
			SymbolType thisType = symbolTable.getType("this",
					ReferenceType.VARIABLE);

			if (thisType != null && thisType.equals(scopeType)) {
				lookupSymbol(n.getField(), ReferenceType.VARIABLE, arg);
			}
		} else {
			lookupSymbol(n.getField(), ReferenceType.VARIABLE, arg);
		}
	}

	public void visit(AssignExpr n, A arg) {
		AccessType old = (AccessType) arg.get(AccessType.ACCESS_TYPE);
		arg.put(AccessType.ACCESS_TYPE, AccessType.WRITE);
		n.getTarget().accept(this, arg);
		arg.put(AccessType.ACCESS_TYPE, AccessType.READ);
		n.getValue().accept(expressionTypeAnalyzer, arg);
		arg.put(AccessType.ACCESS_TYPE, old);
	}

	public void visit(VariableDeclarationExpr n, A arg) {

		Type type = n.getType();
		SymbolType st = (SymbolType) type.getSymbolData();
		if (st == null) {
			type.accept(expressionTypeAnalyzer, arg);
			st = (SymbolType) type.getSymbolData();
		}
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(n);
		}
		List<VariableDeclarator> vars = n.getVars();
		for (VariableDeclarator vd : vars) {
			SymbolType aux = st.clone();
			if (vd.getId().getArrayCount() > 0) {
				aux.setArrayCount(vd.getId().getArrayCount());
			}
			symbolTable.pushSymbol(vd.getId().getName(),
					ReferenceType.VARIABLE, aux,
					(Node) (arg.get(ORIGINAL_LOCATION)), actions);
		}
	}

	@Override
	public void visit(ExpressionStmt n, A arg) {
		Object original = arg.get(ORIGINAL_LOCATION);
		arg.put(ORIGINAL_LOCATION, n);
		n.getExpression().accept(expressionTypeAnalyzer, arg);
		arg.put(ORIGINAL_LOCATION, original);
	}

	@Override
	public void visit(AssertStmt n, A arg) {
		n.getCheck().accept(expressionTypeAnalyzer, arg);
		if (n.getMessage() != null) {
			n.getMessage().accept(this, arg);
		}
	}

	@Override
	public void visit(DoStmt n, A arg) {
		n.getBody().accept(this, arg);
		n.getCondition().accept(expressionTypeAnalyzer, arg);
		SymbolData sd = n.getBody().getSymbolData();
		n.setSymbolData(sd);
	}

	@Override
	public void visit(ExplicitConstructorInvocationStmt n, A arg) {
		if (!n.isThis()) {
			if (n.getExpr() != null) {
				n.getExpr().accept(expressionTypeAnalyzer, arg);
				n.setSymbolData(n.getExpr().getSymbolData());
			}
		} else {
			n.setSymbolData(symbolTable.getType("this", ReferenceType.VARIABLE));
		}
		if (n.getTypeArgs() != null) {
			for (Type t : n.getTypeArgs()) {
				t.accept(this, arg);
			}
		}
		if (n.getArgs() != null) {
			for (Expression e : n.getArgs()) {
				e.accept(expressionTypeAnalyzer, arg);
			}
		}

	}

	@Override
	public void visit(ForeachStmt n, A arg) {
		n.getVariable().accept(expressionTypeAnalyzer, arg);
		n.getIterable().accept(expressionTypeAnalyzer, arg);
		n.getBody().accept(this, arg);
		SymbolData sd = n.getBody().getSymbolData();
		n.setSymbolData(sd);
	}

	@Override
	public void visit(ForStmt n, A arg) {
		if (n.getInit() != null) {
			for (Expression e : n.getInit()) {
				e.accept(expressionTypeAnalyzer, arg);
			}
		}
		if (n.getCompare() != null) {
			n.getCompare().accept(expressionTypeAnalyzer, arg);
		}
		if (n.getUpdate() != null) {
			for (Expression e : n.getUpdate()) {
				e.accept(expressionTypeAnalyzer, arg);
			}
		}
		n.getBody().accept(this, arg);
		SymbolData sd = n.getBody().getSymbolData();
		n.setSymbolData(sd);
	}

	@Override
	public void visit(IfStmt n, A arg) {
		n.getCondition().accept(expressionTypeAnalyzer, arg);
		n.getThenStmt().accept(this, arg);
		SymbolData symData = n.getThenStmt().getSymbolData();
		n.setSymbolData(symData);
		if (n.getElseStmt() != null) {
			n.getElseStmt().accept(this, arg);
			if (symData != null) {
				symData = symData.merge(n.getElseStmt().getSymbolData());
			}
			n.setSymbolData(symData);
		}

	}

	@Override
	public void visit(ReturnStmt n, A arg) {
		if (n.getExpr() != null) {
			n.getExpr().accept(expressionTypeAnalyzer, arg);
			n.setSymbolData(n.getExpr().getSymbolData());
		} else {
			n.setSymbolData(new SymbolType(void.class));
		}
	}

	@Override
	public void visit(SwitchEntryStmt n, A arg) {
		if (n.getLabel() != null) {
			n.getLabel().accept(expressionTypeAnalyzer, arg);
		}
		if (n.getStmts() != null) {
			SymbolData sd = null;
			for (Statement s : n.getStmts()) {
				s.accept(this, arg);
				if (sd == null) {
					sd = s.getSymbolData();
				} else {
					sd = sd.merge(s.getSymbolData());
				}
			}
			n.setSymbolData(sd);
		}
	}

	@Override
	public void visit(SwitchStmt n, A arg) {
		n.getSelector().accept(expressionTypeAnalyzer, arg);
		if (n.getEntries() != null) {
			SymbolData sd = null;
			for (SwitchEntryStmt e : n.getEntries()) {
				e.accept(this, arg);
				if (sd == null) {
					sd = e.getSymbolData();
				} else {
					sd = sd.merge(e.getSymbolData());
				}
			}
		}
	}

	@Override
	public void visit(SynchronizedStmt n, A arg) {
		n.getExpr().accept(expressionTypeAnalyzer, arg);
		n.getBlock().accept(this, arg);
		n.setSymbolData(n.getBlock().getSymbolData());
	}

	@Override
	public void visit(ThrowStmt n, A arg) {
		n.getExpr().accept(expressionTypeAnalyzer, arg);
		n.setSymbolData(null);
	}

	@Override
	public void visit(WhileStmt n, A arg) {
		n.getCondition().accept(expressionTypeAnalyzer, arg);
		n.getBody().accept(this, arg);
		n.setSymbolData(n.getBody().getSymbolData());
	}

	private void lookupSymbol(String name, ReferenceType referenceType, A arg) {
		AccessType atype = (AccessType) arg.get(AccessType.ACCESS_TYPE);
		if (atype != null) {
			if (atype.equals(AccessType.WRITE)) {
				symbolTable.lookUpSymbolForWrite(name);
			} else {
				symbolTable.lookUpSymbolForRead(name, referenceType);
			}
		} else {
			symbolTable.lookUpSymbolForRead(name, referenceType);
		}
	}

	public void visit(NameExpr n, A arg) {
		lookupSymbol(n.toString(), null, arg);
	}

	@Override
	public void visit(MethodReferenceExpr n, A arg) {

		SymbolType scopeType = null;

		if (n.getScope() == null) {
			scopeType = symbolTable.getType("this", ReferenceType.VARIABLE);
		} else {
			scopeType = (SymbolType) n.getScope().getSymbolData();
		}
		SymbolType[] argsType = (SymbolType[]) n.getReferencedArgsSymbolData();

		symbolTable.lookUpSymbolForRead(n.getIdentifier(),
				ReferenceType.METHOD, scopeType, argsType);

	}

	@Override
	public void visit(TypeParameter n, A arg) {
		super.visit(n, arg);
		symbolTable.lookUpSymbolForRead(n.getName(), ReferenceType.TYPE);

	}

	@Override
	public void visit(SuperExpr n, A arg) {
		symbolTable.lookUpSymbolForRead("super", ReferenceType.VARIABLE);
	}

	@Override
	public void visit(ThisExpr n, A arg) {
		symbolTable.lookUpSymbolForRead("this", ReferenceType.VARIABLE);
	}

	@Override
	public void visit(WildcardType n, A arg) {
		n.accept(expressionTypeAnalyzer, arg);
	}

	@Override
	public void visit(VoidType n, A arg) {
		n.accept(expressionTypeAnalyzer, arg);
	}

	@Override
	public void visit(org.walkmod.javalang.ast.type.ReferenceType n, A arg) {
		n.accept(expressionTypeAnalyzer, arg);
	}

	@Override
	public void visit(PrimitiveType n, A arg) {
		n.accept(expressionTypeAnalyzer, arg);
	}

	@Override
	public void visit(ClassOrInterfaceType n, A arg) {
		n.accept(expressionTypeAnalyzer, arg);
	}
	
	@Override
	public void visit(FieldDeclaration n, A arg){
		super.visit(n, arg);
		n.accept(expressionTypeAnalyzer, arg);
	}

}
