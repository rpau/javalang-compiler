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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleLambdaResultPredicate<A> implements Predicate<Method> {

    private LambdaExpr expr;

    private VoidVisitor<A> typeResolver;

    private A ctx;

    private Map<String, SymbolType> typeMapping;

    private SymbolTable symTable;

    public CompatibleLambdaResultPredicate(LambdaExpr expr, VoidVisitor<A> typeResolver, A ctx,
            Map<String, SymbolType> typeMapping, SymbolTable symTable) {
        this.expr = expr;
        this.typeResolver = typeResolver;
        this.ctx = ctx;
        this.typeMapping = typeMapping;
        this.symTable = symTable;
    }

    @Override
    public boolean filter(Method method) throws Exception {

        SymbolData returnType = expr.getBody().getSymbolData();
        if (returnType == null) {
            int scopeLevel = symTable.getScopeLevel();
            try {
                expr.accept(typeResolver, ctx);
            } catch (Exception e) {
                int currentScopeLevel = symTable.getScopeLevel();
                while (currentScopeLevel > scopeLevel) {
                    symTable.popScope(true);
                    currentScopeLevel--;
                }

                return false;
            }
            returnType = expr.getBody().getSymbolData();
            if (returnType == null) {
                returnType = new SymbolType(void.class);
            }
        }
        Map<String, SymbolType> updateMapping = new HashMap<String, SymbolType>();

        SymbolType st = null;

        if (void.class.equals(method.getReturnType())) {
            st = new SymbolType(void.class);
        } else {
            st = SymbolType.valueOf(method.getGenericReturnType(), (SymbolType) returnType, updateMapping, null);
        }
        Map<String, SymbolType> aux = new HashMap<String, SymbolType>(typeMapping);
        aux.putAll(updateMapping);

        boolean isCompatible = ("void".equals(st.getName()) && (expr.getBody() instanceof ExpressionStmt))
                || (st.isCompatible((SymbolType) returnType));
        if (isCompatible) {
            expr.setSymbolData(SymbolType.valueOf(method.getDeclaringClass(), aux));
        }
        return isCompatible;
    }

}
