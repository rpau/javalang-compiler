package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleLambdasPredicate<T> implements Predicate<Method> {

	private VoidVisitor<T> typeResolver;
	private List<Expression> args;
	private T ctx = null;
	private SymbolType scope;
	

	public CompatibleLambdasPredicate(SymbolType scope, VoidVisitor<T> typeResolver,
			List<Expression> args, T ctx) {
		this.typeResolver = typeResolver;
		this.args = args;
		this.ctx = ctx;
		this.scope = scope;
	}

	@Override
	public boolean filter(Method elem) throws Exception {

		Class<?>[] params = elem.getParameterTypes();

		boolean found = false;
		boolean containsLambda = false;
		if (args != null && !args.isEmpty()) {
			Iterator<Expression> it = args.iterator();
			int i = 0;
			while (it.hasNext() && !found) {
				Expression current = it.next();
				if (current instanceof LambdaExpr) {
					containsLambda = true;
					LambdaExpr lambda = (LambdaExpr) current;
					Class<?> interfaceToInspect = null;

					if (params[i].isInterface()) {
						interfaceToInspect = params[i];

					} else if (elem.isVarArgs() && i == params.length - 1) {
						Class<?> componentType = params[i].getComponentType();
						if (componentType.isInterface()) {
							interfaceToInspect = componentType;
						}
					}
					if (interfaceToInspect != null) {
						Method[] methods = interfaceToInspect.getMethods();

						TypeVariable<?>[] generics = interfaceToInspect.getTypeParameters();
						Map<String, SymbolType> mapping = new HashMap<String, SymbolType>();
						List<SymbolType> parameterizedTypes = scope.getParameterizedTypes();
						for(int j = 0; j < generics.length; j++){
							mapping.put(generics[j].getName(), parameterizedTypes.get(j));
						}
						
						// it resolved the symbol data of the used parameters

						ArrayFilter<Method> filter = new ArrayFilter<Method>(
								methods);

						filter.appendPredicate(
								new LambdaParamsTypeResolver(lambda,
										typeResolver, mapping))
								.appendPredicate(new AbstractMethodsPredicate())
								.appendPredicate(
										new CompatibleLambdaArgsPredicate(
												lambda))
								.appendPredicate(
										new CompatibleLambdaResultPredicate<T>(
												lambda, typeResolver, ctx));
						found = filter.filterOne() != null;
					}

				}
				if (i < params.length - 1) {
					i++;
				}

			}
		}

		return (found && containsLambda) || !containsLambda;
	}

}
