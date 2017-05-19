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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.ast.SymbolDefinition;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.stmt.TypeDeclarationStmt;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.exceptions.SymbolTableException;

public class SymbolTable {

    private Stack<Scope> indexStructure;

    private SymbolFactory symbolFactory = null;

    private List<SymbolAction> actions;

    public SymbolTable() {
        indexStructure = new Stack<Scope>();
        setSymbolFactory(new DefaultSymbolFactory());
    }

    public void setActions(List<SymbolAction> actions) {
        this.actions = actions;
    }

    public Stack<Scope> getScopes() {
        return indexStructure;
    }

    public void setSymbolFactory(SymbolFactory symbolFactory) {
        this.symbolFactory = symbolFactory;
    }

    public SymbolType getType(String symbolName, ReferenceType... referenceType) {
        Symbol<?> s = findSymbol(symbolName, referenceType);
        if (s != null) {
            return s.getType();
        }
        return null;
    }

    public Symbol<?> findSymbol(String symbolName, ReferenceType... referenceType) {
        return findSymbol(symbolName, null, null, referenceType);
    }

    public Symbol<?> findSymbol(String symbolName, SymbolType symbolScope, SymbolType[] args,
            ReferenceType... referenceType) {
        return findSymbol(symbolName, symbolScope, args, null, referenceType);
    }

    public Symbol<?> findSymbol(String symbolName, SymbolType symbolScope, SymbolType[] args,
            List<Predicate<?>> predicates, ReferenceType... referenceType) {
        int j = indexStructure.size() - 1;
        Symbol<?> result = null;
        if (symbolScope != null) {
            Class<?> clazz = symbolScope.getClazz();
            Symbol<?> scopeSymbol = null;
            if (clazz.isAnonymousClass()) {
                scopeSymbol = findSymbol(symbolScope.getClazz().getName(), ReferenceType.TYPE);
            } else {
                scopeSymbol =
                        indexStructure.get(0).findSymbol(symbolScope.getClazz().getCanonicalName(), ReferenceType.TYPE);
            }
            if (scopeSymbol != null) {
                if (scopeSymbol.getInnerScope() != null) {
                    // it is an inner class
                    return scopeSymbol.getInnerScope().findSymbol(symbolName, symbolScope, args, predicates,
                            referenceType);
                } else
                    return null;
            }

        }

        Scope currentTypeScope = null;
        SymbolType auxScope = null;

        while (j >= 0 && result == null) {
            Scope scope = indexStructure.get(j);
            Symbol<?> rootSymbol = scope.getRootSymbol();

            if (symbolScope == null && rootSymbol != null) {
                SymbolDefinition sd = rootSymbol.getLocation();
                if (sd instanceof SymbolDataAware<?>) {
                    auxScope = (SymbolType) ((SymbolDataAware<?>) sd).getSymbolData();
                    if (currentTypeScope == null && !(rootSymbol instanceof MethodSymbol)) {
                        currentTypeScope = scope;
                    }
                }

            }
            if (auxScope == null) {
                auxScope = symbolScope;
            }

            SymbolType typeRootSymbol = null;
            if (rootSymbol != null && !(rootSymbol instanceof MethodSymbol)) {
                typeRootSymbol = rootSymbol.getType();
            }
            if (typeRootSymbol != null && symbolScope != null) {

                if (typeRootSymbol.isCompatible(symbolScope)) {
                    result = scope.findSymbol(symbolName, true, auxScope, args, predicates, referenceType);
                }
            } else {
                result = scope.findSymbol(symbolName, true, auxScope, args, predicates, referenceType);
            }

            j--;
        }

        if (currentTypeScope != null && result == null) {
            if (symbolScope == null || auxScope.getName().equals(symbolScope.getName())) {
                result = currentTypeScope.findSymbol(symbolName, false, symbolScope, args, predicates, referenceType);
            }
        }

        return result;
    }

    public List<Symbol<?>> findSymbolsByType(String typeName, ReferenceType referenceType) {
        List<Symbol<?>> result = new LinkedList<Symbol<?>>();
        int i = indexStructure.size() - 1;
        while (i >= 0) {
            Scope scope = indexStructure.get(i);
            result.addAll(scope.getSymbolsByType(typeName, referenceType));
            i--;
        }
        return result;
    }

