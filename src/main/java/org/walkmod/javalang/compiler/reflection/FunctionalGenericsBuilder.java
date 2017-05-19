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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class FunctionalGenericsBuilder<A extends Expression> implements Builder<A> {

    private SymbolType[] args;

    private VoidVisitor<?> typeResolver;

    private Method method;

    private Constructor<?> constructor;

    private Map<String, SymbolType> mapping;

    public FunctionalGenericsBuilder(Method method, VoidVisitor<?> typeResolver, Map<String, SymbolType> mapping)
            throws Exception {
        this.method = method;
        this.mapping = mapping;
        this.typeResolver = typeResolver;
    }

    public FunctionalGenericsBuilder(Constructor<?> constructor, VoidVisitor<?> typeResolver,
            Map<String, SymbolType> mapping) throws Exception {
        this.constructor = constructor;
        this.mapping = mapping;
        this.typeResolver = typeResolver;
    }

    public SymbolType[] getArgs() {
        return args;
    }

    public void setArgs(SymbolType[] args) {
        this.args = args;
    }

    public VoidVisitor<?> getTypeResolver() {
        return typeResolver;
    }

    @Override
    public A build(A expr) throws Exception {
        if (args == null) {
            java.lang.reflect.Type[] generics = null;
            if (method != null) {
                generics = method.getGenericParameterTypes();

            } else {
                generics = constructor.getGenericParameterTypes();
            }
            args = new SymbolType[generics.length];
            for (int i = 0; i < generics.length; i++) {
                args[i] = SymbolType.valueOf(generics[i], mapping);
            }
        }
        return expr;
    }

}
