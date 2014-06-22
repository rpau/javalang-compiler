package org.walkmod.javalang.compiler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;

public class TypeTableTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void testSimpleClass() throws Exception {

		File aux = new File(new File("src/main/java"),
				"org/walkmod/javalang/compiler/TypeTable.java");

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

		TypeTable ttl = new TypeTable();

		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());

		ttl.visit(cu, null);

		Map map = ttl.getTypeTable();

		Assert.assertNotNull(map);

		Assert.assertNotNull(map.get("CompilationUnit"));
	}

	@Test
	public void importsWithAsterisk() throws Exception {
		String code = "import org.walkmod.javalang.compiler.*; public class Foo {}";

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(code);

		TypeTable ttl = new TypeTable();
		
		URL[] classpath = new URL[] { new File("target/classes").toURI().toURL()};
		URLClassLoader urlCL = new URLClassLoader(classpath);

		ttl.setClassLoader(urlCL);

		ttl.visit(cu, null);

		Map<String, String> map = ttl.getTypeTable();

		Assert.assertNotNull(map);

		Assert.assertNotNull(map.get("Scope"));

	}

	@Test
	public void javaLangTypes() throws Exception {
		String code = "public class Foo {}";

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(code);

		TypeTable ttl = new TypeTable();

		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());

		ttl.visit(cu, null);

		Map<String, String> map = ttl.getTypeTable();

		Assert.assertNotNull(map);

		Assert.assertNotNull(map.get("String"));

	}

}
