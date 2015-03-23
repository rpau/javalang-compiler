package org.walkmod.javalang.compiler.reflection;

import java.util.Map;

import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public interface TypeMappingPredicate<T> extends Predicate<T>{

	public void setTypeMapping(Map<String, SymbolType> typeMapping);
}
