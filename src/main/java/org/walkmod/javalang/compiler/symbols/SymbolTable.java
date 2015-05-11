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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.ast.SymbolDefinition;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.compiler.types.TypeSymbolNotFound;
import org.walkmod.javalang.exceptions.SymbolTableException;

public class SymbolTable {

	private Stack<Scope> indexStructure;

	private Stack<SymbolDefinition> definitionsStackTrace;

	
	private SymbolFactory symbolFactory = null;

	private List<SymbolAction> actions;

	public SymbolTable() {
		indexStructure = new Stack<Scope>();
		definitionsStackTrace = new Stack<SymbolDefinition>();
		setSymbolFactory(new DefaultSymbolFactory());
	}

	public void setActions(List<SymbolAction> actions) {
		this.actions = actions;
	}

	

	public void setSymbolFactory(SymbolFactory symbolFactory) {
		this.symbolFactory = symbolFactory;
	}

	public SymbolType getType(String symbolName, ReferenceType... referenceType) {
		Symbol<?> s = findSymbol(symbolName, referenceType);
		if (s != null) {
			return s.getType();
		}
		return null;
	}

	public Symbol<?> findSymbol(String symbolName,
			ReferenceType... referenceType) {
		return findSymbol(symbolName, null, null, referenceType);
	}

	public Symbol<?> findSymbol(String symbolName, SymbolType symbolScope,
			SymbolType[] args, ReferenceType... referenceType) {
		int j = indexStructure.size() - 1;
		Symbol<?> result = null;
		Scope selectedScope = null;
		int k = definitionsStackTrace.size() - 1;
		if (symbolScope != null) {

			while (j > 0 && selectedScope == null) {
				Scope scope = indexStructure.get(j);
				if (scope.isSymbolDefinitionScope()) {
					SymbolDefinition sd = definitionsStackTrace.get(k);
					if (sd instanceof TypeDeclaration
							|| sd instanceof ObjectCreationExpr) {
						if (symbolScope.equals(((SymbolDataAware<?>) sd)
								.getSymbolData())) {
							selectedScope = scope;
						}
					}
					if (selectedScope == null) {
						k--;
					}
				}
				if (selectedScope == null) {
					j--;
				}
			}

		}
		if (selectedScope == null) {
			j = indexStructure.size() - 1;
		}

		while (j >= 0 && result == null) {
			Scope scope = indexStructure.get(j);
			if (selectedScope != null && scope.isSymbolDefinitionScope()) {
				SymbolDefinition sd = definitionsStackTrace.get(k);
				if (sd instanceof SymbolDataAware<?>) {
					symbolScope = (SymbolType) ((SymbolDataAware<?>) sd)
							.getSymbolData();
				}
				k--;
			}
			result = scope.getSymbol(symbolName, symbolScope, args,
					referenceType);

			j--;
		}
		return result;
	}

	public List<Symbol<?>> findSymbolsByType(String typeName,
			ReferenceType referenceType) {
		List<Symbol<?>> result = new LinkedList<Symbol<?>>();
		int i = indexStructure.size() - 1;
		while (i >= 0) {
			Scope scope = indexStructure.get(i);
			result.addAll(scope.getSymbolsByType(typeName, referenceType));
			i--;
		}
		return result;
	}

	public List<Symbol<?>> findSymbolsByLocation(Node node) {
		List<Symbol<?>> result = new LinkedList<Symbol<?>>();
		int i = indexStructure.size() - 1;
		while (i >= 0) {
			Scope scope = indexStructure.get(i);
			result.addAll(scope.getSymbolsByLocation(node));
			i--;
		}
		return result;
	}

	private void invokeActions(Scope scope, Symbol<?> s, SymbolEvent event,
			SymbolReference reference) throws Exception {
		if (s != null) {
			if (actions != null) {
				for (SymbolAction action : actions) {
					action.execute(s, this, event, reference);
				}
			}

			if (scope != null) {
				List<SymbolAction> scopeActions = scope.getActions();
				if (scopeActions != null) {
					for (SymbolAction action : scopeActions) {
						action.execute(s, this, event, reference);
					}
				}
			}
			s.invokeActions(this, event, reference);
		}
	}

	public Symbol<?> lookUpSymbolForRead(String symbolName,
			SymbolReference reference, ReferenceType... referenceType) {
		return lookUpSymbolForRead(symbolName, reference, null, null,
				referenceType);
	}

	public Symbol<?> lookUpSymbolForRead(String symbolName,
			SymbolReference reference, SymbolType symbolScope,
			SymbolType[] args, ReferenceType... referenceType) {
		Symbol<?> s = findSymbol(symbolName, symbolScope, args, referenceType);
		if (s != null) {
			try {
				invokeActions(indexStructure.peek(), s, SymbolEvent.READ,
						reference);
			} catch (Exception e) {
				throw new SymbolTableException(e);
			}
		}
		return s;
	}

