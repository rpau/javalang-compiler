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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;

public class FieldInspector {

	public static SymbolType findFieldType(SymbolType scope, String fieldName) {

		SymbolType result = null;
		if (scope != null) {
			List<Class<?>> classes = scope.getBoundClasses();
			Iterator<Class<?>> it = classes.iterator();

			while (it.hasNext() && result == null) {
				try {
					Class<?> clazz = it.next();

					Field field = null;
					if (scope.getArrayCount() > 0 && fieldName.equals("length")) {
						result = new SymbolType("int");
					} else {
						try {
							field = clazz.getDeclaredField(fieldName);

						} catch (NoSuchFieldException fe) {

							field = getField0(clazz.getPackage(), clazz,
									fieldName);
							if (field == null) {
								// check a field with no visibility

								Class<?> internal = TypeTable.getInstance()
										.loadClass(
												clazz.getName() + "$"
														+ fieldName);
								result = new SymbolType(internal);
							}
						}
						if (result == null) {
							Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();

							GenericBuilderFromGenericClasses builder = new GenericBuilderFromGenericClasses(
									clazz, scope.getParameterizedTypes());
							builder.build(typeMapping);

							result = SymbolType.valueOf(field.getGenericType(),
									typeMapping);
						}
					}
				} catch (ClassNotFoundException e) {
					throw new NoSuchExpressionTypeException(e);

				} catch (Exception e) {
					throw new NoSuchExpressionTypeException(e);

				}
			}
		}

		return result;
	}

	private static Field getField0(Package pkg, Class<?> clazz, String name) {

		Field res = null;
		try {
			res = clazz.getDeclaredField(name);
		} catch (NoSuchFieldException e) {

		}
		// Search declared public fields
		if (res != null) {
			int modifiers = res.getModifiers();
			if (Modifier.isPublic(modifiers)) {
				return res;
			} else if (Modifier.isProtected(modifiers)) {
				return res;
			} else if (!Modifier.isPrivate(modifiers)
					&& res.getDeclaringClass().getPackage().equals(pkg)) {
				return res;
			}
		}
		// Direct superinterfaces, recursively
		Class<?>[] interfaces = clazz.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			Class<?> c = interfaces[i];
			if ((res = getField0(pkg, c, name)) != null) {
				return res;
			}
		}
		// Direct superclass, recursively
		if (!clazz.isInterface()) {
			Class<?> c = clazz.getSuperclass();
			if (c != null) {
				if ((res = getField0(pkg, c, name)) != null) {
					return res;
				}
			}
		}
		return null;
	}
}
