package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.List;

import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.expr.LambdaExpr;

public class CompatibleLambdaArgsPredicate extends CompatibleArgsPredicate{

	private LambdaExpr lambda;
	
	public CompatibleLambdaArgsPredicate(LambdaExpr lambda) {
		this.lambda = lambda;
		
	}
	
	@Override
	public boolean filter(Method method) throws Exception {
		List<Parameter> lambdaParams = lambda.getParameters();
		Class<?>[] typeArgs = null;
		if (lambdaParams != null) {
			typeArgs = new Class<?>[lambdaParams.size()];
			ClassArrayFromSymTypeListBuilder<Parameter> typeArgsBuilder = 
					new ClassArrayFromSymTypeListBuilder<Parameter>(
					lambdaParams);
			typeArgsBuilder.build(typeArgs);
			setTypeArgs(typeArgs);
			return super.filter(method);
		}
		return true;
	}

	
}
