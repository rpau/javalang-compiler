package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;

import org.walkmod.javalang.compiler.symbols.SymbolType;

public class CompatibleArgsPredicate extends AbstractCompatibleArgsPredicate
		implements TypeMappingPredicate<Method> {

	public CompatibleArgsPredicate() {
	}

	public CompatibleArgsPredicate(SymbolType[] typeArgs) {
		super(typeArgs);
	}

	@Override
	public boolean filter(Method method) throws Exception {
		setVarAgs(method.isVarArgs());
		setGenericParameterTypes(method.getGenericParameterTypes());
		setParameterTypesLenght(method.getParameterTypes().length);
		return super.filter();
	}

}