    public ArrayList<Symbol<?>> findSymbolsByType(ReferenceType... referenceType) {
        ArrayList<Symbol<?>> result = new ArrayList<Symbol<?>>();
        int i = indexStructure.size() - 1;
        while (i >= 0) {
            Scope scope = indexStructure.get(i);
            result.addAll(scope.getSymbolsByType(referenceType));
            i--;
        }
        return result;
    }

    public List<Symbol<?>> findSymbolsByLocation(Node node) {
        List<Symbol<?>> result = new LinkedList<Symbol<?>>();
        int i = indexStructure.size() - 1;
        while (i >= 0) {
            Scope scope = indexStructure.get(i);
            result.addAll(scope.getSymbolsByLocation(node));
            i--;
        }
        return result;
    }

    private void invokeActions(Scope scope, Symbol<?> s, SymbolEvent event, SymbolReference reference)
            throws Exception {
        if (s != null) {
            if (actions != null) {
                for (SymbolAction action : actions) {
                    action.execute(s, this, event, reference);
                }
            }

            if (scope != null) {
                List<SymbolAction> scopeActions = scope.getActions();
                if (scopeActions != null) {
                    for (SymbolAction action : scopeActions) {
                        action.execute(s, this, event, reference);
                    }
                }
            }
            s.invokeActions(this, event, reference);
        }
    }

    public Symbol<?> lookUpSymbolForRead(String symbolName, SymbolReference reference, ReferenceType... referenceType) {
        return lookUpSymbolForRead(symbolName, reference, null, null, referenceType);
    }

    public Symbol<?> lookUpSymbolForRead(String symbolName, SymbolReference reference, SymbolType symbolScope,
            SymbolType[] args, ReferenceType... referenceType) {
        Symbol<?> s = findSymbol(symbolName, symbolScope, args, referenceType);
        if (s != null) {
            try {
                invokeActions(indexStructure.peek(), s, SymbolEvent.READ, reference);
            } catch (Exception e) {
                throw new SymbolTableException(e);
            }
        }
        return s;
    }

    public Symbol<?> lookUpSymbolForWrite(String symbolName, SymbolReference reference) {
        return lookUpSymbolForWrite(symbolName, reference, null, null);
    }

    public Map<String, SymbolType> getTypeParams() {
        Map<String, SymbolType> result = new LinkedHashMap<String, SymbolType>();

        Iterator<Scope> it = indexStructure.iterator();
        while (it.hasNext()) {
            Scope scope = it.next();
            Map<String, SymbolType> tp = scope.getLocalTypeParams();
            if (tp != null) {
                result.putAll(tp);
            }
        }
        return result;
    }

    public Map<String, SymbolType> flat() {
        Map<String, SymbolType> result = new LinkedHashMap<String, SymbolType>();

        Iterator<Scope> it = indexStructure.iterator();
        while (it.hasNext()) {
            Scope scope = it.next();
            List<Symbol<?>> symbols = scope.getSymbols();
            for (Symbol<?> s : symbols) {
                result.put(s.getName(), s.getType());
            }
        }
        return result;
    }

    public Symbol<?> lookUpSymbolForWrite(String symbolName, SymbolReference reference, SymbolType symbolScope,
            SymbolType[] args, ReferenceType... referenceType) {

        if (referenceType == null || referenceType.length == 0) {
            referenceType = new ReferenceType[1];
            referenceType[0] = ReferenceType.VARIABLE;
        }
        Symbol<?> s = findSymbol(symbolName, symbolScope, args, referenceType);
        try {
            invokeActions(indexStructure.peek(), s, SymbolEvent.WRITE, reference);
        } catch (Exception e) {
            throw new SymbolTableException(e);
        }
        return s;
    }

    public boolean containsSymbol(String symbolName, ReferenceType referenceType) {
        SymbolType type = getType(symbolName, referenceType);
        return (type != null);
    }

    public int getScopeLevel() {
        if (indexStructure != null) {
            return indexStructure.size() - 1;
        }
        return 0;
    }

    public boolean pushSymbol(Symbol<?> symbol) {
        return pushSymbol(symbol, false);
    }

    public String generateAnonymousClass() {
        return getTypeAnonymousClassPreffix(null);

    }

