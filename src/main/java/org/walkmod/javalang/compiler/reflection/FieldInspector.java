package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Field;
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
					if (scope.getArrayCount()>0 && fieldName.equals("length")) {
						result = new SymbolType("int");
					} else {
						try {
							field = clazz.getDeclaredField(fieldName);

						} catch (NoSuchFieldException fe) {
							try {
								field = clazz.getField(fieldName);
							} catch (NoSuchFieldException fe2) {

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

							result = SymbolType.valueOf(field.getGenericType(),typeMapping);
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
}
