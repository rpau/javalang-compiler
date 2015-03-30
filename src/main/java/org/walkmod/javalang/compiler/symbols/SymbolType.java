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

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.types.TypeTable;
import org.walkmod.javalang.compiler.types.Types;
import org.walkmod.javalang.exceptions.InvalidTypeException;

public class SymbolType implements SymbolData, MethodSymbolData,
		FieldSymbolData {

	private String name;

	private List<SymbolType> bounds = null;

	private List<SymbolType> parameterizedTypes;

	private int arrayCount = 0;

	private boolean isTemplateVariable = false;

	private Class<?> clazz;

	private Method method = null;

	private Field field = null;

	public SymbolType() {
	}

	public SymbolType(List<SymbolType> bounds) {
		this.bounds = bounds;
		if (!bounds.isEmpty()) {
			name = bounds.get(0).getName();
			clazz = bounds.get(0).getClazz();
		}
	}

	public SymbolType(String name, List<SymbolType> bounds) {

		this.name = name;
		if (bounds != null) {
			this.bounds = bounds;
			if (!bounds.isEmpty()) {
				clazz = bounds.get(0).getClazz();
			}
		}
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

	public boolean hasBounds() {
		return bounds != null;
	}

	public List<SymbolType> getBounds() {
		return bounds;
	}

	private List<SymbolType> resolveGenerics(Class<?> clazz) {
		List<SymbolType> result = null;
		TypeVariable<?>[] typeParams = clazz.getTypeParameters();
		if (typeParams.length > 0) {
			result = new LinkedList<SymbolType>();
			for (TypeVariable<?> td : typeParams) {
				Type[] bounds = td.getBounds();
				if (bounds.length == 1) {
					if (bounds[0] instanceof Class) {
						SymbolType st = new SymbolType((Class<?>) bounds[0]);
						result.add(st);
					}
				} else {
					throw new RuntimeException("Multiple bounds not supported");
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

	@SuppressWarnings("unchecked")
	@Override
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
		if (bounds != null) {
			Iterator<SymbolType> it = bounds.iterator();
			boolean isCompatible = true;
			while (it.hasNext() && isCompatible) {
				isCompatible = it.next().isCompatible(other);
			}
			return isCompatible;
		}
		return Types.isCompatible(other.getClazz(), getClazz());
	}

	public Class<?> getClazz() {
		if (clazz == null) {
			try {
				clazz = TypeTable.getInstance().loadClass(this);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Error resolving the class for "
						+ name, e.getCause());
			}

		}
		return clazz;
	}

	public List<Class<?>> getBoundClasses() {
		List<Class<?>> compatibleClasses = new LinkedList<Class<?>>();
		if (hasBounds()) {
			List<SymbolType> bounds = getBounds();
			for (SymbolType bound : bounds) {
				compatibleClasses.add(bound.getClass());
			}

		} else {
			Class<?> clazz = getClazz();
			if (clazz != null) {
				compatibleClasses.add(clazz);
			}
		}
		return compatibleClasses;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(name);
		if (parameterizedTypes != null) {
			result.append("<");
			Iterator<? extends SymbolData> it = parameterizedTypes.iterator();
			while (it.hasNext()) {
				SymbolType next = (SymbolType) it.next();
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
		result.setArrayCount(arrayCount);
		result.isTemplateVariable = isTemplateVariable;
		if (parameterizedTypes != null) {
			List<SymbolType> list = new LinkedList<SymbolType>();

			for (SymbolData type : parameterizedTypes) {
				list.add(((SymbolType) type).clone());
			}
			result.setParameterizedTypes(list);
		}
		return result;
	}

	public static SymbolType valueOf(Type type,
			Map<String, SymbolType> typeMapping) throws InvalidTypeException {

		SymbolType returnType = null;

		if (type instanceof Class<?>) {
			Class<?> aux = ((Class<?>) type);
			returnType = new SymbolType(aux.getName());
			if (aux.isArray()) {
				returnType.setArrayCount(1);
				returnType.setName(aux.getComponentType().getName());
			}

		} else if (type instanceof TypeVariable) {

			String variableName = ((TypeVariable<?>) type).getName();
			SymbolType aux = typeMapping.get(variableName);

			if (aux == null) {
				aux = new SymbolType(Object.class.getName());
				return aux;
			} else {
				return aux;
			}

		} else if (type instanceof ParameterizedType) {
			Class<?> auxClass = (Class<?>) ((ParameterizedType) type)
					.getRawType();

			Type[] types = ((ParameterizedType) type).getActualTypeArguments();

			returnType = new SymbolType(auxClass.getName());

			if (types != null) {
				List<SymbolType> params = new LinkedList<SymbolType>();
				returnType.setParameterizedTypes(params);
				for (Type t : types) {
					SymbolType param = typeMapping.get(t.toString());
					if (param != null) {
						params.add(param);
					} else {
						try {
							SymbolType st = valueOf(t, typeMapping);
							if (st != null) {
								params.add(st);
							}
						} catch (InvalidTypeException e) {
							// LOG.warn("Unmappeable type " + t.toString());
						}
					}
				}
				if (params.isEmpty()) {
					returnType.setParameterizedTypes(null);
				}
			}

		} else if (type instanceof GenericArrayType) {
			// method.getReturnType();(
			returnType = new SymbolType(valueOf(
					((GenericArrayType) type).getGenericComponentType(),
					typeMapping).getName());

			returnType.setArrayCount(1);

		} else {
			throw new InvalidTypeException(type);
		}
		return returnType;

	}

	public Method getMethod() {
		return method;
	}

	public static SymbolType valueOf(Method method,
			Map<String, SymbolType> typeMapping) throws ClassNotFoundException,
			InvalidTypeException {
		java.lang.reflect.Type type = null;
		if (typeMapping == null) {
			typeMapping = new HashMap<String, SymbolType>();
			type = method.getReturnType();
		} else {
			TypeVariable<Method>[] tvs = method.getTypeParameters();
			type = method.getGenericReturnType();
			if (tvs.length > 0) {
				for (TypeVariable<Method> tv : tvs) {
					Type[] bounds = tv.getBounds();
					List<SymbolType> boundsList = new LinkedList<SymbolType>();
					for (int i = 0; i < bounds.length; i++) {
						boundsList.add(new SymbolType((Class<?>) bounds[i]));
					}
					SymbolType st = typeMapping.get(tv.getName());
					if (st == null) {
						if (boundsList.size() == 1) {
							typeMapping.put(tv.getName(), boundsList.get(0));

						} else {
							typeMapping.put(tv.getName(), new SymbolType(
									boundsList));
						}
					}
				}
			}
		}
		SymbolType st = SymbolType.valueOf(type, typeMapping);
		st.method = method;
		return st;
	}

	public void setField(Field field) {
		this.field = field;
	}

	@Override
	public Field getField() {
		return field;
	}

	@Override
	public SymbolData merge(SymbolData other) {
		if (other == null) {
			return this;
		}
		List<Class<?>> bounds = ClassInspector.getTheNearestSuperClasses(
				getBoundClasses(), other.getBoundClasses());
		if(bounds.isEmpty()){
			return null;
		}
		else if (bounds.size() == 1) {
			return new SymbolType(bounds.get(0));
		} else {
			List<SymbolType> boundsList = new LinkedList<SymbolType>();
			for (Class<?> bound : bounds) {
				boundsList.add(new SymbolType(bound));
			}
			return new SymbolType(boundsList);
		}

	}

}
