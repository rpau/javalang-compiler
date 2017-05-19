package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.Assertions;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.SymbolReference;

public class ImportDeclarationAssert extends AbstractNodeAssert<ImportDeclarationAssert, ImportDeclaration> {
    public ImportDeclarationAssert(ImportDeclaration actual) {
        super(actual, ImportDeclarationAssert.class);
    }

    public ExtListAssert<SymbolReferenceAssert, SymbolReference> usages() {
        return AssertUtil.assertThat(actual.getUsages(), SymbolReferenceAssert.class, navigationDescription("usages"));
    }

    public ImportDeclarationAssert hasName(String name) {
        Assertions.assertThat(actual.getName().getName()).as(navigationDescription("name")).isEqualTo(name);
        return this;
    }

    public NameExprAssert name() {
        return AstAssertions.assertThat(actual.getName()).as(navigationDescription("name"));
    }
}
