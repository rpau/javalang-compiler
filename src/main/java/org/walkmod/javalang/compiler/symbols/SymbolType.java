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
import java.util.Set;

import org.walkmod.javalang.ast.ConstructorSymbolData;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.types.Types;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.exceptions.InvalidTypeException;

public class SymbolType implements SymbolData, MethodSymbolData,
		FieldSymbolData, ConstructorSymbolData {

	private String name;

	private List<SymbolType> upperBounds = null;

	private List<SymbolType> lowerBounds = null;

	private List<SymbolType> parameterizedTypes;

	private int arrayCount = 0;

	private String templateVariable = null;

	private Class<?> clazz;

	private Method method = null;

	private Field field = null;

	private Constructor<?> constructor = null;

	public SymbolType() {
	}

	public SymbolType(List<SymbolType> lowerBounds) {
		this(lowerBounds, null);
	}

	public SymbolType(List<SymbolType> upperBounds, List<SymbolType> lowerBounds) {
		this.upperBounds = upperBounds;
		this.lowerBounds = lowerBounds;
		if (upperBounds != null) {
			if (!upperBounds.isEmpty()) {
				name = upperBounds.get(0).getName();
				clazz = upperBounds.get(0).getClazz();
			}
		} else if (lowerBounds != null) {

			name = "java.lang.Object";
			clazz = Object.class;

		}
	}

	public SymbolType(String name, List<SymbolType> upperBounds) {

		this.name = name;
		if (upperBounds != null) {
			this.upperBounds = upperBounds;
			if (!upperBounds.isEmpty()) {
				clazz = upperBounds.get(0).getClazz();
			}
		}
	}

	public SymbolType(String name, List<SymbolType> upperBounds,
			List<SymbolType> lowerBounds) {

		this.name = name;
		this.lowerBounds = lowerBounds;
		if (upperBounds != null) {
			this.upperBounds = upperBounds;
			if (!upperBounds.isEmpty()) {
				clazz = upperBounds.get(0).getClazz();
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
		return upperBounds != null;
	}

	public List<SymbolType> getBounds() {
		return upperBounds;
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

	public List<SymbolType> getLowerBounds() {
		return lowerBounds;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SymbolType> getParameterizedTypes() {
		if (parameterizedTypes == null) {
			if (upperBounds != null && !upperBounds.isEmpty()) {
				List<SymbolType> params = upperBounds.get(0)
						.getParameterizedTypes();
				if (params != null && name != null) {
					Iterator<SymbolType> it = params.iterator();
					List<SymbolType> aux = null;
					while (it.hasNext()) {
						SymbolType st = it.next();
						if (!name.equals(st.getName())) {
							if (aux == null) {
								aux = new LinkedList<SymbolType>();
							}
							aux.add(st);
						}
					}
					return aux;
				}
				return null;

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
		return templateVariable != null;
	}

	public void setTemplateVariable(String templateVariable) {
		this.templateVariable = templateVariable;
	}

	public String getTemplateVariable() {
		return templateVariable;
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

	private Map<String, SymbolType> getTypeMappingVariables() {
		Map<String, SymbolType> result = null;
		List<SymbolType> paramTypes = getParameterizedTypes();
		if (paramTypes != null) {
			TypeVariable<?>[] vars = getClazz().getTypeParameters();
			result = new HashMap<String, SymbolType>();
			for (int i = 0; i < vars.length; i++) {
				result.put(vars[i].getName(), paramTypes.get(i));
			}
		}
		return result;
	}

	public boolean isCompatible(SymbolType other) {
		boolean isCompatible = true;

		if (upperBounds != null) {
			Iterator<SymbolType> it = upperBounds.iterator();

			while (it.hasNext() && isCompatible) {
				isCompatible = it.next().isCompatible(other);
			}

			return isCompatible;
		}
		if (isCompatible && lowerBounds != null) {
			Iterator<SymbolType> it = lowerBounds.iterator();
			while (it.hasNext() && isCompatible) {
				isCompatible = other.isCompatible(it.next());
			}
		}
		if (isCompatible) {

			List<SymbolType> otherParams = other.getParameterizedTypes();
			if (parameterizedTypes != null && otherParams != null) {
				Set<Type> paramTypes = ClassInspector
						.getEquivalentParametrizableClasses(other.getClazz());
				Iterator<Type> paramTypesIt = paramTypes.iterator();
				boolean found = false;
				try {
					Map<String, SymbolType> otherMap = other
							.getTypeMappingVariables();
					while (paramTypesIt.hasNext() && !found) {
						Type currentType = paramTypesIt.next();
						SymbolType st = SymbolType.valueOf(currentType,
								otherMap);
						found = Types.isCompatible(st.getClazz(), getClazz());
						if (isCompatible) {
							otherParams = st.getParameterizedTypes();
							Iterator<SymbolType> it = parameterizedTypes
									.iterator();
							Iterator<SymbolType> otherIt = otherParams
									.iterator();
							while (it.hasNext() && found && otherIt.hasNext()) {
								SymbolType thisType = it.next();
								SymbolType otherType = otherIt.next();
								found = thisType.isCompatible(otherType);
							}
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				isCompatible = found;
			} else {
				isCompatible = Types.isCompatible(other.getClazz(), getClazz());
			}
			if (isCompatible) {
				if (!getClazz().equals(Object.class) || getArrayCount() > 0) {
					isCompatible = getArrayCount() == other.getArrayCount();
				}
			}

		}
		return isCompatible;
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
		result.templateVariable = templateVariable;
		if (parameterizedTypes != null) {
			List<SymbolType> list = new LinkedList<SymbolType>();

			for (SymbolData type : parameterizedTypes) {
				list.add(((SymbolType) type).clone());
			}
			result.setParameterizedTypes(list);
		}
		if (upperBounds != null) {
			List<SymbolType> list = new LinkedList<SymbolType>();
			for (SymbolData type : upperBounds) {
				list.add(((SymbolType) type).clone());
			}
			result.upperBounds = list;
		}
		if (lowerBounds != null) {
			List<SymbolType> list = new LinkedList<SymbolType>();
			for (SymbolData type : lowerBounds) {
				list.add(((SymbolType) type).clone());
			}
			result.lowerBounds = list;
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
			TypeVariable<?>[] typeParams = aux.getTypeParameters();
			if (typeParams.length > 0) {
				List<SymbolType> params = new LinkedList<SymbolType>();

				for (int i = 0; i < typeParams.length; i++) {
					SymbolType tp = valueOf(typeParams[i], null,
							updatedTypeMapping, typeMapping);
					if (tp != null) {
						params.add(tp);
					}
				}
				if (!params.isEmpty()) {
					returnType.setParameterizedTypes(params);
				}
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
					returnType.setTemplateVariable(variableName);
					returnType.setParameterizedTypes(arg
							.getParameterizedTypes());

				} else {
					if (bounds.length == 0) {
						returnType = new SymbolType("java.lang.Object");
					} else {
						List<SymbolType> boundsList = new LinkedList<SymbolType>();
						returnType = new SymbolType(boundsList);
						returnType.setTemplateVariable(variableName);
						Map<String, SymbolType> auxMap = new HashMap<String, SymbolType>(
								typeMapping);
						auxMap.put(variableName, returnType);
						for (Type bound : bounds) {
							SymbolType st = valueOf(bound, null,
									updatedTypeMapping, auxMap);
							if (st != null) {
								boundsList.add(st);
							}
						}
						if (boundsList.isEmpty()) {
							returnType = new SymbolType("java.lang.Object");
						} else if (bounds.length == 1) {
							returnType = boundsList.get(0);
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
						String name = param.getName();
						if (name != null) {
							Class<?> aux = Types.getWrapperClass(name);
							if(aux != null){
								param.setName(aux.getName());
								param.setClazz(aux);
							}
						}
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
						if (st != null) {
							String name = st.getName();
							if (name != null) {
								Class<?> aux = Types.getWrapperClass(name);
								if(aux != null){
									st.setName(aux.getName());
									st.setClazz(aux);
								}
							}
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
			if (arg != null) {
				if (arg.getArrayCount() > 0) {
					arg = arg.clone();
					arg.setArrayCount(arg.getArrayCount() - 1);
				} else {
					arg = null;
				}
			}
			SymbolType st = valueOf(
					((GenericArrayType) type).getGenericComponentType(), arg,
					updatedTypeMapping, typeMapping);

			returnType = st.clone();
			returnType.setArrayCount(returnType.getArrayCount() + 1);
		} else if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			Type[] types = wt.getUpperBounds();

			List<SymbolType> upperBounds = null;
			List<SymbolType> lowerBounds = null;
			if (types != null && types.length > 0) {
				upperBounds = new LinkedList<SymbolType>();
				for (int i = 0; i < types.length; i++) {
					SymbolType st = valueOf(types[i], arg, updatedTypeMapping,
							typeMapping);
					if (st != null) {
						upperBounds.add(st);
					}
				}
				if (upperBounds.isEmpty()) {
					upperBounds = null;
				}

			}
			types = wt.getLowerBounds();
			if (types != null && types.length > 0) {
				lowerBounds = new LinkedList<SymbolType>();
				for (int i = 0; i < types.length; i++) {

					SymbolType st = valueOf(types[i], arg, updatedTypeMapping,
							typeMapping);
					if (st != null) {
						lowerBounds.add(st);
					}

				}
				if (lowerBounds.isEmpty()) {
					lowerBounds = null;
				}
			}
			if (upperBounds != null || lowerBounds != null) {
				returnType = new SymbolType(upperBounds, lowerBounds);
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
			type = method.getGenericReturnType();
		} else {
			TypeVariable<Method>[] tvs = method.getTypeParameters();
			type = method.getGenericReturnType();
			if (tvs.length > 0) {
				for (TypeVariable<Method> tv : tvs) {
					Type[] bounds = tv.getBounds();
					List<SymbolType> boundsList = new LinkedList<SymbolType>();
					for (int i = 0; i < bounds.length; i++) {
						SymbolType st = valueOf(bounds[i], typeMapping);
						if (st != null) {
							boundsList.add(st);
						}
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
		SymbolType result = null;
		if (other == null) {
			result = this;
		} else {
			if (other.getArrayCount() == getArrayCount()) {
				List<Class<?>> bounds = ClassInspector
						.getTheNearestSuperClasses(getBoundClasses(),
								other.getBoundClasses());
				if (bounds.isEmpty()) {
					result = null;
				} else if (bounds.size() == 1) {
					result = new SymbolType(bounds.get(0));
				} else {
					List<SymbolType> boundsList = new LinkedList<SymbolType>();
					for (Class<?> bound : bounds) {
						boundsList.add(new SymbolType(bound));
					}
					result = new SymbolType(boundsList);
				}
				if (result != null) {
					if (lowerBounds != null) {
						result.lowerBounds = new LinkedList<SymbolType>();
						for (SymbolType st : lowerBounds) {
							result.lowerBounds.add(st.clone());
						}
					}
					result.arrayCount = other.getArrayCount();
				}
			} else {
				result = new SymbolType(Object.class);
			}
		}
		return result;
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

	@Override
	public List<Class<?>> getLowerBoundClasses() {
		List<Class<?>> upperBoundClasses = new LinkedList<Class<?>>();
		if (lowerBounds != null) {
			for (SymbolType bound : lowerBounds) {
				upperBoundClasses.add(bound.getClazz());
			}

		}
		return upperBoundClasses;
	}

	private SymbolType refactor_rec(String variable, SymbolType st,
			boolean dynamicVar) {
		if (variable.equals(templateVariable) && dynamicVar) {
			return st;
		} else {
			SymbolType aux;
			if (this.parameterizedTypes != null) {
				aux = this.clone();
				List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();
				for (SymbolType param : this.parameterizedTypes) {
					param = param.refactor_rec(variable, st, dynamicVar);
					parameterizedTypes.add(param);
				}
				aux.setParameterizedTypes(parameterizedTypes);
			} else {
				aux = this;

			}
			return aux;
		}
	}

	public SymbolType refactor(String variable, SymbolType st,
			boolean dynamicVar) {
		if (variable.equals(templateVariable) && dynamicVar) {
			return st;
		} else {
			SymbolType aux;
			if (this.parameterizedTypes != null) {
				aux = this.clone();
				List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();
				for (SymbolType param : this.parameterizedTypes) {
					param = param.refactor_rec(variable, st, dynamicVar);
					parameterizedTypes.add(param);
				}
				aux.setParameterizedTypes(parameterizedTypes);
			} else {
				if (Object.class.equals(getClazz())) {
					aux = st;
				} else {
					aux = this;
				}
			}
			return aux;
		}
	}

}
