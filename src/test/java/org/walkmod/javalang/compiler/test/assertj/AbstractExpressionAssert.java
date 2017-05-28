package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;

public class AbstractExpressionAssert<S extends AbstractExpressionAssert<S, A>, A extends Expression>
        extends AbstractNodeAssert<S, A> {
    AbstractExpressionAssert(A actual, Class<?> selfType) {
        super(actual, selfType);
    }

    /** public for reflection */
    public AbstractExpressionAssert(A actual) {
        this(actual, AbstractExpressionAssert.class);
    }

    public SymbolDataAssert symbolData() {
        return symbolData(actual);
    }

    public MethodCallExprAssert asMethodCallExpr() {
        return AstAssertions.assertThat(asInstanceOf(MethodCallExpr.class))
                .as(navigationDescription("(MethodCallExpr)"));
    }

    public VariableDeclarationExprAssert asVariableDeclarationExpr() {
        return AstAssertions.assertThat(asInstanceOf(VariableDeclarationExpr.class))
                .as(navigationDescription("(VariableDeclarationExpr)"));
    }

    public IntegerLiteralExprAssert asIntegerLiteralExpr() {
        return AstAssertions.assertThat(asInstanceOf(IntegerLiteralExpr.class))
                .as(navigationDescription("(IntegerLiteralExpr)"));
    }

    public AssignExprAssert asAssignExpr() {
        return AstAssertions.assertThat(asInstanceOf(AssignExpr.class)).as(navigationDescription("(AssignExpr)"));
    }

    public NameExprAssert asNameExpr() {
        return AstAssertions.assertThat(asInstanceOf(NameExpr.class)).as(navigationDescription("(NameExpr)"));
    }

    public ObjectCreationExprAssert asObjectCreationExpr() {
        return AstAssertions.assertThat(asInstanceOf(ObjectCreationExpr.class))
                .as(navigationDescription("(ObjectCreationExpr)"));
    }

    public BinaryExprAssert asBinaryExpr() {
        return AstAssertions.assertThat(asInstanceOf(BinaryExpr.class))
                .as(navigationDescription("(BinaryExpr)"));
    }
}
