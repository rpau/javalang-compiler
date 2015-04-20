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
