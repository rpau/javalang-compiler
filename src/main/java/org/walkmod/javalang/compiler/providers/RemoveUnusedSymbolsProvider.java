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
package org.walkmod.javalang.compiler.providers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.AnnotationMemberDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.EmptyTypeDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.MultiTypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.actions.ReferencesCounterAction;
import org.walkmod.javalang.compiler.actions.RemoveUnusedSymbolsAction;
import org.walkmod.javalang.compiler.symbols.SymbolAction;

public class RemoveUnusedSymbolsProvider implements SymbolActionProvider {

    private Stack<List<? extends Node>> siblings = new Stack<List<? extends Node>>();

    private List<SymbolAction> actions = Collections.emptyList();

    @Override
    public List<SymbolAction> getActions(ClassOrInterfaceDeclaration n) {
        buildActionList(n, n.getMembers());
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(Parameter n) {

        return Collections.emptyList();
    }

    @Override
    public List<SymbolAction> getActions(ClassOrInterfaceType n) {

        return Collections.emptyList();
    }

    @Override
    public List<SymbolAction> getActions(MultiTypeParameter n) {

        return Collections.emptyList();
    }

    @Override
    public List<SymbolAction> getActions(VariableDeclarator n) {
        buildActionList();
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(ImportDeclaration n) {
        buildActionList();
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(MethodDeclaration n) {
        buildActionList(n, new LinkedList<Node>());
        return actions;
    }

    private void buildActionList(Node n, List<? extends Node> children) {

        List<? extends Node> lastsiblings = siblings.peek();
        while (!lastsiblings.contains(n) && !siblings.isEmpty()) {
            siblings.pop();
            lastsiblings = siblings.peek();
        }

        RemoveUnusedSymbolsAction unusedSymbols = new RemoveUnusedSymbolsAction(siblings.peek());

        actions = new LinkedList<SymbolAction>();
        actions.add(unusedSymbols);
        siblings.push(children);

    }

    private void buildActionList() {
        RemoveUnusedSymbolsAction unusedSymbols = new RemoveUnusedSymbolsAction(siblings.peek());
        actions = new LinkedList<SymbolAction>();
        actions.add(unusedSymbols);
    }

    @Override
    public List<SymbolAction> getActions(FieldDeclaration n) {
        buildActionList(n, n.getVariables());
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(EnumDeclaration n) {

        buildActionList(n, n.getMembers());

        return actions;
    }

    @Override
    public List<SymbolAction> getActions(EnumConstantDeclaration n) {
        buildActionList();
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(AnnotationDeclaration n) {

        buildActionList(n, n.getMembers());

        return actions;

    }

    @Override
    public List<SymbolAction> getActions(AnnotationMemberDeclaration n) {
        buildActionList();
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(ConstructorDeclaration n) {
        buildActionList();
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(CompilationUnit n) {
        siblings.clear();
        ReferencesCounterAction counter = new ReferencesCounterAction();
        List<SymbolAction> actions = new LinkedList<SymbolAction>();
        actions.add(counter);

        siblings.push(n.getTypes());
        if (n.getImports() != null) {
            siblings.push(n.getImports());
        }
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(EmptyTypeDeclaration n) {
        buildActionList(n, n.getMembers());

        return Collections.emptyList();
    }

    @Override
    public List<SymbolAction> getActions(TypeDeclaration n) {
        buildActionList(n, n.getMembers());
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(BlockStmt n) {
        RemoveUnusedSymbolsAction unusedSymbols = new RemoveUnusedSymbolsAction(siblings.peek());
        actions = new LinkedList<SymbolAction>();
        actions.add(unusedSymbols);
        siblings.push(n.getStmts());
        return Collections.emptyList();
    }

    @Override
    public List<SymbolAction> getActions(VariableDeclarationExpr n) {
        RemoveUnusedSymbolsAction unusedSymbols = new RemoveUnusedSymbolsAction(siblings.peek());
        actions = new LinkedList<SymbolAction>();
        actions.add(unusedSymbols);
        siblings.push(n.getVars());
        return actions;
    }

    @Override
    public List<SymbolAction> getActions(ObjectCreationExpr n) {
        RemoveUnusedSymbolsAction unusedSymbols = new RemoveUnusedSymbolsAction(siblings.peek());
        actions = new LinkedList<SymbolAction>();
        actions.add(unusedSymbols);
        siblings.push(n.getAnonymousClassBody());
        return actions;
    }

}
