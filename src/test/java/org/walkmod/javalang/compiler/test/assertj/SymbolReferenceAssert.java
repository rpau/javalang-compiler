package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.internal.Objects;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolReference;

import static org.walkmod.javalang.compiler.test.assertj.AssertUtil.navigationDescription;

public class SymbolReferenceAssert<S extends SymbolReferenceAssert<S, A>, A extends SymbolReference>
        extends AbstractObjectAssert<S, A> {
    private SymbolReferenceAssert(A actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public SymbolReferenceAssert(A actual) {
        this(actual, SymbolReferenceAssert.class);
    }

    public A asActual() {
        return actual;
    }

    protected <T> T asInstanceOf(final Class<T> clazz) {
        Objects.instance().assertIsInstanceOf(this.info, this.actual, clazz);
        return clazz.cast(actual);
    }

    public MethodSymbolDataAssert asMethodSymbolData() {
        return AstAssertions.assertThat(asInstanceOf(MethodSymbolData.class))
                .as(navigationDescription(this, "(MethodSymbolData)"));
    }
}
