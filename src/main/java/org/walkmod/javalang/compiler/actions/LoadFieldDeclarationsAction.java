package org.walkmod.javalang.compiler.actions;

import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;

public class LoadFieldDeclarationsAction implements SymbolAction {

	private TypeTable<?> typeTable;

	private SymbolActionProvider actionProvider;

	public LoadFieldDeclarationsAction(TypeTable<?> typeTable,
			SymbolActionProvider actionProvider) {
		this.typeTable = typeTable;
		this.actionProvider = actionProvider;
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
						if (member instanceof FieldDeclaration) {

							FieldDeclaration fd = (FieldDeclaration) member;
							Type type = fd.getType();
							List<SymbolAction> actions = null;
							if (actionProvider != null) {
								actions = actionProvider.getActions(fd);
							}
							SymbolType resolvedType = typeTable.valueOf(type);
							resolvedType.setClazz(typeTable
									.loadClass(resolvedType));

							for (VariableDeclarator var : fd.getVariables()) {
								SymbolType symType = resolvedType.clone();
								symType.setArrayCount(var.getId()
										.getArrayCount());

								table.pushSymbol(var.getId().getName(),ReferenceType.VARIABLE,
										symType, fd, actions);
							}

						}
					}
				}
			}
		}
	}
}
