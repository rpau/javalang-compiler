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

import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class LoadTypeParamsAction extends SymbolAction {

    @Override
    public void doPush(Symbol<?> symbol, SymbolTable table) {

        Node n = symbol.getLocation();
        if (n != null) {
            if (n instanceof ClassOrInterfaceDeclaration || n instanceof ObjectCreationExpr) {
                ProcessGenerics<?> gen = new ProcessGenerics<Object>(symbol, table);
                n.accept(gen, null);
            }
        }

    }

    private class ProcessGenerics<A> extends VoidVisitorAdapter<A> {

        private Symbol<?> symbol;

        private SymbolTable table;

        public ProcessGenerics(Symbol<?> symbol, SymbolTable table) {
            this.symbol = symbol;
            this.table = table;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration declaration, A ctx) {
            List<TypeParameter> typeParams = declaration.getTypeParameters();
            SymbolType thisType = symbol.getType();

            load(table, typeParams, thisType);

            if (!declaration.isInterface()) {
                List<ClassOrInterfaceType> extendsList = declaration.getExtends();
                if (extendsList != null && !extendsList.isEmpty()) {
                    processSuperGenerics(table, symbol, extendsList.get(0));
                }
            }
        }

        @Override
        public void visit(ObjectCreationExpr n, A ctx) {
            ClassOrInterfaceType superType = n.getType();
            processSuperGenerics(table, symbol, superType);

        }

        private void processSuperGenerics(SymbolTable table, Symbol<?> symbol, ClassOrInterfaceType type) {
            if (type != null) {

                Map<String, SymbolType> typeResolution = new HashMap<String, SymbolType>();

                ASTSymbolTypeResolver res = new ASTSymbolTypeResolver(typeResolution, table);
                SymbolType superType = type.accept(res, null);

                List<SymbolType> params = superType.getParameterizedTypes();
                Map<String, SymbolType> typeMapping = table.getTypeParams();
                if (params != null) {
                    Symbol<?> superSymbol = symbol.getInnerScope().findSymbol("super");
                    Scope innerScope = superSymbol.getInnerScope();

                    Scope aux = null;

                    if (innerScope != null) {
                        aux = new Scope(superSymbol);

                    } else {
                        aux = new Scope();
                    }
                    superSymbol.setInnerScope(aux);

                    table.pushScope(aux);

                    Symbol<?> intermediateSuper = new Symbol("super", superSymbol.getType(), superSymbol.getLocation());

                    if (innerScope != null) {
                        intermediateSuper.setInnerScope(innerScope);
                    }

                    table.pushSymbol(intermediateSuper);

                    // extends a parameterizable type
                    TypeVariable<?>[] tps = superType.getClazz().getTypeParameters();
                    for (int i = 0; i < tps.length; i++) {

                        table.pushSymbol(tps[i].getName(), ReferenceType.TYPE_PARAM, params.get(i), null);

                    }
                    table.popScope(true);

                }
                Set<String> genericLetters = typeMapping.keySet();
                if (genericLetters != null) {
                    for (String letter : genericLetters) {

                        if (typeResolution.containsKey(letter)) {
                            table.pushSymbol(letter, ReferenceType.TYPE, typeMapping.get(letter), null);
                        }
                    }
                }
            }

        }
    }

    public void load(SymbolTable table, List<TypeParameter> typeParams, SymbolType thisType) {
        if (typeParams != null && !typeParams.isEmpty()) {

            List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();
            for (TypeParameter tp : typeParams) {
                List<ClassOrInterfaceType> typeBounds = tp.getTypeBound();
                List<SymbolType> bounds = new LinkedList<SymbolType>();
                SymbolType st = null;
                if (typeBounds != null) {
                    for (ClassOrInterfaceType type : typeBounds) {
                        SymbolType paramType = ASTSymbolTypeResolver.getInstance().valueOf(type);
                        if (paramType == null) {
                            paramType = new SymbolType(Object.class);
                        }
                        bounds.add(paramType);
                    }
                    st = SymbolType.typeVariableOf(tp.getName(), bounds);
                } else {
                    st = SymbolType.typeVariableOf(tp.getName(), Object.class);
                }
                table.pushSymbol(tp.getName(), ReferenceType.TYPE_PARAM, st, tp);

                parameterizedTypes.add(st);
            }
            Map<String, SymbolType> typeParamsMap = table.getTypeParams();

            for (String key : typeParamsMap.keySet()) {
                SymbolType st = typeParamsMap.get(key);
                recursiveTemplateSubstitution(st, typeParamsMap);
            }

            if (thisType != null && !parameterizedTypes.isEmpty()) {
                thisType.setParameterizedTypes(parameterizedTypes);
            }
        }

    }

    public void recursiveTemplateSubstitution(SymbolType st, Map<String, SymbolType> typeParamsMap) {
        List<SymbolType> bounds = st.getBounds();
        if (bounds != null) {
            for (SymbolType bound : bounds) {
                recursiveTemplateSubstitution(bound, typeParamsMap);
            }
        } else {
            List<SymbolType> params = st.getParameterizedTypes();
            if (params != null) {
                List<SymbolType> paramsFinal = new LinkedList<SymbolType>();
                for (SymbolType param : params) {
                    String tv = param.getTemplateVariable();
                    if (tv != null) {
                        SymbolType genericTypeDef = typeParamsMap.get(tv);
                        if (genericTypeDef != null) {
                            paramsFinal.add(genericTypeDef);
                        } else {
                            recursiveTemplateSubstitution(param, typeParamsMap);
                            paramsFinal.add(param);
                        }
                    } else {
                        recursiveTemplateSubstitution(param, typeParamsMap);
                        paramsFinal.add(param);
                    }
                }
                st.setParameterizedTypes(paramsFinal);
            }

        }

    }
}
