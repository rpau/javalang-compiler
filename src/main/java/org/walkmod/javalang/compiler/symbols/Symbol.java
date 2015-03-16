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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.Node;

public class Symbol {

	private String name;

	private SymbolType type;

	private Node location;

	private List<SymbolAction> actions;

	private Map<String, Object> attributes = new HashMap<String, Object>();

	private ReferenceType referenceType = ReferenceType.VARIABLE;

	public Symbol(String name, SymbolType type, Node location) {
		this(name, type, location, ReferenceType.VARIABLE,
				(List<SymbolAction>) null);
	}

	public Symbol(String name, SymbolType type, Node location,
			ReferenceType referenceType) {
		this(name, type, location, referenceType, (List<SymbolAction>) null);
	}

	public Symbol(String name, SymbolType type, Node location,
			ReferenceType referenceType, SymbolAction action) {
		setName(name);
		setType(type);
		setLocation(location);
		setReferenceType(referenceType);
		actions = new LinkedList<SymbolAction>();
		actions.add(action);
	}

	public Symbol(String name, SymbolType type, Node location,
			SymbolAction action) {
		this(name, type, location, ReferenceType.VARIABLE, action);

	}

	public Symbol(String name, SymbolType type, Node location,
			ReferenceType referenceType, List<SymbolAction> actions) {
		setName(name);
		setType(type);
		setLocation(location);
		setReferenceType(referenceType);
		this.actions = actions;
	}

	public ReferenceType getReferenceType() {
		return referenceType;
	}

	public void setReferenceType(ReferenceType referenceType) {
		this.referenceType = referenceType;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SymbolType getType() {
		return type;
	}

	public void setType(SymbolType type) {
		this.type = type;
	}

	public Node getLocation() {
		return location;
	}

	public void setLocation(Node initNode) {
		this.location = initNode;
	}

	@Override
	public String toString() {
		return name;
	}

	public void invokeActions(SymbolTable table, SymbolEvent event)
			throws Exception {
		if (actions != null) {
			for (SymbolAction action : actions) {
				action.execute(this, table, event);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Symbol) {
			return toString().equals(o.toString())
					&& getReferenceType().equals(
							((Symbol) o).getReferenceType());
		}
		return false;
	}

}
