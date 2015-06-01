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

	private CompilationUnit run(String code) throws Exception {
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
		CompilationUnit cu = runRemoveUnusedMembers(code);
		MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0)
				.getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
		VariableDeclarationExpr expr = (VariableDeclarationExpr) stmt
				.getExpression();
		ObjectCreationExpr oce = (ObjectCreationExpr) expr.getVars().get(0)
				.getInit();
		// The name attribute should be removed
		Assert.assertEquals(1, oce.getAnonymousClassBody().size());
	}

	@Test
	public void testMethodsOverwritingInAnnonymousClasses() throws Exception {
		String code = "public class A{ public Object get() { return null; } public A foo = new A() { public String get(){ return name();} private String name(){ return \"hello\"; }};}";
		CompilationUnit cu = runRemoveUnusedMembers(code);
		BodyDeclaration bd = cu.getTypes().get(0).getMembers().get(1);
		FieldDeclaration aux = (FieldDeclaration) bd;
		ObjectCreationExpr expr = (ObjectCreationExpr) aux.getVariables()
				.get(0).getInit();
		Assert.assertEquals(2, expr.getAnonymousClassBody().size());
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
	public void testGenericsIntroducedByAnInnerClass() throws Exception{
		String innerClass ="class Reference<T> { public T get() { return null;}} ";
		String mainClass ="public class A { "+innerClass+" void foo() { Reference<Reference<String>> r = null; r.get().get().length(); }}";
		run(mainClass);
		Assert.assertTrue(true);
	}


}
