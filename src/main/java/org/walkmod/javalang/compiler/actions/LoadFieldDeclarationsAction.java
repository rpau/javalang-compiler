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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.reflection.FieldInspector;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.InvalidTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class LoadFieldDeclarationsAction extends SymbolAction {

	private SymbolActionProvider actionProvider;

	public LoadFieldDeclarationsAction(SymbolActionProvider actionProvider) {

		this.actionProvider = actionProvider;
	}

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {

		if (symbol.getName().equals("this")) {
			Node node = symbol.getLocation();
			if (node instanceof TypeDeclaration
					|| node instanceof ObjectCreationExpr) {
				node.accept(new FieldsPopulator<Object>(table), null);
			}

		}
	}

	private class FieldsPopulator<T> extends VoidVisitorAdapter<T> {

		private SymbolTable table;

		public FieldsPopulator(SymbolTable table) {
			this.table = table;
		}

		@Override
		public void visit(ObjectCreationExpr o, T ctx) {
			loadFields(o.getAnonymousClassBody());
		}

		@Override
		public void visit(ClassOrInterfaceDeclaration n, T ctx) {
			loadFields(n.getMembers());
			loadExtendsOrImplements(n.getExtends());
			loadExtendsOrImplements(n.getImplements());
		}

		@Override
		public void visit(EnumDeclaration n, T ctx) {
			loadFields(n.getMembers());
			loadExtendsOrImplements(n.getImplements());
		}

		@Override
		public void visit(AnnotationDeclaration n, T ctx) {
			loadFields(n.getMembers());
		}

		public void loadExtendsOrImplements(
				List<ClassOrInterfaceType> extendsList) {
			if (extendsList != null) {
				for (ClassOrInterfaceType type : extendsList) {
					String name = type.getName();
					ClassOrInterfaceType scope = type.getScope();
					if (scope != null) {
						name = scope.toString() + "." + name;
					}
					Symbol<?> s = table.findSymbol(name, ReferenceType.TYPE);
					if (s != null) {
						Object location = s.getLocation();
						if (location != null
								&& location instanceof TypeDeclaration) {

							((TypeDeclaration) location).accept(this, null);

						} else {
							Class<?> clazz = s.getType().getClazz();
							Set<Field> fields = FieldInspector
									.getNonPrivateFields(clazz);
							for (Field field : fields) {
								try {
									table.pushSymbol(field.getName(),
											ReferenceType.VARIABLE, SymbolType
													.valueOf(field
															.getGenericType(),
															null), null);
								} catch (InvalidTypeException e) {
									throw new RuntimeException(e);
								}
							}
						}
					}
				}
			}
		}

		public void loadFields(List<BodyDeclaration> members) {

			if (members != null) {

				for (BodyDeclaration member : members) {
					if (member instanceof FieldDeclaration) {

						FieldDeclaration fd = (FieldDeclaration) member;
						Type type = fd.getType();
						List<SymbolAction> actions = null;
						if (actionProvider != null) {
							actions = actionProvider.getActions(fd);
						}
						SymbolType resolvedType = ASTSymbolTypeResolver
								.getInstance().valueOf(type);

						if (resolvedType == null) {
							resolvedType = new SymbolType(Object.class);
						}
						type.setSymbolData(resolvedType);

						for (VariableDeclarator var : fd.getVariables()) {
							SymbolType symType = resolvedType.clone();
							if (symType.getArrayCount() == 0) {
								symType.setArrayCount(var.getId()
										.getArrayCount());
							}
							table.pushSymbol(var.getId().getName(),
									ReferenceType.VARIABLE, symType, var,
									actions);
						}

					}
				}

			}
		}
	}
}
