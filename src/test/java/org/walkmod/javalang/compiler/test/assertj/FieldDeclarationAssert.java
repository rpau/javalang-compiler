package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;

import java.lang.reflect.Modifier;

/**
 *
 */
public class FieldDeclarationAssert extends AbstractNodeAssert<FieldDeclarationAssert, FieldDeclaration> {
    public FieldDeclarationAssert(FieldDeclaration actual) {
        super(actual, FieldDeclarationAssert.class);
    }

    public AbstractCharSequenceAssert<?, String> symbolName() {
        return Assertions.assertThat(actual.getSymbolName());
    }

    public TypeAssert type() {
        return AstAssertions.assertThat(actual.getType()).as(navigationDescription("type"));
    }

    public ExtListAssert<VariableDeclaratorAssert, VariableDeclarator> variables() {
        return AssertUtil.assertThat(actual.getVariables(), VariableDeclaratorAssert.class,
                navigationDescription("variables"));
    }

    public ExtListAssert<FieldSymbolDataAssert, FieldSymbolData> fieldsSymbolData() {
        return AssertUtil.assertThat(actual.getFieldsSymbolData(), FieldSymbolDataAssert.class,
                navigationDescription("fieldsSymbolData"));
    }

    public FieldDeclarationAssert hasSymbolName(String name) {
        Assertions.assertThat(actual.getSymbolName()).describedAs("symbolName").isEqualTo(name);
        return this;
    }

    public FieldDeclarationAssert isFinal(boolean value) {
        Assertions.assertThat(Modifier.isFinal(actual.getModifiers())).describedAs("isFinal").isEqualTo(value);
        return this;
    }

    public FieldDeclarationAssert isStatic(boolean value) {
        Assertions.assertThat(Modifier.isStatic(actual.getModifiers())).describedAs("isStatic").isEqualTo(value);
        return this;
    }

    public FieldDeclarationAssert isPublic(boolean value) {
        Assertions.assertThat(Modifier.isPublic(actual.getModifiers())).describedAs("isPublic").isEqualTo(value);
        return this;
    }
}
