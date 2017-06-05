package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodCallExpr;

public class MethodCallExprAssert extends AbstractExpressionAssert<MethodCallExprAssert, MethodCallExpr> {
    public MethodCallExprAssert(MethodCallExpr actual) {
        super(actual, MethodCallExprAssert.class);
    }

    public AbstractExpressionAssert<?, ?> arg(int index) {
        return args().item(index);
    }

    public ExtListAssert<AbstractExpressionAssert, Expression> args() {
        return AssertUtil.assertThat(actual.getArgs(), AbstractExpressionAssert.class, navigationDescription("args"));
    }

    public SymbolDataAssert<?,?> symbolData() {
        return symbolData(actual);
    }

    public ExpressionAssert scope() {
        return AstAssertions.assertThat(actual.getScope()).as(navigationDescription("scope"));
    }

    public MethodCallExprAssert hasName(String name) {
        Assertions.assertThat(actual.getName())
                .as(navigationDescription("name"))
                .isEqualTo(name);
        return this;
    }
}
