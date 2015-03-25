package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.SymbolData;
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
			Map<String, SymbolType> mapping = b1.build(typeMapping);
			if (tmp != null) {
				for (TypeMappingPredicate<Method> pred : tmp) {
					pred.setTypeMapping(mapping);
				}
			}
			result = findMethodType(bound, filter, builder, mapping, false);
		}
		if (result.getName().equals("java.lang.Object")
				&& scope.getParameterizedTypes() != null) {
			if (!scope.getParameterizedTypes().isEmpty()) {
				result.setName(scope.getParameterizedTypes().get(0).getName());
			}
		}
		return result;
	}

	public static SymbolType findMethodType(Class<?> clazz,
			ArrayFilter<Method> filter, CompositeBuilder<Method> builder,
			Map<String, SymbolType> typeMapping, boolean throwException)
			throws Exception {

		filter.setElements(clazz.getDeclaredMethods());

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

}
