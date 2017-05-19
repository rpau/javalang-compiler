package org.walkmod.javalang.compiler.reflection;

import java.util.Map;

import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class GenericsBuilderFromClassParameterTypes extends AbstractGenericsBuilderFromParameterTypes
        implements TypeMappingBuilder<Class<?>> {

    private SymbolType scope = null;

    public GenericsBuilderFromClassParameterTypes(Map<String, SymbolType> typeMapping, SymbolType scope,
            SymbolTable symbolTable) {
        super(typeMapping, symbolTable);
        this.scope = scope;
    }

    @Override
    public Class<?> build(Class<?> obj) throws Exception {

        // we build a new symbol table just for the generics resolution (An
        // stack of scopes) based on the parameterized types of the class which
        // belongs the implicit
        // object
        SymbolTable symbolTable = getTypeParamsSymbolTable();

        // we store in the symbol table a new scope with the generics of the
        // containing class of the method
        ResultBuilderFromGenerics generics = new ResultBuilderFromGenerics(scope, obj, getSymbolTable());
        generics.build(symbolTable);

        closeTypeMapping();

        return obj;
    }

}
