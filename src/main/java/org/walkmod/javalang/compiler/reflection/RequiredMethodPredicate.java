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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeVisitorAdapter;
import org.walkmod.javalang.compiler.types.Types;

public class RequiredMethodPredicate<A extends Map<String, Object>> implements Predicate<Class<?>> {

    private MethodCallExpr requiredMethod;

    private TypeVisitorAdapter<A> visitor;

    private A ctx;

    public RequiredMethodPredicate(MethodCallExpr requiredMethod, TypeVisitorAdapter<A> visitor, A ctx) {
        this.requiredMethod = requiredMethod;
        this.visitor = visitor;
        this.ctx = ctx;
    }

    @Override
    public boolean filter(Class<?> candidate) {
        boolean isCompatible = true;

        List<Method> methods = new LinkedList<Method>();

        boolean returnTypeCompatible = false;
        methods.addAll(Arrays.asList(candidate.getDeclaredMethods()));

        methods.addAll(Arrays.asList(candidate.getMethods()));
        Iterator<Method> it = methods.iterator();

        while (it.hasNext() && !returnTypeCompatible) {

            Method currentMethod = it.next();

            // checking method name
            if (currentMethod.getName().equals(requiredMethod.getName())) {
                List<Expression> args = requiredMethod.getArgs();
                Class<?>[] parameterTypes = currentMethod.getParameterTypes();
                if (args != null) {
                    boolean compatibleArgs = true;
                    int k = 0;

                    for (Expression argExpr : args) {
                        argExpr.accept(visitor, ctx);
                        SymbolType typeArg = (SymbolType) argExpr.getSymbolData();
                        if (!Types.isCompatible(typeArg.getClazz(), parameterTypes[k])) {
                            compatibleArgs = false;
                        }
                        k++;
                    }
                    returnTypeCompatible = compatibleArgs;
                } else {
                    returnTypeCompatible = true;
                }
            }
        }

        isCompatible = returnTypeCompatible;

        return isCompatible;
    }
}
