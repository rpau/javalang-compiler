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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.CompositeBuilder;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;

public class MethodInspector {

    private static GenericBuilderFromGenericClasses b1 = new GenericBuilderFromGenericClasses();

    public static <T extends SymbolData> Method findMethod(Class<?> scope, T[] args, String name) {
        Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
        ArrayFilter<Method> filter = new ArrayFilter<Method>(null);
        CompatibleArgsPredicate pred = new CompatibleArgsPredicate(args);
        filter.appendPredicate(new MethodsByNamePredicate(name)).appendPredicate(new InvokableMethodsPredicate())
                .appendPredicate(pred);

        try {
            SymbolType result = MethodInspector.findMethodType(new SymbolType(scope), args, filter, null, typeMapping);
            if (result != null) {
                return result.getMethod();
            }

        } catch (Exception e) {
            throw new NoSuchExpressionTypeException(e);
        }
        return null;
    }

    public static <T extends SymbolData> SymbolType findMethodType(SymbolType scope, T[] args,
            ArrayFilter<Method> filter, CompositeBuilder<Method> builder, Map<String, SymbolType> typeMapping)
            throws Exception {

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
        Class<?>[] argClasses = SymbolType.toClassArray(args);

        while (it.hasNext() && result == null) {
            Class<?> bound = it.next();

            if (scope.getArrayCount() != 0) {
                bound = Array.newInstance(bound, scope.getArrayCount()).getClass();
            }
            b1.setClazz(bound);
            Map<String, SymbolType> mapping = b1.build(typeMapping);
            if (tmp != null) {
                for (TypeMappingPredicate<Method> pred : tmp) {
                    pred.setTypeMapping(mapping);
                }
            }
            result = findMethodType(bound, argClasses, filter, builder, mapping, false);
            if (scope.getArrayCount() != 0 && result != null) {
                Method method = result.getMethod();
                if (method != null && method.getName().equals("clone")) {
                    // JLS 10.7 - The return type of the clone method of an array type T[] is T[].
                    result = result.cloneAsArray(scope.getName(), scope.getArrayCount());
                }
            }
        }
        if (result != null) {
            if ("getClass".equals(result.getMethod().getName()) && args.length == 0) {
                List<SymbolType> paramTypes = new LinkedList<SymbolType>();
                paramTypes.add(scope.clone());
                result.setParameterizedTypes(paramTypes);
            }
        }

        return result;
    }

    public static boolean isGeneric(Method m) {
        boolean isGeneric = m.getTypeParameters().length > 0 && !m.getReturnType().equals(void.class);
        if (!isGeneric) {

            return ClassInspector.isGeneric(m.getGenericReturnType());
        }
        return isGeneric;
    }

    public static SymbolType findMethodType(Class<?> clazz, Class<?>[] args, ArrayFilter<Method> filter,
            CompositeBuilder<Method> builder, Map<String, SymbolType> typeMapping, boolean throwException)
            throws Exception {
        ExecutableSorter sorter = new ExecutableSorter();
        List<Method> auxList = sorter.sort(clazz.getDeclaredMethods(), args);
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

            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                result = findMethodType(superClass, args, filter, builder, typeMapping, false);
            }

