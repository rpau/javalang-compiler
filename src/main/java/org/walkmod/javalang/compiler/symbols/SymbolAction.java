package org.walkmod.javalang.compiler.symbols;

import org.walkmod.javalang.ast.SymbolReference;

public abstract class SymbolAction {

	public final void execute(Symbol<?> symbol, SymbolTable table,
			SymbolEvent event, SymbolReference reference) throws Exception {
		if (event.equals(SymbolEvent.PUSH)) {
			doPush(symbol, table);
		} else if (event.equals(SymbolEvent.POP)) {
			doPop(symbol, table);
		} else if (event.equals(SymbolEvent.READ)) {
			doRead(symbol, table, reference);
		} else {
			doWrite(symbol, table, reference);
		}
	}

	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
	}

	public void doPop(Symbol<?> symbol, SymbolTable table) throws Exception {
	}

	public void doRead(Symbol<?> symbol, SymbolTable table,
			SymbolReference reference) throws Exception {
	}

	public void doWrite(Symbol<?> symbol, SymbolTable table,
			SymbolReference reference) throws Exception {
	}
}
