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

    public ExtListAssert<AbstractClassAssert, Class<?>> boundClasses() {
        return AssertUtil.assertThat(actual.getBoundClasses(), AbstractClassAssert.class, "boundClasses");
    }

    public AbstractCharSequenceAssert<?, String> name() {
        return Assertions.assertThat(actual.getName()).as(navigationDescription("name"));
    }

    public AbstractClassAssert<?> clazz() {
        Assertions.assertThat(actual).as(descriptionText()).isNotNull();
        return Assertions.assertThat(actual.getClazz()).describedAs(navigationDescription("clazz"));
    }

    public AbstractIntegerAssert<?> arrayCount() {
        return Assertions.assertThat(actual.getArrayCount());
    }

    public AbstractBooleanAssert<?> isTemplateVariable() {
        return Assertions.assertThat(actual.isTemplateVariable());
    }

    public SymbolDataAssert<?,?> hasName(String name) {
        name().isEqualTo(name);
        return this;
    }

    public ExtListAssert<? extends SymbolDataAssert, ? extends SymbolData> parameterizedTypes() {
        return AssertUtil.assertThat(asActual().getParameterizedTypes(), SymbolDataAssert.class, "parameterizedTypes");
    }
}
