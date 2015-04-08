package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.List;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleFunctionalMethodPredicate<T> extends
		AbstractCompatibleFunctionalPredicate<T> implements
		TypeMappingPredicate<Method> {

	public CompatibleFunctionalMethodPredicate(SymbolType scope,
			VoidVisitor<T> typeResolver, List<Expression> args, T ctx) {
		super(scope, typeResolver, args, ctx);
	}

	@Override
	public boolean filter(Method elem) throws Exception {

		Class<?>[] params = elem.getParameterTypes();
		setParams(params);
		setVarArgs(elem.isVarArgs());
		return super.filter();
	}

}
