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

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.walkmod.javalang.compiler.types.Types;

public class ClassInspector {

	public static Class<?> getTheNearestSuperClass(Class<?> clazz1,
			Class<?> clazz2) {
		if (clazz2 == null) {
			clazz2 = Object.class;
		}
		if (clazz1 == null) {
			clazz1 = Object.class;
		}

		if (clazz1.equals(clazz2)) {
			return clazz1;
		}
		if (Types.isAssignable(clazz2, clazz1)) {
			return clazz1;
		}
		if (Types.isAssignable(clazz1, clazz2)) {
			return clazz2;
		}
		Class<?> parent = getTheNearestSuperClass(clazz1,
				clazz2.getSuperclass());
		if (parent.equals(Object.class)) {
			Type[] interfaces = clazz2.getGenericInterfaces();
			List<Type> types = new LinkedList<Type>(
					Arrays.<Type> asList(interfaces));
			Class<?> common = null;
			while (!types.isEmpty() && common == null) {
				Type current = types.remove(0);
				if (current instanceof Class<?>) {
					Class<?> clazz = (Class<?>) current;
					if (clazz.isAssignableFrom(clazz1)) {
						common = clazz;
					} else {
						interfaces = clazz.getGenericInterfaces();
						types.addAll(Arrays.<Type> asList(interfaces));
					}
				}
			}
			if (common != null && Types.isAssignable(common, parent)) {
				parent = common;
			}
		}
		return parent;
	}

	public static Class<?> getTheNearestSuperClass(List<Class<?>> classes) {
		if (classes != null && !classes.isEmpty()) {
			if (classes.size() == 1) {
				return classes.get(0);
			} else {
				Class<?> superClass = getTheNearestSuperClass(classes.get(0),
						classes.get(1));
				if (classes.size() > 2) {
					List<Class<?>> sublist = classes.subList(2, classes.size());
					sublist.add(0, superClass);
					return getTheNearestSuperClass(sublist);
				} else {
					return superClass;
				}
			}
		}
		return null;
	}

	public static List<Class<?>> getTheNearestSuperClasses(
			List<Class<?>> classes, List<Class<?>> otherClasses) {
		List<Class<?>> result = new LinkedList<Class<?>>();
		if (classes != null && otherClasses != null) {
			Iterator<Class<?>> it = classes.iterator();
			while (it.hasNext()) {
				Class<?> current = it.next();
				otherClasses.add(0, current);
				Class<?> parent = getTheNearestSuperClass(otherClasses);
				if (!result.contains(parent)) {
					result.add(parent);
				}
				otherClasses.remove(0);
			}
		}
		return result;
	}

	public static Class<?> findClassMember(Package pkg, String name,
			Class<?> clazz) {

		if (clazz == null || clazz.equals(Object.class)) {
			return null;
		}

		Class<?> result = validateInnerClasses(pkg, name, clazz);
		boolean found = result != null;

		if (!found) {
			result = findClassMember(pkg, name, clazz.getSuperclass());
			if (result == null) {
				Class<?>[] interfaces = clazz.getInterfaces();
				for (int i = 0; i < interfaces.length && !found; i++) {
					result = findClassMember(pkg, name, interfaces[i]);
					found = result != null;
				}
			}
		}
		return result;
	}

	private static Class<?> validateInnerClasses(Package pkg, String name,
			Class<?> clazz) {
		if (clazz == null || clazz.equals(Object.class)) {
			return null;
		}

		Class<?>[] innerClasses = clazz.getDeclaredClasses();
		Class<?> result = null;
		boolean found = false;
		for (int i = 0; i < innerClasses.length && !found; i++) {
			String fullName = innerClasses[i].getName();
			String simpleName = innerClasses[i].getSimpleName();
			int index = fullName.indexOf('$');
			String uniqueName = simpleName;
			if (index != -1) {
				int index2 = fullName.indexOf('$', index + 1);
				if (index2 != -1) {
					uniqueName = fullName.substring(index + 1);
					uniqueName = uniqueName.replaceAll("\\$", ".");
				}
			}
			int modifiers = innerClasses[i].getModifiers();
			boolean validModifiers = Modifier.isPublic(modifiers)
					|| Modifier.isProtected(modifiers)
					|| (!Modifier.isPrivate(modifiers) && innerClasses[i]
							.getPackage().equals(pkg));
			found = validModifiers
					&& (uniqueName.equals(name) || simpleName.equals(name));

			if (found) {
				result = innerClasses[i];
			}
			found = result != null;
		}
		if (!found) {
			for (int i = 0; i < innerClasses.length && !found; i++) {
				result = validateInnerClasses(pkg, name, innerClasses[i]);
				found = result != null;
			}
		}
		return result;
	}

	public static Set<Class<?>> getNonPrivateClassMembers(Class<?> clazz) {
		Set<Class<?>> result = new HashSet<Class<?>>();
		if (clazz == null || clazz.equals(Object.class)) {
			return result;
		}
		Class<?>[] declClasses = clazz.getDeclaredClasses();
		for (int i = 0; i < declClasses.length; i++) {
			if (!Modifier.isPrivate(declClasses[i].getModifiers())) {
				result.add(declClasses[i]);
			}
		}
		result.addAll(getNonPrivateClassMembers(clazz.getSuperclass()));
		Class<?>[] interfaces = clazz.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			result.addAll(getNonPrivateClassMembers(interfaces[i]));
		}

		return result;
	}

	public static Set<Type> getEquivalentParametrizableClasses(Type clazz) {
		Set<Type> result = new LinkedHashSet<Type>();
		if (clazz instanceof ParameterizedType) {
			result.add(clazz);

		} else if (clazz instanceof Class<?>) {
			Class<?> type = (Class<?>) clazz;
			if (type.getTypeParameters().length > 0) {

				result.add(type);
				result.addAll(getEquivalentParametrizableClasses(type
						.getGenericSuperclass()));
				Type[] interfaces = type.getGenericInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					result.addAll(getEquivalentParametrizableClasses(interfaces[i]));
				}
			}
		}

		return result;
	}

}
