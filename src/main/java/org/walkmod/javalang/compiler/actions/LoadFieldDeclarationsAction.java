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
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;

public class LoadFieldDeclarationsAction extends SymbolAction {

	private TypeTable<?> typeTable;

	private SymbolActionProvider actionProvider;

	public LoadFieldDeclarationsAction(TypeTable<?> typeTable,
			SymbolActionProvider actionProvider) {
		this.typeTable = typeTable;
		this.actionProvider = actionProvider;
	}

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
		Node node = symbol.getLocation();

		List<BodyDeclaration> members = null;

		if (node instanceof TypeDeclaration) {
			TypeDeclaration n = (TypeDeclaration) node;
			members = n.getMembers();
		} else if (node instanceof ObjectCreationExpr) {
			members = ((ObjectCreationExpr) node).getAnonymousClassBody();
		}

		if (members != null) {

			for (BodyDeclaration member : members) {
				if (member instanceof FieldDeclaration) {

					FieldDeclaration fd = (FieldDeclaration) member;
					Type type = fd.getType();
					List<SymbolAction> actions = null;
					if (actionProvider != null) {
						actions = actionProvider.getActions(fd);
					}
					SymbolType resolvedType = typeTable.valueOf(type);
					resolvedType.setClazz(typeTable.loadClass(resolvedType));
					type.setSymbolData(resolvedType);

					for (VariableDeclarator var : fd.getVariables()) {
						SymbolType symType = resolvedType.clone();
						if (symType.getArrayCount() == 0) {
							symType.setArrayCount(var.getId().getArrayCount());
						}
						table.pushSymbol(var.getId().getName(),
								ReferenceType.VARIABLE, symType, var, actions);
					}

				}
			}

		}
	}
}
