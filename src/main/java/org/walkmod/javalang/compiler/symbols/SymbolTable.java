/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.compiler.types.TypeSymbolNotFound;
import org.walkmod.javalang.exceptions.SymbolTableException;

public class SymbolTable {

	private Stack<Scope> indexStructure;

	private SymbolGenerator symbolGenerator = null;

	private SymbolFactory symbolFactory = null;

	private List<SymbolAction> actions;

	public SymbolTable() {
		indexStructure = new Stack<Scope>();
		setSymbolFactory(new DefaultSymbolFactory());
	}

	public void setActions(List<SymbolAction> actions) {
		this.actions = actions;
	}

	public void setSymbolGenerator(SymbolGenerator symbolGenerator) {
		this.symbolGenerator = symbolGenerator;
		if (symbolGenerator != null) {
			symbolGenerator.setSymbolTable(this);
		}
	}

	public void setSymbolFactory(SymbolFactory symbolFactory) {
		this.symbolFactory = symbolFactory;
	}

	public SymbolType getType(String symbolName, ReferenceType referenceType) {
		Symbol s = findSymbol(symbolName, referenceType);
		if (s != null) {
			return s.getType();
		}
		return null;
	}

	public Symbol findSymbol(String symbolName, ReferenceType referenceType) {
		return findSymbol(symbolName, referenceType, null, null);
	}

	private void loadParentInterfacesSymbols() {
		Queue<java.lang.Class<?>> interfacesQueue = new ConcurrentLinkedQueue<java.lang.Class<?>>();

		Class<?>[] interfaces = findSymbol("this", ReferenceType.VARIABLE)
				.getClass().getInterfaces();

		if (interfaces != null) {
			for (Class<?> inter : interfaces) {
				interfacesQueue.add(inter);
			}
		}

		for (Class<?> inter : interfacesQueue) {
			Field[] fields = inter.getDeclaredFields();
			if (fields != null) {
				for (Field field : fields) {
					if (!Modifier.isPrivate(field.getModifiers())) {
						// if the symbol already exists, it has been
						// declared in a more closed superclass
						if (!containsSymbol(field.getName(),
								ReferenceType.VARIABLE)) {
							pushSymbol(field.getName(),ReferenceType.VARIABLE, new SymbolType(field
									.getType().getName()), null);
						}
					}
				}
			}
			Class<?> superClass = inter.getSuperclass();
			if (superClass != null) {
				if (!interfacesQueue.contains(superClass)) {
					interfacesQueue.add(superClass);
				}
			}
		}
	}

	private void loadParentSymbols() {
		Class<?> parentClazz = findSymbol("this", ReferenceType.VARIABLE)
				.getClass().getSuperclass();
		if (parentClazz != null) {
			SymbolType aux = new SymbolType(parentClazz.getName());
			aux.setClazz(parentClazz);
			pushSymbol("super", ReferenceType.VARIABLE, aux, null);
		}

		while (parentClazz != null) {
			Field[] fields = parentClazz.getDeclaredFields();
			if (fields != null) {
				for (Field field : fields) {
					if (!Modifier.isPrivate(field.getModifiers())) {
						// if the symbol already exists, it has been
						// declared in a more closed superclass
						if (!containsSymbol(field.getName(),
								ReferenceType.VARIABLE)) {
							SymbolType aux = new SymbolType(field.getType()
									.getName());
							aux.setClazz(field.getType());
							pushSymbol(field.getName(), ReferenceType.VARIABLE, aux, null);
						}
					}
				}
			}
			parentClazz = parentClazz.getSuperclass();
		}
	}

	public Symbol findSymbol(String symbolName, ReferenceType referenceType,
			SymbolType symbolScope, SymbolType[] args) {
		int i = indexStructure.size() - 1;
		Symbol result = null;

		while (i >= 0 && result == null) {
			Scope scope = indexStructure.get(i);
			result = scope.getSymbol(symbolName, referenceType, symbolScope,
					args);
			i--;
		}
		return result;
	}

