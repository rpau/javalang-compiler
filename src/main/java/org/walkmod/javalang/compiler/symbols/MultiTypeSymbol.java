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

import java.util.List;

import org.walkmod.javalang.ast.Node;

public class MultiTypeSymbol extends Symbol {

    private List<SymbolType> types;

    @SuppressWarnings("unchecked")
    public MultiTypeSymbol(String name, SymbolType type, Node location) {
        super(name, type, location);
    }

    @SuppressWarnings("unchecked")
    public MultiTypeSymbol(String name, List<SymbolType> types, Node location) {
        super(name, types.get(0), location);
        this.types = types;
    }

    @SuppressWarnings("unchecked")
    public MultiTypeSymbol(String name, List<SymbolType> types, Node location, List<SymbolAction> actions) {
        super(name, types.get(0), location, ReferenceType.VARIABLE, false, actions);
        this.types = types;
    }

    public List<SymbolType> getTypes() {
        return types;
    }

}
