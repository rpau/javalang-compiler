package org.walkmod.javalang.compiler.symbols;

public interface SymbolGenerator {
	
	public void setSymbolTable(SymbolTable symbolTable);
	
	public Symbol generateSymbol(SymbolType typeName);
}
