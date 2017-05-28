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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.Types;
import org.walkmod.javalang.exceptions.InvalidTypeException;

import static java.util.Collections.singletonList;

public class ClassInspector {

    private static final List<Class<Object>> LIST_OF_OBJECT_CLASS = singletonList(Object.class);

    public static List<Type> getInterfaceOrSuperclassImplementations(Type implementation, Class<?> interf) {
        List<Type> result = new LinkedList<Type>();

        Set<Type> types = getEquivalentParametrizableClasses(implementation);
        if (types != null) {
            Iterator<Type> it = types.iterator();
            boolean found = false;
            while (it.hasNext() && !found) {
                Type type = it.next();
                if (type instanceof Class<?>) {
                    Class<?> clazz = (Class<?>) type;
                    if (interf.isAssignableFrom(clazz)) {
                        result.add(clazz);
                        // found = interf.isInterface();
                    }
                } else if (type instanceof ParameterizedType) {
                    ParameterizedType ptype = (ParameterizedType) type;
                    Type rawType = ptype.getRawType();
                    if (rawType instanceof Class<?>) {
                        Class<?> auxClass = (Class<?>) rawType;
                        if (interf.isAssignableFrom(auxClass)) {
                            result.add(ptype);
                            found = interf.isInterface();
                        }
                    }
                }
            }
        }
        return result;
    }

