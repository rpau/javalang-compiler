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

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class CompatibleArgsPredicate<T> extends AbstractCompatibleArgsPredicate implements TypeMappingPredicate<T> {

    public CompatibleArgsPredicate() {}

    public CompatibleArgsPredicate(SymbolType[] typeArgs) {
        super(typeArgs);
    }

    public CompatibleArgsPredicate(SymbolData[] typeArgs) {
        SymbolType[] aux = new SymbolType[typeArgs.length];
        int i = 0;
        for (SymbolData sd : typeArgs) {
            if (sd instanceof SymbolType) {
                aux[i] = (SymbolType) sd;
                i++;
            } else {
                throw new IllegalArgumentException(
                        "The type args argument must be " + SymbolType.class + " implementation");
            }
        }
        setTypeArgs(aux);
    }

    @Override
    public boolean filter(T method) throws Exception {
        if (method instanceof Method) {
            Method realMethod = (Method) method;
            setVarAgs(realMethod.isVarArgs());
            setGenericParameterTypes(realMethod.getGenericParameterTypes());
            setParameterTypesLenght(realMethod.getParameterTypes().length);
            return super.filter();
        } else if (method instanceof Constructor) {
            Constructor<?> constructor = (Constructor<?>) method;
            setVarAgs(constructor.isVarArgs());
            setGenericParameterTypes(constructor.getGenericParameterTypes());
            setParameterTypesLenght(constructor.getGenericParameterTypes().length);
            return super.filter();
        }
        return false;
    }

}
