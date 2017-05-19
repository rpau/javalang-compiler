package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;

public class AbstractTypeDeclarationAssert<S extends AbstractTypeDeclarationAssert<S, A>, A extends TypeDeclaration>
        extends AbstractBodyDeclarationAssert<S, A> {

    AbstractTypeDeclarationAssert(A actual, Class<?> selfType) {
        super(actual, selfType);
    }

    AbstractTypeDeclarationAssert(A actual) {
        this(actual, AbstractTypeDeclarationAssert.class);
    }

    public BodyDeclarationAssert getMember(int index) {
        return members().item(index);
    }

    public S hasName(String name) {
        Assertions.assertThat(actual.getName()).as(navigationDescription("name")).isEqualTo(name);
        return (S) this;
    }

    public ExtListAssert<BodyDeclarationAssert, BodyDeclaration> members() {
        return AssertUtil.assertThat(actual.getMembers(), BodyDeclarationAssert.class,
                navigationDescription("members"));
    }
}
