package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.expr.AssignExpr;

public class AssignExprAssert extends AbstractExpressionAssert<AssignExprAssert, AssignExpr> {
    /** public for reflection */
    public AssignExprAssert(AssignExpr actual) {
        super(actual, AssignExprAssert.class);
    }

    public SymbolDataAssert<?, ?> symbolData() {
        return symbolData(actual);
    }

    public ExpressionAssert target() {
        return AstAssertions.assertThat(actual.getTarget()).as(navigationDescription("target"));
    }

    public ExpressionAssert value() {
        return AstAssertions.assertThat(actual.getValue()).as(navigationDescription("value"));
    }
}
