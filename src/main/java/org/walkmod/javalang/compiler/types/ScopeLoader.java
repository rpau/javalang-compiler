package org.walkmod.javalang.compiler.types;

import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.compiler.actions.LoadEnumConstantLiteralsAction;
import org.walkmod.javalang.compiler.actions.LoadFieldDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadMethodDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeParamsAction;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class ScopeLoader extends VoidVisitorAdapter<SymbolTable> {

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

	private void process(TypeDeclaration declaration, SymbolTable symbolTable) {
		Symbol<?> sym = symbolTable.findSymbol(declaration.getName(),
				ReferenceType.TYPE);
		
		symbolTable.pushScope(sym.getInnerScope());

		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		actions.add(new LoadTypeParamsAction());
		actions.add(new LoadTypeDeclarationsAction(typeTable));
		actions.add(new LoadFieldDeclarationsAction(actionProvider));
		actions.add(new LoadEnumConstantLiteralsAction());
		actions.add(new LoadMethodDeclarationsAction(typeTable, actionProvider,
				expressionTypeAnalyzer));

		symbolTable.pushSymbol("this", ReferenceType.VARIABLE, sym.getType(),
				declaration, actions);
		if (declaration instanceof ClassOrInterfaceDeclaration) {
			if (!((ClassOrInterfaceDeclaration) declaration).isInterface()) {
				symbolTable.pushSymbol("super", ReferenceType.VARIABLE,
						new SymbolType(sym.getType().getClazz().getSuperclass()), null,
						(List<SymbolAction>) null);
			}
		}
		List<BodyDeclaration> members = declaration.getMembers();
		if (members != null) {
			for (BodyDeclaration member : members) {
				if (member instanceof TypeDeclaration) {
					member.accept(this, symbolTable);
				}
			}
		}

		
		symbolTable.popScope(true);
	}

	@Override
	public void visit(ClassOrInterfaceDeclaration n, SymbolTable symbolTable) {
		process(n, symbolTable);

	}

	@Override
	public void visit(EnumDeclaration n, SymbolTable symbolTable) {
		process(n, symbolTable);
	}

	@Override
	public void visit(AnnotationDeclaration n, SymbolTable symbolTable) {
		process(n, symbolTable);
	}

}
