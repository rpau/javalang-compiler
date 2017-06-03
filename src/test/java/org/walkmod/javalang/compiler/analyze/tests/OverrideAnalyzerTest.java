/*
 * Copyright (C) 2015 Raquel Pau
 * 
 * Walkmod is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Walkmod. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.walkmod.javalang.compiler.analyze.tests;

import org.junit.Test;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.stmt.TypeDeclarationStmt;
import org.walkmod.javalang.test.SemanticTest;
import org.walkmod.javalang.compiler.analyze.OverrideAnalyzer;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OverrideAnalyzerTest extends SemanticTest {

    @Test
    public void testOverrideOnSimpleClass() throws Exception {
        CompilationUnit cu = compile("public class Foo{ " + "public String toString(){ return \"\"; }" + " }");
        MethodDeclaration method = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        assertMethods(method, methods(Object.class.getMethod("toString")));
    }

    @Test
    public void testOverrideNotRequired() throws Exception {
        CompilationUnit cu =
                compile("public class Foo{ " + "public boolean equalsTo(Object o){ return false; }" + " }");
        MethodDeclaration method = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        assertNoMethods(method);
    }

    @Test
    public void testOverrideWithSimpleParameter() throws Exception {
        CompilationUnit cu = compile("public class Foo{ " + "public boolean equals(Object o){ return false; }" + " }");
        MethodDeclaration method = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        assertMethods(method, methods(Object.class.getMethod("equals", Object.class)));
    }

    @Test
    public void testOverrideWithInheritance() throws Exception {

        String fooCode = "public class Foo extends Bar{ " + "public void doSomething(){}" + " }";

        String barCode = "public class Bar{ " + "public void doSomething(){}" + " }";

        CompilationUnit cu = compile(fooCode, barCode);
        final TypeDeclaration type = cu.getTypes().get(0);
        final MethodDeclaration method = (MethodDeclaration) type.getMembers().get(0);
        Class<?> overriddenClass = ((ClassOrInterfaceDeclaration) type).getExtends().get(0).getSymbolData().getClazz();

        assertMethods(method, methods(overriddenClass.getMethod("doSomething")));
    }

    @Test
    public void testOverrideWithInterfaces() throws Exception {

        String fooCode = "public class Foo implements Bar{ " + "public void doSomething(){}" + " }";

        String barCode = "public interface Bar{ " + "public void doSomething();" + " }";

        CompilationUnit cu = compile(fooCode, barCode);
        final TypeDeclaration type = cu.getTypes().get(0);
        final MethodDeclaration method = (MethodDeclaration) type.getMembers().get(0);
        Class<?> implemented = ((ClassOrInterfaceDeclaration) type).getImplements().get(0).getSymbolData().getClazz();

        assertMethods(method, methods(implemented.getMethod("doSomething")));
    }

    @Test
    public void testInterfaceOverridesInterfaces() throws Exception {

        String fooCode = "public interface Foo extends Bar{ " + "public void doSomething();" + " }";

        String barCode = "public interface Bar{ " + "public void doSomething();" + " }";

        CompilationUnit cu = compile(fooCode, barCode);
        final TypeDeclaration type = cu.getTypes().get(0);
        final MethodDeclaration method = (MethodDeclaration) type.getMembers().get(0);
        Class<?> override = ((ClassOrInterfaceDeclaration) type).getExtends().get(0).getSymbolData().getClazz();

        assertMethods(method, methods(override.getMethod("doSomething")));
    }

    @Test
    public void testOverrideWithGenerics() throws Exception {

        String fooCode = "import java.util.List; public class Foo implements Bar<List>{ "
                + "public void doSomething(List l){}" + " }";

        String barCode = "import java.util.Collection; public interface Bar<T extends Collection>{ "
                + "public void doSomething(T c);" + " }";

        CompilationUnit cu = compile(fooCode, barCode);
        final TypeDeclaration type = cu.getTypes().get(0);
        final MethodDeclaration method = (MethodDeclaration) type.getMembers().get(0);
        Class<?> implemented = ((ClassOrInterfaceDeclaration) type).getImplements().get(0).getSymbolData().getClazz();

        assertMethods(method, methods(implemented.getMethod("doSomething", Collection.class)));
    }

    @Test
    public void testOverrideWithArrays() throws Exception {

        String fooCode = "import java.util.List; public class Foo implements Bar<List>{ "
                + "public void doSomething(List[] l){}" + " }";

        String barCode = "import java.util.Collection; public interface Bar<T extends Collection>{ "
                + "public void doSomething(T[] c);" + " }";

        CompilationUnit cu = compile(fooCode, barCode);
        final TypeDeclaration type = cu.getTypes().get(0);
        final MethodDeclaration method = (MethodDeclaration) type.getMembers().get(0);
        Class<?> implemented = ((ClassOrInterfaceDeclaration) type).getImplements().get(0).getSymbolData().getClazz();

        assertMethods(method, methods(implemented.getMethod("doSomething", Collection[].class)));
    }

    @Test
    public void testOverrideWithArraysDimensions() throws Exception {

        String fooCode = "import java.util.List; public class Foo extends Bar<List>{ "
                + "public void doSomething(List[] l){}" + " }";

        String barCode = "import java.util.Collection; public class Bar<T extends Collection>{ "
                + "public void doSomething(T[][] c){}" + " }";

        CompilationUnit cu = compile(fooCode, barCode);
        final TypeDeclaration type = cu.getTypes().get(0);
        final MethodDeclaration method = (MethodDeclaration) type.getMembers().get(0);

        assertNoMethods(method);
    }

    @Test
    public void testOverrideWithDynamicArgs() throws Exception {

        String fooCode = "import java.util.List; public class Foo extends Bar<List>{ "
                + "public void doSomething(List... l){}" + " }";

        String barCode = "import java.util.Collection; public class Bar<T extends Collection>{ "
                + "public void doSomething(T... c){}" + " }";

        CompilationUnit cu = compile(fooCode, barCode);
        final TypeDeclaration type = cu.getTypes().get(0);
        final MethodDeclaration method = (MethodDeclaration) type.getMembers().get(0);
        Class<?> extended = ((ClassOrInterfaceDeclaration) type).getExtends().get(0).getSymbolData().getClazz();

        assertMethods(method, methods(extended.getMethod("doSomething", Collection[].class)));
    }

    @Test
    public void testOverrideOnSimpleInnerClass() throws Exception {
        CompilationUnit cu =
                compile("public class Foo{ class Bar {" + "public String toString(){ return \"\"; }" + " }}");

        BodyDeclaration innerClass = cu.getTypes().get(0).getMembers().get(0);
        MethodDeclaration method = (MethodDeclaration) ((ClassOrInterfaceDeclaration) innerClass).getMembers().get(0);

        assertMethods(method, methods(Object.class.getMethod("toString")));
    }

    @Test
    public void testOverrideOnSimpleTypeDeclarationStmt() throws Exception {
        CompilationUnit cu = compile("public class Foo{ public void something() {class Bar {"
                + "public String toString(){ return \"\"; }" + " }}}");

        MethodDeclaration firstMethod = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        TypeDeclarationStmt stmt = (TypeDeclarationStmt) firstMethod.getBody().getStmts().get(0);

        MethodDeclaration method = (MethodDeclaration) stmt.getTypeDeclaration().getMembers().get(0);

        assertMethods(method, methods(Object.class.getMethod("toString")));
    }

    @Test
    public void testCorrectOverrideWithSubtypes() throws Exception {
        CompilationUnit cu = compile("public class Foo { public boolean equals(Foo foo){ return false;} }");

        MethodDeclaration method = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        assertNoMethods(method);
    }

    @Test
    public void testStaticMethods() throws Exception {
        CompilationUnit cu =
                compile("public class Bar extends Foo{ public static void setTestMode(boolean testMode)  {}}",
                        "public class Foo { public static void setTestMode(boolean testMode) { }}");
        MethodDeclaration method = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        assertNoMethods(method);
    }

    @Test
    public void testIssue3() throws Exception {
        CompilationUnit cu = compile(
                "import java.util.Hashtable; final class ThreadLocalMap extends ThreadLocal { public final Object childValue(Object parentValue) { return null; }}");
        MethodDeclaration method = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        assertNoMethods(method);
    }

    @Test
    public void testIssue3Generics() throws Exception {
        CompilationUnit cu = compile("public class C extends Comparator{ public void compare(java.util.List x){}}",
                "public class Comparator<T>{ public void compare(T x){} }");
        MethodDeclaration method = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

        assertNoMethods(method);
    }

    @Test
    public void testGenericCollectionParameterOverrideBug() throws Exception {
        CompilationUnit cu = compile(""
                        + "import java.util.Collection;"
                        + "import java.util.HashSet;"
                        + "public class A extends HashSet<String> {"
                        + " public boolean removeAll(Collection<?> x) { return false; }"
                        + " public boolean removeAll(HashSet<String> x) { return false; }"
                        + "}");
        MethodDeclaration overwrittenMethod = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        assertMethods(overwrittenMethod, methods(HashSet.class.getMethod("removeAll", Collection.class)));

        MethodDeclaration unrelatedMethod = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        assertNoMethods(unrelatedMethod);
    }

    @Test
    public void testAnonymousClass() throws Exception {

        CompilationUnit cu = compile(
                "public class Foo {" + " public void bye() {}" + " abstract static class Scanner{ void hello(){} }"
                        + " static final class DefaultScanner extends Scanner { void bye() {} } }");
        final TypeDeclaration type = cu.getTypes().get(0);
        final ClassOrInterfaceDeclaration extendingClass = (ClassOrInterfaceDeclaration) type.getMembers().get(2);
        final MethodDeclaration method = (MethodDeclaration) extendingClass.getMembers().get(0);

        assertNoMethods(method);
    }

    private static void assertNoMethods(MethodDeclaration method) throws Exception {
        assertMethods(method, Collections.<Method>emptyList());
    }

    private static void assertMethods(MethodDeclaration method, final List<Method> expectedOverriddenMethods)
            throws Exception {
        assertEquals(expectedOverriddenMethods, OverrideAnalyzer.findOverriddenMethods(method));
        if (expectedOverriddenMethods.isEmpty()) {
            assertFalse(OverrideAnalyzer.isMethodOverride(method));
        } else {
            assertTrue(OverrideAnalyzer.isMethodOverride(method));
        }
    }

    private static List<Method> methods(final Method... methods) throws Exception {
        return asList(methods);
    }
}
