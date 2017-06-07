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
package org.walkmod.javalang.compiler.types;

import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.compiler.actions.LoadEnumConstantLiteralsAction;
import org.walkmod.javalang.compiler.actions.LoadFieldDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadMethodDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeDeclarationsAction;
import org.walkmod.javalang.compiler.actions.LoadTypeParamsAction;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.GenericVisitorAdapter;

public class ScopeLoader extends GenericVisitorAdapter<Scope, SymbolTable> {

    private TypesLoaderVisitor<?> typeTable = null;
    private TypeVisitorAdapter<?> expressionTypeAnalyzer = null;
    private SymbolActionProvider actionProvider = null;

    public ScopeLoader(TypesLoaderVisitor<?> typeTable, TypeVisitorAdapter<?> expressionTypeAnalyzer,
            SymbolActionProvider actionProvider) {
        this.typeTable = typeTable;
        this.expressionTypeAnalyzer = expressionTypeAnalyzer;
        this.actionProvider = actionProvider;
    }

    private Scope process(TypeDeclaration declaration, SymbolTable symbolTable) {
        Symbol<?> sym = symbolTable.findSymbol(declaration.getName(), ReferenceType.TYPE);
        Scope scope;
        if (sym == null) {
            // it is a type declaration inside a TypeStmt
            sym = symbolTable.pushSymbol(declaration.getName(), ReferenceType.TYPE,
                    (SymbolType) declaration.getSymbolData(), declaration);
            declaration.setSymbolData(sym.getType());
            scope = new Scope(sym);
            sym.setInnerScope(scope);
        } else {
            scope = sym.getInnerScope();
        }
        symbolTable.pushScope(scope);

        List<SymbolAction> actions = new LinkedList<SymbolAction>();
        actions.add(new LoadTypeParamsAction());
        actions.add(new LoadTypeDeclarationsAction(typeTable));
        actions.add(new LoadFieldDeclarationsAction(actionProvider));
        actions.add(new LoadEnumConstantLiteralsAction());
        actions.add(new LoadMethodDeclarationsAction(actionProvider, expressionTypeAnalyzer));

        if (declaration instanceof ClassOrInterfaceDeclaration) {
            if (!((ClassOrInterfaceDeclaration) declaration).isInterface()) {
                Symbol superSymbol = symbolTable.pushSymbol("super", ReferenceType.VARIABLE,
                        new SymbolType(sym.getType().getClazz().getSuperclass()), declaration,
                        (List<SymbolAction>) null);
                Symbol<?> superType =
                        symbolTable.findSymbol(superSymbol.getType().getClazz().getCanonicalName(), ReferenceType.TYPE);
                if (superType != null) {
                    superSymbol.setInnerScope(superType.getInnerScope());
                }
            } else {
                // Java 8: it references to itself because is could be
                // referenced to call default methods
                Symbol<TypeDeclaration> superSymbol =
                        new Symbol<TypeDeclaration>("super", sym.getType(), null, ReferenceType.VARIABLE, false, null);
                symbolTable.pushSymbol(superSymbol);
            }
        }
        Symbol<TypeDeclaration> thisSymbol =
                new Symbol<TypeDeclaration>("this", sym.getType(), declaration, ReferenceType.VARIABLE, false, actions);
        thisSymbol.setInnerScope(sym.getInnerScope());
        // The preliminary "this" is replaced here.
        // The preliminary "this" is set on scope creation to allow us to do depth first
        // resolution of "extends" axis of thisSymbol here.
        symbolTable.pushSymbol(thisSymbol, true);
        List<BodyDeclaration> members = declaration.getMembers();
        if (members != null) {
            for (BodyDeclaration member : members) {
                if (member instanceof TypeDeclaration) {
                    member.accept(this, symbolTable);
                }
            }
        }

        symbolTable.popScope(true);
        return sym.getInnerScope();
    }

    @Override
    public Scope visit(ClassOrInterfaceDeclaration n, SymbolTable symbolTable) {

        return process(n, symbolTable);

    }

    @Override
    public Scope visit(EnumDeclaration n, SymbolTable symbolTable) {
        return process(n, symbolTable);
    }

    @Override
    public Scope visit(AnnotationDeclaration n, SymbolTable symbolTable) {
        return process(n, symbolTable);
    }

