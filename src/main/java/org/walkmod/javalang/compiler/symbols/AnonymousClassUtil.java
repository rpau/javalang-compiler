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
     * If we don't have symbol data we assume we need one. ;-)
     */
    public static boolean needsSymbolData(ObjectCreationExpr n) {
        final SymbolType st = symbolDataType(n);
        return st == null || !st.isLoadedAnonymousClass();
    }

    /* @Nullable */ private static SymbolData symbolData(Node n) {
        return n instanceof SymbolDataAware ? ((SymbolDataAware) n).getSymbolData() : null;
    }


    /* @Nullable */ private static SymbolType symbolDataType(Node n) {
        final SymbolData sd = symbolData(n);
        return sd instanceof SymbolType ? (SymbolType) sd : null;
    }
}
