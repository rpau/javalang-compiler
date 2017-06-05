package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.body.MethodDeclaration;

public class MethodDeclarationAssert extends AbstractNodeAssert<MethodDeclarationAssert, MethodDeclaration> {
    MethodDeclarationAssert(MethodDeclaration actual) {
        super(actual, MethodDeclarationAssert.class);
    }

    public BlockStmtAssert body() {
        return AstAssertions.assertThat(actual.getBody()).as(navigationDescription("body"));
    }

    public MethodDeclarationAssert hasName(String name) {
        name().isEqualTo(name);
        return this;
    }

    public AbstractCharSequenceAssert<?, String> name() {
        return Assertions.assertThat(actual.getName()).as(navigationDescription("name"));
    }

    public SymbolDataAssert symbolData() {
        return symbolData(actual);
    }
}
