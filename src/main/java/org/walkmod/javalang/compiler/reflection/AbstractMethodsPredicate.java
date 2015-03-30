package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.walkmod.javalang.compiler.Predicate;

public class AbstractMethodsPredicate implements Predicate<Method>{

	@Override
	public boolean filter(Method elem) throws Exception {
	
		return Modifier.isAbstract(elem.getModifiers());
	}

}
