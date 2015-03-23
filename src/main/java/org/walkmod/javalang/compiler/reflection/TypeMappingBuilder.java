package org.walkmod.javalang.compiler.reflection;

import java.util.Map;

import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public interface TypeMappingBuilder<T> extends Builder<T> {

	public void setTypeMapping(Map<String, SymbolType> typeMapping);
}
