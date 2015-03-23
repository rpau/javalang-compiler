package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;

import org.walkmod.javalang.compiler.Predicate;

public class MethodsByNamePredicate implements Predicate<Method>{

	private String name;
	
	public MethodsByNamePredicate(String name){
		this.name = name;
	}
	
	@Override
	public boolean filter(Method elem) {
		return elem.getName().equals(name);
	}

}
