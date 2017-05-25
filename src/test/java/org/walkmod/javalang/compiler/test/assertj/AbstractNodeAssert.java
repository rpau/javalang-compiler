package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Objects;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDataAware;

class AbstractNodeAssert<S extends AbstractAssert<S, A>, A extends Node> extends AbstractAssert<S, A> {

    AbstractNodeAssert(A actual, Class<?> selfType) {
        super(actual, selfType);
    }

    protected <T> T asInstanceOf(final Class<T> clazz) {
        Objects.instance().assertIsInstanceOf(this.info, this.actual, clazz);
        return clazz.cast(actual);
    }

    protected SymbolDataAssert symbolData(SymbolDataAware n) {
        return AstAssertions.assertThat(n.getSymbolData())
                .as(navigationDescription("(" + actual.getClass().getSimpleName() + ") symbolData"));
    }

    /** delegating helper */
    protected String navigationDescription(final String description) {
        return AssertUtil.navigationDescription(this, description);
    }

    /** not part of assertions, just for easy access to class declaration while writing assertions */
    @Deprecated
    public A asNode() {
        return actual;
    }
}
