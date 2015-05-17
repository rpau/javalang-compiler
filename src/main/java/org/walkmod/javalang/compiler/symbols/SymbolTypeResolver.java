package org.walkmod.javalang.compiler.symbols;

import java.util.List;

public interface SymbolTypeResolver<T> {

	public SymbolType valueOf(T node);

	public SymbolType[] valueOf(List<T> nodes);
}
