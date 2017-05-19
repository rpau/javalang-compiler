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
package org.walkmod.javalang.compiler.actions;

import java.util.Map;

import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class ReferencesCounterAction extends SymbolAction {

    public static final String READS = "_READS_";

    public static final String WRITES = "_WRITES_";

    private int readsCounter = 0;

    private int writesCounter = 0;

    @Override
    public void doRead(Symbol<?> symbol, SymbolTable table, SymbolReference emitter) {
        update(symbol, READS);
        readsCounter++;
    }

    @Override
    public void doWrite(Symbol<?> symbol, SymbolTable table, SymbolReference emitter) {
        update(symbol, WRITES);
        writesCounter++;
    }

    private void update(Symbol<?> symbol, String key) {
        Map<String, Object> attrs = symbol.getAttributes();

        Object reads = attrs.get(key);
        if (reads == null) {
            attrs.put(key, new Integer(1));
        } else {
            attrs.put(key, new Integer((Integer) reads) + 1);
        }
    }

    public int getReadsCounter() {
        return readsCounter;
    }

    public void setReadsCounter(int readsCounter) {
        this.readsCounter = readsCounter;
    }

    public int getWritesCounter() {
        return writesCounter;
    }

    public void setWritesCounter(int writesCounter) {
        this.writesCounter = writesCounter;
    }

}
