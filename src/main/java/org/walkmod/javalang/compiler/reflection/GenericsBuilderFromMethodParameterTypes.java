package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class GenericsBuilderFromMethodParameterTypes extends
		AbstractGenericsBuilderFromParameterTypes implements
		TypeMappingBuilder<Method> {

	public GenericsBuilderFromMethodParameterTypes(
			Map<String, SymbolType> typeMapping, List<Expression> args,
			SymbolType[] typeArgs) {
		super(typeMapping, args, typeArgs);
	}

	public GenericsBuilderFromMethodParameterTypes() {
	}

	@Override
	public Method build(Method method) throws Exception {
		setTypes(method.getGenericParameterTypes());
		super.build();
		return method;
	}

}
