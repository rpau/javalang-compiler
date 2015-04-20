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
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.types.TypeTable;

public class TypeTableTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void testSimpleClass() throws Exception {

		File aux = new File(new File("src/main/java"),
				"org/walkmod/javalang/compiler/types/TypeTable.java");

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

		TypeTable ttl = TypeTable.getInstance();
		
		ttl.clear();
		
		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());

		ttl.visit(cu, null);

		Map map = ttl.getTypeTable();

		Assert.assertNotNull(map);

		Assert.assertNotNull(map.get("CompilationUnit"));
	}

	@Test
	public void importsWithAsterisk() throws Exception {
		String code = "import org.walkmod.javalang.compiler.symbols.*; public class Foo {}";

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(code);

		TypeTable ttl = TypeTable.getInstance();
		ttl.clear();
		
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

		TypeTable ttl = TypeTable.getInstance();
		ttl.clear();

		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());

		ttl.visit(cu, null);

		Map<String, String> map = ttl.getTypeTable();

		Assert.assertNotNull(map);

		Assert.assertNotNull(map.get("String"));

	}
	
	@Test
	public void javaInnerClasses() throws Exception {
		String code = "public class Foo { public class A {} public class B extends A{} }";
		CompilationUnit cu = (CompilationUnit) ASTManager.parse(code);

		TypeTable ttl = TypeTable.getInstance();
		ttl.clear();
		
		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());

		ttl.visit(cu, null);

		Map<String, String> map = ttl.getTypeTable();

		Assert.assertNotNull(map);

	}

}
