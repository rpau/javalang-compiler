package org.walkmod.javalang.compiler;

import java.io.File;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;

public class TypeTableLoaderTest {
	
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testSimpleClass() throws Exception{
		
		File aux = new File(new File("src/main/java"),"org/walkmod/javalang/compiler/TypeTableLoader.java");
		
		CompilationUnit cu = (CompilationUnit)ASTManager.parse(aux);
		
		TypeTableLoader ttl = new TypeTableLoader();
		
		ttl.setClassLoader(this.getClass().getClassLoader());
		
		ttl.visit(cu, null);
		
		Map map = ttl.getTypeTable();
		
		Assert.assertNotNull(map);
		
		Assert.assertNotNull(map.get("CompilationUnit"));
	}
	
	@Test
	public void importsWithAsterisk() throws Exception{
		String code = "import org.walkmod.javalang.visitors.*; public class Foo {}";
		
		CompilationUnit cu = (CompilationUnit)ASTManager.parse(code);
		
		TypeTableLoader ttl = new TypeTableLoader();
		
		ttl.setClassLoader(Thread.currentThread().getContextClassLoader());
		
		ttl.visit(cu, null);
		
		Map<String, String> map = ttl.getTypeTable();
		
		Assert.assertNotNull(map);
		
		Assert.assertNotNull(map.get("VoidVisitor"));
		
	}

}
