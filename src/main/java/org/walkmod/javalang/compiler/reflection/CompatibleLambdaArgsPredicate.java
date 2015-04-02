package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.List;

import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class CompatibleLambdaArgsPredicate extends CompatibleArgsPredicate{

	private LambdaExpr lambda;
	
	public CompatibleLambdaArgsPredicate(LambdaExpr lambda) {
		this.lambda = lambda;
		
	}
	
	@Override
	public boolean filter(Method method) throws Exception {
		List<Parameter> lambdaParams = lambda.getParameters();

		if (lambdaParams != null) {
			
			SymbolType[] typeArgs = new SymbolType[lambdaParams.size()];
			int i = 0;
			for(Parameter param: lambdaParams){
				typeArgs[i] = (SymbolType)param.getSymbolData();
			}
			
			setTypeArgs(typeArgs);
			return super.filter(method);
		}
		return true;
	}

	
}
