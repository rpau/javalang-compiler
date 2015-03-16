package org.walkmod.javalang.compiler.symbols;

import java.util.List;

import org.walkmod.javalang.ast.Node;

public class MultiTypeSymbol extends Symbol{
	
	private List<SymbolType> types;

	public MultiTypeSymbol(String name, SymbolType type, Node location) {
		super(name, type, location);
	}
	
	public MultiTypeSymbol(String name, List<SymbolType> types, Node location){
		super(name, types.get(0), location);
		this.types = types;
	}
	
	public MultiTypeSymbol(String name, List<SymbolType> types, Node location, List<SymbolAction> actions){
		super(name, types.get(0), location, ReferenceType.VARIABLE, actions);
		this.types = types;
	}
	
	public List<SymbolType> getTypes(){
		return types;
	}

}
