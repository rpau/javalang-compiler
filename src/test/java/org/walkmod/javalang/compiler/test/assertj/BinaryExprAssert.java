package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.expr.BinaryExpr;

public class BinaryExprAssert extends AbstractExpressionAssert<BinaryExprAssert, BinaryExpr> {
    public BinaryExprAssert(BinaryExpr actual) {
        super(actual, BinaryExprAssert.class);
    }

    public ExpressionAssert left() {
        return AstAssertions.assertThat(actual.getLeft()).as(navigationDescription("left"));
    }

    public BinaryExprAssert hasOperator(BinaryExpr.Operator op) {
        Assertions.assertThat(actual.getOperator()).as(navigationDescription("operator")).isEqualByComparingTo(op);
        return this;
    }

    public ExpressionAssert right() {
        return AstAssertions.assertThat(actual.getRight()).as(navigationDescription("right"));
    }
}
