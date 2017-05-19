package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.MethodSymbolData;

import java.lang.reflect.Method;

public class MethodSymbolDataAssert extends SymbolDataAssert<MethodSymbolDataAssert, MethodSymbolData> {
    MethodSymbolDataAssert(MethodSymbolData actual) {
        super(actual, MethodSymbolDataAssert.class);
    }

    public AbstractObjectAssert<?, Method> method() {
        return Assertions.assertThat(actual.getMethod()).as(navigationDescription("method"));
    }
}
