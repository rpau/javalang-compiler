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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;

/**
 * For a given set of expressions, which some of them could reference an
 * specific class (e.g A.class), when the parameter is generic (e.g. T), then
 * the map that T corresponds to A.class
 * 
 * @author rpau
 *
 */
public class GenericsBuilderFromArgs implements Builder<Map<String, SymbolType>> {

    private Method method;

    private List<Expression> argumentValues;

    public GenericsBuilderFromArgs() {}

    public GenericsBuilderFromArgs(Method method, List<Expression> argumentValues) {
        this.method = method;
        this.argumentValues = argumentValues;

    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setArgumentValues(List<Expression> argumentValues) {
        this.argumentValues = argumentValues;
    }

    @Override
    public Map<String, SymbolType> build(Map<String, SymbolType> obj) {
        if (obj == null) {
            obj = new HashMap<String, SymbolType>();
        }
        TypeVariable<?>[] typeVariables = method.getTypeParameters();

        if (typeVariables != null) {

            for (int i = 0; i < typeVariables.length; i++) {

                Type[] parameterTypes = method.getGenericParameterTypes();

                if (parameterTypes != null && argumentValues != null) {

                    for (int j = 0; j < parameterTypes.length && j < argumentValues.size(); j++) {

                        if (parameterTypes[j] instanceof ParameterizedType) {

                            String variableName =
                                    ((ParameterizedType) parameterTypes[j]).getActualTypeArguments()[0].toString();

                            if (variableName.length() == 1) {
                                if (argumentValues.get(j) instanceof ClassExpr) {
                                    Class<?> paramClass;
                                    try {
                                        paramClass = TypesLoaderVisitor.getClassLoader()
                                                .loadClass(((ClassExpr) argumentValues.get(j)).getType());
                                    } catch (ClassNotFoundException e) {
                                        throw new NoSuchExpressionTypeException(
                                                "Invalid class into the generics resolution", e);
                                    }

                                    SymbolType auxType = new SymbolType(paramClass.getName());
                                    obj.put(variableName, auxType);
                                }
                            }
                        }
                    }
                }
            }
        }

        return obj;
    }

}
