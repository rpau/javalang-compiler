package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractClassAssert;
import org.assertj.core.api.Assertions;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class SymbolTypeAssert extends SymbolDataAssert<SymbolTypeAssert, SymbolType> {
    public SymbolTypeAssert(SymbolType actual) {
        super(actual, SymbolTypeAssert.class);
    }

    public AbstractClassAssert<?> clazz() {
        return Assertions.assertThat(actual.getClazz()).as(navigationDescription("clazz"));
    }

    public ExtListAssert<SymbolTypeAssert, SymbolType> parameterizedTypes() {
        return AssertUtil.assertThat(actual.getParameterizedTypes(), SymbolTypeAssert.class,
                navigationDescription("parameterizedTypes"));
    }
}
