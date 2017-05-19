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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EmptyTypeDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.exceptions.InvalidTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class LoadTypeDeclarationsAction extends SymbolAction {

    private TypesLoaderVisitor typeTable;

    public LoadTypeDeclarationsAction(TypesLoaderVisitor typeTable) {
        this.typeTable = typeTable;
    }

    @Override
    public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
        Node node = symbol.getLocation();

        if (node instanceof TypeDeclaration || node instanceof ObjectCreationExpr) {
            if (symbol.getName().equals("this")) {
                LoadInheritedNestedClasses<?> vis = new LoadInheritedNestedClasses<Object>(table);
                node.accept(vis, null);
            }
        }

    }

    private class LoadInheritedNestedClasses<A> extends VoidVisitorAdapter<A> {

        private SymbolTable symbolTable;

        public LoadInheritedNestedClasses(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, A ctx) {
            loadExtendsOrImplements(n.getExtends());
            loadExtendsOrImplements(n.getImplements());
            n.accept(typeTable, null);

        }

        @Override
        public void visit(AnnotationDeclaration n, A ctx) {
            n.accept(typeTable, null);
        }

        @Override
        public void visit(EnumDeclaration n, A ctx) {
            n.accept(typeTable, null);
            loadExtendsOrImplements(n.getImplements());
        }

        @Override
        public void visit(EmptyTypeDeclaration n, A ctx) {
            n.accept(typeTable, null);
        }

        @Override
        public void visit(ObjectCreationExpr n, A ctx) {
            n.accept(typeTable, null);
            ClassOrInterfaceType type = n.getType();
            List<ClassOrInterfaceType> extendsList = new LinkedList<ClassOrInterfaceType>();
            extendsList.add(type);
            loadExtendsOrImplements(extendsList);
        }

        private void loadExtendsOrImplements(List<ClassOrInterfaceType> extendsList) {
            if (extendsList != null) {
                for (ClassOrInterfaceType type : extendsList) {
                    String name = type.getName();
                    ClassOrInterfaceType scope = type.getScope();
                    if (scope != null) {
                        name = scope.toString() + "." + name;
                    }
                    Symbol<?> s = symbolTable.findSymbol(name, ReferenceType.TYPE, ReferenceType.TYPE_PARAM);
                    if (s != null) {
                        Object location = s.getLocation();
                        if (location != null && location instanceof TypeDeclaration) {

                            ((TypeDeclaration) location).accept(this, null);

                        } else {
                            Class<?> clazz = s.getType().getClazz();
                            Set<Class<?>> innerClasses = ClassInspector.getNonPrivateClassMembers(clazz);
                            innerClasses.remove(clazz);
                            for (Class<?> innerClass : innerClasses) {
                                try {
                                    Symbol<?> aux =
                                            symbolTable.findSymbol(innerClass.getSimpleName(), ReferenceType.TYPE);
                                    boolean add = aux == null;
                                    if (!add) {
                                        Node oldLoc = aux.getLocation();
                                        add = (oldLoc == null || !(oldLoc instanceof TypeDeclaration
                                                || oldLoc instanceof ImportDeclaration));
                                    }

                                    if (add) {

                                        symbolTable.pushSymbol(innerClass.getSimpleName(), ReferenceType.TYPE,
                                                SymbolType.valueOf(innerClass, null), null);

                                    }
                                } catch (InvalidTypeException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
