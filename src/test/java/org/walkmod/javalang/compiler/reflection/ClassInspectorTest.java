package org.walkmod.javalang.compiler.reflection;

import org.junit.Test;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class ClassInspectorTest {

    public static abstract class ImmutableSet<E>
            extends AbstractCollection<E>
            implements Serializable, Set<E>, Collection<E> {}
    
    public static abstract class MySet<E> implements Set<E>, Collection<E> {}

    public static abstract class MySet2<E> implements Collection<E>, Set<E> {}

    @Test
    public void testIntersectionOfRawTypes() throws Exception {
        assertEquals("[java.util.Set<E>, java.util.Collection<E>]",
                asList(MySet.class.getGenericInterfaces()).toString());
        assertEquals("[java.util.Collection<E>, java.util.Set<E>]",
                asList(MySet2.class.getGenericInterfaces()).toString());
        assertIntersection(
                "[class java.util.AbstractCollection, interface java.io.Serializable, interface java.util.Set]",
                "[class java.util.AbstractCollection, interface java.util.Set, interface java.io.Serializable]",
                ImmutableSet.class, HashSet.class);

        assertIntersection("[interface java.util.Set]", ImmutableSet.class, Set.class);

        assertIntersection("[interface java.util.Set]", MySet2.class, MySet.class);

        assertIntersection(
                "[class java.util.AbstractCollection, interface java.lang.Cloneable, interface java.io.Serializable]",
                HashSet.class, ArrayList.class);

        // primitives
        assertIntersection("[interface java.io.Serializable, interface java.lang.Comparable]", int.class, String.class);
        assertIntersection("[class java.lang.Double]", int.class, double.class);
        assertIntersection("[interface java.io.Serializable, interface java.lang.Comparable]", int.class, boolean.class);
    }

    @Test
    public void testIntersectionOfRawTypeLists() throws Exception {
        assertIntersection("[interface java.util.Set, interface java.io.Serializable]",
                "[interface java.util.Set, interface java.io.Serializable]",
                asList(Set.class, Serializable.class),
                Arrays.<Class<?>>asList(HashSet.class));
        assertIntersection("[interface java.util.Collection, interface java.io.Serializable]",
                "[interface java.util.Collection, interface java.io.Serializable]",
                asList(List.class, Serializable.class),
                Arrays.<Class<?>>asList(HashSet.class));
    }

    private static void assertIntersection(String expected, final Class<?> clazz1, final Class<?> clazz2) {
        assertIntersection(expected, expected, clazz1, clazz2);
    }

    private static void assertIntersection(String expected1, String expected2, Class<?> clazz1, Class<?> clazz2) {
        assertEquals(expected1, ClassInspector.intersectRawTypes(clazz1, clazz2).toString());
        assertEquals(expected2, ClassInspector.intersectRawTypes(clazz2, clazz1).toString());
    }

    private static void assertIntersection(String expected1, String expected2,
                                           List<Class<?>> classes1, List<Class<?>> classes2) {
        assertEquals(expected1, ClassInspector.intersectRawTypes(classes1, classes2).toString());
        assertEquals(expected2, ClassInspector.intersectRawTypes(classes2, classes1).toString());
    }
}