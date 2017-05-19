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

import java.lang.reflect.GenericArrayType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class SymbolDataOfMethodReferenceBuilder<T> implements TypeMappingBuilder<MethodCallExpr> {

    private Map<String, SymbolType> typeMapping;

    private VoidVisitor<T> visitor;

    private T ctxt;

    public SymbolDataOfMethodReferenceBuilder(Map<String, SymbolType> typeMapping, VoidVisitor<T> visitor, T ctxt) {
        this.typeMapping = typeMapping;
        this.visitor = visitor;
        this.ctxt = ctxt;
    }

    @Override
    public MethodCallExpr build(MethodCallExpr n) throws Exception {
        List<Expression> args = n.getArgs();
        MethodSymbolData st = n.getSymbolData();
        if (args != null) {
            int i = 0;
            if (st == null) {
                Expression scope = n.getScope();
                String scopeName = "empty";
                if (scope != null) {
                    scopeName = scope.getSymbolData().toString();
                }
                String argsTypeName = "empty";
                List<Expression> argExpr = n.getArgs();
                if (argExpr != null) {
                    argsTypeName = "[";
                    Iterator<Expression> it = argExpr.iterator();
                    while (it.hasNext()) {
                        Expression arg = it.next();
                        if (arg != null && arg.getSymbolData() != null) {
                            argsTypeName += " " + arg.getSymbolData().toString();
                        } else {
                            argsTypeName += " null";
                        }
                        if (it.hasNext()) {
                            argsTypeName += ",";
                        }
                    }
                    argsTypeName += "]";
                }
                throw new Exception("Ops! The method call " + n.toString() + " is not resolved. The scope is ["
                        + scopeName + "] , and the args are : " + argsTypeName);
            }
            java.lang.reflect.Type[] argClasses = st.getMethod().getGenericParameterTypes();
            int paramCount = st.getMethod().getParameterTypes().length;
            for (Expression argument : args) {
                if (argument instanceof MethodReferenceExpr) {
                    SymbolType aux = null;
                    if (i < paramCount) {
                        aux = SymbolType.valueOf(argClasses[i], typeMapping);

                    } else {
                        java.lang.reflect.Type componentType = null;
                        java.lang.reflect.Type lastArg = argClasses[argClasses.length - 1];
                        if (lastArg instanceof Class<?>) {
                            componentType = ((Class<?>) lastArg).getComponentType();
                        } else if (lastArg instanceof GenericArrayType) {
                            componentType = ((GenericArrayType) lastArg).getGenericComponentType();
                        }
                        aux = SymbolType.valueOf(componentType, typeMapping);
                    }
                    argument.setSymbolData(aux);
                    argument.accept(visitor, ctxt);
                }
                i++;
            }
        }
        return null;
    }

    @Override
    public void setTypeMapping(Map<String, SymbolType> typeMapping) {
        this.typeMapping = typeMapping;
    }

}
