package org.walkmod.javalang.compiler.actions;

import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class LoadEnumConstantLiteralsAction extends SymbolAction {
	
	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
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

	@Override
	public void doPop(Symbol<?> symbol, SymbolTable table) throws Exception {
	}

	@Override
	public void doRead(Symbol<?> symbol, SymbolTable table,
			SymbolReference reference) throws Exception {
	}

	@Override
	public void doWrite(Symbol<?> symbol, SymbolTable table,
			SymbolReference reference) throws Exception {
	}
}
