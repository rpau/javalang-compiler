package org.walkmod.javalang.compiler;

public interface Predicate <T>{
	
	public boolean filter(T elem) throws Exception;
	
}
