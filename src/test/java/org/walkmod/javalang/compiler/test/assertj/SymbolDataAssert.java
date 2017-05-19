package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractClassAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.Objects;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class SymbolDataAssert<S extends SymbolDataAssert<S, A>, A extends SymbolData>
        extends AbstractObjectAssert<S, A> {
    SymbolDataAssert(A actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public SymbolDataAssert(A actual) {
        this(actual, SymbolDataAssert.class);
    }

    public A asActual() {
        return actual;
    }

    protected <T> T asInstanceOf(final Class<T> clazz) {
        Objects.instance().assertIsInstanceOf(this.info, this.actual, clazz);
        return clazz.cast(actual);
    }

    protected String navigationDescription(final String description) {
        return AssertUtil.navigationDescription(this, description);
    }

    public MethodSymbolDataAssert asMethodSymbolData() {
        return AstAssertions.assertThat(asInstanceOf(MethodSymbolData.class))
                .as(navigationDescription("(MethodSymbolData)"));
    }

    public FieldSymbolDataAssert asFieldSymbolData() {
        return AstAssertions.assertThat(asInstanceOf(FieldSymbolData.class))
                .as(navigationDescription("(FieldSymbolData)"));
    }

    public SymbolTypeAssert asSymbolType() {
        return AstAssertions.assertThat(asInstanceOf(SymbolType.class)).as(navigationDescription("(SymbolType)"));
    }

    public AbstractCharSequenceAssert<?, String> name() {
        return Assertions.assertThat(actual.getName());
    }

    public AbstractClassAssert<?> clazz() {
        return Assertions.assertThat(actual.getClazz());
    }

    public AbstractIntegerAssert<?> arrayCount() {
        return Assertions.assertThat(actual.getArrayCount());
    }

    public AbstractBooleanAssert<?> isTemplateVariable() {
        return Assertions.assertThat(actual.isTemplateVariable());
    }
}
