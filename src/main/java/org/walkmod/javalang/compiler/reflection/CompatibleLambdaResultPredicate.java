package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleLambdaResultPredicate<A> implements Predicate<Method> {

	private LambdaExpr expr;

	private VoidVisitor<A> typeResolver;
	
	private A ctx;

	public CompatibleLambdaResultPredicate(LambdaExpr expr,
			VoidVisitor<A> typeResolver, A ctx) {
		this.expr = expr;
		this.typeResolver = typeResolver;
		this.ctx = ctx;
	}

	@Override
	public boolean filter(Method method) throws Exception {
		SymbolData returnType = expr.getBody().getSymbolData();
		if (returnType == null) {
			try {
				expr.accept(typeResolver, ctx);
			} catch (Exception e) {
				return false;
			}
			returnType = expr.getBody().getSymbolData();
			if (returnType == null) {
				returnType = new SymbolType(void.class);
			}
		}
		SymbolType st = new SymbolType(method.getReturnType());
		boolean isCompatible = (st.isCompatible((SymbolType) returnType));
		if(isCompatible){
			expr.setSymbolData(new SymbolType(method.getDeclaringClass()));
		}
		return isCompatible;
	}

}
