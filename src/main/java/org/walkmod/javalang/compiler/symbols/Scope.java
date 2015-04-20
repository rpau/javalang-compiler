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
package org.walkmod.javalang.compiler.symbols;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDefinition;

public class Scope {

	private Map<String, List<Symbol<?>>> symbols = new HashMap<String, List<Symbol<?>>>();

	private List<SymbolAction> actions;

	private boolean isSymbolDefinitionScope = false;

	public Scope() {
	}
	
	public Scope(boolean isSymbolDefinitionScope) {
		this(isSymbolDefinitionScope, null);
	}

	public Scope(List<SymbolAction> actions) {
		this(false, actions);
	}

	public Scope(boolean isSymbolDefinitionScope, List<SymbolAction> actions) {
		this.actions = actions;
		this.isSymbolDefinitionScope = isSymbolDefinitionScope;
	}

	public List<Symbol<?>> getSymbols() {
		List<Symbol<?>> result = new LinkedList<Symbol<?>>();

		Iterator<List<Symbol<?>>> it = symbols.values().iterator();
		while (it.hasNext()) {
			result.addAll(it.next());
		}
		return result;
	}

	public Symbol<?> getSymbol(String name) {
		return getSymbol(name, ReferenceType.VARIABLE);
	}

	public Symbol<?> getSymbol(String name, ReferenceType referenceType) {
		List<Symbol<?>> list = symbols.get(name);
		if (list == null) {
			return null;
		} else {
			Iterator<Symbol<?>> it = list.iterator();
			while (it.hasNext()) {
				Symbol<?> s = it.next();
				if (referenceType == null
						|| s.getReferenceType().equals(referenceType)) {
					return s;
				}
			}
			return null;
		}
	}

	public List<Symbol<?>> getSymbols(String name) {
		return symbols.get(name);
	}

	public List<Symbol<?>> getSymbolsByLocation(Node node) {
		List<Symbol<?>> result = new LinkedList<Symbol<?>>();
		Collection<List<Symbol<?>>> values = symbols.values();
		Iterator<List<Symbol<?>>> it = values.iterator();
		while (it.hasNext()) {
			List<Symbol<?>> list = it.next();
			for (Symbol<?> symbol : list) {
				if (symbol.getLocation() == node) {// yes, by reference
					result.add(symbol);
				}
			}
		}

		return result;
	}

	public List<Symbol<?>> getSymbolsByType(String typeName,
			ReferenceType referenceType) {
		List<Symbol<?>> result = new LinkedList<Symbol<?>>();
		Collection<List<Symbol<?>>> values = symbols.values();
		Iterator<List<Symbol<?>>> it = values.iterator();
		while (it.hasNext()) {
			List<Symbol<?>> list = it.next();
			for (Symbol<?> symbol : list) {
				if (symbol.getReferenceType() == referenceType) {// yes, by
																	// reference

					if (symbol.getType().getName().startsWith(typeName)) {
						result.add(symbol);
					}
				}
			}
		}

		return result;

	}

	public Symbol<?> getSymbol(String name, ReferenceType referenceType,
			SymbolType scope, SymbolType[] args) {
		if (args == null) {
			return getSymbol(name, referenceType);
		} else {
			List<Symbol<?>> values = symbols.get(name);
			if (values != null) {
				for (Symbol<?> symbol : values) {
					if (symbol instanceof MethodSymbol) {
						MethodSymbol aux = (MethodSymbol) symbol;
						if (aux.hasCompatibleSignature(scope, args)) {
							return aux;
						}
					}
				}
			}
		}
		return null;
	}

	public void chageSymbol(Symbol<?> oldSymbol, Symbol<?> newSymbol) {
		List<Symbol<?>> list = symbols.get(oldSymbol.getName());
		if (list.remove(oldSymbol)) {
			List<Symbol<?>> values = symbols.get(newSymbol.getName());
			if (values == null) {
				values = new LinkedList<Symbol<?>>();
				symbols.put(newSymbol.getName(), values);
			}
			values.add(newSymbol);
		}
	}

	public <T extends Node & SymbolDefinition> Symbol<T> addSymbol(
			String symbolName, SymbolType type, T location) {

		Symbol<T> s = new Symbol<T>(symbolName, type, location);
		return addSymbol(s);
	}

	public <T extends Node & SymbolDefinition> Symbol<T> addSymbol(
			Symbol<T> symbol) {
		List<Symbol<?>> values = symbols.get(symbol.getName());
		if (values == null) {
			values = new LinkedList<Symbol<?>>();
			symbols.put(symbol.getName(), values);
		} else {
			values.remove(symbol);
		}
		values.add(symbol);
		return symbol;
	}

	public List<SymbolAction> getActions() {
		return actions;
	}

	public void addActions(List<SymbolAction> actions) {
		if (this.actions == null) {
			this.actions = actions;
		} else {
			this.actions.addAll(actions);
		}
	}

	public boolean isSymbolDefinitionScope() {
		return isSymbolDefinitionScope;
	}

}
