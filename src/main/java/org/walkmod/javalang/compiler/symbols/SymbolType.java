/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
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
package org.walkmod.javalang.compiler.symbols;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.compiler.types.Types;

public class SymbolType {

	private String name;

	private List<SymbolType> parameterizedTypes;

	private int arrayCount = 0;

	private boolean isTemplateVariable = false;

	private Class<?> clazz;

	public SymbolType() {
	}

	public SymbolType(Class<?> clazz) {
		setParameterizedTypes(resolveGenerics(clazz));
		setClazz(clazz);
		setName(clazz.getName());
		setArrayCount(resolveDimmensions(clazz));
	}

	private int resolveDimmensions(Class<?> clazz) {
		if (clazz.isArray()) {
			Class<?> component = clazz.getComponentType();
			return resolveDimmensions(component) + 1;
		}
		return 0;
	}

	private List<SymbolType> resolveGenerics(Class<?> clazz) {
		List<SymbolType> result = null;
		TypeVariable<?>[] typeParams = clazz.getTypeParameters();
		if (typeParams.length > 0) {
			result = new LinkedList<SymbolType>();
			for (TypeVariable<?> td : typeParams) {
				GenericDeclaration genDec = td.getGenericDeclaration();
				if (genDec instanceof Class) {
					SymbolType st = new SymbolType((Class<?>) genDec);
					result.add(st);
				} else {
					throw new UnsupportedOperationException(
							"Invalid type resoltion for " + clazz);
				}
			}
		}
		return result;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public SymbolType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<SymbolType> getParameterizedTypes() {
		return parameterizedTypes;
	}

	public void setParameterizedTypes(List<SymbolType> parameterizedTypes) {
		this.parameterizedTypes = parameterizedTypes;
	}

	public int getArrayCount() {
		return arrayCount;
	}

	public void setArrayCount(int arrayCount) {
		this.arrayCount = arrayCount;
	}

	public boolean isTemplateVariable() {
		return isTemplateVariable;
	}

	public void setTemplateVariable(boolean isTemplateVariable) {
		this.isTemplateVariable = isTemplateVariable;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SymbolType) {
			SymbolType aux = (SymbolType) o;
			return name.equals(aux.getName())
					&& arrayCount == aux.getArrayCount();
		}
		return false;
	}

	public boolean isCompatible(SymbolType other) {

		return Types.isCompatible(other.clazz, clazz);
	}
	
	public Class<?> getClazz(){
		return clazz;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(name);
		if (parameterizedTypes != null) {
			result.append("<");
			Iterator<SymbolType> it = parameterizedTypes.iterator();
			while (it.hasNext()) {
				SymbolType next = it.next();
				result.append(next.toString());
				if (it.hasNext()) {
					result.append(", ");
				}
			}
			result.append(">");
		}
		for (int i = 0; i < arrayCount; i++) {
			result.append("[]");
		}
		return result.toString();
	}

	public SymbolType clone() {
		SymbolType result = new SymbolType();
		result.setName(name);
		result.setClazz(clazz);
		result.isTemplateVariable = isTemplateVariable;
		if (parameterizedTypes != null) {
			List<SymbolType> list = new LinkedList<SymbolType>();
			for (SymbolType type : parameterizedTypes) {
				list.add(type.clone());
			}
			result.setParameterizedTypes(list);
		}
		return result;
	}

}
