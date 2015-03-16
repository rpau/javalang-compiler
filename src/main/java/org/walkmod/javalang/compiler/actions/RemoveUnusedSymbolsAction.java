package org.walkmod.javalang.compiler.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.ModifierSet;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.LiteralExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class RemoveUnusedSymbolsAction implements SymbolAction {

	private List<? extends Node> siblings;

	public RemoveUnusedSymbolsAction(List<? extends Node> container) {
		this.siblings = container;
	}

	@Override
	public void execute(Symbol symbol, SymbolTable table, SymbolEvent event) {
		if (event.equals(SymbolEvent.POP)) {
			Node n = symbol.getLocation();
			if (n != null) {
				Map<String, Object> attrs = symbol.getAttributes();

				Object reads = attrs.get(ReferencesCounterAction.READS);

				Object writes = attrs.get(ReferencesCounterAction.WRITES);

				if (reads == null && writes == null) {
					if (n instanceof MethodDeclaration) {
						removeMethod(symbol, table);
					} else if (n instanceof FieldDeclaration) {
						removeField(symbol, table);
					} else if (n instanceof TypeDeclaration) {
						Symbol thisSymbol = table.findSymbol("this",
								ReferenceType.VARIABLE);
						if (symbol.getReferenceType()
								.equals(ReferenceType.TYPE)
								&& !thisSymbol.getType().equals(
										symbol.getType())) {
							removeType(symbol, table);
						}
					} else if (n instanceof ExpressionStmt) {
						removeVariable(symbol, table);
					}
				}
			}
		}
	}

	public List<? extends Node> getSiblings() {
		return siblings;
	}

	public void removeMethod(Symbol symbol, SymbolTable table) {
		MethodDeclaration md = (MethodDeclaration) symbol.getLocation();

		int modifiers = md.getModifiers();
		if (ModifierSet.isPrivate(modifiers)) {
			siblings.remove(md);
		}
	}

	public void removeType(Symbol symbol, SymbolTable table) {
		TypeDeclaration td = (TypeDeclaration) symbol.getLocation();
		int modifiers = td.getModifiers();
		if (ModifierSet.isPrivate(modifiers)) {
			siblings.remove(td);
		}
	}

	public void removeVariable(Symbol symbol, SymbolTable table) {
		ExpressionStmt original = (ExpressionStmt) symbol.getLocation();
		VariableDeclarationExpr vd = (VariableDeclarationExpr) original
				.getExpression();
		List<VariableDeclarator> vds = vd.getVars();
		if (vds.size() == 1) {
			siblings.remove(original);
		} else {
			Iterator<VariableDeclarator> it = vds.iterator();
			boolean finish = false;
			while (it.hasNext() && !finish) {
				VariableDeclarator current = it.next();
				if (current.getId().getName().equals(symbol.getName())) {
					finish = true;
					it.remove();
				}
			}
		}
	}

	public void removeField(Symbol symbol, SymbolTable table) {
		FieldDeclaration fd = (FieldDeclaration) symbol.getLocation();
		int modifiers = fd.getModifiers();
		if (ModifierSet.isPrivate(modifiers)) {
			List<VariableDeclarator> vds = fd.getVariables();
			if (vds != null) {
				Iterator<VariableDeclarator> it = vds.iterator();
				while (it.hasNext()) {
					VariableDeclarator vd = it.next();
					if (vd.getId().getName().equals(symbol.getName())) {
						Expression init = vd.getInit();
						if (init == null || init instanceof LiteralExpr) {
							if (vds.size() == 1) {
								siblings.remove(fd);
							} else {
								it.remove();
							}
						}
					}
				}
			}
		}
	}
}
