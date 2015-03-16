package org.walkmod.javalang.compiler.actions;

import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;

public class LoadTypeDeclarationsAction implements SymbolAction {

	private TypeTable<?> typeTable;

	private SymbolActionProvider actionProvider;

	public LoadTypeDeclarationsAction(TypeTable<?> typeTable,
			SymbolActionProvider actionProvider) {
		this.typeTable = typeTable;
		this.actionProvider = actionProvider;
	}

	private void update(TypeDeclaration node, SymbolTable table)
			throws Exception {
		String className = typeTable.getFullName(node);
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(node);
		}
		SymbolType st = new SymbolType();
		st.setName(className);
		st.setClazz(typeTable.loadClass(className));
		table.pushSymbol(typeTable.getSimpleName(className), ReferenceType.TYPE, st, node, actions);
	}

	@Override
	public void execute(Symbol symbol, SymbolTable table, SymbolEvent event)
			throws Exception {
		if (event.equals(SymbolEvent.PUSH)) {
			Node node = symbol.getLocation();

			if (node instanceof TypeDeclaration) {

				TypeDeclaration n = (TypeDeclaration) node;

				if (n.getMembers() != null) {

					for (BodyDeclaration member : n.getMembers()) {
						if (member instanceof TypeDeclaration) {
							update((TypeDeclaration) member, table);
						}
					}
				}
			}
		}
	}
}
