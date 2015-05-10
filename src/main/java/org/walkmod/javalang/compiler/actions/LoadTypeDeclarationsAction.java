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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class LoadTypeDeclarationsAction extends SymbolAction {

	private SymbolActionProvider actionProvider;

	public LoadTypeDeclarationsAction(
			SymbolActionProvider actionProvider) {
		
		this.actionProvider = actionProvider;
	}

	private void update(Symbol<?> symbol, TypeDeclaration node,
			SymbolTable table) throws Exception {

		String className = symbol.getType().getClazz().getName() + "$"
				+ node.getName();
		List<SymbolAction> actions = null;
		if (actionProvider != null) {
			actions = actionProvider.getActions(node);
		}
		SymbolType st = new SymbolType();
		st.setName(className);

		node.setSymbolData(st);
		table.pushSymbol(node.getName(), ReferenceType.TYPE, st, node, actions);
	}

	private void getInnerClasses(Class<?> clazz, Set<Class<?>> result) {
		if (clazz != null && !clazz.equals(Object.class)) {
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null) {
				Class<?>[] decClasses = superClass.getDeclaredClasses();
				for (int i = 0; i < decClasses.length; i++) {
					result.add(decClasses[i]);
				}
			}
			getInnerClasses(superClass, result);
			Class<?>[] interfaces = clazz.getInterfaces();
			for (int i = 0; i < interfaces.length; i++) {
				Class<?>[] decClasses = interfaces[i].getDeclaredClasses();
				for (int j = 0; j < decClasses.length; j++) {
					result.add(decClasses[j]);
				}
				getInnerClasses(interfaces[i].getSuperclass(), result);
			}
		}
	}

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
		Node node = symbol.getLocation();

		if (node instanceof TypeDeclaration
				|| node instanceof ObjectCreationExpr) {
			if (symbol.getName().equals("this")) {
				Class<?> clazz = symbol.getType().getClazz();
				Set<Class<?>> inheritedTypes = new HashSet<Class<?>>();
				getInnerClasses(clazz, inheritedTypes);
				for (Class<?> inheritedType : inheritedTypes) {
					table.pushSymbol(inheritedType.getSimpleName(),
							ReferenceType.TYPE,
							SymbolType.valueOf(inheritedType, null), null);
				}

				List<BodyDeclaration> members = null;
				if (node instanceof TypeDeclaration) {
					TypeDeclaration n = (TypeDeclaration) node;
					members = n.getMembers();

				} else {
					ObjectCreationExpr n = (ObjectCreationExpr) node;
					members = n.getAnonymousClassBody();
				}
				if (members != null) {

					for (BodyDeclaration member : members) {
						if (member instanceof TypeDeclaration) {
							update(symbol, (TypeDeclaration) member, table);
						}
					}
				}
			}
		}

	}
}
