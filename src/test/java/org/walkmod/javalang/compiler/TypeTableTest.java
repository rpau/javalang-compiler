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
package org.walkmod.javalang.compiler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.test.SemanticTest;

public class TypeTableTest extends SemanticTest {

    public void populateSemantics() throws Exception {}

    public ClassLoader getClassLoader() throws Exception {
        if (cl == null) {
            File aux = new File(CLASSES_DIR);
            cl = new URLClassLoader(new URL[] {aux.toURI().toURL(), new File("target/classes").toURI().toURL()});
        }
        return cl;
    }

    @Test
    public void importsWithAsterisk() throws Exception {
        String code = "import org.walkmod.javalang.compiler.symbols.*; public class Foo {}";

        CompilationUnit cu = compile(code);

        SymbolTable st = new SymbolTable();
        st.pushScope();
        TypesLoaderVisitor<?> ttl = new TypesLoaderVisitor<Object>(st, null, null);
        ttl.setClassLoader(getClassLoader());
        ttl.clear();

        ttl.setClassLoader(getClassLoader());

        cu.accept(ttl, null);

        Assert.assertNotNull(st.findSymbol("Scope"));

    }

    @Test
    public void javaLangTypes() throws Exception {
        String code = "public class Foo {}";

        CompilationUnit cu = compile(code);

        SymbolTable st = new SymbolTable();
        st.pushScope();
        TypesLoaderVisitor<?> ttl = new TypesLoaderVisitor<Object>(st, null, null);

        ttl.clear();
        ttl.setClassLoader(getClassLoader());
        cu.accept(ttl, null);

        Assert.assertNotNull(st.findSymbol("String"));

    }

    @Test
    public void javaInnerClasses() throws Exception {
        String code = "public class Foo { public class A {} public class B extends A{} }";

        CompilationUnit cu = compile(code);

        SymbolTable st = new SymbolTable();
        st.pushScope();
        TypesLoaderVisitor<?> ttl = new TypesLoaderVisitor<Object>(st, null, null);

        ttl.clear();
        ttl.setClassLoader(getClassLoader());
        cu.accept(ttl, null);

        Assert.assertNotNull(st.findSymbol("Foo.B"));

        st.pushScope();

        cu.getTypes().get(0).accept(ttl, null);

        Assert.assertNotNull(st.findSymbol("A"));

        Assert.assertNotNull(st.findSymbol("B"));

        Assert.assertNotNull(st.findSymbol("Foo.B"));

        Assert.assertNotNull(st.findSymbol("Foo"));
    }

    @Test
    public void testInnerClassesInsideClassesOfTheSamePackage() throws Exception {
        String code1 = "package foo; class A {}";
        String code2 = "package foo; class B { class C {}}";
        CompilationUnit cu = compile(code1, code2);
        SymbolTable st = new SymbolTable();
        st.pushScope();
        TypesLoaderVisitor<?> ttl = new TypesLoaderVisitor<Object>(st, null, null);

        ttl.clear();
        ttl.setClassLoader(getClassLoader());
        cu.accept(ttl, null);
        Assert.assertNotNull(st.findSymbol("B.C"));
        Assert.assertNull(st.findSymbol("C"));
    }

}
