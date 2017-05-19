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
package org.walkmod.javalang.compiler.symbols;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDefinition;
import org.walkmod.javalang.ast.SymbolReference;

public class Symbol<T extends Node & SymbolDefinition> {

    private final ReferenceType referenceType;

    private final String name;

    private SymbolType type;

    private T location;

    private List<SymbolAction> actions;

    private Map<String, Object> attributes = new HashMap<String, Object>();

    private Scope scopes;

    /* comes from an static import */
    protected boolean staticallyImported = false;

    public Symbol(String name, SymbolType type, T location) {
        this(name, type, location, ReferenceType.VARIABLE, false, (List<SymbolAction>) null);
    }

    public Symbol(String name, SymbolType type, T location, ReferenceType referenceType) {
        this(name, type, location, referenceType, false, (List<SymbolAction>) null);
    }

    public Symbol(String name, SymbolType type, T location, ReferenceType referenceType, SymbolAction action) {
        this.referenceType = referenceType;
        this.name = name;
        setType(type);
        setLocation(location);
        actions = new LinkedList<SymbolAction>();
        actions.add(action);
    }

    public void setInnerScope(Scope scope) {
        this.scopes = scope;
    }

    public Symbol(String name, SymbolType type, T location, SymbolAction action) {
        this(name, type, location, ReferenceType.VARIABLE, action);

    }

    public Symbol(String name, SymbolType type, T location, ReferenceType referenceType, boolean staticallyImported,
            List<SymbolAction> actions) {
        this.referenceType = referenceType;
        this.name = name;
        setType(type);
        setLocation(location);
        this.actions = actions;
        this.staticallyImported = staticallyImported;
    }

    public boolean isStaticallyImported() {
        return staticallyImported;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public void setType(SymbolType type) {
        this.type = type;
    }

    public T getLocation() {
        return location;
    }

    public void setLocation(T initNode) {
        this.location = initNode;
    }

    @Override
    public String toString() {
        return name;
    }

    public Scope getInnerScope() {

        return scopes;
    }

    public List<SymbolAction> getActions() {
        return actions;
    }

    public void invokeActions(SymbolTable table, SymbolEvent event, SymbolReference reference) throws Exception {
        if (actions != null) {
            for (SymbolAction action : actions) {
                action.execute(this, table, event, reference);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Symbol) {
            return toString().equals(o.toString()) && getReferenceType().equals(((Symbol<?>) o).getReferenceType());
        }
        return false;
    }

}
