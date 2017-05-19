package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;

public class VariableDeclarationExprAssert
        extends AbstractExpressionAssert<VariableDeclarationExprAssert, VariableDeclarationExpr> {
    public VariableDeclarationExprAssert(VariableDeclarationExpr actual) {
        super(actual, VariableDeclarationExprAssert.class);
    }

    public TypeAssert type() {
        return AstAssertions.assertThat(actual.getType()).as(navigationDescription("type"));
    }

    public ExtListAssert<VariableDeclaratorAssert, VariableDeclarator> vars() {
        return AssertUtil.assertThat(actual.getVars(), VariableDeclaratorAssert.class, navigationDescription("vars"));
    }
}
