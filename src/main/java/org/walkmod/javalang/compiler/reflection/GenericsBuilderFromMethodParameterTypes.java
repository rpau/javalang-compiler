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
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class GenericsBuilderFromMethodParameterTypes extends AbstractGenericsBuilderFromParameterTypes
        implements TypeMappingBuilder<Method> {

    private List<Type> callArgs = null;
    private SymbolType scope = null;

    public GenericsBuilderFromMethodParameterTypes(Map<String, SymbolType> typeMapping, List<Expression> args,
            SymbolType scope, SymbolType[] typeArgs, List<Type> callArgs, SymbolTable symbolTable) {
        super(typeMapping, args, typeArgs, symbolTable);
        this.callArgs = callArgs;
        this.scope = scope;

    }

    public GenericsBuilderFromMethodParameterTypes() {}

    @Override
    public Method build(Method method) throws Exception {
        setTypes(method.getGenericParameterTypes());
        if (scope == null) {
            scope = getSymbolTable().getType("this", ReferenceType.VARIABLE);
        }
        // we build a new symbol table just for the generics resolution (An
        // stack of scopes) based on the parameterized types of the class which belongs the implicit
        // object
        SymbolTable symbolTable = getTypeParamsSymbolTable();

        // we store in the symbol table a new scope with the generics of the
        // containing class of the method
        ResultBuilderFromGenerics generics = new ResultBuilderFromGenerics(scope, method, getSymbolTable());
        generics.build(symbolTable);

        // we store in the symbol table a new scope with the generics regarding
        // the argument types
        buildTypeParamsTypes();

        // we store in the symbol table a new scope with the generics regarding
        // the call method call explicit result parameter types
        generics = new ResultBuilderFromGenerics(callArgs, method);
        generics.build(symbolTable);

        closeTypeMapping();
        return method;
    }

}
