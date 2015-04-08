package org.walkmod.javalang.compiler;

import java.io.File;
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
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.TryStmt;
import org.walkmod.javalang.compiler.actions.ReferencesCounterAction;
import org.walkmod.javalang.compiler.providers.RemoveUnusedSymbolsProvider;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.visitors.SemanticVisitorAdapter;

public class SemanticVisitorAdapterTest extends SemanticTest {

	@Test
	public void testNoActions() throws Exception {

		SemanticVisitorAdapter<HashMap<String, Object>> visitor = new SemanticVisitorAdapter<HashMap<String, Object>>();
		visitor.setClassLoader(Thread.currentThread().getContextClassLoader());
		visitor.setSymbolActions(new LinkedList<SymbolAction>());

		File aux = new File(new File("src/main/java"),
				"org/walkmod/javalang/visitors/SemanticVisitorAdapter.java");
		CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

		visitor.visit(cu, new HashMap<String, Object>());
		Assert.assertTrue(true);
	}

	@Test
	public void testReferencesCounterAction() throws Exception {
		SemanticVisitorAdapter<HashMap<String, Object>> visitor = new SemanticVisitorAdapter<HashMap<String, Object>>();

		visitor.setClassLoader(Thread.currentThread().getContextClassLoader());
		visitor.setSymbolActions(new LinkedList<SymbolAction>());

		ReferencesCounterAction counter = new ReferencesCounterAction();
		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		actions.add(counter);
		visitor.setSymbolActions(actions);

		File aux = new File(new File("src/main/java"),
				"org/walkmod/javalang/visitors/SemanticVisitorAdapter.java");
		CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

		visitor.visit(cu, new HashMap<String, Object>());
		Assert.assertTrue(counter.getReadsCounter() > 0);
	}

	private CompilationUnit runRemoveUnusedMembers(String code)
			throws Exception {

		CompilationUnit cu = compile(code);
		SemanticVisitorAdapter<HashMap<String, Object>> visitor = new SemanticVisitorAdapter<HashMap<String, Object>>();
		RemoveUnusedSymbolsProvider provider = new RemoveUnusedSymbolsProvider();

		visitor.setClassLoader(getClassLoader());
		visitor.setActionProvider(provider);
		visitor.visit(cu, new HashMap<String, Object>());
		return cu;
	}

	private CompilationUnit run(String code) throws Exception {
		CompilationUnit cu = compile(code);
		SemanticVisitorAdapter<HashMap<String, Object>> visitor = new SemanticVisitorAdapter<HashMap<String, Object>>();
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

		cu = runRemoveUnusedMembers("public class Foo { private void bar(){} private String getName() { return \"name\";}}");
		Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());

		cu = runRemoveUnusedMembers("public class Foo { private void bar(){} public String getName() { return \"name\";}}");
		Assert.assertEquals(1, cu.getTypes().get(0).getMembers().size());

		cu = runRemoveUnusedMembers("public class Foo { private void bar(){} public String getName() { bar(); return \"name\";}}");
		Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());

		cu = runRemoveUnusedMembers("public class Foo { private void bar(String s){} public String getName() { bar(null); return \"name\";}}");
		Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
	}

	@Test
	public void testRemoveUnusedFields() throws Exception {
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private String bar; }");
		Assert.assertTrue(cu.getTypes().get(0).getMembers().isEmpty());

		cu = runRemoveUnusedMembers("public class Foo { private String bar; public String getBar(){ return bar; }}");
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
		cu = runRemoveUnusedMembers("import javax.annotation.Generated; public class Foo {}");
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
			TryStmt stmt = (TryStmt)md.getBody().getStmts().get(0);
			SymbolData sd = stmt.getCatchs().get(0).getExcept().getSymbolData();
			Assert.assertNotNull(sd);
			List<Class<?>> bounds = sd.getBoundClasses();
			Assert.assertEquals("java.io.FileNotFoundException", bounds.get(0).getName());
			Assert.assertEquals("java.lang.ClassNotFoundException", bounds.get(1).getName());
		}
	}
}
