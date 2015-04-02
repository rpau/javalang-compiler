package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.walkmod.javalang.compiler.symbols.SymbolType;

public class CompatibleArgsPredicate implements TypeMappingPredicate<Method> {

	private SymbolType[] typeArgs;

	private Map<String, SymbolType> typeMapping;

	private Map<String, SymbolType> methodArgsMapping;

	public CompatibleArgsPredicate() {

	}

	public CompatibleArgsPredicate(SymbolType[] typeArgs) {
		this.typeArgs = typeArgs;

	}

	public void setTypeArgs(SymbolType[] typeArgs) {
		this.typeArgs = typeArgs;
	}

	public SymbolType[] getTypeArgs() {
		return typeArgs;
	}

	@Override
	public boolean filter(Method method) throws Exception {

		int numParams = typeArgs == null ? 0 : typeArgs.length;
		SymbolType lastVariableTypeArg = null;
		boolean isCompatible = true;
		if ((method.getParameterTypes().length == numParams)
				|| method.isVarArgs()) {

			// if I enter again, is that the previous method is not valid
			if (methodArgsMapping != null && !methodArgsMapping.isEmpty()) {
				// we restore the typeMapping values, to the original ones.
				for (String key : methodArgsMapping.keySet()) {
					typeMapping.remove(key);
				}
			}
			methodArgsMapping = new HashMap<String, SymbolType>();
			Type[] methodParameterTypes = method.getGenericParameterTypes();
			SymbolType[] methodArgs = new SymbolType[methodParameterTypes.length];

			for (int i = 0; i < methodParameterTypes.length; i++) {

				methodArgs[i] = SymbolType.valueOf(methodParameterTypes[i],
						typeArgs[i], methodArgsMapping, typeMapping);

			}
			if (method.isVarArgs()) {

				if (method.getParameterTypes().length < numParams) {

					lastVariableTypeArg = methodArgs[methodArgs.length - 1];

					numParams = method.getParameterTypes().length;
				}
				if (method.getParameterTypes().length <= numParams) {
					// changing the last argument to an array
					SymbolType[] newTypeArgs = new SymbolType[method
							.getParameterTypes().length];

					for (int i = 0; i < newTypeArgs.length - 1; i++) {
						newTypeArgs[i] = typeArgs[i];
					}

					newTypeArgs[newTypeArgs.length - 1] = lastVariableTypeArg
							.clone();
					newTypeArgs[newTypeArgs.length - 1]
							.setArrayCount(newTypeArgs[newTypeArgs.length - 1]
									.getArrayCount() - 1);
					typeArgs = newTypeArgs;
				}
			}

			for (int i = 0; i < numParams && isCompatible; i++) {

				isCompatible = typeArgs[i] == null
						|| methodArgs[i].isCompatible(typeArgs[i]);

			}

			if (isCompatible && lastVariableTypeArg != null) {

				for (int j = numParams; j < typeArgs.length && isCompatible; j++) {
					isCompatible = lastVariableTypeArg
							.isCompatible(typeArgs[j]);

				}

			}
		}else{
			isCompatible = false;
		}
		if (isCompatible && methodArgsMapping != null) {
			typeMapping.putAll(methodArgsMapping);
		}

		return isCompatible;
	}

	

	@Override
	public void setTypeMapping(Map<String, SymbolType> typeMapping) {
		this.typeMapping = typeMapping;
	}

	public Map<String, SymbolType> getTypeMapping() {
		return typeMapping;
	}

}
