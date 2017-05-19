/*
 * Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 * 
 * Walkmod is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Walkmod. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.walkmod.javalang.test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.Compiler;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolVisitorAdapter;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;

public abstract class SemanticTest {

    public static String SOURCES_DIR = "./src/test/resources/tmp/";
    public static String CLASSES_DIR = "./src/test/resources/tmp/classes";
    private TypesLoaderVisitor tt = null;
    private CompilationUnit cu = null;
    protected ClassLoader cl = null;
    private SymbolTable symTable = null;

    @Before
    public void prepare() throws Exception {
        File compilerDir = new File(CLASSES_DIR);
        compilerDir.mkdir();
    }

    @After
    public void clean() throws Exception {
        File sourcesDir = new File(SOURCES_DIR);

        removeRecursively(sourcesDir);

        tt = null;
        cu = null;
        cl = null;
        symTable = null;
    }

    public void removeRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File aux : files) {
                    removeRecursively(aux);
                }
            }
        }
        file.delete();
    }

    public CompilationUnit compile(String... sources) throws Exception {
        Compiler compiler = new Compiler();
        compiler.compile(new File(CLASSES_DIR), new File(SOURCES_DIR), sources);
        if (sources != null) {
            cu = ASTManager.parse(sources[0]);
            populateSemantics();
        }
        return cu;
    }

    public void populateSemantics() throws Exception {
        populateSemantics(cu);
    }

    protected CompilationUnit populateSemantics(final CompilationUnit cu) throws Exception {
        SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
        visitor.setClassLoader(getClassLoader());
        visitor.visit(cu, new HashMap<String, Object>());
        return cu;
    }

    public ClassLoader getClassLoader() throws Exception {
        if (cl == null) {
            File aux = new File(CLASSES_DIR);
            cl = new URLClassLoader(new URL[] {aux.toURI().toURL()});
        }
        return cl;
    }

    public TypesLoaderVisitor getTypeTable() throws Exception {
        if (tt == null) {
            getSymbolTable().pushScope();
            tt = new TypesLoaderVisitor(getSymbolTable(), null, null);
            tt.setClassLoader(getClassLoader());

            cu.accept(tt, null);
        }
        return tt;
    }

    public void initTypes() throws Exception {
        getTypeTable();
    }

    public SymbolTable getSymbolTable() {
        if (symTable == null) {
            symTable = new SymbolTable();
        }
        return symTable;
    }
}
