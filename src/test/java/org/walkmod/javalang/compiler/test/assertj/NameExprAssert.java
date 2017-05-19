package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.expr.NameExpr;

public class NameExprAssert extends AbstractExpressionAssert<NameExprAssert, NameExpr> {
    public NameExprAssert(NameExpr actual) {
        super(actual, NameExprAssert.class);
    }

    public NameExprAssert hasName(String name) {
        name().isEqualTo(name);
        return this;
    }

    public AbstractCharSequenceAssert<?, String> name() {
        return Assertions.assertThat(actual.getName()).as(navigationDescription("name"));
    }

    public SymbolDataAssert symbolData() {
        return symbolData(actual);
    }
}
