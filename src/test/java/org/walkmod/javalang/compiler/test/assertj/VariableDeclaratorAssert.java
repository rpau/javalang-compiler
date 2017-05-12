package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.body.VariableDeclarator;

public class VariableDeclaratorAssert extends AbstractNodeAssert<VariableDeclaratorAssert, VariableDeclarator> {
    public VariableDeclaratorAssert(VariableDeclarator actual) {
        super(actual, VariableDeclaratorAssert.class);
    }

    public ExpressionAssert expression() {
        return AstAssertions.assertThat(actual.getInit()).as(navigationDescription("expression"));
    }
}
