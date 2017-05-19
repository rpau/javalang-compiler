/*
 * Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 * 
 * Walkmod is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Walkmod. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;

public class FieldInspector {

    public static Set<Field> getNonPrivateFields(Class<?> clazz) {
        Set<Field> result = new HashSet<Field>();
        if (clazz == null || clazz.equals(Object.class)) {
            return result;
        }
        Field[] fields = clazz.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            if (!Modifier.isPrivate(fields[i].getModifiers())) {
                result.add(fields[i]);
            }
        }
        result.addAll(getNonPrivateFields(clazz.getSuperclass()));
        Class<?>[] interfaces = clazz.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            result.addAll(getNonPrivateFields(interfaces[i]));
        }
        return result;
    }

    public static SymbolType findFieldType(SymbolTable symTable, SymbolType scope, String fieldName) {

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

                            field = getField0(clazz.getPackage(), clazz, fieldName);
                            if (field == null) {
                                // check a field with no visibility
                                SymbolType st = symTable.getType("this");

                                Class<?> internal =
                                        ClassInspector.findClassMember(st.getClazz().getPackage(), fieldName, clazz);
                                if (internal != null) {
                                    result = new SymbolType(internal);
                                }

                            }
                        }
                        if (result == null && field != null) {
                            Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();

                            Class<?> container = field.getDeclaringClass();

                            if (!container.getName().equals(clazz.getName())) {
                                Set<java.lang.reflect.Type> equivalentTypeImplementations =
                                        ClassInspector.getEquivalentParametrizableClasses(clazz);
                                if (equivalentTypeImplementations != null) {
                                    Iterator<java.lang.reflect.Type> itTypes = equivalentTypeImplementations.iterator();
                                    while (itTypes.hasNext()) {
                                        java.lang.reflect.Type current = itTypes.next();
                                        if (current instanceof ParameterizedType) {
                                            ParameterizedType paramType = (ParameterizedType) current;
                                            java.lang.reflect.Type rawType = paramType.getRawType();
                                            if (rawType instanceof Class) {
                                                String raw = ((Class<?>) paramType.getRawType()).getName();
                                                if (raw.equals(container.getName())) {

                                                    scope = SymbolType.valueOf(current, typeMapping);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            GenericBuilderFromGenericClasses builder =
                                    new GenericBuilderFromGenericClasses(container, scope.getParameterizedTypes());

                            builder.build(typeMapping);

                            result = SymbolType.valueOf(field.getGenericType(), typeMapping);
                            result.setField(field);
                        }
                    }
                } catch (Exception e) {
                    throw new NoSuchExpressionTypeException(e);

                }
            }
        }
        if (result == null) {
            throw new RuntimeException("The field " + fieldName + " is not found");
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
            } else if (!Modifier.isPrivate(modifiers) && res.getDeclaringClass().getPackage().equals(pkg)) {
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
