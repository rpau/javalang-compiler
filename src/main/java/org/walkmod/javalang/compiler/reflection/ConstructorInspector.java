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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.CompositeBuilder;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class ConstructorInspector {

    private static GenericBuilderFromGenericClasses b1 = new GenericBuilderFromGenericClasses();

    public static SymbolType findConstructor(SymbolType scope, SymbolType[] args, ArrayFilter<Constructor<?>> filter)
            throws Exception {
        ConstructorSorter sorter = new ConstructorSorter();
        Class<?>[] argClasses = null;
        int size = 0;
        if (args != null) {
            size = args.length;
        }
        argClasses = new Class<?>[size];
        for (int i = 0; i < size; i++) {
            if (args[i] != null) {
                argClasses[i] = args[i].getClazz();
            }
        }
        @SuppressWarnings("rawtypes")
        List<Constructor> auxList = null;
        try {
            auxList = sorter.sort(scope.getClazz().getDeclaredConstructors(), argClasses);
        } catch (Throwable e) {
            throw new Exception("Error reading the constructors of " + scope.getName(), e);
        }
        Constructor<?>[] auxArray = new Constructor[auxList.size()];
        auxList.toArray(auxArray);
        filter.setElements(auxArray);

        Constructor<?> constructor = filter.filterOne();
        SymbolType result = scope.clone();
        result.setConstructor(constructor);
        return result;
    }

    public static SymbolType findConstructor(SymbolType scope, SymbolType[] args, ArrayFilter<Constructor<?>> filter,
            CompositeBuilder<Constructor<?>> builder, Map<String, SymbolType> typeMapping) throws Exception {
        b1.setParameterizedTypes(scope.getParameterizedTypes());
        List<TypeMappingPredicate<Constructor<?>>> tmp = null;
        List<Predicate<Constructor<?>>> preds = filter.getPredicates();
        if (preds != null) {
            tmp = new LinkedList<TypeMappingPredicate<Constructor<?>>>();
            for (Predicate<Constructor<?>> pred : preds) {
                if (pred instanceof TypeMappingPredicate) {
                    tmp.add((TypeMappingPredicate<Constructor<?>>) pred);
                }
            }
        }
        Class<?> bound = scope.getClazz();
        b1.setClazz(bound);
        Map<String, SymbolType> mapping = b1.build(typeMapping);
        if (tmp != null) {
            for (TypeMappingPredicate<Constructor<?>> pred : tmp) {
                pred.setTypeMapping(mapping);
            }
        }

        return findConstructor(scope, args, filter);
    }
}
