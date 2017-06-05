package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;

public class ObjectCreationExprAssert extends AbstractExpressionAssert<ObjectCreationExprAssert, ObjectCreationExpr> {
    public ObjectCreationExprAssert(ObjectCreationExpr actual) {
        super(actual, ObjectCreationExprAssert.class);
    }

    public ObjectCreationExprAssert hasSymbolName(String name) {
        symbolName().isEqualTo(name);
        return this;
    }

    public AbstractCharSequenceAssert<?, String> symbolName() {
        return Assertions.assertThat(actual.getSymbolName()).as(navigationDescription("symbolName"));
    }

    public TypeAssert type() {
        return AstAssertions.assertThat(actual.getType()).as(navigationDescription("type"));
    }

    public ExtListAssert<BodyDeclarationAssert, BodyDeclaration> anonymousClassBody() {
        return AssertUtil.assertThat(actual.getAnonymousClassBody(), BodyDeclarationAssert.class, "anonymousClassBody");
    }

    public SymbolDataAssert symbolData() {
        return symbolData(actual);
    }
}