	private void invokeActions(Scope scope, Symbol s, SymbolEvent event)
			throws Exception {
		if (s != null) {
			if (actions != null) {
				for (SymbolAction action : actions) {
					action.execute(s, this, event);
				}
			}

			if (scope != null) {
				List<SymbolAction> scopeActions = scope.getActions();
				if (scopeActions != null) {
					for (SymbolAction action : scopeActions) {
						action.execute(s, this, event);
					}
				}
			}
			s.invokeActions(this, event);
		}
	}

	public Symbol lookUpSymbolForRead(String symbolName,
			ReferenceType referenceType) {
		return lookUpSymbolForRead(symbolName, referenceType, null, null);
	}

	
	public Symbol lookUpSymbolForRead(String symbolName,
			ReferenceType referenceType, SymbolType symbolScope,
			SymbolType[] args) {
		Symbol s = findSymbol(symbolName, referenceType, symbolScope, args);
		try {
			invokeActions(indexStructure.peek(), s, SymbolEvent.READ);
		} catch (Exception e) {
			throw new SymbolTableException(e);
		}
		return s;
	}

	public Symbol lookUpSymbolForWrite(String symbolName) {
		return lookUpSymbolForWrite(symbolName, null, null);
	}

	public Symbol lookUpSymbolForWrite(String symbolName,
			SymbolType symbolScope, SymbolType[] args) {
		Symbol s = findSymbol(symbolName, ReferenceType.VARIABLE, symbolScope,
				args);
		try {
			invokeActions(indexStructure.peek(), s, SymbolEvent.WRITE);
		} catch (Exception e) {
			throw new SymbolTableException(e);
		}
		return s;
	}

	public boolean containsSymbol(String symbolName, ReferenceType referenceType) {
		SymbolType type = getType(symbolName, referenceType);
		return (type != null);
	}

	public void pushSymbol(Symbol symbol) {
		Scope lastScope = indexStructure.peek();
		String name = symbol.getName().toString();
		// the symbol already exists?
		SymbolType type = getType(name, symbol.getReferenceType());
		if (type != null && symbolGenerator != null) {
			Symbol aux = symbolGenerator.generateSymbol(type);
			lastScope.chageSymbol(findSymbol(name, symbol.getReferenceType()),
					aux);
		} else if (symbol.getType().getName() == null) {
			throw new TypeSymbolNotFound("Null symbol type resoltion for "
					+ name);
		}
		// if not, we add it
		lastScope.addSymbol(symbol);
		if (name.equals("this")) {
			loadParentSymbols();
			loadParentInterfacesSymbols();
		}
		try {
			invokeActions(lastScope, symbol, SymbolEvent.PUSH);
		} catch (Exception e) {
			throw new SymbolTableException(e);
		}

	}

	public void pushSymbol(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location) {
		Symbol symbol = symbolFactory.create(symbolName, referenceType,
				symbolType, location);
		pushSymbol(symbol);
	}

	public void pushSymbol(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, SymbolAction action) {
		Symbol symbol = symbolFactory.create(symbolName, referenceType,
				symbolType, location, action);
		pushSymbol(symbol);
	}

	public void pushSymbol(String symbolName, ReferenceType referenceType,
			SymbolType symbolType, Node location, List<SymbolAction> actions) {
		Symbol symbol = symbolFactory.create(symbolName, referenceType,
				symbolType, location, actions);
		pushSymbol(symbol);
	}

	public void pushSymbols(Set<String> names, ReferenceType referenceType,
			SymbolType symbolType, Node location, List<SymbolAction> actions) {
		for (String name : names) {
			pushSymbol(name, referenceType, symbolType, location, actions);
		}
	}

	public void popScope() {
		Scope scope = indexStructure.peek();

		List<Symbol> symbols = scope.getSymbols();
		for (Symbol symbol : symbols) {
			try {
				invokeActions(scope, symbol, SymbolEvent.POP);
			} catch (Exception e) {
				throw new SymbolTableException(e);
			}
		}
		indexStructure.pop();
	}

	public void pushScope() {
		pushScope(null);
	}

	public void pushScope(List<SymbolAction> actions) {
		Scope newScope = new Scope(actions);
		indexStructure.push(newScope);
	}

}
