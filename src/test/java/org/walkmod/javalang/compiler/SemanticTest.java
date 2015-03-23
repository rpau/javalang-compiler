package org.walkmod.javalang.compiler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.types.TypeTable;

public abstract class SemanticTest {

	private static String SOURCES_DIR = "./src/test/resources/tmp/";
	private static String CLASSES_DIR = "./src/test/resources/tmp/classes";
	private TypeTable<Map<String, Object>> tt = null;
	private CompilationUnit cu = null;
	private ClassLoader cl = null;
	private SymbolTable symTable = null;

	@Before
	public void prepare() throws Exception {
		File compilerDir = new File(CLASSES_DIR);
		compilerDir.mkdir();
	}

	@After
	public void clean() throws Exception {
		File compilerDir = new File(CLASSES_DIR);
		compilerDir.delete();
		tt = null;
		cu = null;
		cl = null;
		symTable = null;
	}

	public CompilationUnit compile(String code) throws Exception {
		Compiler compiler = new Compiler();
		compiler.compile(new File(CLASSES_DIR), new File(SOURCES_DIR), code);
		cu = ASTManager.parse(code);
		return cu;
	}

	public ClassLoader getClassLoader() throws Exception {
		if (cl == null) {
			File aux = new File(CLASSES_DIR);
			cl = new URLClassLoader(new URL[] { aux.toURI().toURL() });
		}
		return cl;
	}

	public TypeTable<Map<String, Object>> getTypeTable() throws Exception {
		if (tt == null) {
			tt = TypeTable.getInstance();
			tt.setClassLoader(getClassLoader());
			cu.accept(tt, new HashMap<String, Object>());
		}
		return tt;
	}

	public SymbolTable getSymbolTable() {
		if (symTable == null) {
			symTable = new SymbolTable();
		}
		return symTable;
	}
}
