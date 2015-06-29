/*
 Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 
Walkmod is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Walkmod is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.compiler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.lang.model.SourceVersion;

import org.junit.Assert;
import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.ArrayCreationExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.TryStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.actions.ReferencesCounterAction;
import org.walkmod.javalang.compiler.providers.RemoveUnusedSymbolsProvider;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolVisitorAdapter;
import org.walkmod.javalang.test.SemanticTest;

public class SymbolVisitorAdapterTest extends SemanticTest {

	@Test
	public void testNoActions() throws Exception {

		SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
		String[] classpathEntries = System.getProperty("java.class.path")
				.split(File.pathSeparator);
		URL[] classpath = new URL[classpathEntries.length];
		int i = 0;
		for (String entry : classpathEntries) {
			classpath[i] = new File(entry).toURI().toURL();
			i++;
		}
		visitor.setClassLoader(new URLClassLoader(classpath));
		visitor.setSymbolActions(new LinkedList<SymbolAction>());

		File aux = new File(new File("src/main/java"),
				"org/walkmod/javalang/compiler/symbols/SymbolVisitorAdapter.java");
		CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

		visitor.visit(cu, new HashMap<String, Object>());
		Assert.assertTrue(true);
	}

	@Test
	public void testReferencesCounterAction() throws Exception {
		SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();

		String[] classpathEntries = System.getProperty("java.class.path")
				.split(File.pathSeparator);
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

		File aux = new File(new File("src/main/java"),
				"org/walkmod/javalang/compiler/symbols/SymbolVisitorAdapter.java");
		CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

		visitor.visit(cu, new HashMap<String, Object>());
		Assert.assertTrue(counter.getReadsCounter() > 0);
	}

	private CompilationUnit runRemoveUnusedMembers(String code)
			throws Exception {

		CompilationUnit cu = compile(code);
		SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
		RemoveUnusedSymbolsProvider provider = new RemoveUnusedSymbolsProvider();

		visitor.setClassLoader(getClassLoader());
		visitor.setActionProvider(provider);
		visitor.visit(cu, new HashMap<String, Object>());
		return cu;
	}

	public void populateSemantics() throws Exception {
	}

	private CompilationUnit run(String... code) throws Exception {
		CompilationUnit cu = compile(code);
		SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
		visitor.setClassLoader(getClassLoader());
		visitor.visit(cu, new HashMap<String, Object>());
		return cu;
	}

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

		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private void bar(){} private String getName() { return \"name\";}}");
		Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
	}

	@Test
	public void testRemoveUnusedMethods2() throws Exception {

		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private void bar(){} public String getName() { return \"name\";}}");
		Assert.assertEquals(1, cu.getTypes().get(0).getMembers().size());
	}

	@Test
	public void testRemoveUnusedMethods3() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private void bar(){} public String getName() { bar(); return \"name\";}}");
		Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
	}

	@Test
	public void testRemoveUnusedMethods4() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private void bar(String s){} public String getName() { bar(null); return \"name\";}}");
		Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
	}

	@Test
	public void testRemoveUnusedFields() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private String bar; }");
		Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
	}

	public void testRemoveUnusedFields1() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private String bar; public String getBar(){ return bar; }}");
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
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
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
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private class A{} private String A =\"a\"; public void bar(){ A=\"b\";} }");
		Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
	}

	@Test
	public void testStaticImportsWithWildcard() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("import static java.lang.Math.*; public class HelloWorld { private double compute = PI; private double foo() { return (PI * pow(2.5,2));} }");
		Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());
		Assert.assertTrue(!cu.getImports().isEmpty());
	}

	@Test
	public void testStaticImportsWithSpecificMember() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("import static java.lang.Math.PI; public class HelloWorld { private double compute = PI; private double foo() { return (PI * 2);} }");
		Assert.assertTrue(!cu.getImports().isEmpty());
	}

	@Test
	public void testImportsOfAnnotations() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("import javax.annotation.Generated; @Generated(value=\"WALKMOD\") public class Foo {}");
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
				+ "values in the list are not supported.\n\n"
				+ "<p>\n"
				+ "The returned comparator throws an {@link ClassCastException} when it receives an input\n"
				+ "parameter that isn't among the provided values.\n\n"
				+ "<p>\n*"
				+ "The generated comparator is serializable if all the provided values are serializable.\n"
				+

				" @param valuesInOrder the values that the returned comparator will be able to compare, in the\n"
				+ "order the comparator should induce\n"
				+ " @return the comparator described above\n"
				+ " @throws NullPointerException if any of the provided values is null\n"
				+ " @throws IllegalArgumentException if {@code valuesInOrder} contains any duplicate values\n"
				+ " (according to {@link Object#equals})\n*";

		String code = "public class Foo { /**" + javadoc
				+ "/ public void foo(){}}";
		runRemoveUnusedMembers(code);

		javadoc = "This class provides a skeletal implementation of the {@code Cache} interface to minimize the\n"
				+ " effort required to implement this interface.\n\n"
				+ " <p>\n"
				+ " To implement a cache, the programmer needs only to extend this class and provide an\n"
				+ " implementation for the {@link #get(Object)} and {@link #getIfPresent} methods.\n"
				+ " {@link #getUnchecked}, {@link #get(Object, Callable)}, and {@link #getAll} are implemented in\n"
				+ " terms of {@code get}; {@link #getAllPresent} is implemented in terms of {@code getIfPresent};\n"
				+ " {@link #putAll} is implemented in terms of {@link #put}, {@link #invalidateAll(Iterable)} is\n"
				+ " implemented in terms of {@link #invalidate}. The method {@link #cleanUp} is a no-op. All other\n"
				+ " methods throw an {@link UnsupportedOperationException}.";

		code = "import java.util.concurrent.Callable; public class Foo {/**"
				+ javadoc + "*/ public void foo(){}}";
		CompilationUnit cu = runRemoveUnusedMembers(code);
		Assert.assertTrue(!cu.getImports().isEmpty());

		javadoc = "Returns a comparator that compares two arrays of unsigned {@code int} values lexicographically.\n"
				+ " That is, it compares, using {@link #compare(int, int)}), the first pair of values that follow\n"
				+ " any common prefix, or when one array is a prefix of the other, treats the shorter array as the\n"
				+ " lesser. For example, {@code [] < [1] < [1, 2] < [2] < [1 << 31]}.\n"
				+

				" <p>\n"
				+ " The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays\n"
				+ " support only identity equality), but it is consistent with {@link Arrays#equals(int[], int[])}.\n"
				+

				" @see <a href=\"http://en.wikipedia.org/wiki/Lexicographical_order\"> Lexicographical order\n"
				+ "      article at Wikipedia</a>\n";
		code = "import java.util.Arrays; public class Foo {/**" + javadoc
				+ "*/ public void foo(){}}";
		cu = runRemoveUnusedMembers(code);
		Assert.assertTrue(!cu.getImports().isEmpty());
	}

	@Test
	public void testImportsOfJavadocMultipleLinesTypes() throws Exception {
		String javadoc = "\n*  Weak reference with a {@code finalizeReferent()} method which a background thread invokes after\n"
				+ " * the garbage collector reclaims the referent. This is a simpler alternative to using a {@link\n"
				+ " * Arrays}.\n";
		String code = "import java.util.Arrays; public class Foo {/**"
				+ javadoc + "*/ public void foo(){}}";
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
		String code = "public class A{ " + consumerCode + methodToReferece
				+ methodCode + "}";
		if (SourceVersion.latestSupported().ordinal() >= 8) {
			CompilationUnit cu = runRemoveUnusedMembers(code);
			Assert.assertEquals(3, cu.getTypes().get(0).getMembers().size());
		}
	}

	@Test
	public void testLambdaExpressions() throws Exception {
		String code = "public class A{ private interface C{ public int get(int c); } public void run(){ C a = (b)->b; } }";
		if (SourceVersion.latestSupported().ordinal() >= 8) {
			CompilationUnit cu = runRemoveUnusedMembers(code);
			Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
		}
	}

	@Test
	public void testReferencesToAnnotationMembers() throws Exception {
		String code = "import java.lang.annotation.ElementType;\n"
				+ "import java.lang.annotation.Retention;\n"
				+ "import java.lang.annotation.RetentionPolicy;\n"
				+ "import java.lang.annotation.Target;\n"
				+ "@Retention(RetentionPolicy.RUNTIME)\n"
				+ "@Target(ElementType.METHOD)\n"
				+ "public @interface Foo { public boolean enabled() default true; }";
		CompilationUnit cu = runRemoveUnusedMembers(code);
		Assert.assertTrue(!cu.getImports().isEmpty());
	}

	@Test
	public void testAnnonymousClass() throws Exception {
		String code = "public class Foo{ public void bar() { Foo o = new Foo() { private String name; public void bar() { System.out.println(\"hello\"); }};}}";
		CompilationUnit cu = run(code);
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
		VariableDeclarationExpr expr = (VariableDeclarationExpr) stmt
				.getExpression();
		ObjectCreationExpr oce = (ObjectCreationExpr) expr.getVars().get(0)
				.getInit();
		// The name attribute should be removed
		FieldDeclaration fd =(FieldDeclaration)oce.getAnonymousClassBody().get(0);
		
		Assert.assertNull(fd.getUsages());
	}

	@Test
	public void testMethodsOverwritingInAnnonymousClasses() throws Exception {
		String code = "public class A{ public Object get() { return null; } public A foo = new A() { public String get(){ return name();} private String name(){ return \"hello\"; }};}";
		CompilationUnit cu = run(code);
		BodyDeclaration bd = cu.getTypes().get(0).getMembers().get(1);
		FieldDeclaration aux = (FieldDeclaration) bd;
		ObjectCreationExpr expr = (ObjectCreationExpr) aux.getVariables()
				.get(0).getInit();
		MethodDeclaration md = (MethodDeclaration)expr.getAnonymousClassBody().get(1);
		Assert.assertNotNull(md.getUsages());
	}

	@Test
	public void testAnonymousArrayExpressions() throws Exception {
		CompilationUnit cu = run("public class A{ Integer v[][] = { new Integer[] {3} }; Integer a[] = v[0]; }");
		FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
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
			MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
					.getMembers().get(1);
			TryStmt stmt = (TryStmt) md.getBody().getStmts().get(0);
			SymbolData sd = stmt.getCatchs().get(0).getExcept().getSymbolData();
			Assert.assertNotNull(sd);
			List<Class<?>> bounds = sd.getBoundClasses();
			Assert.assertEquals("java.io.FileNotFoundException", bounds.get(0)
					.getName());
			Assert.assertEquals("java.lang.ClassNotFoundException",
					bounds.get(1).getName());
		}
	}

	@Test
	public void testGenericClasses() throws Exception {
		CompilationUnit cu = run("public class Box<T> {" + "private T t;"
				+ "public T get() {return t;}"
				+ "public void set(T t) { this.t = t;}" + "}");

		FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		SymbolData sd = fd.getType().getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals(true, sd.isTemplateVariable());
		Assert.assertEquals("java.lang.Object", sd.getName());
	}

	@Test
	public void testGenericClassesWithBounds() throws Exception {
		CompilationUnit cu = run("import java.util.*;import java.io.*; "
				+ "public class Foo<A extends Map<String, Object>>"
				+ " extends LinkedList<A> implements Serializable{}");
		ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) cu
				.getTypes().get(0);
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
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		Assert.assertNotNull(md.getSymbolData().getMethod());
		Assert.assertEquals("getName", md.getSymbolData().getMethod().getName());
	}

	@Test
	public void testConstructorResolution() throws Exception {
		CompilationUnit cu = run("public class A { private String name; public A(String name){this.name = name;}}");
		ConstructorDeclaration cd = (ConstructorDeclaration) cu.getTypes()
				.get(0).getMembers().get(1);
		Assert.assertNotNull(cd.getSymbolData().getConstructor());
		Assert.assertEquals(1, cd.getSymbolData().getConstructor()
				.getParameterTypes().length);
	}

	@Test
	public void testFieldResolution() throws Exception {
		CompilationUnit cu = run("public class A { private String name; }");
		FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		Assert.assertNotNull(fd.getFieldsSymbolData());
		Assert.assertEquals(1, fd.getFieldsSymbolData().size());
		Assert.assertEquals("name", fd.getFieldsSymbolData().get(0).getField()
				.getName());
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
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		AnnotationExpr ann = md.getAnnotations().get(0);
		Assert.assertNotNull(ann.getSymbolData());
		Assert.assertEquals("java.lang.Override", ann.getSymbolData().getName());
	}

	@Test
	public void testGenericMethodResultType() throws Exception {
		CompilationUnit cu = run("import java.util.List;import java.util.LinkedList; public class A { public List<? extends A> foo(){ return new LinkedList<A>();} public void bar() { foo().get(0); }}");
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(1);
		ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);

		Assert.assertNotNull(stmt.getExpression().getSymbolData());
		Assert.assertEquals("A", stmt.getExpression().getSymbolData().getName());

	}

	@Test
	public void testGenericMethodResultType2() throws Exception {
		CompilationUnit cu = run("import java.util.List;import java.util.LinkedList; public class A { public class B {} public class C {public List<? extends B> foo(){ return new LinkedList<B>();} public void bar() { foo().get(0);} }}");

		ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu
				.getTypes().get(0).getMembers().get(1);

		MethodDeclaration md = (MethodDeclaration) type.getMembers().get(1);
		ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);

		Assert.assertNotNull(stmt.getExpression().getSymbolData());
		Assert.assertEquals("A$B", stmt.getExpression().getSymbolData()
				.getName());

	}

	@Test
	public void testInnerClassAttributesReferences() throws Exception {
		CompilationUnit cu = run("public class A{ public Object foo() { return C.INSTANCE; } private static class C { private static C INSTANCE; }}");
		ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu
				.getTypes().get(0).getMembers().get(1);
		Assert.assertNotNull(type.getMembers());
		Assert.assertTrue(!type.getMembers().isEmpty());
	}

	@Test
	public void testInnerClassInsideAnnonymousClass() throws Exception {
		run("public class A{ public Object foo() {  A a = new A() { public Object foo() { return new B(); } class B{ int c; }}; return a; }}");
		Assert.assertTrue(true);
	}

	@Test
	public void testScopes() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private int c; class A { int c; } class B extends A { public int x = c; }}");
		ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu
				.getTypes().get(0);
		Assert.assertEquals(2, type.getMembers().size());
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
		CompilationUnit cu = run("public class A { private int var; private void foo() {} class B extends A { void bar() { foo(); var = 1;}}}");
		ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu
				.getTypes().get(0);
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
		String mergeSorted = "public static <T> Iterable<T> mergeSorted(final Iterable<? extends Iterable<? extends T>> iterables){\n";
		mergeSorted += "A.transform(iterables, A.<T>toIterator()).iterator().next().next(); return null; }\n";

		String toIterator = "private static <T> Function<Iterable<? extends T>, Iterator<? extends T>> \n";
		toIterator += "toIterator() {\n";
		toIterator += "return null;\n";
		toIterator += "}\n";

		String transform = "public static <F, T> Iterable<T> transform(final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) { return null; }";

		run("import java.util.*; public class A { " + mergeSorted + toIterator
				+ transform + " public class Function<F,T> {} }");

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
		String mainClass = "public class A { "
				+ innerClass
				+ " void foo() { Reference<Reference<String>> r = null; r.get().get().length(); }}";
		run(mainClass);
		Assert.assertTrue(true);
	}

	@Test
	public void testGenericsWithRewrittenTypeParams() throws Exception {
		String class1 = "class B<T> { public T get() { return null; }}";
		String class2 = "class C<T> extends B<B<T>> {}";
		String main = "public class A { "
				+ class1
				+ " "
				+ class2
				+ " void foo () { C<String> c = null; c.get().get().trim().length(); }}";
		run(main);
		Assert.assertTrue(true);
	}

	@Test
	public void testGenericsWithRewrittenTypeParamsWithInheritance()
			throws Exception {
		String class1 = "class B<T> { public T get() { return null; }}";
		String class2 = "class C<T extends File> extends B<List<T>> { @Override public List<T> get() {return null;}}";
		String main = "import java.util.*; import java.io.File; public class A { "
				+ class1
				+ " "
				+ class2
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

		String main = "import java.util.*; public class A<K,V> { " + class1
				+ " " + class2 + " " + class3 + "}";
		run(main);
		Assert.assertTrue(true);
	}

	@Test
	public void testGenericsWithRewrittenTypeParams4() throws Exception {
		String classB = "class B<K, V> { K getKey() { return null; }}";
		String code = "import java.util.*; public class C<K extends java.io.File, V> { "
				+ classB
				+ " class A extends B<List<K>, V> { void foo(K value){ value.getAbsolutePath().trim();}}}";
		run(code);
		Assert.assertTrue(true);
	}

	@Test
	public void testGenericsWithClassParameters() throws Exception {
		String class1 = "public static class Range { public static <C extends Collection<?>> List<C> range(C lower, boolean x, C upper, boolean y) { return null;}}";
		String code = "import java.util.*; public class A { "
				+ class1
				+ " void bar(){ Range.range(new ArrayList(), true, new ArrayList(), true).iterator().next().get(0).toString(); }}";
		run(code);
		Assert.assertTrue(true);
	}

	@Test
	public void testGenericsWithClassParameters2() throws Exception {
		String class1 = "public static class Range { public static <C extends Collection<?>> C range(C lower, boolean x, C upper, boolean y) { return null;}}";
		String code = "import java.util.*; public class A { "
				+ class1
				+ " void bar(){ Range.range(new ArrayList(), true, new ArrayList(), true).get(0).toString(); }}";
		run(code);
		Assert.assertTrue(true);
	}

	@Test
	public void testFieldMethodCallWithGenerics() throws Exception {
		String code = "import java.util.List; public class A<T> { List<A<? super T>> comparators; String foo() { comparators.get(0).foo().trim().length(); return null;}}";
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
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		Assert.assertNull(md.getUsages());
		md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
		Assert.assertNotNull(md.getUsages());
		Assert.assertEquals(1, md.getUsages().size());
	}
	
	@Test
	public void testConstructorOrdering() throws Exception {
		String code = "public class A { A(long i) {} A(int i) {} void bar(){ A aux = new A(1); }}";
		CompilationUnit cu = run(code);
		ConstructorDeclaration md = (ConstructorDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		Assert.assertNull(md.getUsages());
		md = (ConstructorDeclaration) cu.getTypes().get(0).getMembers().get(1);
		Assert.assertNotNull(md.getUsages());
		Assert.assertEquals(1, md.getUsages().size());
	}

	@Test
	public void testMethodOrdering2() throws Exception {
		String code = "public class A { void foo(long i) {} void foo(int i) {} void bar(){ foo(1L); }}";
		CompilationUnit cu = run(code);
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		Assert.assertNotNull(md.getUsages());
		Assert.assertEquals(1, md.getUsages().size());
	}

	@Test
	public void testObjecMethodsInBasicArrays() throws Exception {
		String code = "public class A { void foo() { int[] c = null; c.toString().toString(); }}";
		run(code);
		Assert.assertTrue(true);
	}

	@Test
	public void testObjecMethodsInBasicArrays2() throws Exception {
		String code = "public class A { void foo() { int[] c = null; int i = c.clone()[0]; }}";
		run(code);
		Assert.assertTrue(true);
	}

	@Test
	public void testObjecMethodsInBasicArrays3() throws Exception {
		String code = "public class A { void bar(int aux){} void foo() { int[] c = null; bar(c.clone().length); }}";
		run(code);
		Assert.assertTrue(true);
	}

	@Test
	public void testArrayInitExprType() throws Exception {
		String code = "public class A { void foo() { int[] c = new int[10];}} ";
		CompilationUnit cu = run(code);
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
		VariableDeclarationExpr expr = (VariableDeclarationExpr) stmt
				.getExpression();
		ArrayCreationExpr array = (ArrayCreationExpr) expr.getVars().get(0)
				.getInit();
		Assert.assertNotNull(array.getDimensions().get(0).getSymbolData());
	}

	@Test
	public void testArrayInitExprType2() throws Exception {
		String code = "public class A { private int x = 10; void foo() { int[] c = new int[x];}} ";
		CompilationUnit cu = runRemoveUnusedMembers(code);
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(1);
		ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
		VariableDeclarationExpr expr = (VariableDeclarationExpr) stmt
				.getExpression();
		Assert.assertNotNull(expr.getVars().get(0).getInit().getSymbolData());
	}

	@Test
	public void testArrayArgumentsOrder() throws Exception {
		String code = "import java.util.Arrays; public class A { void foo( byte[] digest) { foo(Arrays.copyOf(digest, 3)); } } ";
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
	public void testTypeDeclarationStmts2() throws Exception{
		run("public enum A { B { public Object foo() { class D { int c = 0; int x = c;} return new D(); }}, C {}; public Object foo() { class B { int c = 0; int x = c;} return new B(); }}");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testConditionalArgument() throws Exception{
		run("public class A  { String newTypeVariableImpl(String name, Integer[] bounds){ return null; }  void newArtificialTypeVariable(String name, Integer... bounds) { newTypeVariableImpl(name, (bounds.length == 0)? new Integer[] { new Integer(1) }: bounds).concat(\"hello\").length(); }}  ");
		Assert.assertTrue(true);
	}
	
	
	@Test
	public void testSuperExpressionsWithClassContext() throws Exception{
		run("import java.util.*; public class A extends LinkedList{ void foo(){ A.super.add(null);}}");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testInnerClassReferencesFromVarDeclarations() throws Exception{
		run("public class A { void bar() { B.C aux = new B.C(); aux.foo(); } private static final class B { private static final class C { void foo() {}}} }");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testTypeCompatibilityWithGenericsAndBasicTypes() throws Exception{
		run("import java.util.*; public class A { void foo(List<Number> list) { boolean a = list.add(3) && true;} }");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testUpperBoundsInAResolvedType() throws Exception{
		CompilationUnit cu = run("import java.util.*; public class EquivalenceTester<T> { List<? super T> aux; EquivalenceTester(List<? super T> aux){ this.aux = aux;} void foo(){  EquivalenceTester.of(new LinkedList<String>()); } public static <T> EquivalenceTester<T> of(List<? super T> equivalence) {return new EquivalenceTester<T>(equivalence);}}");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(2);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		SymbolData sd = stmt.getExpression().getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals(String.class.getName(), sd.getParameterizedTypes().get(0).getName());
		Assert.assertTrue(true);
	}
	
	@Test
	public void testFloats() throws Exception{
		CompilationUnit cu = run("public class A { void foo() { foo(1.0f); } void foo(float f){} }");
		Assert.assertTrue(true);
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd =  mce.getArgs().get(0).getSymbolData();
		Assert.assertEquals(float.class.getName(), sd.getName());
	}
	
	@Test
	public void genericsWithBasicTypes() throws Exception{
		CompilationUnit cu = run("public class A { void foo() { java.util.Arrays.asList(1, 2, 3, null); } }");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals(Integer.class.getName(), sd.getParameterizedTypes().get(0).getName());
		Assert.assertTrue(true);
	}
	
	@Test
	public void testInnerClassesPlusTypeStmtPlusEnums() throws Exception{
		run("public enum A { B{}, C{}; void foo() { class B {} } void foo1() { class B{} } void foo2(){ class B{ class C{}} new B().new C().toString(); }  }");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testInnerClassesInsideTypeStmts() throws Exception{
		run("public class A { void foo() { class B {} } void foo1() { class B{} } void foo2(){ class B{ class C{}} new B().new C().toString(); }  }");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testInnerClassesInsideTypeStmts2() throws Exception{
		run("public class A { void foo() { class B {} } void foo1() { class C{} } void foo2(){ class B{ class C{}} new B().new C().toString(); }  }");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testDynamicArraysInMethodCalls() throws Exception{
		CompilationUnit cu = run("public class A { private byte[] toByteArray(int... bytes) { return null; }  private void assertWellFormed(int... bytes) { toByteArray(bytes); } } ");
		Assert.assertTrue(true);
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		Assert.assertNotNull(md.getUsages());
	}
	
	@Test
	public void testInterfaceImplementationsWithGenerics() throws Exception{
		CompilationUnit cu = run("public class A { interface B<T> { public T get();} class C implements B<String> { public String get (){return null;}} <F> F bar(B<F> b) { return null; } void car() { bar(new C()); }}");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(3);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals(String.class.getName(), sd.getName());
		
	}
	
	@Test
	public void testImplementationWithGenericsRewrittingLetters() throws Exception{
		CompilationUnit cu = run("public class A { class B <B,K,V> { B get() { return null; }} class C<K,V> extends B<C<K,V>,K, V>{} void foo() { C<String,String> c = new C<String,String>(); c.get(); } }");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(2);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(1);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("A$C", sd.getName());
	}
	
	@Test
	public void testImplementationWithGenericsRewrittingLetters2() throws Exception{
		CompilationUnit cu = run("import java.util.*; public class A { class Z <B> { B get() { return null; }}  class D<K, V> extends C <HashMap<K, V>> {} class C<X> extends Z<X>{} void foo() { D<String, String> c = new D<String, String>(); c.get(); } }");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(3);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(1);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("java.util.HashMap", sd.getName());
		Assert.assertNotNull(sd.getParameterizedTypes());
		Assert.assertEquals("java.lang.String", sd.getParameterizedTypes().get(0).getName());
		Assert.assertEquals("java.lang.String", sd.getParameterizedTypes().get(1).getName());
	}
	
	@Test
	public void testBug1() throws Exception{
		CompilationUnit cu = run("import java.util.*; public class A<K,V> { public static <T> T firstNonNull( T first, T second) { return null;} public Map<K, Collection<V>> asMap() {return null;} void foo(K key, Collection<V> value) { A.firstNonNull(asMap().remove(key),value); } }");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(2);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("java.util.Collection", sd.getName());
		Assert.assertNotNull(sd.getParameterizedTypes());
		Assert.assertEquals("java.lang.Object", sd.getParameterizedTypes().get(0).getName());
	}
	
	@Test
	public void testSingletonList() throws Exception{
		CompilationUnit cu = run("import java.util.Collections; public class A { void foo(String[] array) { Collections.singletonList(array); } }");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
	}
	
	@Test
	public void testTemplateResults() throws Exception{
		CompilationUnit cu = run("public class A<X> { public static <C extends Comparable<?>> A<C> all(){ return null;} void foo() { A.<Integer>all(); } }");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(1);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("A", sd.getName());
		Assert.assertNotNull(sd.getParameterizedTypes());
		Assert.assertEquals("java.lang.Integer", sd.getParameterizedTypes().get(0).getName());
		
	}
	
	@Test
	public void testFieldDeclarationInsideAnonymousClass() throws Exception{
		run("import java.util.*; public class A { public Object get() { Map<String,String> aux = new HashMap<String, String>() {}; return new HashMap<String, String>() {boolean iteratorCalled;}; }}");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testPriorityObjectVersusInt() throws Exception{
		CompilationUnit cu = run("import java.util.*; public class A { void foo(List<Integer> contents, int i) { contents.remove(Integer.valueOf(i)); }}");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("boolean", sd.getName());
	}
	
	@Test
	public void testMethodsThatReturnsMatrixs() throws Exception{
		CompilationUnit cu = run("public class A { char[][] bar(){ return new char[0][0]; } void foo(String s) { int i = bar().length; }}");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(1);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		VariableDeclarationExpr vexpr = (VariableDeclarationExpr)stmt.getExpression();
		Expression expr = vexpr.getVars().get(0).getInit();
		SymbolData sd = expr.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("int", sd.getName());
	}
	
	@Test
	public void methodOrderingArrays() throws Exception{
		String m1 = "public static byte[] aryEq(final byte[] value) {return null;} ";
		String m2 = "public static char[] aryEq(final char[] value) {return null;} ";
		String m3 = "public static double[] aryEq(final double[] value) {return null;} ";
		String m4 = "public static float[] aryEq(final float[] value) {return null;} ";
		String m5 = "public static int[] aryEq(final int[] value) {return null;} ";
		String m6 = "public static long[] aryEq(final long[] value) {return null;} ";
		String m7 = "public static short[] aryEq(final short[] value) {return null; }";
		String m8 = "public static <T> T[] aryEq(final T[] value) {return null; }";
		CompilationUnit cu = run("public class A { public void foo(byte[] arg){aryEq(arg);} "+m1+m2+m3+m4+m5+m6+m7+m8+"}");
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("byte", sd.getName());
	}
	
	@Test
	public void testGenericsOnInheritedAttributes() throws Exception{
		CompilationUnit cu = run("public class A { class C<T> { T data; } class D extends C<byte[]> { void foo(byte[] x){} void aux(){foo(data);}} }");
		TypeDeclaration td = (TypeDeclaration)cu.getTypes().get(0).getMembers().get(1);
		MethodDeclaration md = (MethodDeclaration) td.getMembers().get(1);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr mce = (MethodCallExpr) stmt.getExpression();
		SymbolData sd = mce.getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals(Void.class.getName(), sd.getName());
		sd = mce.getArgs().get(0).getSymbolData();
		Assert.assertNotNull(sd);
		Assert.assertEquals("byte", sd.getName());
	}
	
	@Test
	public void testMethodResolutionWithRecursiveGenericsInMethodScope() throws Exception{
		run("import java.util.*; class A { public <K extends List<V>, V extends List<K>> void withMutualRecursiveBound(List<Map<K, V>> list) {}}");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testMethodResolutionWithMultipleBoundsGenericsInMethodScope() throws Exception{
		run("import java.util.*; class A { <T extends Number & CharSequence> void withUpperBound(List<T> list) {} }");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testObjectCreationExprWithScope() throws Exception{
		run("class Owner<T>{ class Inner<T> {} void foo() { Object o = new Owner<Integer>().new Inner<String>() {}; } }");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testInheritanceOnNestedClassesFromAnObjectCreation() throws Exception{
		run("class A { private static class Owner<T>{ void foo() { Object o =new Owner<Integer>().new Inner<String>() {}; } private static abstract class Nested<X> {} private abstract class Inner<Y> extends Nested<Y> {}}  } ");
		Assert.assertTrue(true);
	}
	
	@Test
	public void testMethodCallsOnAnonymousClasses() throws Exception{
		String mainClass = "public class A { public void testTwoStageResolution() { class ForTwoStageResolution<X extends Number> {  <B extends X> void verifyTwoStageResolution() { TypeToken type = new TypeToken<B>(getClass()) {}.where(new TypeParameter<B>() {}, (Class) Integer.class); } } } }";
		String typeTokenClass = "public class TypeToken<T> { public TypeToken(Class clazz) {}  public final <X> TypeToken<T> where(TypeParameter<X> typeParam, Class<X> typeArg) {return null;} }";
		String typeParamClass = "public class TypeParameter<T> {}";
		run(mainClass , typeTokenClass , typeParamClass);
		Assert.assertTrue(true);
	}
	
	@Test
	public void testClassesInsideAnonymousIds() throws Exception{
		String mainClass = "public class A { void foo() {new From<Integer>() {}.new To<String>().type();}}";
		String fromClass = "public class From<T>{ public class To<T> { void type(){}} }";
		
		run(mainClass, fromClass);
		Assert.assertTrue(true);
	}

	
	@Test
	public void testClassesInsideAnonymousIds2() throws Exception{
		String mainClass = "public class A { void foo() {new From<Integer>() {}.new To<String>() {}.type();}}";
		String fromClass = "public class From<T>{ public class To<T> { void type(){}} }";
		
		run(mainClass, fromClass);
		Assert.assertTrue(true);
	}
	
	@Test
	public void loadMethodsInsideAnonymousClasses() throws Exception{
		String stmt="Service service2=new Service() {@Override public final void addListener(Listener listener, Executor executor) {}};";
		String mainClass="import java.util.concurrent.Executor; public class A { void foo() {"+stmt+"}}";
		String serviceClass ="import java.util.concurrent.Executor; public class Service{ abstract class Listener {}  public void addListener(Listener listener, Executor executor){} }";
		String listenerClass ="class Listener {}";
		run(mainClass, serviceClass, listenerClass);
		Assert.assertTrue(true);
	}
	
}
