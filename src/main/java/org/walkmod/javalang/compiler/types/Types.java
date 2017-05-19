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
package org.walkmod.javalang.compiler.types;

import java.util.HashMap;
import java.util.Map;

import org.walkmod.javalang.ast.expr.BooleanLiteralExpr;
import org.walkmod.javalang.ast.expr.CharLiteralExpr;
import org.walkmod.javalang.ast.expr.DoubleLiteralExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.LiteralExpr;
import org.walkmod.javalang.ast.expr.LongLiteralExpr;

public class Types {

    private static Map<String, LiteralExpr> defaultValues = new HashMap<String, LiteralExpr>();

    private static Map<String, String> wrapperClasses = new HashMap<String, String>();

    private static Map<String, Class<?>> inverseWrapperClasses = new HashMap<String, Class<?>>();

    private static Map<String, Integer> matrixTypePosition;

    /** indexed by matrixTypePosition, [from][to] */
    private static boolean[][] compatibilityMatrix;

    static {

        defaultValues.put("int", new IntegerLiteralExpr("0"));
        defaultValues.put("boolean", new BooleanLiteralExpr(false));
        defaultValues.put("float", new IntegerLiteralExpr("0"));
        defaultValues.put("long", new LongLiteralExpr("0"));
        defaultValues.put("double", new DoubleLiteralExpr("0"));
        defaultValues.put("char", new CharLiteralExpr(""));
        defaultValues.put("short", new IntegerLiteralExpr("0"));
        defaultValues.put("byte", new IntegerLiteralExpr("0"));

        matrixTypePosition = new HashMap<String, Integer>();
        matrixTypePosition.put("byte", 0);
        matrixTypePosition.put("java.lang.Byte", 0);
        matrixTypePosition.put("short", 1);
        matrixTypePosition.put("java.lang.Short", 1);
        matrixTypePosition.put("char", 2);
        matrixTypePosition.put("java.lang.Character", 2);
        matrixTypePosition.put("int", 3);
        matrixTypePosition.put("java.lang.Integer", 3);
        matrixTypePosition.put("long", 4);
        matrixTypePosition.put("java.lang.Long", 4);
        matrixTypePosition.put("float", 5);
        matrixTypePosition.put("java.lang.Float", 5);
        matrixTypePosition.put("double", 6);
        matrixTypePosition.put("java.lang.Double", 6);
        matrixTypePosition.put("boolean", 7);
        matrixTypePosition.put("java.lang.Boolean", 7);
        matrixTypePosition.put("String", 8);
        matrixTypePosition.put("java.lang.String", 8);
        matrixTypePosition.put("java.lang.Object", 9);

        wrapperClasses.put("java.lang.Byte", "byte");
        wrapperClasses.put("java.lang.Character", "char");
        wrapperClasses.put("java.lang.Integer", "int");
        wrapperClasses.put("java.lang.Long", "long");
        wrapperClasses.put("java.lang.Float", "float");
        wrapperClasses.put("java.lang.Double", "double");
        wrapperClasses.put("java.lang.Boolean", "boolean");
        wrapperClasses.put("java.lang.Short", "short");

        inverseWrapperClasses.put("byte", Byte.class);
        inverseWrapperClasses.put("char", Character.class);
        inverseWrapperClasses.put("int", Integer.class);
        inverseWrapperClasses.put("long", Long.class);
        inverseWrapperClasses.put("float", Float.class);
        inverseWrapperClasses.put("double", Double.class);
        inverseWrapperClasses.put("boolean", Boolean.class);
        inverseWrapperClasses.put("short", Short.class);

        compatibilityMatrix = new boolean[][] {{true, true, true, true, true, true, true, false, false, true},
                {false, true, false, true, true, true, true, false, false, true},
                {false, false, true, true, true, true, true, false, false, true},
                {false, false, false, true, true, true, true, false, false, true},
                {false, false, false, false, true, true, true, false, false, true},
                {false, false, false, false, false, true, true, false, false, true},
                {false, false, false, false, false, false, true, false, false, true},
                {false, false, false, false, false, false, false, true, false, true},
                {false, false, false, false, false, false, false, false, true, true},
                {false, false, false, false, false, false, false, false, false, true}
                //{ true, true, true, true, true, true, true, true, true, true }
        };

    }

    public static boolean isCompatible(Class<?> fromClass, Class<?> toClass) {
        if (fromClass == null || toClass == null) {
            return true;
        }
        if (matrixTypePosition.containsKey(fromClass.getName()) && matrixTypePosition.containsKey(toClass.getName())) {
            return compatibilityMatrix[matrixTypePosition.get(fromClass.getName())][matrixTypePosition
                    .get(toClass.getName())];
        } else {
            if (fromClass.isPrimitive() && !toClass.isPrimitive()) {
                fromClass = inverseWrapperClasses.get(fromClass.getName());
            }
            return toClass.isAssignableFrom(fromClass);
        }

    }

    public static boolean isAssignable(Class<?> fromClass, Class<?> toClass) {
        if (fromClass == null || toClass == null) {
            return true;
        }
        final Integer fromKey = matrixTypePosition.get(fromClass.getName());
        final Integer toKey = matrixTypePosition.get(toClass.getName());
        if (fromKey != null && toKey != null) {
            return compatibilityMatrix[fromKey][toKey];
        } else {
            if (fromClass.isPrimitive() && !toClass.isPrimitive()) {
                fromClass = inverseWrapperClasses.get(fromClass.getName());
            }
            return toClass.isAssignableFrom(fromClass);
        }

    }

    public static Integer basicTypeEvaluationOrder(Class<?> clazz) {

        return matrixTypePosition.get(clazz.getName());

    }

    public static boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() || wrapperClasses.containsKey(clazz.getName());
    }

    public static boolean isCompatible(Class<?>[] fromClasses, Class<?>[] toClasses) {

        if (fromClasses.length == toClasses.length) {
            boolean assignable = true;
            for (int i = 0; i < fromClasses.length && assignable; i++) {
                assignable = isCompatible(fromClasses[i], toClasses[i]);
            }
            return assignable;
        }
        return false;
    }

    public static Class<?> getWrapperClass(String name) {
        return inverseWrapperClasses.get(name);
    }

    public static Map<String, String> getWrapperClasses() {
        return wrapperClasses;
    }

}
