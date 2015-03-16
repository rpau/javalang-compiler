package org.walkmod.javalang.compiler.symbols;


public class DefaultSymbolGenerator implements SymbolGenerator{

	private int symbolCounter = 1;
	
	private SymbolTable symbolTable;
	
	@Override
	public Symbol generateSymbol(SymbolType typeName) {
		
		String sname = "v" + symbolCounter;
		SymbolType type = symbolTable.getType(sname, ReferenceType.VARIABLE);
		if (type != null) {
			symbolCounter++;
			return generateSymbol(typeName);
		}

		Symbol result = new Symbol(sname, null, null);

		return result;
	}
	@Override
	public void setSymbolTable(SymbolTable symbolTable) {
		this.symbolTable = symbolTable;
	}

}
