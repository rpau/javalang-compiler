package org.walkmod.javalang.compiler.types;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class TypesLoaderVisitorTest {
    @Test
    public void testRegistrationSymbolName() {
        assertName("A$B$Derived", "A.B.Derived");
        assertName("java.lang.String", "String");
        assertName("a.A.B", "B");
        assertName("bar.Foo$Bar", "Foo.Bar");

        assertName("bar.ExtensionPoint$LegacyInstancesAreScopedToHudson",
                "ExtensionPoint.LegacyInstancesAreScopedToHudson");
        // import bar.ExtensionPoint.LegacyInstancesAreScopedToHudson
        assertImportName("bar.ExtensionPoint$LegacyInstancesAreScopedToHudson", "LegacyInstancesAreScopedToHudson");

        // import java.util.Arrays, inner class
        assertImportName("java.util.Arrays$LegacyMergeSort", "Arrays.LegacyMergeSort", true);
    }

    private void assertName(String name, String expectedRegistrationName) {
        assertThat(TypesLoaderVisitor.resolveSymbolName(name, false, false))
                .describedAs("registrationSymbolName(%s, false)", name).isEqualTo(expectedRegistrationName);
    }

    private void assertImportName(String name, String expectedRegistrationName) {
        assertThat(TypesLoaderVisitor.resolveSymbolName(name, true, false))
                .describedAs("registrationSymbolName(%s, false)", name).isEqualTo(expectedRegistrationName);
    }

    private void assertImportName(String name, String expectedRegistrationName, final boolean importedInner) {
        assertThat(TypesLoaderVisitor.resolveSymbolName(name, true, importedInner))
                .describedAs("registrationSymbolName(%s, false)", name).isEqualTo(expectedRegistrationName);
    }
}
