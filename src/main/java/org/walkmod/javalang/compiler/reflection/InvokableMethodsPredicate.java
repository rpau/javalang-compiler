package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;

import org.walkmod.javalang.compiler.Predicate;

public class InvokableMethodsPredicate implements Predicate<Method>{

	@Override
	public boolean filter(Method elem) {
		return !elem.isBridge() && !elem.isSynthetic();
	}

}