    public static void updateTypeMappingOfInterfaceSubclass(Class<?> subclass, Class<?> interfaceClass,
            Map<String, SymbolType> mapping) throws InvalidTypeException {

        List<Type> types = getInterfaceOrSuperclassImplementations(subclass, interfaceClass);
        Iterator<Type> it = types.iterator();
        while (it.hasNext()) {
            Type current = it.next();
            if (current instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) current;
                Map<String, SymbolType> update = new HashMap<String, SymbolType>();
                SymbolType.valueOf(ptype, null, update, mapping);
                mapping.putAll(update);
            }
        }
    }

    /** @return intersection of raw types of classes and all super classes and interfaces */
	public static List<? extends Class<?> > intersectRawTypes(List<Class<?> > classes1, List<Class<?>> classes2) {
        // most typical case
        if (classes1.size() == 1 && classes2.size() == 1) {
            return intersectRawTypes(classes1.get(0), classes2.get(0));
        } else {
            return list(removeSubClasses(intersection(classesAndInterfaces(classes1), classesAndInterfaces(classes2))));
        }
    }

    private static <T> List<T> list(final Collection<T> coll) {
        return Collections.unmodifiableList(new ArrayList<>(coll));
    }

    /** @return intersection of raw types of classes and all super classes and interfaces */
    public static List<? extends Class<?>> intersectRawTypes(Class<?> clazz1,
			Class<?> clazz2) {
		if (clazz2 == null) {
			clazz2 = Object.class;
		}
		if (clazz1 == null) {
			clazz1 = Object.class;
		}
        if (clazz1.isPrimitive()) {
            clazz1 = Types.getWrapperClass(clazz1.getName());
        }
        if (clazz2.isPrimitive()) {
            clazz2 = Types.getWrapperClass(clazz2.getName());
        }

		if (clazz1.equals(clazz2)) {
            return singletonList(clazz1);
        }
        if (Types.isAssignable(clazz2, clazz1)) {
            return singletonList(clazz1);
        }
        if (Types.isAssignable(clazz1, clazz2)) {
            return singletonList(clazz2);
        }
        final Set<Class<?>> common = commonClasses(clazz1, clazz2);
        final List<Class<?>> list = list(removeSubClasses(common));
        return list.isEmpty() ? LIST_OF_OBJECT_CLASS : list;

    }

    private static Set<Class<?>> commonClasses(Class<?> clazz1, Class<?> clazz2) {
        return intersection(classesAndInterfaces(clazz1), classesAndInterfaces(clazz2));
    }

    private static Collection<Class<?>> removeSubClasses(Collection<Class<?>> common) {
        if (common.size() < 2) {
            return common;
        }
        final Set<Class<?>> reduced = new LinkedHashSet<>(common.size());
        for (Class<?> clazz : common) {
            if (!containsSuperClass(common, clazz)) {
                reduced.add(clazz);
            }
        }
        return reduced;
    }

    private static boolean containsSuperClass(Collection<Class<?>> common, Class<?> clazz) {
        for (Class<?> other : common) {
            if (clazz != other && clazz.isAssignableFrom(other)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Class<?>> classesAndInterfaces(Class<?> clazz) {
        Set<Class<?>> result = new LinkedHashSet<>();
        addSuperAndInterfaces(result, clazz);
        return result;
    }

    private static Set<Class<?>> classesAndInterfaces(List<Class<?>> classes) {
        Set<Class<?>> result = new LinkedHashSet<>();
        for (Class<?> clazz : classes) {
            addSuperAndInterfaces(result, clazz);
        }
        return result;
    }

    // TODO: breadth first depth first. I guess depth first because of raw type rule selecting first bound.
    private static void addSuperAndInterfaces(Set<Class<?>> classes, Class<?> clazz) {
        if (clazz != null) {
            classes.add(clazz);
            addSuperAndInterfaces(classes, clazz.getSuperclass());
            for (Class<?> aClass : clazz.getInterfaces()) {
                addSuperAndInterfaces(classes, aClass);
            }
        }
    }

    private static <E> Set<E> intersection(Set<E> set1, Set<E> set2) {
        final Set<E> set = new LinkedHashSet<>(set1);
        set.retainAll(set2);
        return set;
    }

    /** @deprecated use {@link #intersectRawTypes} */
    @Deprecated
    public static Class<?> getTheNearestSuperClass(Class<?> clazz1, Class<?> clazz2) {
        return intersectRawTypes(clazz1, clazz2).get(0);
	}

    /** @deprecated use {@link #intersectRawTypes} */
    @Deprecated
	public static List<Class<?>> getTheNearestSuperClasses(
			List<Class<?>> classes, List<Class<?>> otherClasses) {
        return new ArrayList<>(intersectRawTypes(classes, otherClasses));
	}

    public static Class<?> findClassMember(Package pkg, String name, Class<?> clazz) {

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

    private static Class<?> validateInnerClasses(Package pkg, String name, Class<?> clazz) {
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
            boolean validModifiers =
                    Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || (!Modifier.isPrivate(modifiers)
                            && innerClasses[i].getPackage() != null && innerClasses[i].getPackage().equals(pkg));
            found = validModifiers && (uniqueName.equals(name) || simpleName.equals(name));

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
        Class<?> classToAnalyze = null;
        if (clazz instanceof ParameterizedType) {
            result.add(clazz);
            ParameterizedType ptype = (ParameterizedType) clazz;
            Type rawType = ptype.getRawType();
            if (rawType instanceof Class<?>) {
                classToAnalyze = (Class<?>) rawType;
            }

        } else if (clazz instanceof Class<?>) {
            Class<?> type = (Class<?>) clazz;
            if (type.getTypeParameters().length > 0) {
                result.add(type);
            }
            classToAnalyze = type;
        }
        if (classToAnalyze != null) {
            result.addAll(getEquivalentParametrizableClasses(classToAnalyze.getGenericSuperclass()));
            Type[] interfaces = classToAnalyze.getGenericInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                result.addAll(getEquivalentParametrizableClasses(interfaces[i]));
            }
        }

        return result;
    }

    public static boolean isGeneric(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            Type[] params = clazz.getTypeParameters();
            boolean isGeneric = false;
            for (int i = 0; i < params.length && !isGeneric; i++) {
                isGeneric = isGeneric(params[i]);
            }
            return isGeneric;
        } else if (type instanceof TypeVariable<?>) {
            return true;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            boolean isGeneric = isGeneric(paramType.getRawType());
            if (!isGeneric) {
                Type[] typeArgs = paramType.getActualTypeArguments();
                for (int i = 0; i < typeArgs.length && !isGeneric; i++) {
                    isGeneric = isGeneric(typeArgs[i]);
                }
            }
            return isGeneric;
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return isGeneric(arrayType.getGenericComponentType());
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type[] bounds = wt.getUpperBounds();
            boolean isGeneric = false;
            for (int i = 0; i < bounds.length && !isGeneric; i++) {
                isGeneric = isGeneric(bounds[i]);
            }
            if (!isGeneric) {
                bounds = wt.getLowerBounds();

                for (int i = 0; i < bounds.length && !isGeneric; i++) {
                    isGeneric = isGeneric(bounds[i]);
                }
            }
            return isGeneric;
        }
        return false;
    }

    public static int getClassHierarchyHeight(Class<?> clazz) {
        if (clazz == null || Object.class.equals(clazz)) {
            return 0;
        } else {
            return getClassHierarchyHeight(clazz.getSuperclass()) + 1;
        }
    }

    public static boolean isAssignable(Class<?> clazz2, Class<?> clazz1) {
        boolean isMethod2First = true;
        Integer order2 = Types.basicTypeEvaluationOrder(clazz2);
        Integer order1 = Types.basicTypeEvaluationOrder(clazz1);
        if (order1 != null && order2 != null) {
            return order2 <= order1;
        }
        boolean isAssignable = Types.isAssignable(clazz2, clazz1);
        if (!isAssignable) {
            if (Types.isAssignable(clazz1, clazz2)) {
                isMethod2First = false;
            } else {
                int h2 = ClassInspector.getClassHierarchyHeight(clazz2);
                int h1 = ClassInspector.getClassHierarchyHeight(clazz1);
                isMethod2First = h2 > h1;
            }
        } else {
            isMethod2First = true;
        }

        return isMethod2First;
    }

    public static boolean isMoreSpecficFor(Class<?> clazz2, Class<?> clazz1, Class<?> reference) {
        boolean isMethod2First = true;
        // we compare, in case these are basic types, their evaluation order.
        Integer order2 = Types.basicTypeEvaluationOrder(clazz2);
        Integer order1 = Types.basicTypeEvaluationOrder(clazz1);
        if (order1 != null && order2 != null) {
            // if both are primitive or wrapper classes, we check class2 vs
            // class1 or if the reference and the class2 and reference are not
            // primitive (ex clazz2 = Object, clazz1 = int, ref = Integer)

            if ((clazz2.isPrimitive() && clazz1.isPrimitive()) || (!clazz2.isPrimitive() && !clazz1.isPrimitive())
                    || reference == null) {
                return (order2 <= order1);
            }
            boolean referenceIsPrimitive = reference != null && reference.isPrimitive();

            boolean clazz2First = (referenceIsPrimitive && clazz2.isPrimitive() && !clazz1.isPrimitive())
                    || ((!referenceIsPrimitive) && !clazz2.isPrimitive() && clazz1.isPrimitive());

            return clazz2First;
        }
        // we check if clazz2 is subtype of clazz1. Notice that Object is supper
        // type of every interface.
        boolean isAssignable = Types.isAssignable(clazz2, clazz1);
        if (!isAssignable) {
            if (Types.isAssignable(clazz1, clazz2)) {
                isMethod2First = false;
            } else {
                int h2 = ClassInspector.getClassHierarchyHeight(clazz2);
                int h1 = ClassInspector.getClassHierarchyHeight(clazz1);

                if (h1 == h2) {
                    // our priority are arrays vs non array types and classes vs
                    // interfaces
                    isMethod2First = clazz2 != null && clazz1 != null && (clazz2.isArray() && !clazz1.isArray()
                            || (!clazz2.isInterface() && clazz1.isInterface()));
                    if (!isMethod2First) {
                        // we compare if the class is assignable to reference.
                        // If so, it has priority
                        isMethod2First = clazz2 != null && reference != null && !clazz2.isArray()
                                && !clazz2.isPrimitive() && clazz2.isAssignableFrom(reference);
                    }
                } else {
                    isMethod2First = h2 > h1;
                }
            }
        } else {
            isMethod2First = true;
        }

        return isMethod2First;
    }

}