    @Override
    public Scope visit(ObjectCreationExpr n, SymbolTable symbolTable) {
        List<BodyDeclaration> body = n.getAnonymousClassBody();
        if (body != null) {

            SymbolType st = ASTSymbolTypeResolver.getInstance().valueOf(n.getType());

            Scope scope = new Scope();
            symbolTable.pushScope(scope);
            List<BodyDeclaration> members = n.getAnonymousClassBody();
            boolean anonymousClass = members != null;
            if (anonymousClass) {

                List<SymbolAction> actions = new LinkedList<SymbolAction>();
                actions.add(new LoadTypeParamsAction());
                actions.add(new LoadTypeDeclarationsAction(typeTable));
                actions.add(new LoadFieldDeclarationsAction(actionProvider));
                actions.add(new LoadMethodDeclarationsAction(actionProvider, expressionTypeAnalyzer));

                actions.add(new LoadEnumConstantLiteralsAction());

                if (actionProvider != null) {
                    actions.addAll(actionProvider.getActions(n));
                }

                Symbol<?> superSymbol =
                        symbolTable.pushSymbol("super", ReferenceType.VARIABLE, st, n, (List<SymbolAction>) null);

                if (st == null) {
                    throw new RuntimeException("Error resolving " + n.getType().toString() + " in " + n.toString()
                            + ", line: " + n.getBeginLine());
                }
                Class<?> superTypeClass = st.getClazz();
                Symbol<?> superType = symbolTable.findSymbol(superTypeClass.getCanonicalName(), ReferenceType.TYPE);
                if (superType != null) {
                    superSymbol.setInnerScope(superType.getInnerScope());
                }
                String name = symbolTable.generateAnonymousClass();

                SymbolType type = SymbolType.anonymousClassOf(name);
                try {
                    type.getClazz();
                } catch (TypeNotFoundException e) {
                    // The java compiler does not generate classes for code disabled via conditional compilation.
                    // For definition of conditional compilation see
                    // JLS 14.21. Unreachable Statements,
                    // http://docs.oracle.com/javase/specs/jls/se8/html/jls-14.html#jls-14.21
                    if (ConditionalCompilationUtil.isDisabledCode(n)) {
                        // if we have symbol data of base type we keep that.
                        final SymbolType base = (SymbolType) n.getSymbolData();
                        type = base != null ? base.markDisabledCode() : new SymbolType(Object.class).markDisabledCode();
                    } else {
                        throw e;
                    }
                }
                Symbol<?> anonymousType = symbolTable.pushSymbol(name, ReferenceType.TYPE, type, n);
                anonymousType.setInnerScope(scope);

                Symbol<ObjectCreationExpr> thisSymbol =
                        new Symbol<ObjectCreationExpr>("this", type, n, ReferenceType.VARIABLE, false, actions);
                scope.setRootSymbol(thisSymbol);
                thisSymbol.setInnerScope(scope);

                symbolTable.pushSymbol(thisSymbol);
                for (BodyDeclaration member : members) {
                    if (member instanceof TypeDeclaration) {
                        process((TypeDeclaration) member, symbolTable);
                    }
                }

            }
            symbolTable.popScope(true);

            return scope;
        }
        return null;
    }

    @Override
    public Scope visit(EnumConstantDeclaration n, SymbolTable symbolTable) {
        final Symbol<?> s = symbolTable.findSymbol(n.getName(), ReferenceType.ENUM_LITERAL);
        s.setInnerScope(new Scope(s));
        symbolTable.pushScope(s.getInnerScope());

        SymbolType parentType = symbolTable.getType("this", ReferenceType.VARIABLE);

        List<SymbolAction> actions = new LinkedList<SymbolAction>();
        actions.add(new LoadTypeParamsAction());
        actions.add(new LoadTypeDeclarationsAction(typeTable));
        actions.add(new LoadFieldDeclarationsAction(actionProvider));
        actions.add(new LoadMethodDeclarationsAction(actionProvider, expressionTypeAnalyzer));

        if (actionProvider != null) {
            actions.addAll(actionProvider.getActions(n));
        }
        symbolTable.pushSymbol("super", ReferenceType.VARIABLE, parentType, n.getParentNode(),
                (List<SymbolAction>) null);

        String name = symbolTable.generateAnonymousClass();
        final SymbolType type = SymbolType.enumConstantOf(name);
        symbolTable.pushSymbol(name, ReferenceType.TYPE, type, n, actions);

        symbolTable.pushSymbol("this", ReferenceType.VARIABLE, type, n, actions);
        n.setSymbolData(type);

        symbolTable.popScope(true);
        return s.getInnerScope();
    }

}
