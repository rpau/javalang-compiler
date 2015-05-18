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
package org.walkmod.javalang.compiler.symbols;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.ConstructorSymbolData;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.compiler.types.Types;
import org.walkmod.javalang.exceptions.InvalidTypeException;

public class SymbolType implements SymbolData, MethodSymbolData,
		FieldSymbolData, ConstructorSymbolData {

	private String name;

	private List<SymbolType> bounds = null;

	private List<SymbolType> parameterizedTypes;

	private int arrayCount = 0;

	private boolean isTemplateVariable = false;

	private Class<?> clazz;

	private Method method = null;

	private Field field = null;

	private Constructor<?> constructor = null;

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

		setClazz(clazz);
		setName(clazz.getName());
		setParameterizedTypes(resolveGenerics(clazz));
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

			for (TypeVariable<?> td : typeParams) {
				Type[] bounds = td.getBounds();
				for (int i = 0; i < bounds.length; i++) {
					try {
						SymbolType st = valueOf(bounds[i], null);
						if (result == null) {
							result = new LinkedList<SymbolType>();
						}
						result.add(st);
					} catch (InvalidTypeException e) {
						throw new RuntimeException(e);
					}
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
		if (parameterizedTypes == null) {
			if (bounds != null && !bounds.isEmpty()) {
				return bounds.get(0).getParameterizedTypes();
			}
		}
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
				clazz = TypesLoaderVisitor.getClassLoader().loadClass(this);
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
				compatibleClasses.add(bound.getClazz());
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
		if (bounds != null) {
			List<SymbolType> list = new LinkedList<SymbolType>();
			for (SymbolData type : bounds) {
				list.add(((SymbolType) type).clone());
			}
			result.bounds = list;
		}
		return result;
	}

	/**
	 * Builds a symbol type from a Java type.
	 * 
	 * @param type
	 *            type to convert
	 * @param arg
	 *            reference class to take into account if the type is a generic
	 *            variable.
	 * @param updatedTypeMapping
	 *            place to put the resolved generic variables.
	 * @param typeMapping
	 *            reference type mapping for generic variables.
	 * @return the representative symbol type
	 */
	public static SymbolType valueOf(Type type, SymbolType arg,
			Map<String, SymbolType> updatedTypeMapping,
			Map<String, SymbolType> typeMapping) {
		if (typeMapping == null) {
			typeMapping = Collections.emptyMap();
		}

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
				Type[] bounds = ((TypeVariable<?>) type).getBounds();

				if (arg != null) {

					for (Type bound : bounds) {
						valueOf(bound, arg, updatedTypeMapping, typeMapping);
					}
					returnType = new SymbolType(arg.getName());
					returnType.setParameterizedTypes(arg
							.getParameterizedTypes());

				} else {
					if (bounds.length == 0) {
						returnType = new SymbolType("java.lang.Object");
					} else {
						List<SymbolType> boundsList = new LinkedList<SymbolType>();
						for (Type bound : bounds) {
							boundsList.add(valueOf(bound, null,
									updatedTypeMapping, typeMapping));
						}
						if (boundsList.size() == 1) {
							returnType = boundsList.get(0);
						} else {
							returnType = new SymbolType(boundsList);
						}
					}
				}
				if (!updatedTypeMapping.containsKey(variableName)) {

					updatedTypeMapping.put(variableName, returnType);
					return returnType;
				} else {
					SymbolType previousSymbol = updatedTypeMapping
							.get(variableName);
					if (!returnType.getName().equals("java.lang.Object")) {
						returnType = (SymbolType) previousSymbol
								.merge(returnType);
						previousSymbol.setClazz(returnType.getClazz());
						updatedTypeMapping.put(variableName, previousSymbol);
					}
					return updatedTypeMapping.get(variableName);
				}

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
				List<SymbolType> paramTypes = null;
				if (arg != null) {
					paramTypes = arg.getParameterizedTypes();
				}
				int i = 0;
				for (Type t : types) {
					SymbolType param = typeMapping.get(t.toString());
					if (param != null) {
						params.add(param);
					} else {
						SymbolType argToAnalyze = null;
						if (paramTypes != null && paramTypes.size() > i) {
							argToAnalyze = paramTypes.get(i);
						}
						boolean validParameterizedType = true;
						if (t instanceof TypeVariable<?>) {
							Type[] bounds = ((TypeVariable<?>) t).getBounds();
							validParameterizedType = !(bounds.length == 1 && bounds[0] == type);
						}

						SymbolType st = null;
						if (validParameterizedType) {
							st = valueOf(t, argToAnalyze, updatedTypeMapping,
									typeMapping);
						}
						if (st == null) {
							st = new SymbolType("java.lang.Object");
						}

						params.add(st);

					}
					i++;
				}
				if (params.isEmpty()) {
					returnType.setParameterizedTypes(null);
				}
			}

		} else if (type instanceof GenericArrayType) {
			SymbolType st = valueOf(
					((GenericArrayType) type).getGenericComponentType(), arg,
					updatedTypeMapping, typeMapping);

			returnType = st.clone();
			returnType.setArrayCount(returnType.getArrayCount() + 1);
		} else if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			Type[] types = wt.getUpperBounds();

			if (types != null && types.length > 0) {
				List<SymbolType> bounds = new LinkedList<SymbolType>();
				for (int i = 0; i < types.length; i++) {
					bounds.add(valueOf(types[i], arg, updatedTypeMapping,
							typeMapping));
				}
				returnType = new SymbolType(bounds);
			}
		}
		return returnType;
	}

	public static SymbolType valueOf(Type type,
			Map<String, SymbolType> typeMapping) throws InvalidTypeException {

		return valueOf(type, null, new HashMap<String, SymbolType>(),
				typeMapping);

	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
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
						boundsList.add(valueOf(bounds[i], typeMapping));
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
		if (bounds.isEmpty()) {
			return null;
		} else if (bounds.size() == 1) {
			return new SymbolType(bounds.get(0));
		} else {
			List<SymbolType> boundsList = new LinkedList<SymbolType>();
			for (Class<?> bound : bounds) {
				boundsList.add(new SymbolType(bound));
			}
			return new SymbolType(boundsList);
		}

	}

	public void setConstructor(Constructor<?> constructor) {
		this.constructor = constructor;
	}

	@Override
	public Constructor<?> getConstructor() {
		return constructor;
	}

	public boolean belongsToAnonymousClass() {
		return belongsToAnonymous(getClazz());
	}

	private boolean belongsToAnonymous(Class<?> clazz) {
		if (clazz == null || clazz.equals(Object.class)) {
			return false;
		}
		if (clazz.isAnonymousClass()) {
			return true;
		} else {
			return belongsToAnonymous(clazz.getDeclaringClass());
		}
	}

}
