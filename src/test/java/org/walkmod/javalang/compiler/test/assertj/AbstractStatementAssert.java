package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.IfStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.stmt.TypeDeclarationStmt;

public class AbstractStatementAssert<S extends AbstractStatementAssert<S, A>, A extends Statement>
        extends AbstractNodeAssert<S, A> {
    AbstractStatementAssert(A actual, Class<?> selfType) {
        super(actual, selfType);
    }

    /** public for reflection */
    public AbstractStatementAssert(A actual) {
        this(actual, AbstractStatementAssert.class);
    }

    public SymbolDataAssert symbolData() {
        return symbolData(actual);
    }

    public BlockStmtAssert asBlockStmt() {
        return AstAssertions.assertThat(asInstanceOf(BlockStmt.class))
                .as(navigationDescription("(BlockStmt)"));
    }

    public ExpressionStmtAssert asExpressionStmt() {
        return AstAssertions.assertThat(asInstanceOf(ExpressionStmt.class))
                .as(navigationDescription("(ExpressionStmt)"));
    }

    public IfStmtAssert asIfStmt() {
        return AstAssertions.assertThat(asInstanceOf(IfStmt.class))
                .as(navigationDescription("(IfStmt)"));
    }

    public ReturnStmtAssert asReturnStmt() {
        return AstAssertions.assertThat(asInstanceOf(ReturnStmt.class)).as(navigationDescription("(ReturnStmt)"));
    }

    public TypeDeclarationStmtAssert asTypeDeclarationStmt() {
        return AstAssertions.assertThat(asInstanceOf(TypeDeclarationStmt.class))
                .as(navigationDescription("(TypeDeclarationStmt)"));
    }
}
