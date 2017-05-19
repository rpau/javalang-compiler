package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;

public class IntegerLiteralExprAssert extends AbstractExpressionAssert<IntegerLiteralExprAssert, IntegerLiteralExpr> {
    public IntegerLiteralExprAssert(IntegerLiteralExpr actual) {
        super(actual, IntegerLiteralExprAssert.class);
    }

    public IntegerLiteralExprAssert hasValue(int value) {
        Assertions.assertThat(Integer.valueOf(actual.getValue())).as(navigationDescription("value"));
        return this;
    }
}
