package org.walkmod.javalang.compiler.types;

import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.compiler.actions.LoadEnumConstantLiteralsAction;
import org.walkmod.javalang.compiler.actions.LoadFieldDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadMethodDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeParamsAction;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.GenericVisitorAdapter;

public class ScopeLoader extends GenericVisitorAdapter<Scope, SymbolTable> {

	private TypesLoaderVisitor<?> typeTable = null;
	private TypeVisitorAdapter<?> expressionTypeAnalyzer = null;
	private SymbolActionProvider actionProvider = null;

	public ScopeLoader(TypesLoaderVisitor<?> typeTable,
			TypeVisitorAdapter<?> expressionTypeAnalyzer,
			SymbolActionProvider actionProvider) {
		this.typeTable = typeTable;
		this.expressionTypeAnalyzer = expressionTypeAnalyzer;
		this.actionProvider = actionProvider;
	}

	private Scope process(TypeDeclaration declaration, SymbolTable symbolTable) {
		Symbol<?> sym = symbolTable.findSymbol(declaration.getName(),
				ReferenceType.TYPE);

		symbolTable.pushScope(sym.getInnerScope());

		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		actions.add(new LoadTypeParamsAction());
		actions.add(new LoadTypeDeclarationsAction(typeTable));
		actions.add(new LoadFieldDeclarationsAction(actionProvider));
		actions.add(new LoadEnumConstantLiteralsAction());
		actions.add(new LoadMethodDeclarationsAction(actionProvider,
				expressionTypeAnalyzer));

		if (declaration instanceof ClassOrInterfaceDeclaration) {
			if (!((ClassOrInterfaceDeclaration) declaration).isInterface()) {
				Symbol<?> superSymbol = symbolTable.pushSymbol("super",
						ReferenceType.VARIABLE, new SymbolType(sym.getType()
								.getClazz().getSuperclass()), null,
						(List<SymbolAction>) null);
				Symbol<?> superType = symbolTable.findSymbol(superSymbol
						.getType().getClazz().getCanonicalName(),
						ReferenceType.TYPE);
				if (superType != null) {
					superSymbol.setInnerScope(superType.getInnerScope());
				}
			}
		}
		Symbol<TypeDeclaration> thisSymbol = new Symbol<TypeDeclaration>(
				"this", sym.getType(), declaration, ReferenceType.VARIABLE,
				actions);
		thisSymbol.setInnerScope(sym.getInnerScope());
		symbolTable.pushSymbol(thisSymbol);
		List<BodyDeclaration> members = declaration.getMembers();
		if (members != null) {
			for (BodyDeclaration member : members) {
				if (member instanceof TypeDeclaration) {
					member.accept(this, symbolTable);
				}
			}
		}

		symbolTable.popScope(true);
		return sym.getInnerScope();
	}

	@Override
	public Scope visit(ClassOrInterfaceDeclaration n, SymbolTable symbolTable) {

		return process(n, symbolTable);

	}

	@Override
	public Scope visit(EnumDeclaration n, SymbolTable symbolTable) {
		return process(n, symbolTable);
	}

	@Override
	public Scope visit(AnnotationDeclaration n, SymbolTable symbolTable) {
		return process(n, symbolTable);
	}

	@Override
	public Scope visit(ObjectCreationExpr n, SymbolTable symbolTable) {
		List<BodyDeclaration> body = n.getAnonymousClassBody();
		if (body != null) {

			SymbolType st = ASTSymbolTypeResolver.getInstance().valueOf(
					n.getType());
			
			Scope scope = new Scope();
			symbolTable.pushScope(scope);
			List<BodyDeclaration> members = n.getAnonymousClassBody();
			boolean anonymousClass = members != null;
			if (anonymousClass) {
				String className = symbolTable
						.findSymbol("this", ReferenceType.VARIABLE).getType()
						.getName();

				List<SymbolAction> actions = new LinkedList<SymbolAction>();
				actions.add(new LoadTypeParamsAction());
				actions.add(new LoadTypeDeclarationsAction(typeTable));
				actions.add(new LoadFieldDeclarationsAction(actionProvider));
				actions.add(new LoadMethodDeclarationsAction(actionProvider,
						expressionTypeAnalyzer));

				actions.add(new LoadEnumConstantLiteralsAction());

				if (actionProvider != null) {
					actions.addAll(actionProvider.getActions(n));
				}

				SymbolType type = null;
				type = new SymbolType(className);
				Symbol<?> superSymbol = symbolTable.pushSymbol("super",
						ReferenceType.VARIABLE, new SymbolType(st.getClazz()),
						n, (List<SymbolAction>) null);
				Symbol<?> superType = symbolTable.findSymbol(st.getClazz()
						.getCanonicalName(), ReferenceType.TYPE);
				if (superType != null) {
					superSymbol.setInnerScope(superType.getInnerScope());
				}
				String name = symbolTable.generateAnonymousClass();
				
				type = new SymbolType(name);
				Symbol<?> anonymousType = symbolTable.pushSymbol(name, ReferenceType.TYPE, type, n);
				anonymousType.setInnerScope(scope);

				Symbol<ObjectCreationExpr> thisSymbol = new Symbol<ObjectCreationExpr>(
						"this", type, n, ReferenceType.VARIABLE,
						actions);
				scope.setRootSymbol(thisSymbol);
				thisSymbol.setInnerScope(scope);
				
				symbolTable.pushSymbol(thisSymbol);
				for (BodyDeclaration member : members) {
					if (member instanceof TypeDeclaration) {
						process((TypeDeclaration) member, symbolTable);
					}
				}

			}

			symbolTable.popScope(true);
			return scope;
		}
		return null;
	}

	@Override
	public Scope visit(EnumConstantDeclaration n, SymbolTable symbolTable) {
		Symbol<?> s = symbolTable.findSymbol(n.getName(),
				ReferenceType.ENUM_LITERAL);
		s.setInnerScope(new Scope(s));
		symbolTable.pushScope(s.getInnerScope());

		SymbolType parentType = symbolTable.getType("this",
				ReferenceType.VARIABLE);

		SymbolType type = symbolTable.getType(n.getName(),
				ReferenceType.ENUM_LITERAL);

		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		actions.add(new LoadTypeParamsAction());
		actions.add(new LoadTypeDeclarationsAction(typeTable));
		actions.add(new LoadFieldDeclarationsAction(actionProvider));
		actions.add(new LoadMethodDeclarationsAction(actionProvider,
				expressionTypeAnalyzer));

		if (actionProvider != null) {
			actions.addAll(actionProvider.getActions(n));
		}
		symbolTable.pushSymbol("super", ReferenceType.VARIABLE, parentType,
				n.getParentNode(), (List<SymbolAction>) null);

		String name = symbolTable.generateAnonymousClass();
		type = new SymbolType(name);
		symbolTable.pushSymbol(name, ReferenceType.TYPE, type, n, actions);

		symbolTable
				.pushSymbol("this", ReferenceType.VARIABLE, type, n, actions);
		n.setSymbolData(type);

		symbolTable.popScope(true);
		return s.getInnerScope();
	}

}
