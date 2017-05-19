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

import junit.framework.Assert;

import org.junit.Test;

import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class SymbolTableTest {

    @Test
    public void tesPushSymbol() {
        SymbolTable st = new SymbolTable();
        st.pushScope();
        SymbolType type = new SymbolType("java.lang.String");
        st.pushSymbol("a", ReferenceType.VARIABLE, type, null);
        Symbol<?> s = st.findSymbol("a", ReferenceType.VARIABLE);
        Assert.assertNotNull(s);
        st.popScope();
    }

    @Test
    public void testVisibility() {
        SymbolTable st = new SymbolTable();
        st.pushScope();
        SymbolType type = new SymbolType("java.lang.String");
        st.pushSymbol("a", ReferenceType.VARIABLE, type, null);
        st.pushScope();
        Symbol<?> s = st.findSymbol("a", ReferenceType.VARIABLE);
        Assert.assertNotNull(s);
        st.pushSymbol("a", ReferenceType.VARIABLE, new SymbolType("int"), null);
        s = st.findSymbol("a", ReferenceType.VARIABLE);
        Assert.assertEquals("int", s.getType().getName());
        st.popScope();
        s = st.findSymbol("a", ReferenceType.VARIABLE);
        Assert.assertEquals("java.lang.String", s.getType().getName());
        st.popScope();
    }

    @Test
    public void testMultipleSymbolsWithSameName() {
        SymbolTable st = new SymbolTable();
        st.pushScope();
        SymbolType type = new SymbolType("A");
        st.pushSymbol("a", ReferenceType.VARIABLE, type, null);
        st.pushScope();
    }

}