	public Symbol<?> lookUpSymbolForWrite(String symbolName,
			SymbolReference reference) {
		return lookUpSymbolForWrite(symbolName, reference, null, null);
	}

	public Symbol<?> lookUpSymbolForWrite(String symbolName,
			SymbolReference reference, SymbolType symbolScope, SymbolType[] args) {
		Symbol<?> s = findSymbol(symbolName, symbolScope, args,
				ReferenceType.VARIABLE);
		try {
			invokeActions(indexStructure.peek(), s, SymbolEvent.WRITE,
					reference);
		} catch (Exception e) {
			throw new SymbolTableException(e);
		}
		return s;
	}

	public boolean containsSymbol(String symbolName, ReferenceType referenceType) {
		SymbolType type = getType(symbolName, referenceType);
		return (type != null);
	}

	public int getScopeLevel() {
		if (indexStructure != null) {
			return indexStructure.size() - 1;
		}
		return 0;
	}

	public boolean pushSymbol(Symbol<?> symbol) {
		Scope lastScope = indexStructure.peek();
		String name = symbol.getName().toString();

		if (symbol.getType().getName() == null) {
			throw new TypeSymbolNotFound("Null symbol type resoltion for "
					+ name);
		}
		Object definition = symbol.getLocation();
		if (name.equals("this")
				&& (definition instanceof ObjectCreationExpr || definition instanceof EnumConstantDeclaration)) {
			int max = this.definitionsStackTrace.size() - 2;
			int j = indexStructure.size() - 2;
			String suffixName = null;

			// we upgrade the class counter in the closest inner class
			while (j > 0 && suffixName == null) {
				Scope sc = indexStructure.get(j);
				if (sc.isSymbolDefinitionScope()) {
					SymbolDefinition sd = definitionsStackTrace.get(max);
					if (sd instanceof ObjectCreationExpr
							|| sd instanceof TypeDeclaration) {

						sc.incrInnerAnonymousClassCounter();

						int num = sc.getInnerAnonymousClassCounter();
						suffixName = "$" + num;
						symbol.getType().setName(
								((SymbolDataAware<?>) sd).getSymbolData()
										.getName() + suffixName);
					}
					max--;
				}
				j--;
			}

		}
		// if not, we add it
		if (lastScope.addSymbol(symbol)) {
			try {
				invokeActions(lastScope, symbol, SymbolEvent.PUSH, null);
			} catch (Exception e) {
				throw new SymbolTableException(e);
			}
			return true;
		}
		else {
			return false;
		}

	}

	public boolean pushSymbol(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location) {
		Symbol<?> symbol = symbolFactory.create(symbolName, referenceType,
				symbolType, location);
		return pushSymbol(symbol);
	}

	public boolean pushSymbol(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, SymbolAction action) {
		Symbol<?> symbol = symbolFactory.create(symbolName, referenceType,
				symbolType, location, action);
		return pushSymbol(symbol);
	}

	public boolean pushSymbol(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, List<SymbolAction> actions) {
		Symbol<?> symbol = symbolFactory.create(symbolName, referenceType,
				symbolType, location, actions);
		return pushSymbol(symbol);
	}

	public boolean pushSymbols(Set<String> names, ReferenceType referenceType,
			SymbolType symbolType, Node location, List<SymbolAction> actions) {
		boolean allAdded = true;
		for (String name : names) {
			allAdded = allAdded && pushSymbol(name, referenceType, symbolType, location, actions);
		}
		return allAdded;
	}

	public void popScope() {
		Scope scope = indexStructure.peek();

		List<Symbol<?>> symbols = scope.getSymbols();
		for (Symbol<?> symbol : symbols) {
			try {
				invokeActions(scope, symbol, SymbolEvent.POP, null);
			} catch (Exception e) {
				throw new SymbolTableException(e);
			}
		}
		indexStructure.pop();
		if (scope.isSymbolDefinitionScope()) {
			definitionsStackTrace.pop();
		}
	}

	public void pushScope() {
		pushScope(null, null);
	}

	public void pushScope(SymbolDefinition symbolDefinition) {
		pushScope(symbolDefinition, null);
	}

	public void addActionsToScope(List<SymbolAction> actions) {
		indexStructure.peek().addActions(actions);
	}

	public void pushScope(SymbolDefinition symbolDefinition,
			List<SymbolAction> actions) {
		Scope newScope = new Scope(symbolDefinition != null, actions);
		indexStructure.push(newScope);
		if (symbolDefinition != null) {
			definitionsStackTrace.push(symbolDefinition);
		}
	}

	public Stack<SymbolDefinition> getDefinitionsStackTrace() {
		return definitionsStackTrace;
	}

}
