package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class LambdaParamsSymbolDataBuilder implements Builder<LambdaExpr> {

	private SymbolType[] args;

	private VoidVisitor<?> typeResolver;

	public LambdaParamsSymbolDataBuilder(Method method,
			VoidVisitor<?> typeResolver, Map<String, SymbolType> mapping)
			throws Exception {
		java.lang.reflect.Type[] generics = method.getGenericParameterTypes();
		args = new SymbolType[generics.length];
		for (int i = 0; i < generics.length; i++) {
			args[i] = SymbolType.valueOf(generics[i], mapping);
		}

		this.typeResolver = typeResolver;
	}

	@Override
	public LambdaExpr build(LambdaExpr lambda) throws Exception {
		List<Parameter> lambdaParams = lambda.getParameters();
		if (lambdaParams != null) {
			Iterator<Parameter> lambdaIt = lambdaParams.iterator();
			int k = 0;
			while (lambdaIt.hasNext()) {
				Parameter p = lambdaIt.next();
				Type type = p.getType();
				if (type == null) {
					if (k < args.length) {
						p.setSymbolData(args[k]);
					}
				} else {
					SymbolData sd = type.getSymbolData();
					if (sd == null) {
						type.accept(typeResolver, null);
						sd = type.getSymbolData();
						p.setSymbolData(sd);
					}
				}
				k++;
			}
		}
		return lambda;
	}

}
