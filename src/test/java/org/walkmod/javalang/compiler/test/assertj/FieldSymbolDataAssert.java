package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractClassAssert;
import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.SymbolData;

public class FieldSymbolDataAssert extends SymbolDataAssert<FieldSymbolDataAssert, FieldSymbolData> {
    public FieldSymbolDataAssert(FieldSymbolData actual) {
        super(actual, FieldSymbolDataAssert.class);
    }

    public AbstractClassAssert<?> clazz() {
        return Assertions.assertThat(actual.getClazz()).as(navigationDescription("clazz"));
    }

    public ExtListAssert<SymbolDataAssert, SymbolData> parameterizedTypes() {
        return AssertUtil.assertThat(actual.getParameterizedTypes(), SymbolDataAssert.class,
                navigationDescription("parameterizedTypes"));
    }
}
