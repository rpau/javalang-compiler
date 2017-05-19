package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.expr.Expression;

public class ExpressionAssert extends AbstractExpressionAssert<ExpressionAssert, Expression> {
    public ExpressionAssert(Expression actual) {
        super(actual, ExpressionAssert.class);
    }
}
