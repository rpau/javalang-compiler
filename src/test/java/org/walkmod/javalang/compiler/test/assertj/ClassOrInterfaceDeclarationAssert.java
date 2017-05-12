package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;

public class ClassOrInterfaceDeclarationAssert
        extends AbstractTypeDeclarationAssert<ClassOrInterfaceDeclarationAssert, ClassOrInterfaceDeclaration> {
    ClassOrInterfaceDeclarationAssert(ClassOrInterfaceDeclaration actual) {
        super(actual, ClassOrInterfaceDeclarationAssert.class);
    }

    public SymbolDataAssert symbolData() {
        return symbolData(actual);
    }
}
