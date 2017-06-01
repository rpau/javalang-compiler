package org.walkmod.javalang.compiler.symbols;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;

public class AnonymousClassUtil {
    public static boolean isAnonymousClass(ObjectCreationExpr n) {
        return n.getAnonymousClassBody() != null;
    }

    /**
     * For anonymous creations the initial symbol data is symbol data of super class.
     * That needs to be replaced with symbol data of anonymous class.
     * If we don't have a super class we can't say and assume we need proper info.
     */
    public static boolean needsSymbolData(ObjectCreationExpr n) {
        final Class<?> typeRefClass = symbolDataClazz(n.getType());
        final Class<?> clazz = symbolDataClazz(n);
        return typeRefClass == null || clazz == null || clazz.equals(typeRefClass);
    }

    /* @Nullable */ private static Class<?> symbolDataClazz(Node n) {
        final SymbolData sd = symbolData(n);
        return sd != null ? sd.getClazz() : null;
    }

    /* @Nullable */ private static SymbolData symbolData(Node n) {
        return n instanceof SymbolDataAware ? ((SymbolDataAware) n).getSymbolData() : null;
    }
}
