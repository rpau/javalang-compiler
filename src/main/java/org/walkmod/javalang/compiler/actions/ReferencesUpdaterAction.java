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

import java.util.Iterator;
import java.util.Stack;

import org.walkmod.javalang.ast.SymbolDefinition;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.compiler.symbols.MethodSymbol;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;

public class ReferencesUpdaterAction extends SymbolAction {

    @Override
    public void doPush(Symbol<?> symbol, SymbolTable table) {
        SymbolDefinition def = symbol.getLocation();
        if (def != null) {
            int scope = table.getScopeLevel();
            def.setScopeLevel(scope);
        }
    }

    private void updateDefinitions(SymbolTable table, SymbolReference emitter) {
        Stack<Scope> defs = table.getScopes();
        Iterator<Scope> it = defs.iterator();
        while (it.hasNext()) {
            Scope scope = it.next();
            Symbol<?> rootSymbol = scope.getRootSymbol();
            if (rootSymbol != null) {
                SymbolDefinition sd = rootSymbol.getLocation();
                sd.addBodyReference(emitter);
            }
        }
    }

    @Override
    public void doRead(Symbol<?> symbol, SymbolTable table, SymbolReference emitter) {
        SymbolDefinition def = symbol.getLocation();

        if (def != null) {
            boolean addUsage = true;
            ReferenceType refType = symbol.getReferenceType();
            if (refType.equals(ReferenceType.METHOD)) {
                if (symbol.isStaticallyImported()) {
                    if (emitter instanceof MethodCallExpr) {
                        MethodCallExpr mce = (MethodCallExpr) emitter;
                        addUsage = mce.getScope() == null;
                    }
                }
            } else if (refType.equals(ReferenceType.VARIABLE)) {
                if (symbol.isStaticallyImported()) {
                    if (emitter instanceof FieldAccessExpr) {
                        FieldAccessExpr mce = (FieldAccessExpr) emitter;
                        addUsage = mce.getScope() == null;
                    }
                }
            } else if (refType.equals(ReferenceType.TYPE)) {
                if (def instanceof TypeDeclaration) {
                    Scope baseScope = table.getScopes().get(0);
                    Symbol<?> importSymbol = baseScope.findSymbol(symbol.getName(), ReferenceType.TYPE);
                    if (importSymbol != null) {
                        SymbolDefinition auxLocation = importSymbol.getLocation();
                        if (auxLocation != null) {
                            auxLocation.addUsage(emitter);
                        }
                    }
                }
            }
            if (addUsage) {
                def.addUsage(emitter);
            }
        }
        updateDefinitions(table, emitter);
    }

    @Override
    public void doWrite(Symbol<?> symbol, SymbolTable table, SymbolReference emitter) {
        SymbolDefinition def = symbol.getLocation();
        if (def != null) {
            def.addUsage(emitter);
        }
        updateDefinitions(table, emitter);
    }

}
