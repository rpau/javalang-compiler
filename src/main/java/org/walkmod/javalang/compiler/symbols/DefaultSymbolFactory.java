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

public class DefaultSymbolFactory implements SymbolFactory {

    @Override
    public Symbol create(String symbolName, ReferenceType referenceType, SymbolType type, Node location) {
        return new Symbol(symbolName, type, location, referenceType);
    }

    @Override
    public Symbol create(String symbolName, ReferenceType referenceType, SymbolType symbolType, Node location,
            SymbolAction action) {
        return new Symbol(symbolName, symbolType, location, referenceType, action);
    }

    @Override
    public Symbol create(String symbolName, ReferenceType referenceType, SymbolType symbolType, Node location,
            List<SymbolAction> actions) {
        return new Symbol(symbolName, symbolType, location, referenceType, false, actions);
    }

}
