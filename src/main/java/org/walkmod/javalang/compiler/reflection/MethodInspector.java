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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.CompositeBuilder;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class MethodInspector {

	private static GenericBuilderFromGenericClasses b1 = new GenericBuilderFromGenericClasses();

	public static SymbolType findMethodType(SymbolType scope,
			ArrayFilter<Method> filter, CompositeBuilder<Method> builder,
			Map<String, SymbolType> typeMapping) throws Exception {

		SymbolType result = null;
		List<Class<?>> bounds = null;
		if (scope.getArrayCount() != 0) {
			bounds = new LinkedList<Class<?>>();
			bounds.add(Object.class);
		} else {
			bounds = scope.getBoundClasses();
		}
		

		b1.setParameterizedTypes(scope.getParameterizedTypes());

		Iterator<Class<?>> it = bounds.iterator();
		List<Predicate<Method>> preds = filter.getPredicates();
		List<TypeMappingPredicate<Method>> tmp = null;
		if (preds != null) {
			tmp = new LinkedList<TypeMappingPredicate<Method>>();
			for (Predicate<Method> pred : preds) {
				if (pred instanceof TypeMappingPredicate) {
					tmp.add((TypeMappingPredicate<Method>) pred);
				}
			}
		}

		while (it.hasNext() && result == null) {
			Class<?> bound = it.next();
			b1.setClazz(bound);
			Map<String, SymbolType> mapping = b1.build(typeMapping);
			if (tmp != null) {
				for (TypeMappingPredicate<Method> pred : tmp) {
					pred.setTypeMapping(mapping);
				}
			}
			result = findMethodType(bound, filter, builder, mapping, false);
		}

		return result;
	}

	public static boolean isGeneric(Method m) {
		boolean isGeneric = m.getTypeParameters().length > 0
				&& !m.getReturnType().equals(void.class);
		if (!isGeneric) {

			return ClassInspector.isGeneric(m.getGenericReturnType());
		}
		return isGeneric;
	}

	public static SymbolType findMethodType(Class<?> clazz,
			ArrayFilter<Method> filter, CompositeBuilder<Method> builder,
			Map<String, SymbolType> typeMapping, boolean throwException)
			throws Exception {
		List<Method> auxList = sortMethods(clazz.getDeclaredMethods());
		Method[] auxArray = new Method[auxList.size()];
		auxList.toArray(auxArray);
		filter.setElements(auxArray);

		Method aux = filter.filterOne();

		SymbolType result = null;
		if (aux != null) {
			if (builder != null) {
				builder.build(aux);
			}
			result = SymbolType.valueOf(aux, typeMapping);

		}

		if (result == null) {

			if (clazz.isMemberClass()) {

				result = findMethodType(clazz.getDeclaringClass(), filter,
						builder, typeMapping, false);

			} else if (clazz.isAnonymousClass()) {

				result = findMethodType(clazz.getEnclosingClass(), filter,
						builder, typeMapping, false);
			}
			if (result == null) {
				Class<?> superClass = clazz.getSuperclass();
				if (superClass != null) {
					result = findMethodType(superClass, filter, builder,
							typeMapping, false);
				}

				if (result == null) {
					Type[] types = clazz.getGenericInterfaces();
					if (types.length > 0) {

						for (int i = 0; i < types.length && result == null; i++) {

							Class<?> type = SymbolType.valueOf(types[i],
									typeMapping).getClazz();

							result = findMethodType(type, filter, builder,
									typeMapping, false);
						}

					}
					if (result == null && clazz.isInterface()) {
						result = findMethodType(Object.class, filter, builder,
								typeMapping, false);
					}
				}
			}
		}
		if (result == null && throwException) {
			throw new NoSuchMethodException("The method  cannot be found");
		}
		return result;
	}

	public static List<Method> sortMethods(Method[] methods) {

		Map<String, List<Method>> map = new HashMap<String, List<Method>>();

		LinkedList<Method> result = new LinkedList<Method>();
		for (Method method : methods) {
			List<Method> aux = map.get(method.getName());
			if (aux == null) {
				aux = new LinkedList<Method>();
				map.put(method.getName(), aux);
			}
			aux.add(method);
		}

		Set<String> entries = map.keySet();
		Comparator<Method> comp = new MethodComparator();
		for (String entry : entries) {
			List<Method> aux = map.get(entry);

			ArrayList<Method> sortedList = new ArrayList<Method>();
			Iterator<Method> it = aux.iterator();
			while (it.hasNext()) {
				ListIterator<Method> li = sortedList.listIterator(sortedList
						.size());
				Method method = it.next();
				boolean inserted = false;
				if (sortedList.isEmpty()) {
					sortedList.add(method);
				} else {
					int pos = sortedList.size() - 1;
					while (!inserted && li.hasPrevious()) {
						Method previous = li.previous();
						if (comp.compare(method, previous) == 1) {
							sortedList.add(pos + 1, method);
							inserted = true;
						}
						pos--;
					}
					if (!inserted) {
						sortedList.add(0, method);
					}
				}
			}

			result.addAll(sortedList);
		}

		return result;
	}

	public static Set<Method> getNonPrivateMethods(Class<?> clazz) {
		Set<Method> result = new HashSet<Method>();
		HashMap<String, Set<Method>> aux = new HashMap<String, Set<Method>>();

		if (clazz == null || clazz.equals(Object.class)) {
			return result;
		}
		Method[] declMethods = clazz.getDeclaredMethods();
		for (int i = 0; i < declMethods.length; i++) {
			if (!Modifier.isPrivate(declMethods[i].getModifiers())
					&& !Modifier.isAbstract(declMethods[i].getModifiers())
					&& !declMethods[i].isBridge()
					&& !declMethods[i].isSynthetic()) {
				result.add(declMethods[i]);
				Set<Method> auxSet = aux.get(declMethods[i].getName());
				if (auxSet == null) {
					auxSet = new HashSet<Method>();
				}
				auxSet.add(declMethods[i]);

				aux.put(declMethods[i].getName(), auxSet);
			}
		}

		Set<Method> superClassMethods = getNonPrivateMethods(clazz
				.getSuperclass());
		for (Method superMethod : superClassMethods) {
			Set<Method> auxSet = aux.get(superMethod.getName());
			boolean found = false;
			if (auxSet == null) {
				auxSet = new HashSet<Method>();
			} else {
				Class<?>[] superParams = superMethod.getParameterTypes();
				Iterator<Method> it = auxSet.iterator();

				while (it.hasNext() && !found) {
					Method prev = it.next();
					Class<?>[] prevParams = prev.getParameterTypes();
					if (prevParams.length == superParams.length) {
						if (prevParams.length > 0) {
							boolean compatibleArgs = false;
							for (int i = 0; i < prevParams.length
									&& compatibleArgs; i++) {
								compatibleArgs = superParams[i]
										.isAssignableFrom(prevParams[i]);
							}
							found = compatibleArgs;
						} else {
							found = true;
						}
					}
				}
			}
			if (!found) {
				aux.put(superMethod.getName(), auxSet);
				result.add(superMethod);
			}
		}

		return result;
	}

}
