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

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;

public class LoadTypeDeclarationsAction extends SymbolAction {

	private TypesLoaderVisitor<?> typeTable;

	public LoadTypeDeclarationsAction(TypesLoaderVisitor<?> typeTable) {
		this.typeTable = typeTable;
	}

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
		Node node = symbol.getLocation();

		if (node instanceof TypeDeclaration
				|| node instanceof ObjectCreationExpr) {
			if (symbol.getName().equals("this")) {
				node.accept(typeTable, null);
			}
		}

	}
}
