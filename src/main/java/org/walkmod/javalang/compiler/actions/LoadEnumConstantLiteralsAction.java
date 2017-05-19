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

import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class LoadEnumConstantLiteralsAction extends SymbolAction {

    @Override
    public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
        if (symbol.getName().equals("this")) {
            Node node = symbol.getLocation();
            if (node instanceof EnumDeclaration) {
                EnumDeclaration ed = (EnumDeclaration) node;
                List<EnumConstantDeclaration> entries = ed.getEntries();
                if (entries != null) {
                    for (EnumConstantDeclaration ecd : entries) {
                        table.pushSymbol(ecd.getName(), ReferenceType.ENUM_LITERAL, symbol.getType(), ecd);
                    }
                }
            }
        }
    }

    @Override
    public void doPop(Symbol<?> symbol, SymbolTable table) throws Exception {}

    @Override
    public void doRead(Symbol<?> symbol, SymbolTable table, SymbolReference reference) throws Exception {}

    @Override
    public void doWrite(Symbol<?> symbol, SymbolTable table, SymbolReference reference) throws Exception {}
}
