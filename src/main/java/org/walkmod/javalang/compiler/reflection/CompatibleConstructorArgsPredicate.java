package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Constructor;

import org.walkmod.javalang.compiler.symbols.SymbolType;

public class CompatibleConstructorArgsPredicate extends
		AbstractCompatibleArgsPredicate implements
		TypeMappingPredicate<Constructor<?>> {

	public CompatibleConstructorArgsPredicate(){}
	
	public CompatibleConstructorArgsPredicate(SymbolType[] typeArgs){
		super(typeArgs);
	}

	public boolean filter(Constructor<?> method) throws Exception {
		setVarAgs(method.isVarArgs());
		setGenericParameterTypes(method.getGenericParameterTypes());
		setParameterTypesLenght(method.getParameterTypes().length);
		return super.filter();
	}
}
