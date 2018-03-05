/*
 * Copyright (C) 2015 Raquel Pau and Albert Coroleu.
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
package org.walkmod.javalang.compiler;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.lang.model.SourceVersion;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.ArrayCreationExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.ExplicitConstructorInvocationStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.stmt.TryStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.actions.ReferencesCounterAction;
import org.walkmod.javalang.compiler.providers.RemoveUnusedSymbolsProvider;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.symbols.SymbolVisitorAdapter;
import org.walkmod.javalang.compiler.test.assertj.AstAssertions;
import org.walkmod.javalang.compiler.test.assertj.BlockStmtAssert;
import org.walkmod.javalang.compiler.test.assertj.ClassOrInterfaceDeclarationAssert;
import org.walkmod.javalang.compiler.test.assertj.ExtListAssert;
import org.walkmod.javalang.compiler.test.assertj.FieldDeclarationAssert;
import org.walkmod.javalang.compiler.test.assertj.MethodCallExprAssert;
import org.walkmod.javalang.compiler.test.assertj.StatementAssert;
import org.walkmod.javalang.compiler.test.assertj.VariableDeclarationExprAssert;
import org.walkmod.javalang.test.SemanticTest;
import org.walkmod.javalang.util.FileUtils;

import static org.walkmod.javalang.compiler.test.assertj.AstAssertions.assertThat;

public class SymbolVisitorAdapterTest extends SymbolVisitorAdapterTestSupport {

    @Test
    public void testNoActions() throws Exception {

        SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
        String[] classpathEntries = System.getProperty("java.class.path").split(File.pathSeparator);
        URL[] classpath = new URL[classpathEntries.length];
        int i = 0;
        for (String entry : classpathEntries) {
            classpath[i] = new File(entry).toURI().toURL();
            i++;
        }
        visitor.setClassLoader(new URLClassLoader(classpath));
        visitor.setSymbolActions(new LinkedList<SymbolAction>());

        File aux =
                new File(new File("src/main/java"), "org/walkmod/javalang/compiler/symbols/SymbolVisitorAdapter.java");
        CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

        visitor.visit(cu, new HashMap<String, Object>());
        Assert.assertTrue(true);
    }

    @Test
    public void testReferencesCounterAction() throws Exception {
        SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();

        String[] classpathEntries = System.getProperty("java.class.path").split(File.pathSeparator);
        URL[] classpath = new URL[classpathEntries.length];
        int i = 0;
        for (String entry : classpathEntries) {
            classpath[i] = new File(entry).toURI().toURL();
            i++;
        }
        visitor.setClassLoader(new URLClassLoader(classpath));
        visitor.setSymbolActions(new LinkedList<SymbolAction>());

        ReferencesCounterAction counter = new ReferencesCounterAction();
        List<SymbolAction> actions = new LinkedList<SymbolAction>();
        actions.add(counter);
        visitor.setSymbolActions(actions);

        File aux =
                new File(new File("src/main/java"), "org/walkmod/javalang/compiler/symbols/SymbolVisitorAdapter.java");
        CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

        visitor.visit(cu, new HashMap<String, Object>());
        Assert.assertTrue(counter.getReadsCounter() > 0);
    }

    private CompilationUnit runRemoveUnusedMembers(String... code) throws Exception {

        CompilationUnit cu = compile(code);
        SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
        RemoveUnusedSymbolsProvider provider = new RemoveUnusedSymbolsProvider();

        visitor.setClassLoader(getClassLoader());
        visitor.setActionProvider(provider);
        visitor.visit(cu, new HashMap<String, Object>());
        return cu;
    }

    public void populateSemantics() throws Exception {}

    @Test
    public void testRemoveUnusedMethods() throws Exception {
        /*
         * TODO: cadena de dependencias: que pasa si lo q eliminamos hace
         * referencia a otro elemento privado que sólo referencia el que
         * eliminamos?
         */
        CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private void bar(){} }");
        Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
    }

    @Test
    public void testRemoveUnusedMethods1() throws Exception {

        CompilationUnit cu = runRemoveUnusedMembers(
                "public class Foo { private void bar(){} private String getName() { return \"name\";}}");
        Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
    }

    @Test
    public void testRemoveUnusedMethods2() throws Exception {

        CompilationUnit cu = runRemoveUnusedMembers(
                "public class Foo { private void bar(){} public String getName() { return \"name\";}}");
        Assert.assertEquals(1, cu.getTypes().get(0).getMembers().size());
    }

    @Test
    public void testRemoveUnusedMethods3() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers(
                "public class Foo { private void bar(){} public String getName() { bar(); return \"name\";}}");
        Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
    }

    @Test
    public void testRemoveUnusedMethods4() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers(
                "public class Foo { private void bar(String s){} public String getName() { bar(null); return \"name\";}}");
        Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
    }

    @Test
    public void testRemoveUnusedFields() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private String bar; }");
        Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
    }

    public void testRemoveUnusedFields1() throws Exception {
        CompilationUnit cu =
                runRemoveUnusedMembers("public class Foo { private String bar; public String getBar(){ return bar; }}");
        Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
    }

    @Test
    public void testRemoveUnusedTypes() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private class Bar{} }");
        Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
    }

    @Test
    public void testRemoveUnusedVariables() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers("public class Foo { public void bar(){ int i;} }");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertTrue(md.getBody().getStmts().isEmpty());

    }

    @Test
    public void testRemoveUnusedImports() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers("import java.util.List; public class Foo {  }");
        Assert.assertTrue(cu.getImports().isEmpty());
    }

    @Test
    public void testDifferentReferenceTypesUnderTheSameName() throws Exception {
        // the latest one is the unique that can be referenced
        CompilationUnit cu = runRemoveUnusedMembers(
                "public class Foo { private class A{} private String A =\"a\"; public void bar(){ A=\"b\";} }");
        Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
    }

    @Test
    public void testStaticImportsWithWildcard() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers(
                "import static java.lang.Math.*; public class HelloWorld { private double compute = PI; private double foo() { return (PI * pow(2.5,2));} }");
        Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
        Assert.assertTrue(!cu.getImports().isEmpty());
    }

    @Test
    public void testStaticImportsWithSpecificMember() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers(
                "import static java.lang.Math.PI; public class HelloWorld { private double compute = PI; private double foo() { return (PI * 2);} }");
        Assert.assertTrue(!cu.getImports().isEmpty());
    }

    @Test
    public void testImportsOfAnnotations() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers(
                "import javax.annotation.Generated; @Generated(value=\"WALKMOD\") public class Foo {}");
        Assert.assertTrue(!cu.getImports().isEmpty());

    }

    @Test
    public void testImportsOfAnnotations2() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers("import javax.annotation.Generated; public class Foo {}");
        Assert.assertTrue(cu.getImports().isEmpty());
    }

    @Test
    public void testImportsOfJavadocTypes() throws Exception {
        String javadoc = " Returns an ordering that compares objects according to the order "
                + "in which they appear in the\n"
                + "given list. Only objects present in the list (according to {@link Object#equals}) may be\n"
                + "compared. This comparator imposes a \"partial ordering\" over the type {@code T}. Subsequent\n"
                + "changes to the {@code valuesInOrder} list will have no effect on the returned comparator. Null\n"
                + "values in the list are not supported.\n\n" + "<p>\n"
                + "The returned comparator throws an {@link ClassCastException} when it receives an input\n"
                + "parameter that isn't among the provided values.\n\n" + "<p>\n*"
                + "The generated comparator is serializable if all the provided values are serializable.\n" +

                " @param valuesInOrder the values that the returned comparator will be able to compare, in the\n"
                + "order the comparator should induce\n" + " @return the comparator described above\n"
                + " @throws NullPointerException if any of the provided values is null\n"
                + " @throws IllegalArgumentException if {@code valuesInOrder} contains any duplicate values\n"
                + " (according to {@link Object#equals})\n*";

        String code = "public class Foo { /**" + javadoc + "/ public void foo(){}}";
        runRemoveUnusedMembers(code);

        javadoc = "This class provides a skeletal implementation of the {@code Cache} interface to minimize the\n"
                + " effort required to implement this interface.\n\n" + " <p>\n"
                + " To implement a cache, the programmer needs only to extend this class and provide an\n"
                + " implementation for the {@link #get(Object)} and {@link #getIfPresent} methods.\n"
                + " {@link #getUnchecked}, {@link #get(Object, Callable)}, and {@link #getAll} are implemented in\n"
                + " terms of {@code get}; {@link #getAllPresent} is implemented in terms of {@code getIfPresent};\n"
                + " {@link #putAll} is implemented in terms of {@link #put}, {@link #invalidateAll(Iterable)} is\n"
                + " implemented in terms of {@link #invalidate}. The method {@link #cleanUp} is a no-op. All other\n"
                + " methods throw an {@link UnsupportedOperationException}.";

        code = "import java.util.concurrent.Callable; public class Foo {/**" + javadoc + "*/ public void foo(){}}";
        CompilationUnit cu = runRemoveUnusedMembers(code);
        Assert.assertTrue(!cu.getImports().isEmpty());

        javadoc = "Returns a comparator that compares two arrays of unsigned {@code int} values lexicographically.\n"
                + " That is, it compares, using {@link #compare(int, int)}), the first pair of values that follow\n"
                + " any common prefix, or when one array is a prefix of the other, treats the shorter array as the\n"
                + " lesser. For example, {@code [] < [1] < [1, 2] < [2] < [1 << 31]}.\n" +

                " <p>\n" + " The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays\n"
                + " support only identity equality), but it is consistent with {@link Arrays#equals(int[], int[])}.\n" +

                " @see <a href=\"http://en.wikipedia.org/wiki/Lexicographical_order\"> Lexicographical order\n"
                + "      article at Wikipedia</a>\n";
        code = "import java.util.Arrays; public class Foo {/**" + javadoc + "*/ public void foo(){}}";
        cu = runRemoveUnusedMembers(code);
        Assert.assertTrue(!cu.getImports().isEmpty());
    }

    @Test
    public void testImportsOfJavadocMultipleLinesTypes() throws Exception {
        String javadoc =
                "\n*  Weak reference with a {@code finalizeReferent()} method which a background thread invokes after\n"
                        + " * the garbage collector reclaims the referent. This is a simpler alternative to using a {@link\n"
                        + " * Arrays}.\n";
        String code = "import java.util.Arrays; public class Foo {/**" + javadoc + "*/ public void foo(){}}";
        CompilationUnit cu = runRemoveUnusedMembers(code);
        Assert.assertTrue(!cu.getImports().isEmpty());
    }

    @Test
    public void testReferencesToEnum() throws Exception {
        String code = "public enum Foo { A, B; private void bar(Foo o){} public void bar2(){ bar(Foo.B);}}";
        CompilationUnit cu = runRemoveUnusedMembers(code);
        Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
    }

    @Test
    public void testMethodReferences() throws Exception {
        String consumerCode = "private interface Consumer{ public void accept(String t); } ";
        String methodToReferece = "private static void printNames(String name) {System.out.println(name);}";
        String methodCode = "public void run(){ Consumer consumer = A::printNames; }";
        String code = "public class A{ " + consumerCode + methodToReferece + methodCode + "}";
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            CompilationUnit cu = runRemoveUnusedMembers(code);
            Assert.assertEquals(3, cu.getTypes().get(0).getMembers().size());
        }
    }

    @Test
    public void testLambdaExpressions() throws Exception {
        String code =
                "public class A{ private interface C{ public int get(int c); } public void run(){ C a = (b)->b; } }";
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            CompilationUnit cu = runRemoveUnusedMembers(code);
            Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
        }
    }

    @Test
    public void testReferencesToAnnotationMembers() throws Exception {
        String code = "import java.lang.annotation.ElementType;\n" + "import java.lang.annotation.Retention;\n"
                + "import java.lang.annotation.RetentionPolicy;\n" + "import java.lang.annotation.Target;\n"
                + "@Retention(RetentionPolicy.RUNTIME)\n" + "@Target(ElementType.METHOD)\n"
                + "public @interface Foo { public boolean enabled() default true; }";
        CompilationUnit cu = runRemoveUnusedMembers(code);
        Assert.assertTrue(!cu.getImports().isEmpty());
    }

    @Test
    public void testAnnonymousClass() throws Exception {
        String code =
                "public class Foo{ public void bar() { Foo o = new Foo() { private String name; public void bar() { System.out.println(\"hello\"); }};}}";
        CompilationUnit cu = run(code);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        VariableDeclarationExpr expr = (VariableDeclarationExpr) stmt.getExpression();
        ObjectCreationExpr oce = (ObjectCreationExpr) expr.getVars().get(0).getInit();
        // The name attribute should be removed
        FieldDeclaration fd = (FieldDeclaration) oce.getAnonymousClassBody().get(0);

        Assert.assertNull(fd.getUsages());
    }

    @Test
    public void testMethodsOverwritingInAnnonymousClasses() throws Exception {
        String code =
                "public class A{ public Object get() { return null; } public A foo = new A() { public String get(){ return name();} private String name(){ return \"hello\"; }};}";
        CompilationUnit cu = run(code);
        BodyDeclaration bd = cu.getTypes().get(0).getMembers().get(1);
        FieldDeclaration aux = (FieldDeclaration) bd;
        ObjectCreationExpr expr = (ObjectCreationExpr) aux.getVariables().get(0).getInit();
        MethodDeclaration md = (MethodDeclaration) expr.getAnonymousClassBody().get(1);
        Assert.assertNotNull(md.getUsages());
    }

    @Test
    public void testAnonymousArrayExpressions() throws Exception {
        CompilationUnit cu = run("public class A{ Integer v[][] = { new Integer[] {3} }; Integer a[] = v[0]; }");
        FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0).getMembers().get(0);
        SymbolData sd = fd.getType().getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals(Integer.class.getName(), sd.getName());
        SymbolData sd2 = fd.getVariables().get(0).getInit().getSymbolData();
        Assert.assertNotNull(sd2);
        Assert.assertEquals(Integer.class.getName(), sd2.getName());
        Assert.assertEquals(2, sd2.getArrayCount());
    }

    @Test
    public void testMulticatchStatements() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            CompilationUnit cu = run("import java.io.*; public class A { "
                    + "void test() throws FileNotFoundException, ClassNotFoundException {} "
                    + "public void run (){ try{ test(); }catch(FileNotFoundException|ClassNotFoundException e){}}}");
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
            TryStmt stmt = (TryStmt) md.getBody().getStmts().get(0);
            SymbolData sd = stmt.getCatchs().get(0).getExcept().getSymbolData();
            Assert.assertNotNull(sd);
            List<Class<?>> bounds = sd.getBoundClasses();
            Assert.assertEquals("java.io.FileNotFoundException", bounds.get(0).getName());
            Assert.assertEquals("java.lang.ClassNotFoundException", bounds.get(1).getName());
        }
    }

    @Test
    public void testGenericClasses() throws Exception {
        CompilationUnit cu = run("public class Box<T> {" + "private T t;" + "public T get() {return t;}"
                + "public void set(T t) { this.t = t;}" + "}");

        FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0).getMembers().get(0);
        SymbolData sd = fd.getType().getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals(true, sd.isTemplateVariable());
        Assert.assertEquals("java.lang.Object", sd.getName());
    }

    @Test
    public void testGenericClassesWithBounds() throws Exception {
        CompilationUnit cu =
                run("import java.util.*;import java.io.*; " + "public class Foo<A extends Map<String, Object>>"
                        + " extends LinkedList<A> implements Serializable{}");
        ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);
        List<ClassOrInterfaceType> implementsList = declaration.getImplements();
        SymbolData serializable = implementsList.get(0).getSymbolData();
        Assert.assertNotNull(serializable);
        Assert.assertEquals("java.io.Serializable", serializable.getName());
        List<ClassOrInterfaceType> extendsList = declaration.getExtends();
        SymbolData sd = extendsList.get(0).getSymbolData();
        Assert.assertEquals(false, sd.isTemplateVariable());
        Assert.assertEquals("java.util.LinkedList", sd.getName());
        List<SymbolData> params = sd.getParameterizedTypes();
        Assert.assertEquals(1, params.size());
        Assert.assertEquals("java.util.Map", params.get(0).getName());
        params = params.get(0).getParameterizedTypes();
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("java.lang.String", params.get(0).getName());
        Assert.assertEquals("java.lang.Object", params.get(1).getName());
    }

    @Test
    public void testMethodResolution() throws Exception {
        CompilationUnit cu = run("public class A { public String getName() { return \"hello\"; }}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertNotNull(md.getSymbolData().getMethod());
        Assert.assertEquals("getName", md.getSymbolData().getMethod().getName());
    }

    @Test
    public void testConstructorResolution() throws Exception {
        CompilationUnit cu = run("public class A { private String name; public A(String name){this.name = name;}}");
        ConstructorDeclaration cd = (ConstructorDeclaration) cu.getTypes().get(0).getMembers().get(1);
        Assert.assertNotNull(cd.getSymbolData().getConstructor());
        Assert.assertEquals(1, cd.getSymbolData().getConstructor().getParameterTypes().length);
    }

    @Test
    public void testExplicitThisConstructorInvocationResolution() throws Exception {
        CompilationUnit cu = run(
                "public class A { private String name; public A() { this(\"empty\"); } public A(String name){this.name = name;}}");
        ExplicitConstructorInvocationStmt st =
                (ExplicitConstructorInvocationStmt) ((ConstructorDeclaration) cu.getTypes().get(0).getMembers().get(1))
                        .getBlock().getStmts().get(0);
        Assert.assertNotNull(st.getSymbolData());
        Assert.assertEquals("public A(java.lang.String)", st.getSymbolData().getConstructor().toString());
    }

    @Test
    public void testExplicitSuperConstructorInvocationResolution() throws Exception {
        CompilationUnit cu = run("class A { private String name; public A(String name){this.name = name;}}"
                + "class B extends A { B() { super(\"empty\"); } } ");
        ExplicitConstructorInvocationStmt st =
                (ExplicitConstructorInvocationStmt) ((ConstructorDeclaration) cu.getTypes().get(1).getMembers().get(0))
                        .getBlock().getStmts().get(0);
        Assert.assertNotNull(st.getSymbolData());
        Assert.assertEquals("public A(java.lang.String)", st.getSymbolData().getConstructor().toString());
    }

    @Test
    public void testFieldResolution() throws Exception {
        CompilationUnit cu = run("public class A { private String name; }");
        FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertNotNull(fd.getFieldsSymbolData());
        Assert.assertEquals(1, fd.getFieldsSymbolData().size());
        Assert.assertEquals("name", fd.getFieldsSymbolData().get(0).getField().getName());
    }

    @Test
    public void testTypeResolution() throws Exception {
        CompilationUnit cu = run("public class A {}");
        TypeDeclaration td = (TypeDeclaration) cu.getTypes().get(0);
        Assert.assertNotNull(td.getSymbolData());
        Assert.assertEquals("A", td.getSymbolData().getName());
    }

    @Test
    public void testAnnotationResolution() throws Exception {
        CompilationUnit cu = run("public class A { @Override public String toString(){ return \"A\"; }}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        AnnotationExpr ann = md.getAnnotations().get(0);
        Assert.assertNotNull(ann.getSymbolData());
        Assert.assertEquals("java.lang.Override", ann.getSymbolData().getName());
    }

    @Test
    public void testGenericMethodResultType() throws Exception {
        CompilationUnit cu = run(
                "import java.util.List;import java.util.LinkedList; public class A { public List<? extends A> foo(){ return new LinkedList<A>();} public void bar() { foo().get(0); }}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);

        Assert.assertNotNull(stmt.getExpression().getSymbolData());
        Assert.assertEquals("A", stmt.getExpression().getSymbolData().getName());

    }

    @Test
    public void testGenericMethodClassParameter() throws Exception {
        run("import java.util.List;\n" + "\n" + "public class TypedMethod {\n"
                + "  public interface Mixin<T extends Mixin> {\n" + "}\n"
                + "public class AMixin implements Mixin<AMixin> {\n" + "  public List<String> getList() {\n"
                + "    return null;\n" + "  }\n" + "}\n"
                + "public <T extends Mixin> T mixin(Class<? extends  T > mixin) {\n" + "  return null;\n" + "}\n"
                + "public void m() {\n" + "  mixin(AMixin.class).getList().get(0);\n" + "}\n" + "}\n");
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithArrayClassParameter() throws Exception {
        run("public class A {\n" + "  void setContent(byte[] bytes) {}\n"
                + "  <T> T getBody(Class<T> type) { return null; }\n"
                + "  void f() { setContent(getBody(byte[].class)); }\n" + "}\n");
        Assert.assertTrue(true);
    }

    @Test
    public void testRawAndGenericSymbolsDiffer() throws Exception {
        CompilationUnit cu =
                run("import java.util.Collection;" + "public class A {" + "  Collection raw() { return null; }"
                        + "  Collection<java.io.Serializable> col() { return null; }" + "}");
        MethodDeclaration mdRaw = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        final MethodSymbolData sdRaw = mdRaw.getSymbolData();
        Assert.assertEquals("raw", sdRaw.getMethod().getName());
        MethodDeclaration mdCol = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        final MethodSymbolData sdCol = mdCol.getSymbolData();
        Assert.assertEquals("col", sdCol.getMethod().getName());

        Assert.assertNull(sdRaw.getParameterizedTypes());
        Assert.assertNotNull(sdCol.getParameterizedTypes());
    }

    @Test
    public void testGenericMethodWithRawArgument() throws Exception {
        run("import java.util.Collection;\n" + "    public class U {\n"
                + "        void print(Collection<java.io.Serializable> list) { }\n" + "        void m() {\n"
                + "            Raw raw = new Raw();\n" + "            print(raw.rawList());\n" + "        }\n"
                + "    }\n",
                "import java.util.Collection;\n" + "    public class Raw {\n"
                        + "        public Collection rawList() {return null;} \n" + "    }\n");
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericMethodResultType2() throws Exception {
        CompilationUnit cu = run(
                "import java.util.List;import java.util.LinkedList; public class A { public class B {} public class C {public List<? extends B> foo(){ return new LinkedList<B>();} public void bar() { foo().get(0);} }}");

        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(1);

        MethodDeclaration md = (MethodDeclaration) type.getMembers().get(1);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);

        Assert.assertNotNull(stmt.getExpression().getSymbolData());
        Assert.assertEquals("A$B", stmt.getExpression().getSymbolData().getName());

    }

    @Test
    public void testGenericMethodWithArrays() throws Exception {
        CompilationUnit cu = run("import java.util.Collections;\n" + "import java.util.Map;\n"
                + "import java.util.HashMap;\n" + " public class A {\n"
                + "  private final Map<String, Integer[]> params;\n" + "  public A(Map<String,Integer[]> p) {\n"
                + "    Map<String,Integer[]> m = new HashMap<String,Integer[]>(p);\n"
                + "    this.params = Collections.unmodifiableMap(m);\n" + "  }\n" + "}");

        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);

        ConstructorDeclaration md = (ConstructorDeclaration) type.getMembers().get(1);
        Assert.assertNotNull(md.getSymbolData());
    }

    @Test
    public void testInnerClassAttributesReferences() throws Exception {
        CompilationUnit cu = run(
                "public class A{ public Object foo() { return C.INSTANCE; } private static class C { private static C INSTANCE; }}");
        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(1);
        Assert.assertNotNull(type.getMembers());
        Assert.assertTrue(!type.getMembers().isEmpty());
    }

    @Test
    public void testInnerClassInsideAnnonymousClass() throws Exception {
        run("public class A{ public Object foo() {  A a = new A() { public Object foo() { return new B(); } class B{ int c; }}; return a; }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testAssertThatMethodChainingOfTypeVariableHasClazz() throws Exception {
        // ... otherwise NPE exceptions may be thrown in SymbolType.findSymbol
        CompilationUnit cu = run("import static org.assertj.core.api.Assertions.assertThat;\n"
                + "public class A { public static interface MyIf {} " + "public void f(MyIf a, MyIf b, MyIf c) {\n"
                + "assertThat(a).isNotNull().isNotSameAs(b).isNotSameAs(c);\n" + "}}");
        assertThat(cu).types().item(0).asClassOrInterfaceDeclaration().members().item(1).asMethodDeclaration()
                .body().stmts().item(0).asExpressionStmt().expression().asMethodCallExpr()
                .hasName("isNotSameAs")
                .has(hasSymbolDataWithClazz())
                .scope().asMethodCallExpr()
                .hasName("isNotSameAs")
                .has(hasSymbolDataWithClazz())
                .scope().asMethodCallExpr()
                .hasName("isNotNull")
                .has(hasSymbolDataWithClazz())
                .scope().asMethodCallExpr()
                .hasName("assertThat")
                .symbolData()
                .clazz().isNotNull();
    }

    private static Condition<MethodCallExpr> hasSymbolDataWithClazz() {
        return new Condition<MethodCallExpr>() {
            @Override
            public boolean matches(MethodCallExpr value) {
                Assertions.assertThat(value.getSymbolData().getClazz()).describedAs("symbolData.clazz").isNotNull();
                return true;
            }
        };
    }

    @Test
    public void testScopes() throws Exception {
        CompilationUnit cu = runRemoveUnusedMembers(
                "public class Foo { private int c; class A { int c; } class B extends A { public int x = c; }}");
        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);
        Assert.assertEquals(2, type.getMembers().size());
    }

    @Test
    public void testScopeOfSuperClassOfInnerBeforeStaticImport() throws Exception {
        CompilationUnit cu = run(
                "" + "import static java.util.Locale.getDefault;\n" + " public class A {\n"
                        + "  public void trans(String p) { }" + "  public class Derived extends Base {\n"
                        + "   private void foo() { trans(getDefault());}\n" + "  }\n" + " }",
                "public abstract class Base { public String getDefault() { return null; } }");
        Assert.assertTrue(true);
    }

    @Test
    public void testScopeOfSuperClassOfInnerStaticBeforeStaticImport() throws Exception {
        CompilationUnit cu = run(
                "" + "import static java.util.Locale.getDefault;\n" + " public class A {\n"
                        + "  public static void trans(String p) { }" + "  public static class Derived extends Base {\n"
                        + "   private void foo() { trans(getDefault());}\n" + "  }\n" + " }",
                "public abstract class Base { public String getDefault() { return null; } }");
        Assert.assertTrue(true);
    }

    @Test
    public void testScopeOfSuperClassOfInnerStaticConstructorBeforeStaticImport() throws Exception {
        CompilationUnit cu = run(
                "" + "import static java.util.Locale.getDefault;\n" + " public class A {\n"
                        + "  public static void trans(String p) { }" + "  public class B {"
                        + "   public class Derived extends Base {\n" + "    private Derived() { trans(getDefault());}\n"
                        + "   }\n" + "  }\n" + " }",
                "public abstract class Base { public String getDefault() { return null; } }");
        Assert.assertTrue(true);
    }

    @Test
    public void testScopeOfSuperClassOfLocalConstructorBeforeStaticImport() throws Exception {
        // TODO
        CompilationUnit cu = run(
                "" + "import stat" + "ic java.util.Locale.getDefault;\n" + " public class A {\n"
                        + "  public static void trans(String p) { }" + "  public static class B {" + "   public B() {"
                        + "    class Derived extends Base {\n" + "     private void foo() { trans(getDefault());}\n"
                        + "    }\n" + "   }\n" + "  }\n" + " }",
                "public abstract class Base { public String getDefault() { return null; } }");
    }

    @Test
    public void testScopeOfSuperOfAnonymousClassBeforeStaticImport() throws Exception {
        CompilationUnit cu = run(
                "" + "import static java.util.Locale.getDefault;\n" + " public class A {\n"
                        + "  public void trans(String p) { }" + "  private Base base = new Base() {\n"
                        + "   private void foo() { trans(getDefault());}\n" + "  };\n" + " }",
                "public abstract class Base { public String getDefault() { return null; } }");
        Assert.assertTrue(true);
    }

    @Test
    public void testScopeOfSuperOfAnonymousClass2BeforeStaticImport() throws Exception {
        CompilationUnit cu = run("" + "import static java.util.Locale.getDefault;\n" + " public class A {\n"
                + "  public void trans(String p) { }" + "  private void bar() {\n" + "   Base base = new Base() {\n"
                + "    public void foo() { trans(getDefault());}\n" + "   };\n" + "  };\n" + " }",
                "public abstract class Base { public String getDefault() { return null; } }");
        Assert.assertTrue(true);
    }

    @Test
    public void testTypeDeclarationStmts() throws Exception {
        run("public class A { public Object foo() { class B { int c = 0; int x = c;} return new B(); }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testTypeDeclarationStmtsWithAnonymousClass() throws Exception {
        run("public class A { public Object foo() { class B { public A get() {return new A() {public String toString(){ return \"hello\";}};}} return new B(); }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testInheritedAttributesInAnonymousClass() throws Exception {
        run("public class A { public class B { int c; } B aux = new B() { public int getC() { return c; } };}");
        Assert.assertTrue(true);
    }

    @Test
    public void testInvisibleMethodByReflection() throws Exception {
        run("public class A { public void foo() {sun.misc.Unsafe.getUnsafe();}}");
        Assert.assertTrue(true);
    }

    @Test
    public void testUpperBounds() throws Exception {
        run("import java.util.*; public class A { List<Collection> list; List add(List<? super List> list) { return list; }  void test() { add(list).isEmpty(); }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testInheritance() throws Exception {
        CompilationUnit cu = run(
                "public class A { private int var; private void foo() {} class B extends A { void bar() { foo(); var = 1;}}}");
        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);
        MethodDeclaration md = (MethodDeclaration) type.getMembers().get(1);
        Assert.assertNotNull(md.getUsages());
        FieldDeclaration fd = (FieldDeclaration) type.getMembers().get(0);
        Assert.assertNotNull(fd.getUsages());

    }

    @Test
    public void testGenericsInTheSameClass() throws Exception {
        run("public class A<T>{ T foo() { return null;} class B extends A<C> {} class C { void bar () { B b = new B(); b.foo().bar(); } } }");
        Assert.assertTrue(true);
    }

    @Test
    public void testRecursiveWildcards() throws Exception {
        run("import java.util.*; public class A { static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(Iterable<E> elements) { return orderedPermutations(elements);}}");
        Assert.assertTrue(true);

    }

    @Test
    public void testNestedCalls() throws Exception {
        run("public class A { B foo(){ return null;}  class B { void bar() {foo().bar();}}}");
        Assert.assertTrue(true);
    }

    @Test
    public void testCompositeGenerics() throws Exception {
        String mergeSorted =
                "public static <T> Iterable<T> mergeSorted(final Iterable<? extends Iterable<? extends T>> iterables){\n";
        mergeSorted += "A.transform(iterables, A.<T>toIterator()).iterator().next().next(); return null; }\n";

        String toIterator = "private static <T> Function<Iterable<? extends T>, Iterator<? extends T>> \n";
        toIterator += "toIterator() {\n";
        toIterator += "return null;\n";
        toIterator += "}\n";

        String transform =
                "public static <F, T> Iterable<T> transform(final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) { return null; }";

        run("import java.util.*; public class A { " + mergeSorted + toIterator + transform
                + " public class Function<F,T> {} }");

        Assert.assertTrue(true);
    }

    @Test
    public void testInheritedPamaterizedClass() throws Exception {
        run("import java.util.List; public class A{ interface B<T> extends List<List<T>> {} B<String> aux; String foo(List<List<String>> b){ return \"hello\"; } void bar() { foo(aux).toString(); }}");
        Assert.assertTrue(true);
    }

    @Test
    public void errorWithArrayCopy() throws Exception {
        String method = "static <T> T[] arraysCopyOf(T[] original, int newLength) {\n";
        method += "T[] copy = null;\n";
        method += "System.arraycopy(\n";
        method += "original, 0, copy, 0, Math.min(original.length, newLength));\n";
        method += "return copy;}";

        run("public class A { " + method + " }");
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsIntroducedByAnInnerClass() throws Exception {
        String innerClass = "class Reference<T> { public T get() { return null;}} ";
        String mainClass = "public class A { " + innerClass
                + " void foo() { Reference<Reference<String>> r = null; r.get().get().length(); }}";
        run(mainClass);
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithRewrittenTypeParams() throws Exception {
        String class1 = "class B<T> { public T get() { return null; }}";
        String class2 = "class C<T> extends B<B<T>> {}";
        String main = "public class A { " + class1 + " " + class2
                + " void foo () { C<String> c = null; c.get().get().trim().length(); }}";
        run(main);
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithRewrittenTypeParamsWithInheritance() throws Exception {
        String class1 = "class B<T> { public T get() { return null; }}";
        String class2 = "class C<T extends File> extends B<List<T>> { @Override public List<T> get() {return null;}}";
        String main = "import java.util.*; import java.io.File; public class A { " + class1 + " " + class2
                + " void foo () { C<File> c = null; c.get().iterator().next().getAbsolutePath().trim(); }}";
        run(main);
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithRewrittenTypeParams2() throws Exception {
        String class1 = "class B<T> { public T get() { return null; }}";
        String class2 = "class C extends B<B<V>> { void foo() { get().get();}}";
        String main = "public class A<V> { " + class2 + " " + class1 + " }";
        run(main);
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithRewrittenTypeParams3() throws Exception {
        String class1 = "interface B<K,V> { public V get(Object key); public Set<K> keySet(); }";
        String class2 = "class C extends HashMap<K, Set<V>> { }";
        String class3 = "class D extends C { void foo() { get(null).iterator().hasNext();} }";

        String main = "import java.util.*; public class A<K,V> { " + class1 + " " + class2 + " " + class3 + "}";
        run(main);
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithRewrittenTypeParams4() throws Exception {
        String classB = "class B<K, V> { K getKey() { return null; }}";
        String code = "import java.util.*; public class C<K extends java.io.File, V> { " + classB
                + " class A extends B<List<K>, V> { void foo(K value){ value.getAbsolutePath().trim();}}}";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithClassParameters() throws Exception {
        String class1 =
                "public static class Range { public static <C extends Collection<?>> List<C> range(C lower, boolean x, C upper, boolean y) { return null;}}";
        String code = "import java.util.*; public class A { " + class1
                + " void bar(){ Range.range(new ArrayList(), true, new ArrayList(), true).iterator().next().get(0).toString(); }}";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsWithClassParameters2() throws Exception {
        String class1 =
                "public static class Range { public static <C extends Collection<?>> C range(C lower, boolean x, C upper, boolean y) { return null;}}";
        String code = "import java.util.*; public class A { " + class1
                + " void bar(){ Range.range(new ArrayList(), true, new ArrayList(), true).get(0).toString(); }}";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testFieldMethodCallWithGenerics() throws Exception {
        String code =
                "import java.util.List; public class A<T> { List<A<? super T>> comparators; String foo() { comparators.get(0).foo().trim().length(); return null;}}";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testParameterTypeReferences() throws Exception {
        String code = "import java.util.List; public class A { void foo(List list){}}";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu.getImports().get(0).getUsages());
        Assert.assertEquals(1, cu.getImports().get(0).getUsages().size());
    }

    @Test
    public void testMethodOrdering() throws Exception {
        String code = "public class A { void foo(long i) {} void foo(int i) {} void bar(){ foo(1); }}";
        CompilationUnit cu = run(code);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertNull(md.getUsages());
        md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        Assert.assertNotNull(md.getUsages());
        Assert.assertEquals(1, md.getUsages().size());
    }

    @Test
    public void testConstructorOrdering() throws Exception {
        String code = "public class A { A(long i) {} A(int i) {} void bar(){ A aux = new A(1); }}";
        CompilationUnit cu = run(code);
        ConstructorDeclaration md = (ConstructorDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertNull(md.getUsages());
        md = (ConstructorDeclaration) cu.getTypes().get(0).getMembers().get(1);
        Assert.assertNotNull(md.getUsages());
        Assert.assertEquals(1, md.getUsages().size());
    }

    @Test
    public void testMethodOrdering2() throws Exception {
        String code = "public class A { void foo(long i) {} void foo(int i) {} void bar(){ foo(1L); }}";
        CompilationUnit cu = run(code);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertNotNull(md.getUsages());
        Assert.assertEquals(1, md.getUsages().size());
    }

    @Test
    public void testObjectMethodsInBasicArrays() throws Exception {
        String code = "public class A { void foo() { int[] c = null; c.toString().toString(); }}";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testObjectMethodsInBasicArrays2() throws Exception {
        String code = "public class A { void foo() { int[] c = null; int i = c.clone()[0]; }}";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testObjectMethodsInBasicArrays3() throws Exception {
        String code = "public class A { void bar(int aux){} void foo() { int[] c = null; bar(c.clone().length); }}";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testCloneMethodOfStringArray() throws Exception {
        // JLS 10.7 - The return type of the clone method of an array type T[] is T[].
        String code = "public class A { void f() { String[] c = null; c.clone(); }}";
        CompilationUnit cu = run(code);
        final MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        final ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(1);
        Assert.assertEquals("java.lang.String[]", stmt.getExpression().getSymbolData().toString());
    }

    @Test
    public void testArrayInitExprType() throws Exception {
        String code = "public class A { void foo() { int[] c = new int[10];}} ";
        CompilationUnit cu = run(code);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        VariableDeclarationExpr expr = (VariableDeclarationExpr) stmt.getExpression();
        ArrayCreationExpr array = (ArrayCreationExpr) expr.getVars().get(0).getInit();
        Assert.assertNotNull(array.getDimensions().get(0).getSymbolData());
    }

    @Test
    public void testArrayInitExprType2() throws Exception {
        String code = "public class A { private int x = 10; void foo() { int[] c = new int[x];}} ";
        CompilationUnit cu = runRemoveUnusedMembers(code);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        VariableDeclarationExpr expr = (VariableDeclarationExpr) stmt.getExpression();
        Assert.assertNotNull(expr.getVars().get(0).getInit().getSymbolData());
    }

    @Test
    public void testArrayArgumentsOrder() throws Exception {
        String code =
                "import java.util.Arrays; public class A { void foo( byte[] digest) { foo(Arrays.copyOf(digest, 3)); } } ";
        run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testMethodWithArrayArgsSignature() throws Exception {
        run("public class A { void write(char cbuf[], int off, int len) {} }");
        Assert.assertTrue(true);
    }

    @Test
    public void testWrappingTypes() throws Exception {
        run("public class A { private static int indexOf(short[] array, short target, int start, int end) { return 0; } void bar(short[] array, Short target,int start, int end) { int aux = A.indexOf(array,target,start, end) + 1; }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testArrayOfArrayResolution() throws Exception {
        run("public class StackedArrayParam {\n" + "    private int[][] rowGroups;\n" + "\n" + "    void f() {\n"
                + "        setRowGroups(new int[][] {{2, 4, 6, 8, 10, 12, 14}});\n" + "    }\n" + "\n"
                + "    public void setRowGroups(int[][] rowGroups) {\n" + "        this.rowGroups = rowGroups;\n"
                + "    }\n" + "}\n");
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsInsideAnonymousClasses() throws Exception {
        run("import java.util.Iterator; public class A { class C<T> {  Iterator<T> get() { return null; } } void bar() { C aux = new C<String>() { void test(String s) { get().next().concat(s).length(); }}; }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testGenericsInsideAnonymousClasses2() throws Exception {
        run("import java.util.Iterator; public class A { class C<T> {  Iterator<T> get() { return null; } } void bar() { C aux = new C<String>() { void test(String s) { super.get().next().concat(s).length(); }}; }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testTypeDeclarationStmts2() throws Exception {
        run("public enum A { B { public Object foo() { class D { int c = 0; int x = c;} return new D(); }}, C {}; public Object foo() { class B { int c = 0; int x = c;} return new B(); }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testConditionalArgument() throws Exception {
        run("public class A  { String newTypeVariableImpl(String name, Integer[] bounds){ return null; }  void newArtificialTypeVariable(String name, Integer... bounds) { newTypeVariableImpl(name, (bounds.length == 0)? new Integer[] { new Integer(1) }: bounds).concat(\"hello\").length(); }}  ");
        Assert.assertTrue(true);
    }

    @Test
    public void testSuperExpressionsWithClassContext() throws Exception {
        run("import java.util.*; public class A extends LinkedList{ void foo(){ A.super.add(null);}}");
        Assert.assertTrue(true);
    }

    @Test
    public void testInnerClassReferencesFromVarDeclarations() throws Exception {
        run("public class A { void bar() { B.C aux = new B.C(); aux.foo(); } private static final class B { private static final class C { void foo() {}}} }");
        Assert.assertTrue(true);
    }

    @Test
    public void testTypeCompatibilityWithGenericsAndBasicTypes() throws Exception {
        run("import java.util.*; public class A { void foo(List<Number> list) { boolean a = list.add(3) && true;} }");
        Assert.assertTrue(true);
    }

    @Test
    public void testUpperBoundsInAResolvedType() throws Exception {
        CompilationUnit cu = run(
                "import java.util.*; public class EquivalenceTester<T> { List<? super T> aux; EquivalenceTester(List<? super T> aux){ this.aux = aux;} void foo(){  EquivalenceTester.of(new LinkedList<String>()); } public static <T> EquivalenceTester<T> of(List<? super T> equivalence) {return new EquivalenceTester<T>(equivalence);}}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(2);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        SymbolData sd = stmt.getExpression().getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals(String.class.getName(), sd.getParameterizedTypes().get(0).getName());
        Assert.assertTrue(true);
    }

    @Test
    public void testFloats() throws Exception {
        CompilationUnit cu = run("public class A { void foo() { foo(1.0f); } void foo(float f){} }");
        Assert.assertTrue(true);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getArgs().get(0).getSymbolData();
        Assert.assertEquals(float.class.getName(), sd.getName());
    }

    @Test
    public void genericsWithBasicTypes() throws Exception {
        CompilationUnit cu = run("public class A { void foo() { java.util.Arrays.asList(1, 2, 3, null); } }");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals(Integer.class.getName(), sd.getParameterizedTypes().get(0).getName());
        Assert.assertTrue(true);
    }

    @Test
    public void testInnerClassesPlusTypeStmtPlusEnums() throws Exception {
        run("public enum A { B{}, C{}; void foo() { class B {} } void foo1() { class B{} } void foo2(){ class B{ class C{}} new B().new C().toString(); }  }");
        Assert.assertTrue(true);
    }

    @Test
    public void testInnerClassesInsideTypeStmts() throws Exception {
        run("public class A { void foo() { class B {} } void foo1() { class B{} } void foo2(){ class B{ class C{}} new B().new C().toString(); }  }");
        Assert.assertTrue(true);
    }

    @Test
    public void testInnerClassesInsideTypeStmts2() throws Exception {
        run("public class A { void foo() { class B {} } void foo1() { class C{} } void foo2(){ class B{ class C{}} new B().new C().toString(); }  }");
        Assert.assertTrue(true);
    }

    @Test
    public void testDynamicArraysInMethodCalls() throws Exception {
        CompilationUnit cu = run(
                "public class A { private byte[] toByteArray(int... bytes) { return null; }  private void assertWellFormed(int... bytes) { toByteArray(bytes); } } ");
        Assert.assertTrue(true);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertNotNull(md.getUsages());
    }

    @Test
    public void testInterfaceImplementationsWithGenerics() throws Exception {
        CompilationUnit cu = run(
                "public class A { interface B<T> { public T get();} class C implements B<String> { public String get (){return null;}} <F> F bar(B<F> b) { return null; } void car() { bar(new C()); }}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(3);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals(String.class.getName(), sd.getName());

    }

    @Test
    public void testImplementationWithGenericsRewrittingLetters() throws Exception {
        CompilationUnit cu = run(
                "public class A { class B <B,K,V> { B get() { return null; }} class C<K,V> extends B<C<K,V>,K, V>{} void foo() { C<String,String> c = new C<String,String>(); c.get(); } }");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(2);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(1);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals("A$C", sd.getName());
    }

    @Test
    public void testImplementationWithGenericsRewrittingLetters2() throws Exception {
        CompilationUnit cu = run(
                "import java.util.*; public class A { class Z <B> { B get() { return null; }}  class D<K, V> extends C <HashMap<K, V>> {} class C<X> extends Z<X>{} void foo() { D<String, String> c = new D<String, String>(); c.get(); } }");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(3);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(1);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals("java.util.HashMap", sd.getName());
        Assert.assertNotNull(sd.getParameterizedTypes());
        Assert.assertEquals("java.lang.String", sd.getParameterizedTypes().get(0).getName());
        Assert.assertEquals("java.lang.String", sd.getParameterizedTypes().get(1).getName());
    }

    @Test
    public void testBug1() throws Exception {
        CompilationUnit cu = run(
                "import java.util.*; public class A<K,V> { public static <T> T firstNonNull( T first, T second) { return null;} public Map<K, Collection<V>> asMap() {return null;} void foo(K key, Collection<V> value) { A.firstNonNull(asMap().remove(key),value); } }");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(2);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals("java.util.Collection", sd.getName());
        Assert.assertNotNull(sd.getParameterizedTypes());
        Assert.assertEquals("java.lang.Object", sd.getParameterizedTypes().get(0).getName());
    }

    @Test
    public void testSingletonList() throws Exception {
        CompilationUnit cu = run(
                "import java.util.Collections; public class A { void foo(String[] array) { Collections.singletonList(array); } }");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
    }

	@Test
	public void testGenericResultForParameterizedTypeWithInterfaceWitness() throws Exception {
		checkTypeParameterWithWitness("<C extends Comparable<?>>",
				"A<C>",
				"Comparable<java.io.Serializable>",
				"A<Comparable<java.io.Serializable>>",
				"A<java.lang.Comparable<java.io.Serializable>>");
	}

	@Test
	public void testGenericResultForParameterizedTypeWithClassWitness() throws Exception {
		checkTypeParameterWithWitness("<C extends Comparable<?>>",
				"A<C>",
				"Integer",
				"A<Integer>",
				"A<java.lang.Integer>");
	}

	@Test
	public void testGenericResultForTypeVariableWithInterfaceWitness() throws Exception {
		checkTypeParameterWithWitness("<C>",
				"C",
				"Comparable<java.io.Serializable>",
				"Comparable<java.io.Serializable>",
				"java.lang.Comparable<java.io.Serializable>");
	}

	@Test
	public void testGenericResultForTypeVariableWithClassWitness() throws Exception {
		checkTypeParameterWithWitness("<C>",
				"C",
				"Integer",
				"Integer",
				// root for improvement
				"java.lang.Integer");
	}

	private void checkTypeParameterWithWitness(String typeParam, final String returnType, final String witnessType, final String effectiveType, String expectedType) throws Exception {
		CompilationUnit cu = run(
				"public class A<X> {"
						+ " public static " + typeParam + " " + returnType + " all(){ return null;}\n"
						+ " void foo() {\n"
						// expression only
						+ " A.<" + witnessType + ">all(); \n"
						// variable
						+ " " + effectiveType + " v = A.<" + witnessType + ">all();\n"
						// method parameter
						+ "  bar(A.<" + witnessType + ">all());\n"
						+ " }\n"
						+ " void bar(" + effectiveType + " p) {}\n"
						+ " }");
        final ClassOrInterfaceDeclarationAssert type
                = assertThat(cu).types().item(0).asClassOrInterfaceDeclaration();
        final ExtListAssert<StatementAssert, Statement> stmts2
                = type.members().item(1).asMethodDeclaration().body().stmts();
		stmts2.item(0)
                .asExpressionStmt().expression().asMethodCallExpr().symbolData()
                .hasToString(expectedType);
        stmts2.item(1).asExpressionStmt().expression()
                .asVariableDeclarationExpr().vars().item(0).expression().symbolData()
                .hasToString(expectedType);
        stmts2.item(2).asExpressionStmt().expression()
                .asMethodCallExpr().args().item(0).symbolData()
                .hasToString(expectedType);
    }

    @Test
    public void testFieldDeclarationInsideAnonymousClass() throws Exception {
        run("import java.util.*; public class A { public Object get() { Map<String,String> aux = new HashMap<String, String>() {}; return new HashMap<String, String>() {boolean iteratorCalled;}; }}");
        Assert.assertTrue(true);
    }

    @Test
    public void testPriorityObjectVersusInt() throws Exception {
        CompilationUnit cu = run(
                "import java.util.*; public class A { void foo(List<Integer> contents, int i) { contents.remove(Integer.valueOf(i)); }}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals("boolean", sd.getName());
    }

    @Test
    public void testMethodsThatReturnsMatrixs() throws Exception {
        CompilationUnit cu = run(
                "public class A { char[][] bar(){ return new char[0][0]; } void foo(String s) { int i = bar().length; }}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        VariableDeclarationExpr vexpr = (VariableDeclarationExpr) stmt.getExpression();
        Expression expr = vexpr.getVars().get(0).getInit();
        SymbolData sd = expr.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals("int", sd.getName());
    }

    @Test
    public void methodOrderingArrays() throws Exception {
        String m1 = "public static byte[] aryEq(final byte[] value) {return null;} ";
        String m2 = "public static char[] aryEq(final char[] value) {return null;} ";
        String m3 = "public static double[] aryEq(final double[] value) {return null;} ";
        String m4 = "public static float[] aryEq(final float[] value) {return null;} ";
        String m5 = "public static int[] aryEq(final int[] value) {return null;} ";
        String m6 = "public static long[] aryEq(final long[] value) {return null;} ";
        String m7 = "public static short[] aryEq(final short[] value) {return null; }";
        String m8 = "public static <T> T[] aryEq(final T[] value) {return null; }";
        CompilationUnit cu = run("public class A { public void foo(byte[] arg){aryEq(arg);} " + m1 + m2 + m3 + m4 + m5
                + m6 + m7 + m8 + "}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals("byte", sd.getName());
    }

    @Test
    public void testGenericsOnInheritedAttributes() throws Exception {
        CompilationUnit cu = run(
                "public class A { class C<T> { T data; } class D extends C<byte[]> { void foo(byte[] x){} void aux(){foo(data);}} }");
        TypeDeclaration td = (TypeDeclaration) cu.getTypes().get(0).getMembers().get(1);
        MethodDeclaration md = (MethodDeclaration) td.getMembers().get(1);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        SymbolData sd = mce.getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals(Void.class.getName(), sd.getName());
        sd = mce.getArgs().get(0).getSymbolData();
        Assert.assertNotNull(sd);
        Assert.assertEquals("byte", sd.getName());
    }

    @Test
    public void testMethodResolutionWithRecursiveGenericsInMethodScope() throws Exception {
        run("import java.util.*; class A { public <K extends List<V>, V extends List<K>> void withMutualRecursiveBound(List<Map<K, V>> list) {}}");
        Assert.assertTrue(true);
    }

    @Test
    public void testMethodResolutionWithMultipleBoundsGenericsInMethodScope() throws Exception {
        run("import java.util.*; class A { <T extends Number & CharSequence> void withUpperBound(List<T> list) {} }");
        Assert.assertTrue(true);
    }

    @Test
    public void testObjectCreationExprWithScope() throws Exception {
        CompilationUnit cu = run(
                "class Owner<T>{ class Inner<T> {} void foo() { Object o = new Owner<Integer>().new Inner<String>() {}; } }");
        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertNotNull(type.getUsages());
    }

    @Test
    public void testObjectCreationExprWithScope2() throws Exception {
        CompilationUnit cu = run(
                "class A { class Owner<T>{ class Inner<T> {}} void foo() { Object o = new Owner<Integer>().new Inner<String>() {}; } }");
        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(0);
        type = (ClassOrInterfaceDeclaration) type.getMembers().get(0);
        Assert.assertNotNull(type.getUsages());
    }

    @Test
    public void testInheritanceOnNestedClassesFromAnObjectCreation() throws Exception {
        run("class A { private static class Owner<T>{ void foo() { Object o =new Owner<Integer>().new Inner<String>() {}; } private static abstract class Nested<X> {} private abstract class Inner<Y> extends Nested<Y> {}}  } ");
        Assert.assertTrue(true);
    }

    @Test
    public void testMethodCallsOnAnonymousClasses() throws Exception {
        String mainClass =
                "public class A { public void testTwoStageResolution() { class ForTwoStageResolution<X extends Number> {  <B extends X> void verifyTwoStageResolution() { TypeToken type = new TypeToken<B>(getClass()) {}.where(new TypeParameter<B>() {}, (Class) Integer.class); } } } }";
        String typeTokenClass =
                "public class TypeToken<T> { public TypeToken(Class clazz) {}  public final <X> TypeToken<T> where(TypeParameter<X> typeParam, Class<X> typeArg) {return null;} }";
        String typeParamClass = "public class TypeParameter<T> {}";
        run(mainClass, typeTokenClass, typeParamClass);
        Assert.assertTrue(true);
    }

    @Test
    public void testClassesInsideAnonymousIds() throws Exception {
        String mainClass = "public class A { void foo() {new From<Integer>() {}.new To<String>().type();}}";
        String fromClass = "public class From<T>{ public class To<T> { void type(){}} }";

        run(mainClass, fromClass);
        Assert.assertTrue(true);
    }

    @Test
    public void testClassesInsideAnonymousIds2() throws Exception {
        String mainClass = "public class A { void foo() {new From<Integer>() {}.new To<String>() {}.type();}}";
        String fromClass = "public class From<T>{ public class To<T> { void type(){}} }";

        run(mainClass, fromClass);
        Assert.assertTrue(true);
    }

    @Test
    public void testClassesInsideAnonymousIds3() throws Exception {
        String mainClass =
                "public class A { void foo() {new From<Integer>() {}.new To<String>() {};} class From<T>{ class To<T> { }}}";

        CompilationUnit cu = run(mainClass);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        ObjectCreationExpr call = (ObjectCreationExpr) stmt.getExpression();
        SymbolReference ref = call.getType();
        Assert.assertNotNull(ref.getSymbolDefinition());
    }

    @Test
    public void loadMethodsInsideAnonymousClasses() throws Exception {
        String stmt =
                "Service service2=new Service() {@Override public final void addListener(Listener listener, Executor executor) {}};";
        String mainClass = "import java.util.concurrent.Executor; public class A { void foo() {" + stmt + "}}";
        String serviceClass =
                "import java.util.concurrent.Executor; public class Service{ abstract class Listener {}  public void addListener(Listener listener, Executor executor){} }";
        String listenerClass = "class Listener {}";
        run(mainClass, serviceClass, listenerClass);
        Assert.assertTrue(true);
    }

    @Test
    public void testImportsWithAsteriskUsages() throws Exception {
        CompilationUnit cu = run("import java.util.*; class A { List<String> list; }");
        Assert.assertNotNull(cu.getImports().get(0).getUsages());
    }

    @Test
    public void testObjectCreationReferences() throws Exception {

        CompilationUnit cu = run("import java.util.LinkedList; class A { void foo() { new LinkedList<String>(); }}");
        Assert.assertNotNull(cu.getImports().get(0).getUsages());
    }

    @Test
    public void testMethodsOrderAsStaticImport() throws Exception {
        String importedClass =
                "package test; public class Test { public static <T extends Comparable<?>> T assertThat(T target) {return null;}\n "
                        + "public static Iterable<?> assertThat(Iterable target) {return null;}" + "}";
        String mainClass =
                "import static test.Test.assertThat; import java.util.*; class A{ void foo() { assertThat(new LinkedList<String>()); } }";
        CompilationUnit cu = run(mainClass, importedClass);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr call = (MethodCallExpr) stmt.getExpression();

        Assert.assertEquals("java.lang.Iterable", call.getSymbolData().getMethod().getReturnType().getName());
    }

    @Test
    public void testMethodsOrderAsStaticImport2() throws Exception {
        String importedClass =
                "package test; import java.util.*; public class Test { public static <T extends Comparable<?>> List<T> assertThat(T target) {return null;}\n "
                        + "public static List assertThat(List target) {return null;}"
                        + "public static LinkedList assertThat(LinkedList target) {return null;}"
                        + "public static Collection assertThat(Collection target) {return null;}"
                        + "public static Boolean assertThat(Boolean target) {return null;}"
                        + "public static String assertThat(String target) {return null;}"
                        + "public static Object assertThat(Object target) {return null;}" + "}";
        String mainClass =
                "import static test.Test.assertThat; import java.util.*; class A{ void foo() { assertThat(\"hello\"); } }";
        CompilationUnit cu = run(mainClass, importedClass);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr call = (MethodCallExpr) stmt.getExpression();

        Assert.assertEquals("java.lang.String", call.getSymbolData().getMethod().getReturnType().getName());
    }

    @Test
    public void testMethodsOrderAsStaticImport3() throws Exception {
        String importedClass =
                "package test; public class Test { public static <T extends Comparable<?>> T assertThat(T target) {return null;}\n "
                        + "public static Iterable<?> assertThat(Object target) {return null;}" + "}";
        String mainClass =
                "import static test.Test.assertThat; import java.util.*; class A{ void foo() { assertThat(\"hello\"); } }";
        CompilationUnit cu = run(mainClass, importedClass);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr call = (MethodCallExpr) stmt.getExpression();

        Assert.assertEquals("java.lang.Comparable", call.getSymbolData().getMethod().getReturnType().getName());
    }

    @Test
    public void testMultipleStaticImportsFromTheSameClass() throws Exception {
        String externalClass =
                "package foo; class Files { public static void touch() {} public static void createTempDir(){} public static void bar3(){} }";
        String mainClass =
                "package foo; import static foo.Files.createTempDir; import static foo.Files.touch; import java.io.File; class A { void foo() { Files.createTempDir(); }}";
        CompilationUnit cu = run(mainClass, externalClass);
        Assert.assertNull(cu.getImports().get(0).getUsages());
        Assert.assertNull(cu.getImports().get(1).getUsages());
    }

    @Test
    public void testStreams1() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            CompilationUnit cu = run(
                    "import java.util.function.*;import java.util.*;import java.util.concurrent.*;import java.util.stream.*;"
                            + " public class A<T> {" + " Map<Object, Function<Stream<T>, ?>> forks; "
                            + " public void foo(){  " + "List<BlockingQueue<T>> queues = new ArrayList<>(); "
                            + "forks.entrySet().stream().reduce(" + "new HashMap<Object, Future<?>>(), "
                            + "(map, e) -> { map.put(e.getKey(), getOperationResult(queues, e.getValue())); return map;},"
                            + "(m1, m2) -> {return m1;}); " + "} "
                            + "private Future<?> getOperationResult(List<BlockingQueue<T>> queues, Function<Stream<T>, ?> f) {return null; }}");
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
            ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(1);
            SymbolData sd = stmt.getExpression().getSymbolData();
            Assert.assertNotNull(sd);
        }

    }

    @Test
    public void testStreams2() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String file1 =
                    "package foo; import java.util.function.*;import java.util.stream.*; public class StreamForker<T>{  public StreamForker<T> fork(Object key, Function<Stream<T>, ?> f) {return null;}}";
            String file2 = "package foo; public class Dish{ public String getName() {return null; } }";
            String file3 =
                    "package foo; import java.util.stream.*; public class A { public void foo(StreamForker<Dish> stream) { stream.fork(\"shortMenu\", s -> s.map(Dish::getName).collect(joining(\", \")));} "
                            + "public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {return null;}}";
            CompilationUnit cu = run(file3, file2, file1);
            Assert.assertTrue(true);
        }

    }

    @Test
    public void testStreams() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {

            run(FileUtils.fileToString("src/test/resources/codeStreams.txt"));
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testLambdasAsVariables() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import java.util.function.Function; public class Lambda {  Function<Object, String> f = obj -> obj.toString(); }");
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testTemplatesWithMethodReferences() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            CompilationUnit cu = run(
                    "import java.util.concurrent.CompletableFuture; public class A { public static A parse(String s) { return null; } void foo(CompletableFuture<String> future) { future.thenApply(A::parse); } }");
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
            ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
            SymbolData sd = stmt.getExpression().getSymbolData();
            Assert.assertNotNull(sd);
            Assert.assertEquals("java.util.concurrent.CompletableFuture", sd.getName());
            Assert.assertNotNull(sd.getParameterizedTypes());
            Assert.assertEquals("A", sd.getParameterizedTypes().get(0).getName());

        }
    }

    @Test
    public void testMethodReferenceScopes() throws Exception {

        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import java.util.*; public class A { void foo(List<List<Integer>> subs) {  subs.forEach(System.out::println); }}");
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testMethodReferenceScopes2() throws Exception {

        if (SourceVersion.latestSupported().ordinal() >= 8) {
            CompilationUnit cu = run(
                    "import java.util.*; import java.util.stream.*; public class A { void foo(List<String> names) {Stream<String> s = names.stream(); s.forEach(System.out::println); }}");
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
            ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(1);
            SymbolData sd = stmt.getExpression().getSymbolData();
            Assert.assertNotNull(sd);
        }
    }

    @Test
    public void testTryStmtWithScope() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import java.io.*; public class A { void foo() throws Exception { try (BufferedReader br = new BufferedReader(new FileReader(\"data.txt\"))) {  br.readLine(); } } } ");
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testSortingWithLambdas() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import java.util.List; public class A { void foo(List<String> list){ list.sort((a1, a2) -> a1.toLowerCase().compareTo(a2.toLowerCase()));} }");
            Assert.assertTrue(true);
            // Comparator<T> List<E>
        }
    }

    @Test
    public void testStaticImportsWithNestedClasses() throws Exception {

        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import static java.util.stream.Collector.Characteristics.*; public class A {}");
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testLambdaExpressionsInsideReturnStmts() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import java.util.function.*; import java.util.List; public class A<T> {  public BiConsumer<List<T>, T> accumulator() {  return (list, item) -> list.add(item); } } ");
        }
    }

    @Test
    public void testLambdaExpressionsInsideReturnStmts2() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            // BinaryOperator<T> -> BiFunction<T,T,T>
            run("import java.util.function.*; import java.util.List; public class A<T> {  public BinaryOperator<List<T>> accumulator() {   return (list1, list2) -> { list1.addAll(list2);  return list1; }; } }");
        }
    }

    @Test
    public void testMethodReferencesForTypes() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import java.util.*; import java.util.function.*;import java.util.stream.*; " + "public class A { "
                    + "void foo(Stream<Character> stream) {"
                    + " stream.reduce(new WordCounter(0, true), WordCounter::accumulate, WordCounter::combine);" + " }"
                    + "  private static class WordCounter { public WordCounter(int counter, boolean lastSpace) {}  public WordCounter accumulate(Character c) {return null;} public WordCounter combine(WordCounter wordCounter) { return null; } } }");
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testMethodReferencesAsObjectCreation() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            run("import java.util.function.Supplier;" + "import java.util.HashMap;" + "import java.util.Map;"
                    + "public class A{ " + " static private interface Product {}"
                    + " static private class Loan implements Product {} "
                    + "final static private Map<String, Supplier<Product>> map = new HashMap<>();"
                    + "    static {map.put(\"loan\", Loan::new);}}");
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testEnsureLambdasInImportDeclarationUsages() throws Exception {
        String code = "package example;" +
                "import java.util.Collection;" +
                "import java.util.Objects;" +
                "import static java.util.stream.Collectors.toSet;" +
                "		public class ExampleGroupPickerSearcher {" +
                "			public ExampleGroupPickerSearcher() {" +
                "				final Resolver<String> nameResolver = rawValues ->" +
                "						rawValues.stream()" +
                "								.map(this::convertToIndexValue)" +
                "								.filter(Objects::nonNull)" +
                "								.collect(toSet());" +
                "			}" +
                "			public String convertToIndexValue(final Object rawValue) {" +
                "				return \"\";" +
                "			}" +
                "			public interface Resolver<T> {" +
                "				Collection<T> resolveNames(Collection<Object> rawValues);" +
                "			}" +
                "		}";
        CompilationUnit cu = run(code);
        String codeBefore = cu.toString();
        ImportDeclaration importDeclaration = cu.getImports().get(0);
        Assert.assertEquals("java.util.Collection", importDeclaration.getName().toString());
        Assert.assertNotNull(importDeclaration.getUsages());

        importDeclaration = cu.getImports().get(1);
        Assert.assertEquals("java.util.Objects", importDeclaration.getName().toString());
        Assert.assertNotNull(importDeclaration.getUsages());

        importDeclaration = cu.getImports().get(2);
        Assert.assertEquals("java.util.stream.Collectors.toSet", importDeclaration.getName().toString());
        Assert.assertNotNull(importDeclaration.getUsages());
    }

    @Test
    public void testStaticFieldImportedButUnnecessary() throws Exception {
        String code1 = "package foo; public class A { public static String name; }";
        String code2 = "package foo; import static foo.A.name; public class B { String c = A.name; }";
        CompilationUnit cu = run(code2, code1);
        Assert.assertNull(cu.getImports().get(0).getUsages());
    }

    @Test
    public void testMultipleVariablesAtDifferentScopes() throws Exception {
        String code = "public class A { public final int value = 4; public void doIt() { " + " int value = 6; "
                + "Runnable r = new Runnable(){ " + "public final int value = 5;" + "public void run(){"
                + "int value = 10;" + "System.out.println(this.value);" + "}" + "}; " + " r.run(); }" + "}";
        CompilationUnit cu = run(code);
        List<BodyDeclaration> members = cu.getTypes().get(0).getMembers();
        FieldDeclaration fd = (FieldDeclaration) members.get(0);
        Assert.assertNull(fd.getUsages());

        MethodDeclaration md = (MethodDeclaration) members.get(1);
        List<Statement> stmts = md.getBody().getStmts();
        ExpressionStmt expression = (ExpressionStmt) stmts.get(0);

        VariableDeclarationExpr vdexpr = (VariableDeclarationExpr) expression.getExpression();
        List<VariableDeclarator> vds = vdexpr.getVars();
        Assert.assertNull(vds.get(0).getUsages());
        expression = (ExpressionStmt) stmts.get(1);
        vdexpr = (VariableDeclarationExpr) expression.getExpression();
        vds = vdexpr.getVars();

        ObjectCreationExpr typeStmt = (ObjectCreationExpr) vds.get(0).getInit();
        List<BodyDeclaration> typeMembers = typeStmt.getAnonymousClassBody();

        fd = (FieldDeclaration) typeMembers.get(0);
        Assert.assertNotNull(fd.getUsages());

        md = (MethodDeclaration) typeMembers.get(1);
        stmts = md.getBody().getStmts();
        expression = (ExpressionStmt) stmts.get(0);

        vdexpr = (VariableDeclarationExpr) expression.getExpression();
        vds = vdexpr.getVars();
        Assert.assertNull(vds.get(0).getUsages());

    }

    @Test
    public void testSyntheticIntersectionTypes() throws Exception {
	    /*
	     The type of the ternary operator is the intersection type of the sub expression types.
	     */
        String mySet = ""
                + "import java.io.Serializable;\n"
                + "import java.util.Set;\n"
                + "public abstract class MySet<A> implements Set<A>, Serializable, Cloneable {}\n";
        String code = ""
                + "import java.io.Serializable;\n"
                + "import java.util.Set;\n"
                + "import java.util.HashSet;\n"
                + "public class A {\n"
                + " void doSer(Serializable p) {}\n"
                + " void doClone(Cloneable p) {}\n"
                + " void doSet(Set p) {}\n"
                + " void f(boolean b, HashSet<String> set1, MySet<String> set2) {\n"
                + "  doSer(b ? set1 : set2);\n"
                + "  doClone(b ? set1 : set2);\n"
                + "  doSet(b ? set1 : set2);\n"
                + " }\n"
                + "}\n";
        CompilationUnit cu = run(code, mySet);
        final ExtListAssert<StatementAssert, Statement> fStmts =
                assertThat(cu)
                        .types().item(0).members().item(3).asMethodDeclaration()
                        .body().stmts();
        fStmts.item(0).asExpressionStmt().expression().asMethodCallExpr()
                .hasName("doSer")
                .args().item(0)
                .symbolData().boundClasses().contains(Serializable.class);
        fStmts.item(1).asExpressionStmt().expression().asMethodCallExpr()
                .hasName("doClone")
                .args().item(0)
                .symbolData().boundClasses().contains(Cloneable.class);
        fStmts.item(2).asExpressionStmt().expression().asMethodCallExpr()
                .hasName("doSet")
                .args().item(0)
                .symbolData().boundClasses().contains(Set.class);
    }

    @Test
    public void testSyntheticIntersectionTypeWithPrimitives() throws Exception {
        String code = ""
                + "public class A {\n"
                + " void doSer(java.io.Serializable s) {}\n"
                + " void doComp(Comparable s) {}\n"
                + " void foo(int value) {\n"
                + "  Object o = (value < 10 ? \"0\" + value : value) + \" %\";\n"
                + "  doSer(value < 10 ? \"0\" + value : value);\n"
                + "  doComp(value < 10 ? \"0\" + value : value);\n"
                + "  doSer(value < 10 ? 0.0 : value);\n"
                + "  doComp(value < 10 ? 0.0 : value);\n"
                + "  doSer(value < 10 ? false : value);\n"
                + "  doComp(value < 10 ? false : value);\n"
                + " }\n"
                +"}\n";
        CompilationUnit cu = run(code);
        final ExtListAssert<StatementAssert, Statement> fooStmts = assertThat(cu)
                .types().item(0).asClassOrInterfaceDeclaration().members().item(2).asMethodDeclaration()
                .body().stmts();
        fooStmts.item(0).asExpressionStmt().expression().asVariableDeclarationExpr()
                .vars().item(0).expression().asBinaryExpr()
                .left().symbolData()
                .boundClasses()
                .hasSize(2)
                .extracting("name")
                .containsOnly("java.io.Serializable", "java.lang.Comparable");
        fooStmts.item(1).asExpressionStmt().expression().asMethodCallExpr()
                .hasName("doSer")
                .symbolData().asMethodSymbolData()
                .method()
                .isNotNull();
        fooStmts.item(2).asExpressionStmt().expression().asMethodCallExpr()
                .hasName("doComp")
                .symbolData().asMethodSymbolData()
                .method()
                .isNotNull();
    }

    @Test
    public void testMethodReferencesWithSuperAsContext() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String codeA = "public class A extends B{ void bar2(C c){} void bar() { bar2(super::foo);} }";
            String codeB = "public class B{ void foo(){} }";
            String codeC = "public interface C { void doIt(); }";
            CompilationUnit cu = run(codeA, codeB, codeC);
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
            List<Statement> stmts = md.getBody().getStmts();
            ExpressionStmt stmt = (ExpressionStmt) stmts.get(0);
            MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
            Assert.assertNotNull(call.getSymbolData());
            MethodReferenceExpr mce = (MethodReferenceExpr) call.getArgs().get(0);
            Assert.assertNotNull(mce.getSymbolData());
        }
    }

    @Test
    public void testMethodReferencesWithTypeSuperAsContext() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String codeA = "public class A extends B{ void bar2(C c){} void bar() { bar2(A.super::foo);} }";
            String codeB = "public class B{ void foo(){} }";
            String codeC = "public interface C { void doIt(); }";
            CompilationUnit cu = run(codeA, codeB, codeC);
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
            List<Statement> stmts = md.getBody().getStmts();
            ExpressionStmt stmt = (ExpressionStmt) stmts.get(0);
            MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
            Assert.assertNotNull(call.getSymbolData());
            MethodReferenceExpr mce = (MethodReferenceExpr) call.getArgs().get(0);
            Assert.assertNotNull(mce.getSymbolData());
        }
    }

    @Test
    public void testIntersectionType() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String code =
                    "import java.util.*; public class A { public void foo(Collection c) {} public void bar(LinkedList l) {foo((Collection & java.io.Serializable)l);} }";
            CompilationUnit cu = run(code);
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
            ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
            MethodCallExpr expr = (MethodCallExpr) stmt.getExpression();
            Assert.assertNotNull(expr.getArgs().get(0).getSymbolData());
        }
    }

    @Test
    public void testSuperExpressionsToReferenceDefaultMethods() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String codeInterface = "interface Superinterface { default void foo() { System.out.println(\"Hi\"); } }";
            String code =
                    "public class Subclass2 implements Superinterface { public void foo() { throw new UnsupportedOperationException(); } void tweak() {  Superinterface.super.foo(); }}";
            CompilationUnit cu = run(code, codeInterface);
            MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
            ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
            MethodCallExpr expr = (MethodCallExpr) stmt.getExpression();
            Assert.assertNotNull(expr.getSymbolData());
        }
    }

    @Test
    public void testDefaultMethodInheritance() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String code =
                    "public class A { interface B{ default void foo() { System.out.println(\"Hi\"); }}  class D implements B{ void bar(){this.foo();}}}";
            CompilationUnit cu = run(code);
            ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(1);
            MethodDeclaration md = (MethodDeclaration) type.getMembers().get(0);
            ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
            MethodCallExpr expr = (MethodCallExpr) stmt.getExpression();
            Assert.assertNotNull(expr.getSymbolData());

            type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(0);
            md = (MethodDeclaration) type.getMembers().get(0);
            Assert.assertNotNull(md.getUsages());
        }
    }

    @Test
    public void testDefaultMethodInheritance2() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String code =
                    "public class A { interface Z {default void foo() { System.out.println(\"Hi\"); }} interface B extends Z{ }  class D implements B{ void bar(){this.foo();}}}";
            CompilationUnit cu = run(code);
            ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(2);
            MethodDeclaration md = (MethodDeclaration) type.getMembers().get(0);
            ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
            MethodCallExpr expr = (MethodCallExpr) stmt.getExpression();
            Assert.assertNotNull(expr.getSymbolData());

            type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(0);
            md = (MethodDeclaration) type.getMembers().get(0);
            Assert.assertNotNull(md.getUsages());
        }
    }

    @Test
    public void testMethodOrderingWithLongsAndWrappers() throws Exception {
        String code = "public class A {void foo(long i){} void foo(Integer x){} void bar(){foo(1);}}";
        CompilationUnit cu = run(code);
        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);
        MethodDeclaration md = (MethodDeclaration) type.getMembers().get(0);
        Assert.assertNotNull(md.getUsages());

    }

    @Test
    public void testFieldOverridingUsingThis() throws Exception {
        String parentCode =
                "import java.util.Collection; public abstract class Parent<N extends Collection>{ protected N attr;}";
        String childCode =
                "import java.util.LinkedList; public class Child extends Parent<LinkedList> { public void foo() { this.attr.remove(0); }}";
        CompilationUnit cu = run(childCode, parentCode);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);

        Assert.assertNotNull(stmt.getExpression().getSymbolData());

    }

    @Test
    public void testTryConditionsTypeMark() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 7) {
            String code =
                    "import java.io.BufferedReader;import java.io.InputStreamReader;import java.io.InputStream; public class A { public void foo(InputStream in) throws Exception{ try (BufferedReader c = new BufferedReader(new InputStreamReader(in))) { c.readLine(); }}}";
            CompilationUnit cu = run(code);

            List<ImportDeclaration> imports = cu.getImports();
            Assert.assertNotNull(imports.get(1).getUsages());
        }
    }

    @Test
    public void testVariableDeclarationsWithFieldsWithSameName() throws Exception {
        String code =
                "import java.util.List; public class A{ List<String> nodes; public void foo() {  String[] nodes = this.nodes.toArray(new String[]{}); }}";
        CompilationUnit cu = run(code);
        Assert.assertTrue(true);
    }

    @Test
    public void testStaticClassWithFieldModification() throws Exception {
        String code = "package bar; import foo.Bar; public class Test1{ public void test() { Bar.x = 1;} }";
        String dependentCode = "package foo; public class Bar{ public static int x; }";

        CompilationUnit cu = run(code, dependentCode);

        ImportDeclaration id = cu.getImports().get(0);

        Assert.assertNotNull(id.getUsages());

        Assert.assertEquals(1, id.getUsages().size());

    }

    @Test
    public void testGenericsWithoutSpecificNestedType() throws Exception {
        String code =
                "import java.util.List; class A { List l; public void foo() {foo(l); } public void foo(List<A> l) {} }";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testGenericsWithoutSpecificNestedType2() throws Exception {
        String code =
                "import java.util.List; class A { Object l; public void foo() {foo((List)l); } public void foo(List<A> l) {} }";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testDynamicArgsWithDifferentTypes() throws Exception {
        String code =
                "import java.util.List; public class B { public void test(List<Object> l) { A.<List<Object>>pick(l);} }";
        String aux = "import java.util.ArrayList;" + " public class A { public static <T> T pick(T a1) { return a1; }}";

        CompilationUnit cu = run(code, aux);
        Assert.assertNotNull(cu);

        List<ImportDeclaration> imports = cu.getImports();

        Assert.assertNotNull(imports);

        Assert.assertNotNull(imports.get(0).getUsages());

        Assert.assertEquals(2, imports.get(0).getUsages().size());
    }

    @Test
    public void testgetClassRedefinition() throws Exception {
        String code =
                "public abstract class A<T> implements B<String>{ public void set(Class<? extends B> arg) {  set(getClass()); }}";
        String bcode = "public interface B<X>{}";

        CompilationUnit cu = run(code, bcode);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testRecursiveTemplatesOnTypeChecking() throws Exception {
        String genericClassCode =
                "import java.util.List; public class MyClass< S extends MyClass, T extends List>{ public List<MyClass> testMethod() { return null;} }";
        String code =
                "import java.util.List; public class A {  public void execute(List<MyClass> aux) { for(MyClass<?,?> elem: aux) { execute(elem.testMethod()); } }}";

        CompilationUnit cu = run(code, genericClassCode);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testRecursiveTemplatesOnTypeChecking2() throws Exception {
        String genericClassCode =
                "import java.util.List; public class MyClass< S extends MyClass, T extends List>{ public List<MyClass> testMethod() { return null;} }";
        String code =
                "import java.util.List; public class A {  public void execute(List<? extends MyClass> aux) { for(MyClass<?,?> elem: aux) { execute(elem.testMethod()); } }}";

        CompilationUnit cu = run(code, genericClassCode);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testTemplateVariableRedefinition() throws Exception {
        String parentClass = "public class ParentClass<D> { public D get() { return null; }}";
        String childClass =
                "import java.util.List; import java.util.Collection; public class ChildrenClass<T extends Collection<T>, D extends List<T>> extends ParentClass<D> { public void foo() { get().listIterator();}}";
        CompilationUnit cu = run(childClass, parentClass);
        Assert.assertNotNull(cu);

        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr expr = (MethodCallExpr) stmt.getExpression();
        Assert.assertNotNull(expr.getSymbolData());
    }

    @Test
    public void testTypeArgsOnClassGenericsInInnerClasses() throws Exception {
        String code =
                "import java.util.List; public class A { public void foo(InnerClass a) { bar(a.annotationType); } public void bar (Class<? extends List> aux) {}  static abstract class InnerClass<T extends List> { public final Class<T> annotationType = null; } }";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testThisSymbolContruction() throws Exception {
        String abstractProject = "public class AbstractProject<T, K>{}";
        String code =
                "public class AbstractBuild<P extends AbstractProject<P,R>,R extends AbstractBuild<P,R>> { public Object get() { return this; } }";
        CompilationUnit cu = run(code, abstractProject);

        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ReturnStmt returnStmt = (ReturnStmt) md.getBody().getStmts().get(0);
        SymbolData sd = returnStmt.getExpr().getSymbolData();

        List<SymbolData> params = sd.getParameterizedTypes();
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());

        List<SymbolType> params2 = ((SymbolType) params.get(0)).getBounds().get(0).getParameterizedTypes();
        Assert.assertNotNull(params2);

        Assert.assertEquals("AbstractProject", params2.get(0).getClazz().getSimpleName());

        Assert.assertNotNull(sd);

        Assert.assertNotNull(cu);
    }

    @Test
    public void testConflictImportsWildCardWithPackage() throws Exception {
        String staticClass = "package foo; public class List{}";

        String mainCode = "package foo; import java.util.*; public class Foo{ List aux; }";

        CompilationUnit cu = run(mainCode, staticClass);
        Assert.assertNotNull(cu);

        FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0).getMembers().get(0);

        Assert.assertEquals("foo.List", fd.getType().getSymbolData().getName());
    }

    @Test
    public void testMethodResolutionConflictBetweenInnerClassHierachyAndContainerClass() throws Exception {
        String code =
                "public class Container { void hello() {} class Foo extends Bar { void something() { hello(); }}}";
        String otherClass = "public class Bar { void hello() {} }";
        CompilationUnit cu = run(code, otherClass);
        Assert.assertNotNull(cu);

        ClassOrInterfaceDeclaration barType = (ClassOrInterfaceDeclaration) cu.getTypes().get(0).getMembers().get(1);
        MethodDeclaration md = (MethodDeclaration) barType.getMembers().get(0);
        ExpressionStmt exprStmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) exprStmt.getExpression();
        Assert.assertEquals("Bar", mce.getSymbolData().getMethod().getDeclaringClass().getName());

    }

    @Test
    public void testGenericTypesWithoutParameters() throws Exception {
        String code =
                "public class Foo { public Class getItemType(){ return null; } public void bar() { getItemType(); } }";
        CompilationUnit cu = run(code);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertEquals("java.lang.Class", md.getSymbolData().getName());

        Assert.assertEquals(null, md.getSymbolData().getParameterizedTypes());
    }

    @Test
    public void testGenericTypesWithoutParameters3() throws Exception {
        String codeParent = "public class FooParent { public Class getItemType(){ return null; }}";
        String code =
                "public class Foo extends FooParent { public void bar(Class<? extends Foo> arg) {  bar(getItemType()); } }";
        CompilationUnit cu = run(code, codeParent);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testGenericTypesWithoutParameters2() throws Exception {
        String code =
                "public class Foo { public Class getItemType(){ return null; } public void bar(Class<? extends Foo> arg) { bar(getItemType()); } }";
        CompilationUnit cu = run(code);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        Assert.assertEquals("java.lang.Class", md.getSymbolData().getName());

        Assert.assertEquals(null, md.getSymbolData().getParameterizedTypes());
    }

    @Test
    public void testFieldTypeRedefinition() throws Exception {
        String parentClass =
                "import java.util.Collection; public class ParentClass<T extends Collection<T>>{ T project; }";
        String childClass =
                "import java.util.List; public class ChildClass<P extends List<P>> extends ParentClass<P> { public void foo() { project.listIterator(); } }";
        CompilationUnit cu = run(childClass, parentClass);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        Assert.assertNotNull(stmt.getExpression().getSymbolData());
    }

    @Test
    public void testFieldTypeRedefinition2() throws Exception {
        String grandParentClass = "public class GrandParentClass<R>{ R project; }";
        String parentClass =
                "import java.util.Collection; public class ParentClass<T extends Collection<T>> extends GrandParentClass<T>{ }";
        String childClass =
                "import java.util.List; public class ChildClass<P extends List<P>> extends ParentClass<P> { public void foo() { project.listIterator(); } }";
        CompilationUnit cu = run(childClass, parentClass, grandParentClass);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr method = (MethodCallExpr) stmt.getExpression();
        Assert.assertNotNull(method.getScope().getSymbolData());
        Assert.assertEquals("List", method.getScope().getSymbolData().getClazz().getSimpleName());
        Assert.assertNotNull(stmt.getExpression().getSymbolData());
    }

    @Test
    public void testMultipleBounds() throws Exception {
        String topLevelItem = "public interface TopLevelItem{}";
        String abstractItem = "public class AbstractItem{}";

        String code =
                "public class A { public void foo(TopLevelItem item){} public <I extends AbstractItem & TopLevelItem> void move(I item){ foo(item); }  }";

        CompilationUnit cu = run(code, abstractItem, topLevelItem);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testStaticImportsWithInheritedInnerClassesFromInterfaces() throws Exception {
        String importedType = "package foo; public class ExternalClass implements SomeInterface{}";
        String interfaceType = "package foo; public interface SomeInterface{ public class InnerClass{} }";
        String code =
                "package bar; import static foo.ExternalClass.*; public class MainClass{ public void something(){ InnerClass c = new InnerClass(); }}";

        CompilationUnit cu = run(code, importedType, interfaceType);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testThrowExceptionsWithFullClassName() throws Exception {
        String codeException = "package foo; public class Bar{ public class BarException extends Exception{} } ";
        String code = "public class Foo { public void hello() throws foo.Bar.BarException{} }";
        CompilationUnit cu = run(code, codeException);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testInheritedFieldByAnonymousClass() throws Exception {
        String externalCode = "package bar; import java.util.List; public class A{ public List DEFAULT = null; }";
        String code =
                "public class Foo{  public void foo() { bar.A x = new bar.A(){  void hello() { Object aux = DEFAULT.get(0); }};}}";
        CompilationUnit cu = run(code, externalCode);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testGenericsResolutionForClassParameters() throws Exception {
        String externalCode =
                "package test; import java.util.Collection; public class Foo{ public <T extends Collection> T getProperty(Class<T> clazz) { return null;}}";
        String code =
                "import test.Foo; import java.util.List; public class Bar{ public void foo(Foo x){ Class<? extends List> pt = null; x.getProperty(pt).listIterator();} }";
        CompilationUnit cu = run(code, externalCode);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(1);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        Assert.assertNotNull(mce.getSymbolData());

    }

    @Test
    public void testGenericsWithWildcardAndUpperBoundTypes() throws Exception {
        String scmClass = "public class SCMClass{ SCMDescriptor<?> getDescriptor(){return null;} }";
        String scmDescriptor = "import java.util.List; public class SCMDescriptor<T extends List>{}";
        String mainClass =
                "import java.util.Set; import java.util.HashSet; public class Foo{ public void bar(SCMClass scm) { Set<SCMDescriptor<?>> descriptors = new HashSet<SCMDescriptor<?>>(); descriptors.add(scm.getDescriptor()); } }";
        CompilationUnit cu = run(mainClass, scmDescriptor, scmClass);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testFieldTypeOverriding() throws Exception {
        String jobClass = "import java.util.Collection; public abstract class Trigger<J extends Collection>{ J job; }";
        String code =
                "import java.util.List; public class TimerTrigger extends Trigger<List> { public void foo() { job.listIterator(); } }";
        CompilationUnit cu = run(code, jobClass);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        Assert.assertNotNull(mce.getSymbolData());

    }

    @Test
    public void testMethodScopeResolution() throws Exception {
        String externalClass = "package foo; public class ExternalClass{ public String foo(String s) { return null; }}";
        String code =
                "package bar; import foo.ExternalClass; public class Foo{ void bar(ExternalClass a) { a.foo(\"hello\"); } void foo(String s){}}";
        CompilationUnit cu = run(code, externalClass);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        Assert.assertNotNull(mce.getSymbolData());
        Assert.assertEquals("java.lang.String", mce.getSymbolData().getName());

    }

    @Test
    public void testGenericsWithFieldAccessResolution() throws Exception {
        String externalClass = "package hudson.model; public class Job<T, K>{ public transient String runIdMigrator; }";
        String code =
                "import hudson.model.Job;import java.util.Map;public abstract class LazyBuildMixIn<JobT extends Job<String, String>, RunT extends Map<JobT, String>> { protected abstract JobT asJob(); public void foo() { String aux = asJob().runIdMigrator; } }";
        CompilationUnit cu = run(code, externalClass);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        VariableDeclarationExpr ae = (VariableDeclarationExpr) stmt.getExpression();
        SymbolData sd = ae.getVars().get(0).getInit().getSymbolData();

        Assert.assertNotNull(sd);

    }

    @Test
    public void testMethodResolutionWithArgsThatHaveMultipleUpperBounds() throws Exception {
        String externalClass = "package foo; public interface ExternalClass{ }";
        String otherClass = "package foo; public class Bar { public void hello(ExternalClass c){} }";
        String main =
                "import java.util.LinkedList; import foo.Bar; import foo.ExternalClass; public abstract class Foo <T extends LinkedList<String> & Foo.InnerClass>{ public abstract T get(); void execute(Bar bar){ bar.hello(get()); } public static interface InnerClass extends ExternalClass{}}";
        CompilationUnit cu = run(main, otherClass, externalClass);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testFieldAccessWithGenericsFromVars() throws Exception {
        String generics = "import java.util.Collection; public class Generic<T extends Collection>{ public T job; }";
        String helper = "import java.util.List; public class Helper{ public static void doStmt(List t){} }";
        String code =
                "import java.util.List; public class Foo extends Generic<List> { void execute(Foo var) { Helper.doStmt(var.job); } }";
        CompilationUnit cu = run(code, generics, helper);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testUsagesFromArrayCreation() throws Exception {
        String other = "import java.net.URL; public class Bar { public static void help(URL[] url) {} } ";
        String code = "import java.net.URL; public class Foo { public void execute() { Bar.help(new URL[0]); } }";
        CompilationUnit cu = run(code, other);
        Assert.assertNotNull(cu);

        Assert.assertNotNull(cu.getImports().get(0).getUsages());
        Assert.assertEquals(1, cu.getImports().get(0).getUsages().size());
    }

    @Test
    public void testInnerClassUsages() throws Exception {
        String innerClass =
                "package bar; import static java.lang.annotation.ElementType.TYPE;import java.lang.annotation.Retention;import static java.lang.annotation.RetentionPolicy.RUNTIME;import java.lang.annotation.Target; public interface ExtensionPoint {  @Target(TYPE) @Retention(RUNTIME) @interface LegacyInstancesAreScopedToHudson {} }";
        String code =
                "package bar; import bar.ExtensionPoint.LegacyInstancesAreScopedToHudson; @LegacyInstancesAreScopedToHudson public abstract class CLICommand implements ExtensionPoint{}";
        CompilationUnit cu = run(code, innerClass);
        Assert.assertNotNull(cu);

        Assert.assertNotNull(cu.getImports().get(0).getUsages());

    }

    @Test
    public void testParameterizedClassUsages() throws Exception {
        String extCode = "public interface Bar<T>{}";
        String code = "import java.net.URL; public class Foo implements Bar<URL>{}";
        CompilationUnit cu = run(code, extCode);
        Assert.assertNotNull(cu);

        Assert.assertNotNull(cu.getImports().get(0).getUsages());
    }

    @Test
    public void testImportsOfNestedClasses() throws Exception {
        String code =
                "package bar; import bar.Foo.Bar;import java.util.LinkedList; public class Foo extends LinkedList<Bar>{ public static final class Bar{}}";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);

        Assert.assertNotNull(cu.getImports().get(0).getUsages());
    }

    @Test
    public void testImportsOfNestedClasses2() throws Exception {
        String code = "" + "import bar.Arrays;\n" + "import bar.Arrays2.Leg;\n" + "public class Foo {\n"
                + "  Arrays.Leg leg1a = new bar.Arrays.Leg();\n" + "  bar.Arrays.Leg leg1b = new bar.Arrays.Leg();\n"
                + "  bar.Arrays.Leg leg1c = new Arrays.Leg();\n"

                + "  bar.Arrays2.Leg leg2a = new bar.Arrays2.Leg();\n" + "  Leg leg2b = new bar.Arrays2.Leg();\n" + "}";
        String lib = "package bar; public class Arrays { public static class Leg {} }";
        String lib2 = "package bar; public class Arrays2 { public static class Leg {} }";
        CompilationUnit cu = run(code, lib, lib2);
        Assert.assertNotNull(cu);

        Assert.assertNotNull(cu.getImports().get(0).getUsages());
    }

    @Test
    public void testStaticMethodsCalls() throws Exception {
        String externalClass =
                "package foo; public class Bar { public String name; public static Bar hello() { return null; } }";
        String code =
                "package bar; import foo.Bar; public class Foo { public void bar() { Bar.hello().name = null; } }";
        CompilationUnit cu = run(code, externalClass);
        Assert.assertNotNull(cu);
        Assert.assertNotNull(cu.getImports().get(0).getUsages());
    }

    @Test
    public void testMethodCallsWithGenerics() throws Exception {
        String code =
                "import java.util.List; public class HistoryPageFilter<T> { public void add(List<T> items) {} private boolean add(T entry) { return true; } private void bar(T aux){ add(aux); } }";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(2);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        Assert.assertEquals("java.lang.Object", mce.getSymbolData().getMethod().getParameterTypes()[0].getName());
    }

    @Test
    public void testMethodCallsWithDynamicArgsResolution() throws Exception {
        String causeOfInterruption = "public class CauseOfInterruption{}";
        String result = "public class Result{}";
        String code = "public class Executor{ " + "public void interrupt(Result result){}"
                + "private void interrupt(Result result, boolean forShutdown, CauseOfInterruption... causes) {} "
                + "private void interrupt(Result result, boolean forShutdown) {} "
                + "public void interrupt(Result result, CauseOfInterruption... causes) {interrupt(result, true, causes);}"
                + " }";
        CompilationUnit cu = run(code, result, causeOfInterruption);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(3);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        Assert.assertEquals(3, mce.getSymbolData().getMethod().getParameterTypes().length);
    }

    @Test
    public void testTernaryOperators() throws Exception {
        String code =
                "import java.util.List; import java.util.ArrayList; public class Foo{ void test(List<Integer> x) { List<Integer> dogs = x; bar(dogs == null ? new ArrayList<Integer>(): dogs);} void bar(List<Integer> list){} }";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(1);
        MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
        Assert.assertNotNull(mce.getSymbolData());
    }

    @Test
    public void testIfAssignments() throws Exception {
        String code =
                "public class Foo{String hello() {return null;} void getMessageKeyInto() { String messageKey; if (!(messageKey = hello()).equals(null)) { System.out.println(messageKey);}}}";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testReflectionCastOperation() throws Exception {
        CompilationUnit cu = run(
                "public class GroupRole extends GroupObject{ public static void isGroupRole(GroupObject o){} public void foo(GroupObject o) {GroupRole.isGroupRole(GroupObject.class.cast(o));}}",
                "public class GroupObject{}");
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
        ExpressionStmt expr = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) expr.getExpression();

        Assert.assertNotNull(mce.getSymbolData());
    }

    @Test
    public void testRecursiveGenericsInInterfaces() throws Exception {

        CompilationUnit cu = run("public interface Mixin<T extends Mixin> extends java.io.Serializable {}");
        Assert.assertNotNull(cu);
    }

    @Test
    public void testMethodFindingByteMatrix() throws Exception {
        CompilationUnit cu = run(
                "public class Foo{ private byte[][] files; public byte[][] getFiles(){ return files;} public void bar(Foo other){java.util.Arrays.equals(this.files, other.getFiles());} }");

        Assert.assertNotNull(cu);
    }

    @Test
    public void testIssueArrayClassNotpropagated() throws Exception {
        CompilationUnit cu = run(
                "public class Foo { public void bar() { anyObject(Foo[].class); } public static <T> T anyObject(final Class<T> clazz) { return null; } }");
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ExpressionStmt expr = (ExpressionStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) expr.getExpression();
        Assert.assertNotNull(mce.getSymbolData());
    }

    @Test
    public void testFieldTemplateTypeRedefinition() throws Exception {
        CompilationUnit cu =
                run("public class SubSubClass extends TypedSubclass{ public boolean foo(){ return arg.isEmpty(); } }",
                        "public class TypedSubclass extends SuperClass<java.util.List>{}",
                        "public class SuperClass<T extends java.util.Collection>{ protected T arg; }");
        Assert.assertNotNull(cu);
        MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
        ReturnStmt expr = (ReturnStmt) md.getBody().getStmts().get(0);
        MethodCallExpr mce = (MethodCallExpr) expr.getExpr();
        NameExpr ne = (NameExpr) mce.getScope();
        Assert.assertEquals("java.util.List", ne.getSymbolData().getName());
    }

    @Test
    public void testTemplateParametersWithNullValues() throws Exception {
        CompilationUnit cu = run(
                "public class ClassLiteral { <T> T doClass(String x, Class<T> clazz) {return null;}public void x() { doClass(\"x\", null); }}");
        Assert.assertNotNull(cu);
    }

    @Test
    public void testTypePropagation() throws Exception {
        CompilationUnit cu = run(
                "public class Foo{ public static <T> T capture(final Capture<T> captured) { return null; } public void doFile(java.io.File f){} void x(){ final FileContentCapture capturedFileContent = new FileContentCapture();  doFile(capture(capturedFileContent)); } }",
                "public class FileContentCapture extends Capture<java.io.File> {}", "public class Capture<T>{}");
        Assert.assertNotNull(cu);
    }

    @Test
    public void testCastTypeInferrence() throws Exception {
        CompilationUnit cu = run(
                "import java.util.List; public class Foo{ void intercept(List l, Class x){}  void bar(Object o) { intercept(List.class.cast(o), String.class); } }");
        Assert.assertNotNull(cu);
    }

    @Test
    public void testDeeperEmbeddedClasses() throws Exception {
        CompilationUnit cu = run("import foo.SearchModule.ExtendedSearchHandler.ESResult; public class Foo{}",
                "package foo; public class SearchModule{ public static interface ExtendedSearchHandler { public static class ESResult {} } }");
        Assert.assertNotNull(cu); //the imported class is at foo/SearchModule$ExtendedSearchHandler$ESResult.class
    }

    @Test
    public void testErrorWithStaticAndNotStaticImports() throws Exception {
        CompilationUnit cu = run(
                "package bar; import static foo.SomeInterface.Inner1; import foo.SomeInterface;  public class Importer{ public static String foo(SomeInterface.Inner1 v) {return v.toString();} public static String bar(Inner1 v) { return v.toString(); } }",
                "package foo; public interface SomeInterface { static enum Inner1 { A,B; } }");

        Assert.assertNotNull(cu);
    }

    @Test
    public void testSymbolResolutionIndexBug() throws Exception {
        // bug modified the symbol of class to "array" and CU could not be created
        String code = "package x;"
                + "public class A {\n"
                + " private x.A[] children;\n"
                + " public x.A[] getChildren() { return children; }\n"
                + "}\n";
        CompilationUnit cu = run(code);
        Assert.assertNotNull(cu);
    }

    @Test
    public void testForwardInheritanceBetweenStaticInnerClasses() throws Exception {
   		/*
   		 * Bug description:
   		 * while resolving "this" of "Derived" scope the "Base" scope is used via LoadMethodDeclarationAction
         * but the "Base" scope did not get a "this" symbol yet.
         */
        final CompilationUnit cu = run(""
                + "public class A {\n"
                + " private static class Derived extends Base {\n"
                + "  public String get() { return null; }\n"
                + " }\n"
                + " private abstract static class Base {\n"
                + "  abstract public String get();\n"
                + " }\n"
                + "}\n"
        );
        AstAssertions.assertThat(cu)
                .types().item(0)
                .members().item(1).asClassOrInterfaceDeclaration()
                .hasName("Base")
                .members().item(0).asMethodDeclaration()
                .symbolData().asMethodSymbolData()
                .method().hasToString("public abstract java.lang.String A$Base.get()");
    }

    @Test
    public void testForwardExtendingBetweenInnerInterfaces() throws Exception {
        /*
         * Bug description:
         * while resolving "this" of "Derived" scope the "Base" scope is used via LoadMethodDeclarationAction
         * but the "Base" scope did not get a "this" symbol yet.
         */
        CompilationUnit cu = run(""
                + "public class A {\n"
                + " public interface StringifierRegistry extends Stringifier<Object> {\n"
                + "  @Override\n"
                + "  Object stringify(Object o);\n"
                + " }\n"
                + " public interface Stringifier<T> {\n"
                + "  Object stringify(T o);\n"
                + " }\n"
                + "}\n"
        );
        AstAssertions.assertThat(cu)
                .types().item(0)
                .members().item(0).asClassOrInterfaceDeclaration()
                .hasName("StringifierRegistry")
                .members().item(0).asMethodDeclaration()
                .symbolData().asMethodSymbolData()
                .method().hasToString("public abstract java.lang.Object A$StringifierRegistry.stringify(java.lang.Object)");
        AstAssertions.assertThat(cu)
                .types().item(0)
                .members().item(1).asClassOrInterfaceDeclaration()
                .hasName("Stringifier")
                .members().item(0).asMethodDeclaration()
                .symbolData().asMethodSymbolData()
                .method().hasToString("public abstract java.lang.Object A$Stringifier.stringify(java.lang.Object)");
    }

    private final String genericColumnAccessor = ""
            + "package apkg;\n"
            + " public class Column<T> {\n"
            + "  public static final Column<Long> Maxtime = new Column<Long>();\n"
            + " }\n";

    @Test
    public void testTypeOfGenericConstant() throws Exception {
        CompilationUnit cu = run(genericColumnAccessor);

        final FieldDeclarationAssert field =
                assertThat(cu).types().item(0).members().item(0).asFieldDeclaration();

        field.fieldsSymbolData().item(0).hasToString("apkg.Column<java.lang.Long>");
        field.type().hasToString("Column<Long>");

        field.variables().item(0).expression().asObjectCreationExpr()
                .type().hasToString("Column<Long>")
                .symbolData().asSymbolType().parameterizedTypes().item(0).hasToString("java.lang.Long");

        field.isFinal(true).isStatic(true).isPublic(true)
                .fieldsSymbolData().item(0).parameterizedTypes().item(0)
                .asSymbolType().hasToString("java.lang.Long");
    }

    @Test
    public void testTypeOfAssignmentOfGenericConstant() throws Exception {
        final String code = ""
                + "import static apkg.Column.*;\n"
                + "public class A {\n"
                + " public void foo() {\n"
                + "  long maxtime = getValue(Maxtime);\n"
                + " }\n"
                + " public <T> T getValue(apkg.Column<T> col) { return null; }\n"
                + "}\n";
        CompilationUnit cu = run(code, genericColumnAccessor);

        final MethodCallExprAssert call = assertThat(cu).types().item(0).members().item(0)
                .asMethodDeclaration().body().stmts().item(0).asExpressionStmt().expression()
                .asVariableDeclarationExpr().vars().item(0).expression().asMethodCallExpr();

        call.args().item(0).asNameExpr().symbolData().asSymbolType().hasToString("apkg.Column<java.lang.Long>");
        call.symbolData().asSymbolType().hasToString("java.lang.Long");
    }

    @Test
    public void testMethodResolveWithBoxingWithGenerics() throws Exception {
        final String code = ""
                + "import static apkg.Column.*;\n"
                + "public class A {\n"
                + " public void foo() {\n"
                + "  long maxtime = 0;\n"
                + "  maxtime = Math.max(maxtime, getValue(Maxtime));\n"
                + " }\n"
                + " public <T> T getValue(apkg.Column<T> col) { return null; }\n"
                + "}\n";
        CompilationUnit cu = run(code, genericColumnAccessor);

        final BlockStmtAssert body =
                assertThat(cu).types().item(0).members().item(0).asMethodDeclaration().body();
        final VariableDeclarationExprAssert varDecl =
                body.stmts().item(0).asExpressionStmt().expression().asVariableDeclarationExpr();
        varDecl.type().symbolData().hasToString("long");
        varDecl.vars().item(0).expression().asIntegerLiteralExpr().hasValue(0);

        body.stmts().item(1).asExpressionStmt().expression()
                .asAssignExpr().value().asMethodCallExpr().symbolData().asMethodSymbolData()
                .method().hasToString("public static long java.lang.Math.max(long,long)");
    }

    @Test
    public void testMethodResolvesMethodReferencesAsStatements() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String externalClass = "package blah;" +
                    "import java.util.function.Supplier;" +
                    "public class MyClassThatNeedsToBeImported {" +
                    "public static <T> Supplier<T> memoize(Supplier<T> delegate) {" +
                    "return null;" + //Implementation not relevant to testcase
                    "}" +
                    "}";
            String code =
                    "import blah.MyClassThatNeedsToBeImported;" +
                            "import java.util.function.Supplier;" +
                            "public class MyClass {" +
                            "Supplier o;" +
                            "public MyClass() {" +
                            " this.o = MyClassThatNeedsToBeImported.memoize(this::load)::get;" +
                            "load();" +
                            "}" +
                            "private Object load() {" +
                            "return null;" + //Implementation not relevant to testcase
                            "}}";
            CompilationUnit cu = run(code, externalClass);
            Assert.assertEquals(1, cu.getImports().get(0).getUsages().size());
            Assert.assertEquals(2,
                    ((MethodDeclaration) cu.getTypes().get(0).getMembers().get(2)).getUsages().size());
        }
    }
}
