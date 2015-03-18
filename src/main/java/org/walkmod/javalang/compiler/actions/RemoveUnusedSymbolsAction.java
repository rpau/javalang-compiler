package org.walkmod.javalang.compiler.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.ModifierSet;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.LiteralExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
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
					} else if (n instanceof ImportDeclaration) {
						removeImport(symbol, table);
					}
				}
			}
		}
	}

	public List<? extends Node> getSiblings() {
		return siblings;
	}

	public void removeImport(Symbol symbol, SymbolTable table) {
		ImportDeclaration id = (ImportDeclaration) symbol.getLocation();
		if (id.isAsterisk() || id.isStatic()) {

			String type = id.getName().toString();
			if (!id.isAsterisk()) {
				int pos = type.lastIndexOf('.');
				type = type.substring(0, pos);
			}
			List<Symbol> symbols = table.findSymbolsByType(type,
					ReferenceType.TYPE);
			boolean used = false;
			List<Symbol> staticElems = null;
			if (id.isStatic()) {
				staticElems = table.findSymbolsByLocation(id);
			}
			Iterator<Symbol> it = symbols.iterator();
			while (it.hasNext() && !used) {
				Symbol candidate = it.next();

				if (id.isStatic()) {

					Iterator<Symbol> itStatic = staticElems.iterator();
					while (itStatic.hasNext() && !used) {
						Symbol staticElem = itStatic.next();

						ReferenceType rt = staticElem.getReferenceType();
						if (rt.equals(ReferenceType.METHOD)
								|| rt.equals(ReferenceType.VARIABLE)) {
							Map<String, Object> attrs = staticElem
									.getAttributes();
							Object reads = attrs
									.get(ReferencesCounterAction.READS);
							Object writes = attrs
									.get(ReferencesCounterAction.WRITES);
							used = (reads != null || writes != null);
						}

					}

					if (!used) {
						Map<String, Object> attrs = candidate.getAttributes();
						Object reads = attrs.get(ReferencesCounterAction.READS);
						used = (reads != null);
					}
				}
			}
			if (!used) {
				remove(id);
			}
		} else {
			String type = id.getName().toString();
			// internal classes
			List<Symbol> symbols = table.findSymbolsByType(type,
					ReferenceType.TYPE);
			boolean used = false;
			Iterator<Symbol> it = symbols.iterator();
			while (it.hasNext() && !used) {
				Symbol next = it.next();
				Map<String, Object> attrs = next.getAttributes();
				Object reads = attrs.get(ReferencesCounterAction.READS);
				Object writes = attrs.get(ReferencesCounterAction.WRITES);
				used = (reads != null || writes != null);
			}
			if (!used) {
				remove(id);
			}
		}
	}

	private void remove(Object o) {
		Iterator<? extends Node> it = siblings.iterator();
		boolean removed = false;
		while (it.hasNext() && !removed) {
			Node aux = it.next();
			if (aux == o) { // yes, by reference
				it.remove();
				removed = true;
			}
		}
	}

	public void removeMethod(Symbol symbol, SymbolTable table) {
		MethodDeclaration md = (MethodDeclaration) symbol.getLocation();

		int modifiers = md.getModifiers();
		if (ModifierSet.isPrivate(modifiers)) {
			remove(md);
		}
	}

	public void removeType(Symbol symbol, SymbolTable table) {
		TypeDeclaration td = (TypeDeclaration) symbol.getLocation();
		int modifiers = td.getModifiers();
		if (ModifierSet.isPrivate(modifiers)) {
			remove(td);
		}
	}

	public void removeVariable(Symbol symbol, SymbolTable table) {
		ExpressionStmt original = (ExpressionStmt) symbol.getLocation();
		VariableDeclarationExpr vd = (VariableDeclarationExpr) original
				.getExpression();
		List<VariableDeclarator> vds = vd.getVars();
		if (vds.size() == 1) {
			remove(original);
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
						if (init == null || init instanceof LiteralExpr
								|| init instanceof NameExpr) {
							if (vds.size() == 1) {
								remove(fd);
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
