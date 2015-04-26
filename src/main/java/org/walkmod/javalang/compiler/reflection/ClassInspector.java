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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
		if (clazz1.isAssignableFrom(clazz2)) {
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
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
			if (common != null && parent.isAssignableFrom(common)) {
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
					return getTheNearestSuperClass(classes);
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

}
