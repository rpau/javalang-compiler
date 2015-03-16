package org.walkmod.javalang.visitors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.MultiTypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.CatchClause;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.actions.LoadFieldDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadMethodDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeParamsAction;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.providers.SymbolActionProviderAware;
import org.walkmod.javalang.compiler.symbols.AccessType;
import org.walkmod.javalang.compiler.symbols.MultiTypeSymbol;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.ExpressionTypeAnalyzer;
import org.walkmod.javalang.compiler.types.TypeTable;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class SemanticVisitorAdapter<A extends Map<String, Object>> extends
		VoidVisitorAdapter<A> implements SymbolActionProviderAware {

	private SymbolTable symbolTable;

	private ClassLoader classLoader;

	private TypeTable<A> typeTable;

	private ExpressionTypeAnalyzer<A> expressionTypeAnalyzer;

	private List<SymbolAction> actions = null;

	private SymbolActionProvider actionProvider = null;

	private static final String ORIGINAL_LOCATION = "SemanticVisitorAdapter_original_location";

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

	public TypeTable<A> getTypeTable() {
		return typeTable;
	}

	public void setTypeTable(TypeTable<A> typeTable) {
		this.typeTable = typeTable;
	}

	public void setSymbolActions(List<SymbolAction> actions) {
		this.actions = actions;
	}

	public void setActionProvider(SymbolActionProvider actionProvider) {
		this.actionProvider = actionProvider;
	}

	@Override
	public void visit(CompilationUnit unit, A arg) {
		if (actionProvider != null) {
			actions = actionProvider.getActions(unit);
		}
		symbolTable = new SymbolTable();
		symbolTable.setActions(actions);
		typeTable = new TypeTable<A>();
		typeTable.setClassLoader(classLoader);
		typeTable.visit(unit, arg);
		symbolTable.pushScope();

		expressionTypeAnalyzer = new ExpressionTypeAnalyzer<A>(typeTable,
				symbolTable);

		super.visit(unit, arg);
		symbolTable.popScope();
	}

	public void visit(ImportDeclaration id, A arg) {
		try {
			String name = id.getName().toString();
			Set<String> types = typeTable.findTypesByPrefix(name);
			List<SymbolAction> actions = null;
			if (actionProvider != null) {
				actions = actionProvider.getActions(id);
			}
			for (String type : types) {
				SymbolType stype = new SymbolType();
				stype.setName(type);
				stype.setClazz(typeTable.loadClass(type));

				symbolTable.pushSymbol(typeTable.getSimpleName(type),
						ReferenceType.TYPE, stype, id, actions);
			}
		} catch (ClassNotFoundException e) {
			new NoSuchExpressionTypeException(e);
		}
	}

	public void loadThisSymbol(ClassOrInterfaceDeclaration declaration, A arg)
			throws ClassNotFoundException {
		SymbolType lastScope = symbolTable.getType("this",
				ReferenceType.VARIABLE);

		String className = typeTable.getFullName(declaration);
		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		actions.add(new LoadTypeParamsAction());
		actions.add(new LoadFieldDeclarationsAction(typeTable, actionProvider));
		actions.add(new LoadMethodDeclarationsAction(typeTable, actionProvider));
		actions.add(new LoadTypeDeclarationsAction(typeTable, actionProvider));
		
		if (actionProvider != null) {
			actions.addAll(actionProvider.getActions(declaration));
		}
		
		SymbolType type = null;
		if (lastScope == null) {
			type = new SymbolType(className);
			type.setClazz(typeTable.loadClass(type));
			symbolTable.pushSymbol(typeTable.getSimpleName(className),
					ReferenceType.TYPE, type, declaration);
			symbolTable.pushSymbol("this", ReferenceType.VARIABLE, type,
					declaration, actions);

		} else {
			type = new SymbolType(lastScope.getName() + "$"
					+ declaration.getName());
			type.setClazz(typeTable.loadClass(type));
			symbolTable.pushSymbol(typeTable.getSimpleName(className),
					ReferenceType.TYPE, type, declaration);
			symbolTable.pushSymbol("this", ReferenceType.VARIABLE, type,
					declaration, actions);
		}

	}

	@Override
	public void visit(ClassOrInterfaceDeclaration n, A arg) {

		try {
			symbolTable.pushScope();
			loadThisSymbol(n, arg);
			super.visit(n, arg);
			symbolTable.popScope();
		} catch (ClassNotFoundException e) {
			new NoSuchExpressionTypeException(e);
		}
	}

	@Override
	public void visit(BlockStmt n, A arg) {
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(n);
		}
		symbolTable.pushScope(actions);
		super.visit(n, arg);
		symbolTable.popScope();
	}

	@Override
	public void visit(Parameter n, A arg) {
		SymbolType type = typeTable.valueOf(n.getType());
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(n);
		}
		symbolTable.pushSymbol(n.getId().getName(), ReferenceType.VARIABLE,
				type, n, actions);
		super.visit(n, arg);
	}

	@Override
	public void visit(CatchClause n, A arg) {
		symbolTable.pushScope();
		super.visit(n, arg);
		symbolTable.popScope();
	}

	@Override
	public void visit(ClassOrInterfaceType n, A arg) {
		symbolTable.lookUpSymbolForRead(n.getName(), ReferenceType.TYPE);
		super.visit(n, arg);
	}

	@Override
	public void visit(MultiTypeParameter n, A arg) {
		List<Type> types = n.getTypes();
		List<SymbolType> symbolTypes = new LinkedList<SymbolType>();
		Iterator<Type> it = types.iterator();

		while (it.hasNext()) {
			Type aux = it.next();
			SymbolType type = typeTable.valueOf(aux);
			symbolTypes.add(type);
		}
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(n);
		}
		MultiTypeSymbol symbol = new MultiTypeSymbol(n.getId().getName(),
				symbolTypes, n, actions);
		symbolTable.pushSymbol(symbol);

		super.visit(n, arg);
	}

	@Override
	public void visit(MethodCallExpr n, A arg) {
		try {
			SymbolType scopeType = null;

			if (n.getScope() == null) {
				scopeType = symbolTable.getType("this", ReferenceType.VARIABLE);
			} else {
				n.getScope().accept(expressionTypeAnalyzer, arg);
				scopeType = (SymbolType) arg
						.remove(ExpressionTypeAnalyzer.TYPE_KEY);
				Class<?> clazz = typeTable.loadClass(scopeType);
				scopeType.setClazz(clazz);
			}
			List<Expression> args = n.getArgs();
			SymbolType[] argsType = null;
			if (args != null) {
				Iterator<Expression> it = args.iterator();
				argsType = new SymbolType[args.size()];
				int i = 0;
				while (it.hasNext()) {
					Expression current = it.next();
					current.accept(expressionTypeAnalyzer, arg);
					SymbolType argType = (SymbolType) arg
							.remove(ExpressionTypeAnalyzer.TYPE_KEY);
					if (argType != null) { // current instance of
											// NullLiteralExpr
						Class<?> clazz = typeTable.loadClass(argType);
						argType.setClazz(clazz);
					}
					argsType[i] = argType;

					i++;

				}
			} else {
				argsType = new SymbolType[0];
			}

			symbolTable.lookUpSymbolForRead(n.getName(), ReferenceType.METHOD,
					scopeType, argsType);
		} catch (ClassNotFoundException e) {
			new NoSuchExpressionTypeException(e);
		}

	}

	@Override
	public void visit(FieldAccessExpr n, A arg) {

		if (n.getScope() != null) {
			n.getScope().accept(expressionTypeAnalyzer, arg);
			SymbolType scopeType = (SymbolType) arg
					.remove(ExpressionTypeAnalyzer.TYPE_KEY);
			if (symbolTable.getType("this", ReferenceType.VARIABLE).equals(
					scopeType)) {
				lookupSymbol(n.getField(), ReferenceType.VARIABLE, arg);
			}
		}
	}

	public void visit(AssignExpr n, A arg) {
		AccessType old = (AccessType) arg.get(AccessType.ACCESS_TYPE);
		arg.put(AccessType.ACCESS_TYPE, AccessType.WRITE);
		n.getTarget().accept(this, arg);
		arg.put(AccessType.ACCESS_TYPE, AccessType.READ);
		n.getValue().accept(this, arg);
		arg.put(AccessType.ACCESS_TYPE, old);
	}

	public void visit(VariableDeclarationExpr n, A arg) {
		try {
			Type type = n.getType();
			SymbolType st = typeTable.valueOf(type);
			Class<?> clazz = typeTable.loadClass(st);
			st.setClazz(clazz);
			List<SymbolAction> actions = null;
			if (actionProvider != null) {
				actions = actionProvider.getActions(n);
			}
			List<VariableDeclarator> vars = n.getVars();
			for (VariableDeclarator vd : vars) {
				SymbolType aux = st.clone();

				symbolTable.pushSymbol(vd.getId().getName(),
						ReferenceType.VARIABLE, aux,
						(Node) (arg.get(ORIGINAL_LOCATION)), actions);
			}

		} catch (ClassNotFoundException e) {
			new NoSuchExpressionTypeException(e);
		}
		super.visit(n, arg);
	}

	public void visit(ExpressionStmt n, A arg) {
		Object original = arg.get(ORIGINAL_LOCATION);
		arg.put(ORIGINAL_LOCATION, n);
		super.visit(n, arg);
		arg.put(ORIGINAL_LOCATION, original);
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

}
