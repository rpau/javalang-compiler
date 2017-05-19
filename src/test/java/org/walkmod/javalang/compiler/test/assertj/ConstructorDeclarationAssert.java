package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.body.ConstructorDeclaration;

public class ConstructorDeclarationAssert
        extends AbstractNodeAssert<ConstructorDeclarationAssert, ConstructorDeclaration> {
    ConstructorDeclarationAssert(ConstructorDeclaration actual) {
        super(actual, ConstructorDeclarationAssert.class);
    }

    public BlockStmtAssert block() {
        return AstAssertions.assertThat(actual.getBlock()).as(navigationDescription("block"));
    }
}
