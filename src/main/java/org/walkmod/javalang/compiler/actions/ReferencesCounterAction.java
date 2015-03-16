package org.walkmod.javalang.compiler.actions;

import java.util.Map;

import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class ReferencesCounterAction implements SymbolAction {

	public static final String READS = "_READS_";

	public static final String WRITES = "_WRITES_";

	private int readsCounter = 0;

	private int writesCounter = 0;

	@Override
	public void execute(Symbol symbol, SymbolTable table, SymbolEvent event) {
		if (event.equals(SymbolEvent.READ)) {
			update(symbol, READS);
			readsCounter++;
		} else if (event.equals(SymbolEvent.WRITE)) {
			update(symbol, WRITES);
			writesCounter++;
		}
	}

	private void update(Symbol symbol, String key) {
		Map<String, Object> attrs = symbol.getAttributes();

		Object reads = attrs.get(key);
		if (reads == null) {
			attrs.put(key, new Integer(1));
		} else {
			attrs.put(key, new Integer((Integer) reads) + 1);
		}
	}

	public int getReadsCounter() {
		return readsCounter;
	}

	public void setReadsCounter(int readsCounter) {
		this.readsCounter = readsCounter;
	}

	public int getWritesCounter() {
		return writesCounter;
	}

	public void setWritesCounter(int writesCounter) {
		this.writesCounter = writesCounter;
	}

}
