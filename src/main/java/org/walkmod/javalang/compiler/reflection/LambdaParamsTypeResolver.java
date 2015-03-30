package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class LambdaParamsTypeResolver implements Predicate<Method> {

	private LambdaExpr lambda;
	
	private  VoidVisitor<?> typeResolver;
	
	private Map<String, SymbolType> mapping;

	public LambdaParamsTypeResolver(LambdaExpr expr, VoidVisitor<?> typeResolver, Map<String, SymbolType> mapping) {
		this.lambda = expr;
		this.typeResolver = typeResolver;
		this.mapping = mapping;
	}

	@Override
	public boolean filter(Method elem) throws Exception {
		

		LambdaParamsSymbolDataBuilder builder = new LambdaParamsSymbolDataBuilder(
				elem, typeResolver, mapping);
		builder.build(lambda);
		
		return true;
	}

}
