package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;

public class AbstractBodyDeclarationAssert<S extends AbstractBodyDeclarationAssert<S, A>, A extends BodyDeclaration>
        extends AbstractNodeAssert<S, A> {
    AbstractBodyDeclarationAssert(A actual, Class<?> selfType) {
        super(actual, selfType);
    }

    AbstractBodyDeclarationAssert(A actual) {
        this(actual, AbstractBodyDeclarationAssert.class);
    }

    public ClassOrInterfaceDeclarationAssert asClassOrInterfaceDeclaration() {
        return AstAssertions.assertThat(asInstanceOf(ClassOrInterfaceDeclaration.class))
                .as(navigationDescription("(ClassOrInterfaceDeclaration)"));
    }

    public ConstructorDeclarationAssert asConstructorDeclaration() {
        return AstAssertions.assertThat(asInstanceOf(ConstructorDeclaration.class))
                .as(navigationDescription("(ConstructorDeclaration)"));
    }

    public MethodDeclarationAssert asMethodDeclaration() {
        return AstAssertions.assertThat(asInstanceOf(MethodDeclaration.class))
                .as(navigationDescription("(MethodDeclaration)"));
    }

    public FieldDeclarationAssert asFieldDeclaration() {
        return AstAssertions.assertThat(asInstanceOf(FieldDeclaration.class))
                .as(navigationDescription("(FieldDeclaration)"));
    }
}
