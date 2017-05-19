package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;

public class ReturnStmtAssert extends AbstractStatementAssert<ReturnStmtAssert, ReturnStmt> {
    ReturnStmtAssert(ReturnStmt actual) {
        super(actual, ReturnStmtAssert.class);
    }

    public ExpressionAssert expression() {
        return AstAssertions.assertThat(actual.getExpr()).as(navigationDescription("expr"));
    }
}
