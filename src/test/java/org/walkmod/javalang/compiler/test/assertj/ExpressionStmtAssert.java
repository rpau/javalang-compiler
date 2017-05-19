package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.stmt.ExpressionStmt;

public class ExpressionStmtAssert extends AbstractStatementAssert<ExpressionStmtAssert, ExpressionStmt> {
    ExpressionStmtAssert(ExpressionStmt actual) {
        super(actual, ExpressionStmtAssert.class);
    }

    public ExpressionAssert expression() {
        return AstAssertions.assertThat(actual.getExpression()).as(navigationDescription("expression"));
    }
}
