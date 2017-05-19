package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.type.Type;

public class TypeAssert extends AbstractNodeAssert<TypeAssert, Type> {
    public TypeAssert(Type actual) {
        super(actual, TypeAssert.class);
    }

    public SymbolDataAssert<?, ?> symbolData() {
        return symbolData(actual);
    }
}
