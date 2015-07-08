/*
 Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 
Walkmod is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Walkmod is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public abstract class AbstractCompatibleFunctionalPredicate<T> {

	private VoidVisitor<T> typeResolver;
	private List<Expression> args;
	private T ctx = null;
	private SymbolType scope;
	private Map<String, SymbolType> typeMapping;

	private Class<?>[] params;
	private boolean isVarArgs;
	private SymbolTable symTable;
	private SymbolType[] calculatedTypeArgs;

	private AbstractCompatibleArgsPredicate previousPredicate;

	public AbstractCompatibleFunctionalPredicate(SymbolType scope,
			VoidVisitor<T> typeResolver, List<Expression> args, T ctx,
			SymbolTable symTable,
			AbstractCompatibleArgsPredicate previousPredicate,
			SymbolType[] calculatedTypeArgs) {
		this.typeResolver = typeResolver;
		this.args = args;
		this.ctx = ctx;
		this.scope = scope;
		this.symTable = symTable;
		this.previousPredicate = previousPredicate;
		this.calculatedTypeArgs = calculatedTypeArgs;
	}

	public Map<String, SymbolType> createMapping(Class<?> interfaceToInspect,
			SymbolType inferredArg, Executable method) throws Exception {
		Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();

		GenericsBuilderFromClassParameterTypes builder = new GenericsBuilderFromClassParameterTypes(
				typeMapping, scope, symTable);
		builder.build(method.getDeclaringClass());
		typeMapping = builder.getTypeMapping();

		Map<String, SymbolType> update = new HashMap<String, SymbolType>();
		if (inferredArg != null) {
			SymbolType.valueOf(interfaceToInspect, inferredArg, update, null);
		}
		Map<String, SymbolType> subset = new HashMap<String, SymbolType>();
		Set<String> keys = update.keySet();
		for (String key : keys) {
			SymbolType st1 = update.get(key);
			Class<?> clazz = st1.getClazz();
			if (Object.class.equals(clazz)) {
				SymbolType aux = typeMapping.get(key);
				if (aux != null) {
					subset.put(key, aux);
				} 
			}

		}
		update.clear();
		if (inferredArg != null) {
			SymbolType.valueOf(interfaceToInspect, inferredArg, update, subset);
		}
		update.putAll(subset);

		return update;
	}

	public boolean filter(LambdaExpr lambda, Class<?> interfaceToInspect,
			SymbolType inferredArg, Executable method) throws Exception {

		boolean found = false;

		Method[] methods = interfaceToInspect.getMethods();

		ArrayFilter<Method> filter = new ArrayFilter<Method>(methods);

		Map<String, SymbolType> typeMapping = createMapping(interfaceToInspect,
				inferredArg, method);
		CompatibleLambdaArgsPredicate predArgs = new CompatibleLambdaArgsPredicate(
				lambda);
		predArgs.setTypeMapping(typeMapping);

		filter.appendPredicate(
				new LambdaParamsTypeResolver(lambda, typeResolver, typeMapping))
				.appendPredicate(new AbstractMethodsPredicate())
				.appendPredicate(predArgs)
				.appendPredicate(
						new CompatibleLambdaResultPredicate<T>(lambda,
								typeResolver, ctx, typeMapping));
		found = filter.filterOne() != null;
		return found;
	}

	public boolean filter(MethodReferenceExpr methodRef,
			Class<?> interfaceToInspect, SymbolType inferredArg,
			Executable method) throws Exception {

		boolean found = false;
		Map<String, SymbolType> aux = createMapping(interfaceToInspect,
				inferredArg, method);
		aux.putAll(typeMapping);
		CompatibleMethodReferencePredicate<T, Method> predArgs = new CompatibleMethodReferencePredicate<T, Method>(
				methodRef, typeResolver, ctx, aux, symTable);

		Method[] methods = interfaceToInspect.getMethods();
		ArrayFilter<Method> filter = new ArrayFilter<Method>(methods);
		filter.appendPredicate(new AbstractMethodsPredicate());
		filter.appendPredicate(predArgs);
		found = filter.filterOne() != null;

		return found;
	}

	public boolean filter(Executable method) throws Exception {

		boolean found = false;
		boolean containsLambda = false;
		SymbolType[] inferredTypes = null;
		if (previousPredicate != null) {
			inferredTypes = previousPredicate.getInferredMethodArgs();
		}
		if (args != null && !args.isEmpty()) {
			Iterator<Expression> it = args.iterator();
			int i = 0;
			while (it.hasNext() && !found) {
				SymbolType argType = null;
				if (inferredTypes != null) {
					argType = inferredTypes[i];
				}
				Expression current = it.next();
				if (current instanceof LambdaExpr
						|| current instanceof MethodReferenceExpr) {

					containsLambda = true;
					Class<?> interfaceToInspect = null;

					if (params[i].isInterface()) {
						interfaceToInspect = params[i];

					} else if (isVarArgs && i == params.length - 1) {
						Class<?> componentType = params[i].getComponentType();
						if (componentType.isInterface()) {
							interfaceToInspect = componentType;
						}
					}
					if (interfaceToInspect != null) {
						if (current instanceof LambdaExpr) {
							found = filter((LambdaExpr) current,
									interfaceToInspect, argType, method);
							if(found){
								calculatedTypeArgs[i] = (SymbolType)current.getSymbolData();
							}
						} else {
							found = filter((MethodReferenceExpr) current,
									interfaceToInspect, argType, method);
							if(found){
								calculatedTypeArgs[i] = (SymbolType)current.getSymbolData();
							}
						}
					}

				}
				if (i < params.length - 1) {
					i++;
				}

			}
		}

		return (found && containsLambda) || !containsLambda;
	}

	public void setTypeMapping(Map<String, SymbolType> typeMapping) {
		this.typeMapping = typeMapping;
	}

	public Class<?>[] getParams() {
		return params;
	}

	public void setParams(Class<?>[] params) {
		this.params = params;
	}

	public boolean isVarArgs() {
		return isVarArgs;
	}

	public void setVarArgs(boolean isVarArgs) {
		this.isVarArgs = isVarArgs;
	}

}
