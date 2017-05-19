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
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.CollectionFilter;

import org.walkmod.javalang.compiler.symbols.SymbolType;

public class RequiredMethodSignaturePredicate implements TypeMappingPredicate<Method> {

    private CollectionFilter<Class<?>> resultTypeFilters;

    private GenericsBuilderFromArgs b2 = new GenericsBuilderFromArgs();

    private List<Expression> argumentValues;

    private Map<String, SymbolType> typeMapping;

    public RequiredMethodSignaturePredicate() {}

    public RequiredMethodSignaturePredicate(CollectionFilter<Class<?>> resultTypeFilters,
            List<Expression> argumentValues, Map<String, SymbolType> typeMapping) {
        this.argumentValues = argumentValues;
        this.typeMapping = typeMapping;
        this.resultTypeFilters = resultTypeFilters;
    }

    public void setResultTypeFilters(CollectionFilter<Class<?>> resultTypeFilters) {
        this.resultTypeFilters = resultTypeFilters;
    }

    public void setArgumentValues(List<Expression> argumentValues) {
        this.argumentValues = argumentValues;
    }

    @Override
    public void setTypeMapping(Map<String, SymbolType> typeMapping) {
        this.typeMapping = typeMapping;
    }

    @Override
    public boolean filter(Method method) throws Exception {

        if (resultTypeFilters != null) {
            b2.setMethod(method);
            b2.setArgumentValues(argumentValues);

            typeMapping = b2.build(new HashMap<String, SymbolType>(typeMapping));
            SymbolType result = SymbolType.valueOf(method, typeMapping);
            List<Class<?>> classes = result.getBoundClasses();
            resultTypeFilters.setElements(classes);
            return resultTypeFilters.filterOne() != null;

        }

        return false;
    }

}
