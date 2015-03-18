package org.walkmod.javalang.compiler.actions;

import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class LoadEnumConstantLiteralsAction implements SymbolAction {

	@Override
	public void execute(Symbol symbol, SymbolTable table, SymbolEvent event)
			throws Exception {
		if (event.equals(SymbolEvent.PUSH)) {
			Node node = symbol.getLocation();
			if (node instanceof EnumDeclaration) {
				EnumDeclaration ed = (EnumDeclaration) node;
				List<EnumConstantDeclaration> entries = ed.getEntries();
				for (EnumConstantDeclaration ecd : entries) {
					table.pushSymbol(ecd.getName(), ReferenceType.ENUM_LITERAL,
							symbol.getType(), ecd);
				}
			}
		}
	}
}
