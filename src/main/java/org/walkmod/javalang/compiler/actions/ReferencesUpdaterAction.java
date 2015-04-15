package org.walkmod.javalang.compiler.actions;

import java.util.Iterator;
import java.util.Stack;

import org.walkmod.javalang.ast.SymbolDefinition;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class ReferencesUpdaterAction extends SymbolAction {

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) {
		SymbolDefinition def = symbol.getLocation();
		if (def != null) {
			int scope = table.getScopeLevel();
			def.setScopeLevel(scope);
		}
	}
	
	private void updateDefinitions(SymbolTable table, SymbolReference emitter){
		Stack<SymbolDefinition> defs = table.getDefinitionsStackTrace();
		Iterator<SymbolDefinition> it = defs.iterator();
		while(it.hasNext()){
			SymbolDefinition sd = it.next();
			sd.addBodyReference(emitter);
		}
	}

	@Override
	public void doRead(Symbol<?> symbol, SymbolTable table,
			SymbolReference emitter) {
		SymbolDefinition def = symbol.getLocation();
		if (def != null) {
			def.addUsage(emitter);
		}
		updateDefinitions(table, emitter);
	}

	@Override
	public void doWrite(Symbol<?> symbol, SymbolTable table,
			SymbolReference emitter) {
		SymbolDefinition def = symbol.getLocation();
		if (def != null) {
			def.addUsage(emitter);
		}
		updateDefinitions(table, emitter);
	}

}
