package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Constructor;
import java.util.List;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleFunctionalConstructorPredicate<T> extends
		AbstractCompatibleFunctionalPredicate<T> implements
		TypeMappingPredicate<Constructor<?>> {

	public CompatibleFunctionalConstructorPredicate(SymbolType scope,
			VoidVisitor<T> typeResolver, List<Expression> args, T ctx) {
		super(scope, typeResolver, args, ctx);
	}

	@Override
	public boolean filter(Constructor<?> elem) throws Exception {
		Class<?>[] params = elem.getParameterTypes();
		setParams(params);
		setVarArgs(elem.isVarArgs());
		return super.filter();
	}

}
