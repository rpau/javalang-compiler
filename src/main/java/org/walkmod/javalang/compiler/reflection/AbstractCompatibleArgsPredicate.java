/*
 Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 
Walkmod is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Walkmod is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import org.walkmod.javalang.compiler.symbols.SymbolType;

public abstract class AbstractCompatibleArgsPredicate {

	private SymbolType[] typeArgs;

	private Map<String, SymbolType> typeMapping;

	private Map<String, SymbolType> methodArgsMapping;

	private Type[] genericParameterTypes;

	private boolean isVarAgs;

	private int paramsCount;

	public AbstractCompatibleArgsPredicate() {
	}

	public AbstractCompatibleArgsPredicate(SymbolType[] typeArgs) {
		this.typeArgs = typeArgs;
	}
	

	public boolean filter() throws Exception {
		int numParams = typeArgs == null ? 0 : typeArgs.length;
		SymbolType lastVariableTypeArg = null;
		SymbolType[] newTypeArgs = typeArgs;
		boolean isCompatible = true;
		if ((paramsCount == numParams)
				|| (isVarAgs && numParams >= (paramsCount - 1))) {

			// if I enter again, is that the previous method is not valid
			if (methodArgsMapping != null && !methodArgsMapping.isEmpty() && typeMapping != null) {
				// we restore the typeMapping values, to the original ones.
				for (String key : methodArgsMapping.keySet()) {
					typeMapping.remove(key);
				}
			}
			methodArgsMapping = new HashMap<String, SymbolType>();
			SymbolType[] methodArgs = new SymbolType[genericParameterTypes.length];

			for (int i = 0; i < genericParameterTypes.length && i < numParams; i++) {
				methodArgs[i] = SymbolType.valueOf(genericParameterTypes[i],
						typeArgs[i], methodArgsMapping, typeMapping);

			}
			if (isVarAgs) {

				if (paramsCount <= numParams) {
					if (numParams == (paramsCount - 1)) {
						lastVariableTypeArg = SymbolType.valueOf(
								genericParameterTypes[paramsCount - 1],
								typeMapping);
					} else {
						lastVariableTypeArg = methodArgs[methodArgs.length - 1];
					}
					numParams = paramsCount;

					// changing the last argument to an array
					newTypeArgs = new SymbolType[paramsCount];

					for (int i = 0; i < newTypeArgs.length - 1
							&& i < typeArgs.length; i++) {
						newTypeArgs[i] = typeArgs[i];
					}

					if (methodArgs.length == numParams && newTypeArgs[numParams-1] != null) {
						if (methodArgs[numParams - 1].getArrayCount() != newTypeArgs[numParams - 1]
								.getArrayCount()) {
							newTypeArgs[newTypeArgs.length - 1] = lastVariableTypeArg
									.clone();
							newTypeArgs[newTypeArgs.length - 1]
									.setArrayCount(newTypeArgs[newTypeArgs.length - 1]
											.getArrayCount() - 1);
						}
					}

				} else {
					if (methodArgs.length > 0 && methodArgs[methodArgs.length-1] != null) {
						methodArgs[methodArgs.length-1]
								.setArrayCount(methodArgs[methodArgs.length-1]
										.getArrayCount() - 1);
					}
				}
			}

			for (int i = 0; i < numParams && isCompatible; i++) {

				isCompatible = newTypeArgs[i] == null
						|| methodArgs[i].isCompatible(newTypeArgs[i]);

			}

			if (isCompatible && lastVariableTypeArg != null) {

				for (int j = numParams; j < newTypeArgs.length && isCompatible; j++) {
					isCompatible = lastVariableTypeArg
							.isCompatible(newTypeArgs[j]);

				}

			}
		} else {
			isCompatible = false;
		}
		if (isCompatible && methodArgsMapping != null && typeMapping != null) {
			typeMapping.putAll(methodArgsMapping);
		}

		return isCompatible;
	}

	public void setTypeArgs(SymbolType[] typeArgs) {
		this.typeArgs = typeArgs;
	}

	public SymbolType[] getTypeArgs() {
		return typeArgs;
	}

	public void setTypeMapping(Map<String, SymbolType> typeMapping) {
		this.typeMapping = typeMapping;
	}

	public Map<String, SymbolType> getTypeMapping() {
		return typeMapping;
	}

	public Map<String, SymbolType> getMethodArgsMapping() {
		return methodArgsMapping;
	}

	public void setMethodArgsMapping(Map<String, SymbolType> methodArgsMapping) {
		this.methodArgsMapping = methodArgsMapping;
	}

	public Type[] getGenericParameterTypes() {
		return genericParameterTypes;
	}

	public void setGenericParameterTypes(Type[] methodParameterTypes) {
		this.genericParameterTypes = methodParameterTypes;
	}

	public boolean isVarAgs() {
		return isVarAgs;
	}

	public void setVarAgs(boolean isVarAgs) {
		this.isVarAgs = isVarAgs;
	}

	public int getParameterTypesLenght() {
		return paramsCount;
	}

	public void setParameterTypesLenght(int paramsCount) {
		this.paramsCount = paramsCount;
	}

}
