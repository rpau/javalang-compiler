package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.GenericArrayType;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class SymbolDataOfMethodReferenceBuilder<T> implements TypeMappingBuilder<MethodCallExpr> {

	private Map<String, SymbolType> typeMapping;
	
	private VoidVisitor<T> visitor;
	
	private T ctxt;
	
	public  SymbolDataOfMethodReferenceBuilder(Map<String, SymbolType> typeMapping, VoidVisitor<T> visitor, T ctxt){
		this.typeMapping = typeMapping;
		this.visitor = visitor;
		this.ctxt = ctxt;
	}
	
	@Override
	public MethodCallExpr build(MethodCallExpr n) throws Exception {
		List<Expression> args = n.getArgs();
		MethodSymbolData st =  n.getSymbolData();
		if (args != null) {
			int i = 0;
			java.lang.reflect.Type[] argClasses = st.getMethod()
					.getGenericParameterTypes();
			int paramCount = st.getMethod().getParameterTypes().length;
			for (Expression argument : args) {
				if (argument instanceof MethodReferenceExpr) {
					SymbolType aux = null;
					if (i < paramCount) {
						aux = SymbolType.valueOf(argClasses[i],
								typeMapping);

					} else {
						java.lang.reflect.Type componentType = null;
						java.lang.reflect.Type lastArg = argClasses[argClasses.length - 1];
						if (lastArg instanceof Class<?>) {
							componentType = ((Class<?>) lastArg)
									.getComponentType();
						} else if (lastArg instanceof GenericArrayType) {
							componentType = ((GenericArrayType) lastArg)
									.getGenericComponentType();
						}
						aux = SymbolType.valueOf(componentType,
								typeMapping);
					}
					argument.setSymbolData(aux);
					argument.accept(visitor, ctxt);
				}
				i++;
			}
		}
		return null;
	}

	@Override
	public void setTypeMapping(Map<String, SymbolType> typeMapping) {
		this.typeMapping = typeMapping;
	}

}
