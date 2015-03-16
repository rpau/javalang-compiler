package org.walkmod.javalang.compiler.symbols;

import java.util.List;

import org.walkmod.javalang.ast.Node;

public interface SymbolFactory {

	public Symbol create(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location);

	public Symbol create(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, SymbolAction action);

	public Symbol create(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, List<SymbolAction> actions);
}
