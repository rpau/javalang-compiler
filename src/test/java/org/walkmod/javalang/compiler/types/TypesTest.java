package org.walkmod.javalang.compiler.types;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class TypesTest {
    @Test
    public void isAssignable() throws Exception {
        assertFalse(Types.isAssignable(Object.class, Short.class));
        assertTrue(Types.isAssignable(Short.class, Object.class));
        assertFalse(Types.isAssignable(Object.class, String.class));
        assertTrue(Types.isAssignable(String.class, Object.class));
    }

}
