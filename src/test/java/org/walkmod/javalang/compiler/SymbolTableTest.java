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
		Symbol s = st.findSymbol("a", ReferenceType.VARIABLE);
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
		Symbol s = st.findSymbol("a", ReferenceType.VARIABLE);
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
