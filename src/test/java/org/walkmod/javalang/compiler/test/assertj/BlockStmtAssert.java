package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.Statement;

public class BlockStmtAssert extends AbstractNodeAssert<BlockStmtAssert, BlockStmt> {
    public BlockStmtAssert(BlockStmt actual) {
        super(actual, BlockStmtAssert.class);
    }

    public StatementAssert getStmt(int index) {
        return stmts().item(index);
    }

    public ExtListAssert<StatementAssert, Statement> stmts() {
        return AssertUtil.assertThat(actual.getStmts(), StatementAssert.class, navigationDescription("stmts"));
    }
}
