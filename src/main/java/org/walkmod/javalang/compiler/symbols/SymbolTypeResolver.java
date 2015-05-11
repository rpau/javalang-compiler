package org.walkmod.javalang.compiler.symbols;

public interface SymbolTypeResolver<T> {

	public SymbolType valueOf(T node);
}
