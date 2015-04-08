package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class GenericsBuilderFromConstructorParameterTypes extends
		AbstractGenericsBuilderFromParameterTypes implements
		TypeMappingBuilder<Constructor<?>> {

	public GenericsBuilderFromConstructorParameterTypes(
			Map<String, SymbolType> typeMapping, List<Expression> args,
			SymbolType[] typeArgs) {
		super(typeMapping, args, typeArgs);
	}

	public GenericsBuilderFromConstructorParameterTypes() {
	}

	@Override
	public Constructor<?> build(Constructor<?> obj) throws Exception {
		setTypes(obj.getGenericParameterTypes());
		super.build();
		return obj;
	}

}
