package org.walkmod.javalang.compiler;

public interface Builder<T> {

	public T build (T obj) throws Exception;
}
