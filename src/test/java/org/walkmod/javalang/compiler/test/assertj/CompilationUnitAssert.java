package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;

public class CompilationUnitAssert extends AbstractNodeAssert<CompilationUnitAssert, CompilationUnit> {
    CompilationUnitAssert(CompilationUnit actual) {
        super(actual, CompilationUnitAssert.class);
    }

    public ExtListAssert<ImportDeclarationAssert, ImportDeclaration> imports() {
        return AssertUtil.assertThat(actual.getImports(), ImportDeclarationAssert.class,
                navigationDescription("imports"));
    }

    public ExtListAssert<TypeDeclarationAssert, TypeDeclaration> types() {
        return AssertUtil.assertThat(actual.getTypes(), TypeDeclarationAssert.class, navigationDescription("types"));
    }

    public AbstractTypeDeclarationAssert<?, ?> getType(int index) {
        return types().item(index);
    }
}
