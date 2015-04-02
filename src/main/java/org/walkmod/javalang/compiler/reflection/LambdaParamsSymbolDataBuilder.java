package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class LambdaParamsSymbolDataBuilder extends
		FunctionalGenericsBuilder<LambdaExpr> {

	public LambdaParamsSymbolDataBuilder(Method method,
			VoidVisitor<?> typeResolver, Map<String, SymbolType> mapping)
			throws Exception {
		super(method, typeResolver, mapping);
	}

	@Override
	public LambdaExpr build(LambdaExpr lambda) throws Exception {
		super.build(lambda);
		List<Parameter> lambdaParams = lambda.getParameters();
		if (lambdaParams != null) {
			Iterator<Parameter> lambdaIt = lambdaParams.iterator();
			int k = 0;
			SymbolType[] args = getArgs();
			VoidVisitor<?> typeResolver = getTypeResolver();
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
