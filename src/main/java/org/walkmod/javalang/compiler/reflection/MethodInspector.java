package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class MethodInspector {

	private static GenericBuilderFromGenericClasses b1 = new GenericBuilderFromGenericClasses();

	public static SymbolType findMethodType(SymbolType scope,
			ArrayFilter<Method> filter) throws Exception {

		SymbolType result = null;
		List<Class<?>> bounds = scope.getBoundClasses();

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
			Map<String, SymbolType> mapping = b1.build(null);
			if (tmp != null) {
				for (TypeMappingPredicate<Method> pred : tmp) {
					pred.setTypeMapping(mapping);
				}
			}
			result = findMethodType(bound, filter, mapping, false);
		}
		return result;
	}

	public static SymbolType findMethodType(Class<?> clazz,
			ArrayFilter<Method> filter, Map<String, SymbolType> typeMapping,
			boolean throwException) throws Exception {

		filter.setElements(clazz.getDeclaredMethods());

		Method aux = filter.filterOne();
		SymbolType result = null;
		if (aux != null) {
			result = SymbolType.valueOf(aux, typeMapping);
		}

		if (result == null) {

			if (clazz.isMemberClass()) {

				result = findMethodType(clazz.getDeclaringClass(), filter,
						typeMapping, false);

			} else if (clazz.isAnonymousClass()) {

				result = findMethodType(clazz.getEnclosingClass(), filter,
						typeMapping, false);
			}
			if (result == null) {
				Class<?> superClass = clazz.getSuperclass();
				if (superClass != null) {
					result = findMethodType(superClass, filter, typeMapping,
							false);
				}

				if (result == null) {
					Type[] types = clazz.getGenericInterfaces();
					if (types.length > 0) {

						for (int i = 0; i < types.length && result == null; i++) {

							Class<?> type = SymbolType.valueOf(types[i],
									typeMapping).getClazz();

							result = findMethodType(type, filter, typeMapping,
									false);
						}

					}
					if (result == null && clazz.isInterface()) {
						result = findMethodType(Object.class, filter,
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

	/*
	 * ArrayFilter<Method> filter = new ArrayFilter<Method>(methods);
	 * filter.appendPredicate(new MethodsByNamePredicate(methodName))
	 * .appendPredicate(new InvokableMethodsPredicate()) .appendPredicate(new
	 * CompatibleArgsPredicate(typeArgs));
	 */

}
