package org.walkmod.javalang.compiler.symbols;

public interface SymbolAction {

	public void execute(Symbol symbol, SymbolTable table, SymbolEvent event) throws Exception;
}
