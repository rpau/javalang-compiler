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

import org.walkmod.javalang.ast.SymbolReference;

public abstract class SymbolAction {

    public final void execute(Symbol<?> symbol, SymbolTable table, SymbolEvent event, SymbolReference reference)
            throws Exception {
        if (event.equals(SymbolEvent.PUSH)) {
            doPush(symbol, table);
        } else if (event.equals(SymbolEvent.POP)) {
            doPop(symbol, table);
        } else if (event.equals(SymbolEvent.READ)) {
            doRead(symbol, table, reference);
        } else if (event.equals(SymbolEvent.WRITE)) {
            doWrite(symbol, table, reference);
        }
    }

    public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {}

    public void doPop(Symbol<?> symbol, SymbolTable table) throws Exception {}

    public void doRead(Symbol<?> symbol, SymbolTable table, SymbolReference reference) throws Exception {}

    public void doWrite(Symbol<?> symbol, SymbolTable table, SymbolReference reference) throws Exception {}
}
