package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.stmt.TypeDeclarationStmt;

public class TypeDeclarationStmtAssert extends AbstractStatementAssert<TypeDeclarationStmtAssert, TypeDeclarationStmt> {
    TypeDeclarationStmtAssert(TypeDeclarationStmt actual) {
        super(actual, TypeDeclarationStmtAssert.class);
    }

    public AbstractTypeDeclarationAssert<?, ?> typeDeclaration() {
        return AstAssertions.assertThat(actual.getTypeDeclaration()).as(navigationDescription("typeDeclaration"));
    }
}
