/*
 Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 
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
package org.walkmod.javalang.compiler.actions;

import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;

public class LoadTypeDeclarationsAction extends SymbolAction {

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
		node.setSymbolData(st);
		table.pushSymbol(typeTable.getSimpleName(className),
				ReferenceType.TYPE, st, node, actions);
	}

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
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