            if (result == null) {
                Type[] types = clazz.getGenericInterfaces();
                if (types.length > 0) {

                    for (int i = 0; i < types.length && result == null; i++) {

                        Class<?> type = SymbolType.valueOf(types[i], typeMapping).getClazz();

                        result = findMethodType(type, args, filter, builder, typeMapping, false);
                    }

                }
                if (result == null && clazz.isInterface()) {
                    result = findMethodType(Object.class, args, filter, builder, typeMapping, false);
                }
            }
            if (result == null) {
                if (clazz.isMemberClass()) {

                    result = findMethodType(clazz.getDeclaringClass(), args, filter, builder, typeMapping, false);

                } else if (clazz.isAnonymousClass()) {

                    result = findMethodType(clazz.getEnclosingClass(), args, filter, builder, typeMapping, false);
                }
            }
        }
        if (result == null && throwException) {
            throw new NoSuchMethodException("The method  cannot be found");
        }
        return result;
    }

    public static Set<Method> getInhertitedDefaultMethods(Class<?> interf, Class<?> clazz) {
        Set<Method> result = new HashSet<Method>();
        if (!clazz.isInterface()) {
            Method[] declMethods = clazz.getDeclaredMethods();

            Set<Method> methods = getVisibleDefaultMethods(interf, clazz);
            Iterator<Method> it = methods.iterator();
            while (it.hasNext()) {
                Method current = it.next();
                boolean found = false;
                Class<?>[] params2 = current.getParameterTypes();
                for (int j = 0; j < declMethods.length && !found; j++) {

                    if (declMethods[j].getName().equals(current.getName())) {
                        Class<?>[] params1 = declMethods[j].getParameterTypes();
                        if (params1.length == params2.length) {
                            boolean compatible = true;
                            for (int k = 0; k < params2.length && compatible; k++) {
                                compatible = params1[k].isAssignableFrom(params2[k]);
                            }
                            found = compatible;
                        }
                    }
                }
                if (!found) {
                    result.add(current);
                }
            }

        }

        return result;
    }

    private static Set<Method> getVisibleDefaultMethods(Class<?> clazz, Class<?> invocationClass) {
        Set<Method> result = new HashSet<Method>();
        HashMap<String, Set<Method>> aux = new HashMap<String, Set<Method>>();

        if (clazz == null || clazz.equals(Object.class)) {
            return result;
        }
        Method[] declMethods = clazz.getDeclaredMethods();
        for (int i = 0; i < declMethods.length; i++) {
            int modifiers = declMethods[i].getModifiers();

            boolean isDefault =
                    Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers);

            boolean isVisible = clazz.getName().equals(invocationClass.getName());

            boolean samePackage = clazz.getPackage() == null && invocationClass.getPackage() == null;
            samePackage = samePackage || (clazz.getPackage() != null && invocationClass.getPackage() != null
                    && clazz.getPackage().getName().equals(invocationClass.getPackage().getName()));
            isVisible = isVisible || Modifier.isPublic(modifiers) || (!Modifier.isPrivate(modifiers) && samePackage);

            if (isVisible && isDefault && !declMethods[i].isBridge() && !declMethods[i].isSynthetic()) {
                result.add(declMethods[i]);
                Set<Method> auxSet = aux.get(declMethods[i].getName());
                if (auxSet == null) {
                    auxSet = new HashSet<Method>();
                }
                auxSet.add(declMethods[i]);

                aux.put(declMethods[i].getName(), auxSet);
            }
        }
        if (clazz.isInterface()) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                Set<Method> auxSet = getVisibleDefaultMethods(interfaces[i], invocationClass);
                result.addAll(auxSet);
            }
        }
        return result;
    }

    public static Method getLambdaMethod(Class<?> clazz, int paramsSize) {

        if (clazz == null || clazz.equals(Object.class)) {
            return null;
        }
        boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());

        if (!clazz.isInterface() && !isAbstract) {
            // it is an
            return null;
        }

        Method[] declMethods = clazz.getDeclaredMethods();
        for (int i = 0; i < declMethods.length; i++) {
            int modifiers = declMethods[i].getModifiers();
            /*
             * If a public, non-abstract, non-static method appears in an
             * interface, it must be a default method.
             */
            boolean isDefault =
                    Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers);

            if (!isDefault && declMethods[i].getParameterTypes().length == paramsSize) {
                return declMethods[i];
            }
        }
        Method result = getLambdaMethod(clazz.getSuperclass(), paramsSize);
        if (isAbstract) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length && result == null; i++) {
                result = getLambdaMethod(interfaces[i], paramsSize);
            }
        }

        return result;

    }

    public static Set<Method> getVisibleMethods(Class<?> clazz, Class<?> invocationClass) {
        Set<Method> result = new HashSet<Method>();
        HashMap<String, Set<Method>> aux = new HashMap<String, Set<Method>>();

        if (clazz == null || clazz.equals(Object.class)) {
            return result;
        }
        Method[] declMethods = clazz.getDeclaredMethods();
        for (int i = 0; i < declMethods.length; i++) {
            boolean isVisible = clazz.getName().equals(invocationClass.getName());
            int modifiers = declMethods[i].getModifiers();
            boolean samePackage = clazz.getPackage() == null && invocationClass.getPackage() == null;
            samePackage = samePackage || (clazz.getPackage() != null && invocationClass.getPackage() != null
                    && clazz.getPackage().getName().equals(invocationClass.getPackage().getName()));
            isVisible = isVisible || Modifier.isPublic(modifiers) || (!Modifier.isPrivate(modifiers) && samePackage);

            if (isVisible && !declMethods[i].isBridge() && !declMethods[i].isSynthetic()) {
                result.add(declMethods[i]);
                Set<Method> auxSet = aux.get(declMethods[i].getName());
                if (auxSet == null) {
                    auxSet = new HashSet<Method>();
                }
                auxSet.add(declMethods[i]);

                aux.put(declMethods[i].getName(), auxSet);
            }
        }
        if (clazz.isInterface()) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                Set<Method> auxSet = getVisibleMethods(interfaces[i], invocationClass);
                result.addAll(auxSet);
            }
        } else {
            Set<Method> superClassMethods = getVisibleMethods(clazz.getSuperclass(), invocationClass);
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
                                for (int i = 0; i < prevParams.length && compatibleArgs; i++) {
                                    compatibleArgs = superParams[i].isAssignableFrom(prevParams[i]);
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
        }
        return result;
    }

    public static Set<Method> getInheritedMethods(Class<?> clazz) {
        Set<Method> result = new HashSet<Method>();
        HashMap<String, Set<Method>> aux = new HashMap<String, Set<Method>>();

        if (clazz == null || clazz.equals(Object.class)) {
            return result;
        }
        Method[] declMethods = clazz.getDeclaredMethods();
        for (int i = 0; i < declMethods.length; i++) {
            if (!Modifier.isPrivate(declMethods[i].getModifiers())
                    && !Modifier.isAbstract(declMethods[i].getModifiers()) && !declMethods[i].isBridge()
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

        Set<Method> superClassMethods = getInheritedMethods(clazz.getSuperclass());
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
                            for (int i = 0; i < prevParams.length && compatibleArgs; i++) {
                                compatibleArgs = superParams[i].isAssignableFrom(prevParams[i]);
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
