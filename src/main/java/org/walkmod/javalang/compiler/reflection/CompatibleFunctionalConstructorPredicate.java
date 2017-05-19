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
import java.util.List;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleFunctionalConstructorPredicate<T> extends AbstractCompatibleFunctionalPredicate<T>
        implements TypeMappingPredicate<Constructor<?>> {

    public CompatibleFunctionalConstructorPredicate(SymbolType scope, VoidVisitor<T> typeResolver,
            List<Expression> args, T ctx, SymbolTable symTable, AbstractCompatibleArgsPredicate previousPredicate,
            SymbolType[] calculatedTypeArgs) {
        super(scope, typeResolver, args, ctx, symTable, previousPredicate, calculatedTypeArgs);
    }

    @Override
    public boolean filter(Constructor<?> elem) throws Exception {
        Class<?>[] params = elem.getParameterTypes();
        setParams(params);
        setVarArgs(elem.isVarArgs());
        return super.filter(elem);
    }

}
