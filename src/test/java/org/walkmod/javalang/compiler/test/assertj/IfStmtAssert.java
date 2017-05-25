package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.stmt.IfStmt;

public class IfStmtAssert extends AbstractStatementAssert<IfStmtAssert, IfStmt> {
    IfStmtAssert(IfStmt actual) {
        super(actual, IfStmtAssert.class);
    }

    public ExpressionAssert condition() {
        return AstAssertions.assertThat(actual.getCondition()).as(navigationDescription("condition"));
    }

    public StatementAssert thenStmt() {
        return AstAssertions.assertThat(actual.getThenStmt()).as(navigationDescription("thenStmt"));
    }

    public StatementAssert elseStmt() {
        return AstAssertions.assertThat(actual.getElseStmt()).as(navigationDescription("elseStmt"));
    }
}
