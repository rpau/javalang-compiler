package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.ExpressionTypeAnalyzer;
import org.walkmod.javalang.compiler.types.Types;

public class RequiredMethodPredicate<A extends Map<String, Object>> implements
		Predicate<Class<?>> {

	private MethodCallExpr requiredMethod;

	private ExpressionTypeAnalyzer<A> visitor;

	private A ctx;

	public RequiredMethodPredicate(MethodCallExpr requiredMethod,
			ExpressionTypeAnalyzer<A> visitor, A ctx) {
		this.requiredMethod = requiredMethod;
		this.visitor = visitor;
		this.ctx = ctx;
	}

	@Override
	public boolean filter(Class<?> candidate) {
		boolean isCompatible = true;

		List<Method> methods = new LinkedList<Method>();

		boolean returnTypeCompatible = false;
		methods.addAll(Arrays.asList(candidate.getDeclaredMethods()));

		methods.addAll(Arrays.asList(candidate.getMethods()));
		Iterator<Method> it = methods.iterator();

		while (it.hasNext() && !returnTypeCompatible) {

			Method currentMethod = it.next();

			// checking method name
			if (currentMethod.getName().equals(requiredMethod.getName())) {
				List<Expression> args = requiredMethod.getArgs();
				Class<?>[] parameterTypes = currentMethod.getParameterTypes();
				if (args != null) {
					boolean compatibleArgs = true;
					int k = 0;

					for (Expression argExpr : args) {
						argExpr.accept(visitor, ctx);
						SymbolType typeArg = (SymbolType) argExpr.getSymbolData();
						if (!Types.isCompatible(typeArg.getClazz(),
								parameterTypes[k])) {
							compatibleArgs = false;
						}
						k++;
					}
					returnTypeCompatible = compatibleArgs;
				} else {
					returnTypeCompatible = true;
				}
			}
		}

		isCompatible = returnTypeCompatible;

		return isCompatible;
	}
}
