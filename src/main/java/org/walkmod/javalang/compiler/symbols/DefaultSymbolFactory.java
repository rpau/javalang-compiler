package org.walkmod.javalang.compiler.symbols;

import java.util.List;

import org.walkmod.javalang.ast.Node;

public class DefaultSymbolFactory implements SymbolFactory {

	@Override
	public Symbol create(String symbolName, ReferenceType referenceType,
			SymbolType type, Node location) {
		return new Symbol(symbolName, type, location, referenceType);
	}

	@Override
	public Symbol create(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, SymbolAction action) {
		return new Symbol(symbolName, symbolType, location, referenceType,
				action);
	}

	@Override
	public Symbol create(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, List<SymbolAction> actions) {
		return new Symbol(symbolName, symbolType, location, referenceType,
				actions);
	}

}
