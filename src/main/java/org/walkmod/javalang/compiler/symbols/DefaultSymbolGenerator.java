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

public class DefaultSymbolGenerator implements SymbolGenerator {

    private int symbolCounter = 1;

    private SymbolTable symbolTable;

    @Override
    public Symbol generateSymbol(SymbolType typeName) {

        String sname = "v" + symbolCounter;
        SymbolType type = symbolTable.getType(sname, ReferenceType.VARIABLE);
        if (type != null) {
            symbolCounter++;
            return generateSymbol(typeName);
        }

        Symbol result = new Symbol(sname, null, null);

        return result;
    }

    @Override
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

}
