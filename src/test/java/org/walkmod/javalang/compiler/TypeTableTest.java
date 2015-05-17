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

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;

public class TypeTableTest {

	@Test
	public void testSimpleClass() throws Exception {

		File aux = new File(new File("src/main/java"),
				"org/walkmod/javalang/compiler/types/TypesLoaderVisitor.java");

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(aux);

		SymbolTable st = new SymbolTable();
		st.pushScope();

		TypesLoaderVisitor ttl = new TypesLoaderVisitor(st, null,
				null);

		ttl.clear();

		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());

		cu.accept(ttl, null);

		Assert.assertNotNull(st.findSymbol("TypesLoaderVisitor"));

	}

	@Test
	public void importsWithAsterisk() throws Exception {
		String code = "import org.walkmod.javalang.compiler.symbols.*; public class Foo {}";

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(code);

		SymbolTable st = new SymbolTable();
		st.pushScope();
		TypesLoaderVisitor ttl = new TypesLoaderVisitor(st, null,
				null);
		ttl.clear();

		URL[] classpath = new URL[] { new File("target/classes").toURI()
				.toURL() };
		URLClassLoader urlCL = new URLClassLoader(classpath);

		ttl.setClassLoader(urlCL);

		cu.accept(ttl, null);

		Assert.assertNotNull(st.findSymbol("Scope"));

	}

	@Test
	public void javaLangTypes() throws Exception {
		String code = "public class Foo {}";

		CompilationUnit cu = (CompilationUnit) ASTManager.parse(code);

		SymbolTable st = new SymbolTable();
		st.pushScope();
		TypesLoaderVisitor ttl = new TypesLoaderVisitor(st, null,
				null);

		ttl.clear();

		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());

		cu.accept(ttl, null);

		Assert.assertNotNull(st.findSymbol("String"));

	}

	@Test
	public void javaInnerClasses() throws Exception {
		String code = "public class Foo { public class A {} public class B extends A{} }";
		CompilationUnit cu = (CompilationUnit) ASTManager.parse(code);

		SymbolTable st = new SymbolTable();
		st.pushScope();
		TypesLoaderVisitor ttl = new TypesLoaderVisitor(st, null,
				null);

		ttl.clear();

		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());
		cu.accept(ttl, null);

		Assert.assertNotNull(st.findSymbol("Foo.B"));

		st.pushScope();

		cu.getTypes().get(0).accept(ttl, null);

		Assert.assertNotNull(st.findSymbol("A"));

		Assert.assertNotNull(st.findSymbol("B"));

		Assert.assertNotNull(st.findSymbol("Foo.B"));

		Assert.assertNotNull(st.findSymbol("Foo"));
	}

}
