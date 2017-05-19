package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.ListAssert;
import org.walkmod.javalang.ast.body.TypeDeclaration;

import java.util.List;

public class TypeDeclarationsAssert extends ListAssert<TypeDeclaration> {

    TypeDeclarationsAssert(List<? extends TypeDeclaration> actual) {
        super(actual);
    }

    public AbstractTypeDeclarationAssert<?, ?> item(int index) {
        return AstAssertions.assertThat(actual.get(index)).as(navigationDescription("types"));
    }
}
