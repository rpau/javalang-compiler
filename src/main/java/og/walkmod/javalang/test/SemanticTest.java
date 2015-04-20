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
package og.walkmod.javalang.test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.Compiler;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolVisitorAdapter;
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
		File sourcesDir = new File(SOURCES_DIR);
		File[] sources = sourcesDir.listFiles();
		if (sources != null) {
			for (File source : sources) {
				source.delete();
			}
		}

		File compilerDir = new File(CLASSES_DIR);
		File[] files = compilerDir.listFiles();
		if (files != null) {
			for (File file : files) {
				file.delete();
			}
		}
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
		populateSemantics();
		return cu;
	}
	
	public void populateSemantics() throws Exception{
		SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
		visitor.setClassLoader(getClassLoader());
		visitor.visit(cu, new HashMap<String, Object>());
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
