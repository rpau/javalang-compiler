package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;

import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.types.Types;

public class CompatibleArgsPredicate implements Predicate<Method> {

	private Class<?>[] typeArgs;
	
	public CompatibleArgsPredicate(){
		
	}

	public CompatibleArgsPredicate(Class<?>[] typeArgs) {
		this.typeArgs = typeArgs;

	}
	
	public void setTypeArgs(Class<?>[] typeArgs){
		this.typeArgs = typeArgs;
	}

	@Override
	public boolean filter(Method method) throws Exception {
		int numParams = typeArgs == null ? 0 : typeArgs.length;
		Class<?> lastVariableTypeArg = null;
		boolean isCompatible = true;
		if ((method.getParameterTypes().length == numParams)
				|| method.isVarArgs()) {

			if (method.isVarArgs()) {

				if (method.getParameterTypes().length < numParams) {

					lastVariableTypeArg = method.getParameterTypes()[method
							.getParameterTypes().length - 1];

					numParams = method.getParameterTypes().length;
				}
				if (method.getParameterTypes().length <= numParams) {
					// changing the last argument to an array
					Class<?>[] newTypeArgs = new Class<?>[method
							.getParameterTypes().length];

					for (int i = 0; i < newTypeArgs.length - 1; i++) {
						newTypeArgs[i] = typeArgs[i];
					}

					newTypeArgs[newTypeArgs.length - 1] = method
							.getParameterTypes()[method.getParameterTypes().length - 1];

					typeArgs = newTypeArgs;
				}
			}
			Class<?>[] methodParameterTypes = method.getParameterTypes();
			int i = 0;
			for (i = 0; i < numParams && isCompatible; i++) {

				isCompatible = Types.isCompatible(typeArgs[i],
						methodParameterTypes[i]);

			}

			if (isCompatible && lastVariableTypeArg != null) {
				int j = numParams;
				for (; j < typeArgs.length && isCompatible; j++) {
					isCompatible = Types.isCompatible(typeArgs[j],
							lastVariableTypeArg);

				}

			}
		}
		return isCompatible;
	}

}