    private String getTypeAnonymousClassPreffix(String name) {
        int j = indexStructure.size() - 2;
        String suffixName = null;

        // we upgrade the class counter in the closest inner class
        while (j > 0 && suffixName == null) {
            Scope sc = indexStructure.get(j);
            Symbol<?> rootSymbol = sc.getRootSymbol();
            if (rootSymbol != null) {
                SymbolDefinition sd = rootSymbol.getLocation();
                if (sd instanceof ObjectCreationExpr || sd instanceof TypeDeclaration
                        || sd instanceof TypeDeclarationStmt || sd instanceof EnumConstantDeclaration) {

                    int num = 1;
                    String preffix = ((SymbolDataAware<?>) sd).getSymbolData().getName();
                    if (name == null) {
                        sc.incrInnerAnonymousClassCounter();
                        num = sc.getInnerAnonymousClassCounter();
                    } else {
                        num = 1;

                        Symbol<?> aux =
                                indexStructure.get(0).findSymbol(preffix + "$" + num + name, ReferenceType.TYPE);

                        while (aux != null) {
                            num++;
                            aux = indexStructure.get(0).findSymbol(preffix + "$" + num + name, ReferenceType.TYPE);

                        }

                    }

                    suffixName = "$" + num;
                    return preffix + suffixName;
                }

            }
            j--;
        }
        return null;
    }

    public String getTypeStatementPreffix(String name) {
        return getTypeAnonymousClassPreffix(name);
    }

    public boolean pushSymbol(Symbol<?> symbol, boolean override) {
        Scope lastScope = indexStructure.peek();

        if (lastScope.addSymbol(symbol, override)) {
            try {
                invokeActions(lastScope, symbol, SymbolEvent.PUSH, null);
            } catch (Exception e) {
                throw new SymbolTableException(e);
            }
            return true;
        } else {
            return false;
        }

    }

    public Symbol<?> pushSymbol(String symbolName, ReferenceType referenceType, SymbolType symbolType, Node location) {
        return pushSymbol(symbolName, referenceType, symbolType, location, false);
    }

    public Symbol<?> pushSymbol(String symbolName, ReferenceType referenceType, SymbolType symbolType, Node location,
            boolean override) {
        Symbol<?> symbol = symbolFactory.create(symbolName, referenceType, symbolType, location);
        if (pushSymbol(symbol, override)) {
            return symbol;
        }
        return null;
    }

    public Symbol<?> pushSymbol(String symbolName, ReferenceType referenceType, SymbolType symbolType, Node location,
            SymbolAction action) {
        Symbol<?> symbol = symbolFactory.create(symbolName, referenceType, symbolType, location, action);
        if (pushSymbol(symbol, false)) {
            return symbol;
        }
        return null;
    }

    public Symbol<?> pushSymbol(String symbolName, ReferenceType referenceType, SymbolType symbolType, Node location,
            List<SymbolAction> actions) {
        return pushSymbol(symbolName, referenceType, symbolType, location, actions, false);
    }

    public Symbol<?> pushSymbol(String symbolName, ReferenceType referenceType, SymbolType symbolType, Node location,
            List<SymbolAction> actions, boolean override) {
        Symbol<?> symbol = symbolFactory.create(symbolName, referenceType, symbolType, location, actions);
        if (pushSymbol(symbol, override)) {
            return symbol;
        }
        return null;
    }

    public List<Symbol<?>> pushSymbols(Set<String> names, ReferenceType referenceType, SymbolType symbolType,
            Node location, List<SymbolAction> actions) {
        List<Symbol<?>> pushedSymbols = new LinkedList<Symbol<?>>();

        for (String name : names) {
            Symbol<?> s = pushSymbol(name, referenceType, symbolType, location, actions);
            if (s != null) {
                pushedSymbols.add(s);
            }
        }
        return pushedSymbols;
    }

    public Scope popScope() {
        return popScope(false);
    }

    public Scope popScope(boolean silent) {
        Scope scope = indexStructure.peek();
        if (!silent) {
            List<Symbol<?>> symbols = scope.getSymbols();
            for (Symbol<?> symbol : symbols) {
                try {
                    invokeActions(scope, symbol, SymbolEvent.POP, null);
                } catch (Exception e) {
                    throw new SymbolTableException(e);
                }
            }
        }
        return indexStructure.pop();
    }

    public void pushScope() {
        pushScope(null, null);
    }

    public void pushScope(Scope scope) {
        pushScope(scope, null);
    }

    public void addActionsToScope(List<SymbolAction> actions) {
        indexStructure.peek().addActions(actions);
    }

    public void pushScope(Scope scope, List<SymbolAction> actions) {
        if (scope == null) {
            scope = new Scope(actions);
        }
        indexStructure.push(scope);

    }

}
