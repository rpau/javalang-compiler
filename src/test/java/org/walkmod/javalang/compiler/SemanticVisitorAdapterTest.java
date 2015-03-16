package org.walkmod.javalang.compiler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.compiler.actions.ReferencesCounterAction;
import org.walkmod.javalang.compiler.providers.RemoveUnusedSymbolsProvider;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.visitors.SemanticVisitorAdapter;

public class SemanticVisitorAdapterTest {

	private static String SOURCES_DIR = "./src/test/resources/tmp/";
	private static String CLASSES_DIR = "./src/test/resources/tmp/classes";

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

	@Before
	public void prepare() throws Exception {
		File compilerDir = new File(CLASSES_DIR);
		compilerDir.mkdir();
	}

	@After
	public void clean() throws Exception {
		File compilerDir = new File(CLASSES_DIR);
		compilerDir.delete();
	}

	private CompilationUnit runRemoveUnusedMembers(String code)
			throws Exception {
		Compiler compiler = new Compiler();

		compiler.compile(new File(CLASSES_DIR), new File(SOURCES_DIR), code);
		CompilationUnit cu = ASTManager.parse(code);
		SemanticVisitorAdapter<HashMap<String, Object>> visitor = new SemanticVisitorAdapter<HashMap<String, Object>>();
		RemoveUnusedSymbolsProvider provider = new RemoveUnusedSymbolsProvider();
		File aux = new File(CLASSES_DIR);

		ClassLoader cl = new URLClassLoader(new URL[] { aux.toURI().toURL() });
		visitor.setClassLoader(cl);
		visitor.setActionProvider(provider);
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
	public void testDifferentReferenceTypesUnderTheSameName() throws Exception {
		// the latest one is the unique that can be referenced
		CompilationUnit cu = runRemoveUnusedMembers("public class Foo { private class A{} private String A =\"a\"; public void bar(){ A=\"b\";} }");
		Assert.assertEquals(2, cu.getTypes().get(0).getMembers().size());
	}

	@Test
	public void testStaticImports() {
		// TODO: static imports implies its members are available from "this"
	}

	@Test
	public void testReferencesToAnnotations() {
		// TODO: mark the types of the annotations
	}

	@Test
	public void testReferencesToEnum() {
		// TODO: which is the type of an expression using annotations? it is
		// well resolved?
	}
}
