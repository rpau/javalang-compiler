package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.SymbolType;

/**
 * For a given generic letter (K, M, T,..) resolves if the contained 
 * class has a value for that letter
 * @author rpau
 *
 */
public class GenericBuilderFromGenericClasses implements
		Builder<Map<String, SymbolType>> {

	private Class<?> clazz;

	private List<SymbolType> parameterizedTypes;

	public GenericBuilderFromGenericClasses(Class<?> clazz,
			List<SymbolType> parameterizedTypes) {
		this.clazz = clazz;
		this.parameterizedTypes = parameterizedTypes;
	}
	
	public GenericBuilderFromGenericClasses(){
		
	}
	
	

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public void setParameterizedTypes(List<SymbolType> parameterizedTypes) {
		this.parameterizedTypes = parameterizedTypes;
	}

	@Override
	public Map<String, SymbolType> build(Map<String, SymbolType> obj) {
		if (obj == null) {
			obj = new HashMap<String, SymbolType>();
		}
		TypeVariable<?>[] typeParams = clazz.getTypeParameters();

		if (typeParams != null) {

			for (int i = 0; i < typeParams.length; i++) {
				if (parameterizedTypes != null) {
					obj.put(typeParams[i].getName(), parameterizedTypes.get(i));
				} else {
					obj.put(typeParams[i].getName(), new SymbolType("java.lang.Object"));
				}
			}
		}
		return obj;
	}

}
