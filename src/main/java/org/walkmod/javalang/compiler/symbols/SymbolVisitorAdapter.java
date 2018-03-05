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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.JavadocManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.PackageDeclaration;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.AnnotationMemberDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.JavadocComment;
import org.walkmod.javalang.ast.body.JavadocTag;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.MultiTypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.body.VariableDeclaratorId;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.NormalAnnotationExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.SingleMemberAnnotationExpr;
import org.walkmod.javalang.ast.expr.SuperExpr;
import org.walkmod.javalang.ast.expr.ThisExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.AssertStmt;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.CatchClause;
import org.walkmod.javalang.ast.stmt.DoStmt;
import org.walkmod.javalang.ast.stmt.ExplicitConstructorInvocationStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.ForStmt;
import org.walkmod.javalang.ast.stmt.ForeachStmt;
import org.walkmod.javalang.ast.stmt.IfStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.stmt.SwitchEntryStmt;
import org.walkmod.javalang.ast.stmt.SwitchStmt;
import org.walkmod.javalang.ast.stmt.SynchronizedStmt;
import org.walkmod.javalang.ast.stmt.ThrowStmt;
import org.walkmod.javalang.ast.stmt.TryStmt;
import org.walkmod.javalang.ast.stmt.TypeDeclarationStmt;
import org.walkmod.javalang.ast.stmt.WhileStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.VoidType;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.compiler.actions.LoadTypeParamsAction;
import org.walkmod.javalang.compiler.actions.ReferencesUpdaterAction;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.providers.SymbolActionProviderAware;
import org.walkmod.javalang.compiler.types.ScopeLoader;
import org.walkmod.javalang.compiler.types.TypeVisitorAdapter;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

/**
 * Resolve all the symbols and store them into the symbol table.
 */
public class SymbolVisitorAdapter<A extends Map<String, Object>> extends VoidVisitorAdapter<A>
        implements SymbolActionProviderAware {

    private SymbolTable symbolTable;

    private ClassLoader classLoader;

    private TypesLoaderVisitor<?> typeTable;

    private TypeVisitorAdapter<A> expressionTypeAnalyzer;

    private List<SymbolAction> actions = null;

    private SymbolActionProvider actionProvider = null;

    private ASTSymbolTypeResolver symbolResolver = null;

    public static String VISITOR_SCOPE_PROCESSOR = "_visitor_scope_processor";

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setSymbolActions(List<SymbolAction> actions) {
        this.actions = actions;
    }

    public void setActionProvider(SymbolActionProvider actionProvider) {
        this.actionProvider = actionProvider;
    }

    public void setExpressionTypeAnalyzer(TypeVisitorAdapter<A> expressionTypeAnalyzer) {
        this.expressionTypeAnalyzer = expressionTypeAnalyzer;
    }

    @Override
    public void visit(CompilationUnit unit, A arg) {
        if (actionProvider != null) {
            actions = actionProvider.getActions(unit);
        } else {
            if (actions == null) {
                actions = new LinkedList<SymbolAction>();
            }
            actions.add(new ReferencesUpdaterAction());
        }

        symbolTable = new SymbolTable();
        symbolTable.setActions(actions);
        symbolTable.pushScope();
        typeTable = new TypesLoaderVisitor<A>(symbolTable, actionProvider, actions);
        symbolResolver = ASTSymbolTypeResolver.getInstance();
        symbolResolver.setSymbolTable(symbolTable);
        typeTable.clear();
        typeTable.setClassLoader(classLoader);

        typeTable.visit(unit, null);
        expressionTypeAnalyzer = new TypeVisitorAdapter<A>(symbolTable, this);
        ScopeLoader scopeLoader = new ScopeLoader(typeTable, expressionTypeAnalyzer, actionProvider);
        PackageDeclaration pkg = unit.getPackage();
        if (pkg != null) {
            pkg.accept(this, arg);
        }
        if (unit.getTypes() != null) {

            for (TypeDeclaration typeDeclaration : unit.getTypes()) {
                //System.out.println("type: "+typeDeclaration.getName());
                typeDeclaration.accept(scopeLoader, symbolTable);
            }

            for (TypeDeclaration typeDeclaration : unit.getTypes()) {
                typeDeclaration.accept(this, arg);
            }
        }

        symbolTable.popScope();
    }

    public void visit(NormalAnnotationExpr n, A arg) {
        String type = n.getName().toString();
        Symbol<?> s = symbolTable.lookUpSymbolForRead(type, n, ReferenceType.TYPE);
        SymbolData sd = null;
        if (s == null) {
            // it is a full name and thus, it is not imported
            sd = new SymbolType(type);
        } else {
            sd = s.getType();
        }
        n.setSymbolData(sd);
        super.visit(n, arg);
    }

    public void visit(MarkerAnnotationExpr n, A arg) {
        String type = n.getName().toString();
        Symbol<?> s = symbolTable.lookUpSymbolForRead(type, n, ReferenceType.TYPE);
        SymbolData sd = null;
        if (s == null) {
            // it is a full name and thus, it is not imported
            sd = new SymbolType(type);
        } else {
            sd = s.getType();
        }
        n.setSymbolData(sd);
        super.visit(n, arg);
    }

    public void visit(SingleMemberAnnotationExpr n, A arg) {
        String typeName = n.getName().toString();
        Symbol<?> s = symbolTable.lookUpSymbolForRead(typeName, n, ReferenceType.TYPE);
        if (s != null) {
            n.setSymbolData(s.getType());
        } else {
            SymbolType st = new SymbolType(typeName);
            n.setSymbolData(st);
        }

        super.visit(n, arg);
    }

    private void processJavadocTypeReference(String type, JavadocTag n) {
        if (type != null) {
            String[] split = type.split("#");
            String typeName = split[0];
            if (!"".equals(typeName)) {
                symbolTable.lookUpSymbolForRead(typeName, n, ReferenceType.TYPE);
            }
            if (split.length == 2) {
                String signature = split[1];
                int start = signature.indexOf("(");
                if (start > 0 && signature.endsWith(")")) {
                    signature = signature.substring(start + 1, signature.length() - 1);
                    String[] params = signature.split(",");
                    if (params != null) {
                        for (String param : params) {
                            if (!"".equals(param)) {
                                if (param.endsWith("[]")) {
                                    param = param.substring(0, param.length() - 2);
                                }
                                symbolTable.lookUpSymbolForRead(param.trim(), n, ReferenceType.TYPE);
                            }
                        }
                    }
                }
            }
        }
    }

    public void visit(JavadocComment n, A arg) {
        List<JavadocTag> tags = null;
        try {
            tags = JavadocManager.parse(n.getContent());
        } catch (Exception e) {
            // nothing, javadoc can be bad writen and the code compiles
        }
        if (tags != null) {
            for (JavadocTag tag : tags) {
                String name = tag.getName();
                if ("@link".equals(name) || "@linkplain".equals(name) || "@throws".equals(name)) {
                    List<String> values = tag.getValues();
                    if (values != null) {
                        String type = values.get(0);
                        processJavadocTypeReference(type, tag);

                    }
                } else if ("@see".equals(name)) {
                    List<String> values = tag.getValues();
                    if (values != null) {
                        String type = values.get(0);
                        if (type != null && !type.startsWith("<") && !type.startsWith("\"")) {
                            processJavadocTypeReference(type, tag);

                        }
                    }
                }
            }
        }
    }

    public void visit(EnumDeclaration n, A arg) {

        pushScope(n);
        super.visit(n, arg);
        symbolTable.popScope();

    }

    private void loadThisSymbol(ObjectCreationExpr n, A arg) {
        if (AnonymousClassUtil.isAnonymousClass(n) && AnonymousClassUtil.needsSymbolData(n)) {
            ScopeLoader scopeLoader = new ScopeLoader(typeTable, expressionTypeAnalyzer, actionProvider);
            Scope scope = n.accept(scopeLoader, symbolTable);
            if (scope != null) {
                symbolTable.pushScope(scope);
            }
            if (n.getAnonymousClassBody() != null) {
                for (BodyDeclaration member : n.getAnonymousClassBody()) {
                    member.accept(this, arg);
                }
            }
            if (scope != null) {
                symbolTable.popScope();
            }
        }

    }

    private void loadThisSymbol(EnumConstantDeclaration n, A arg) {
        ScopeLoader scopeLoader = new ScopeLoader(typeTable, expressionTypeAnalyzer, actionProvider);
        Scope scope = n.accept(scopeLoader, symbolTable);
        if (scope != null) {
            symbolTable.pushScope(scope);
        }

        for (BodyDeclaration member : n.getClassBody()) {
            member.accept(this, arg);
        }
        symbolTable.popScope();
    }

    @Override
    public void visit(TypeDeclarationStmt n, A arg) {
        if (n.getSymbolData() == null) {
            String name = n.getTypeDeclaration().getName();
            SymbolType st = new SymbolType(symbolTable.getTypeStatementPreffix(name) + name);
            Symbol<?> s = symbolTable.pushSymbol(n.getTypeDeclaration().getName(), ReferenceType.TYPE, st, n);
            Symbol<?> globalSymbol = new Symbol<TypeDeclarationStmt>(st.getName(), st, n, ReferenceType.TYPE);

            symbolTable.getScopes().get(0).addSymbol(globalSymbol);
            n.getTypeDeclaration().setSymbolData(s.getType());
            s.setInnerScope(new Scope(s));
            globalSymbol.setInnerScope(s.getInnerScope());

            ScopeLoader scopeLoader = new ScopeLoader(typeTable, expressionTypeAnalyzer, actionProvider);
            n.setSymbolData(s.getType());
            Scope scope = n.getTypeDeclaration().accept(scopeLoader, symbolTable);
            s.setInnerScope(scope);

        }
        super.visit(n, arg);
    }

    @Override
    public void visit(ConstructorDeclaration n, A arg) {

        List<Symbol<?>> symbols = symbolTable.getScopes().peek().getSymbolsByLocation(n);
        Scope scope = symbols.get(0).getInnerScope();
        if (scope == null) {
            scope = new Scope(symbols.get(0));
            symbols.get(0).setInnerScope(scope);
        }
        symbolTable.pushScope(scope);
        LoadTypeParamsAction action = new LoadTypeParamsAction();
        action.load(symbolTable, n.getTypeParameters(), (SymbolType) n.getSymbolData());
        super.visit(n, arg);
        symbolTable.popScope();
    }

    @Override
    public void visit(ObjectCreationExpr n, A arg) {

        loadThisSymbol(n, arg);
        SymbolType[] argsType = argsType(n.getArgs());
        SymbolType scopeType = (SymbolType) n.getType().getSymbolData();
        symbolTable.lookUpSymbolForRead(scopeType.getClazz().getSimpleName(), n, scopeType, argsType,
                ReferenceType.METHOD);

        List<Type> typeargs = n.getTypeArgs();

        if (typeargs != null) {
            for (Type type : typeargs) {
                type.accept(this, arg);
            }
        }
    }

    private static SymbolType[] argsType(final List<Expression> exprs) {
        List<Expression> args = exprs;
        SymbolType[] argsType = null;
        if (args != null) {
            Iterator<Expression> it = args.iterator();
            argsType = new SymbolType[args.size()];
            int i = 0;
            while (it.hasNext()) {
                Expression current = it.next();

                SymbolType argType = (SymbolType) current.getSymbolData();

                argsType[i] = argType;

                i++;

            }
        } else {
            argsType = new SymbolType[0];
        }
        return argsType;
    }

    public void pushScope(TypeDeclaration n) {
        Symbol<?> s = symbolTable.findSymbol(n.getName(), ReferenceType.TYPE);
        Scope scope = s.getInnerScope();
        symbolTable.pushScope(scope);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, A arg) {

        pushScope(n);
        //System.out.println(n.getName());
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        if (n.getAnnotations() != null) {
            for (AnnotationExpr a : n.getAnnotations()) {
                a.accept(this, arg);
            }
        }
        if (n.getTypeParameters() != null) {
            for (TypeParameter t : n.getTypeParameters()) {
                t.accept(this, arg);
            }
        }
        if (n.getExtends() != null) {
            for (ClassOrInterfaceType c : n.getExtends()) {
                c.accept(this, arg);
            }
        }
        if (n.getImplements() != null) {
            for (ClassOrInterfaceType c : n.getImplements()) {
                c.accept(this, arg);
                SymbolData sd = c.getSymbolData();
                if (sd != null) {
                    // Java 8 super in an interface is itself
                    Symbol<?> s = symbolTable.findSymbol(sd.getName(), ReferenceType.TYPE);
                    if (s != null) {
                        Scope scope = s.getInnerScope();
                        if (scope == null) {
                            scope = new Scope();
                            scope.addSymbol(new Symbol("super", s.getType(), null));
                            s.setInnerScope(scope);
                        }
                    }
                }
            }
        }
        if (n.getMembers() != null) {
            for (BodyDeclaration member : n.getMembers()) {
                member.accept(this, arg);
            }
        }
        symbolTable.popScope();

    }

    @Override
    public void visit(AnnotationDeclaration n, A arg) {

        pushScope(n);
        super.visit(n, arg);
        symbolTable.popScope();

    }

    @Override
    public void visit(EnumConstantDeclaration n, A arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        if (n.getAnnotations() != null) {
            for (AnnotationExpr a : n.getAnnotations()) {
                a.accept(this, arg);
            }
        }
        if (n.getArgs() != null) {
            for (Expression e : n.getArgs()) {
                e.accept(expressionTypeAnalyzer, arg);
            }
        }
        if (n.getClassBody() != null) {

            loadThisSymbol(n, arg);

        }
    }

    public void visit(VariableDeclarator n, A arg) {

        if (n.getInit() != null) {
            Symbol<?> aux = symbolTable.findSymbol(n.getId().getName(), ReferenceType.VARIABLE);
            Scope scope = new Scope(aux);
            aux.setInnerScope(scope);
            symbolTable.pushScope(scope);
            n.getInit().accept(expressionTypeAnalyzer, arg);
            symbolTable.popScope();
        }
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, A arg) {

        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        if (n.getAnnotations() != null) {
            for (AnnotationExpr a : n.getAnnotations()) {
                a.accept(this, arg);
            }
        }
        n.getType().accept(expressionTypeAnalyzer, arg);

        Expression expr = n.getDefaultValue();
        if (expr != null) {
            expr.accept(expressionTypeAnalyzer, arg);
        }
    }

    @Override
    public void visit(BlockStmt n, A arg) {
        List<SymbolAction> actions = null;
        if (actionProvider != null) {
            actions = actionProvider.getActions(n);
        }
        symbolTable.addActionsToScope(actions);
        symbolTable.pushScope();
        super.visit(n, arg);
        List<Statement> stmts = n.getStmts();
        if (stmts != null) {
            SymbolData sd = null;
            for (Statement stmt : stmts) {
                SymbolData stmtSd = stmt.getSymbolData();
                if (stmtSd != null) {
                    if (sd == null) {
                        sd = stmtSd;
                    } else {
                        sd = sd.merge(stmtSd);
                    }
                }
            }
            n.setSymbolData(sd);
        }
        symbolTable.popScope();
    }

    @Override
    public void visit(MethodDeclaration n, A arg) {
        List<Symbol<?>> symbols = symbolTable.getScopes().peek().getSymbolsByLocation(n);
        Scope scope = symbols.get(0).getInnerScope();
        if (scope == null) {
            scope = new Scope(symbols.get(0));
            symbols.get(0).setInnerScope(scope);
        }
        symbolTable.pushScope(scope);
        super.visit(n, arg);
        symbolTable.popScope();
    }

    @Override
    public void visit(Parameter n, A arg) {
        super.visit(n, arg);
        Type ptype = n.getType();
        SymbolType type = null;
        if (ptype != null) {
            type = (SymbolType) ptype.getSymbolData();

        } else {
            type = (SymbolType) n.getSymbolData();
        }
        VariableDeclaratorId id = n.getId();
        if (id != null) {
            int arrayCount = id.getArrayCount();
            if (arrayCount > 0) {
                type.setArrayCount(type.getArrayCount() + arrayCount);
            }
        }
        if (n.isVarArgs()) {
            type.setArrayCount(type.getArrayCount() + 1);
        }
        List<SymbolAction> actions = null;
        if (actionProvider != null) {
            actions = actionProvider.getActions(n);
        }
        symbolTable.pushSymbol(n.getId().getName(), ReferenceType.VARIABLE, type, n, actions);
        n.setSymbolData(type);
    }

    @Override
    public void visit(TryStmt n, A arg) {
        symbolTable.pushScope();
        super.visit(n, arg);
        symbolTable.popScope();
    }

    @Override
    public void visit(CatchClause n, A arg) {
        symbolTable.pushScope();
        super.visit(n, arg);
        symbolTable.popScope();
    }

    @Override
    public void visit(MultiTypeParameter n, A arg) {
        super.visit(n, arg);
        List<Type> types = n.getTypes();
        List<SymbolType> symbolTypes = new LinkedList<SymbolType>();
        Iterator<Type> it = types.iterator();

        while (it.hasNext()) {
            Type aux = it.next();
            SymbolType type = symbolResolver.valueOf(aux);
            aux.setSymbolData(type);
            symbolTypes.add(type);
        }
        List<SymbolAction> actions = null;
        if (actionProvider != null) {
            actions = actionProvider.getActions(n);
        }
        MultiTypeSymbol symbol = new MultiTypeSymbol(n.getId().getName(), symbolTypes, n, actions);
        symbolTable.pushSymbol(symbol);
        n.setSymbolData(new SymbolType(symbolTypes));

    }

    @Override
    public void visit(MethodCallExpr n, A arg) {

        SymbolType scopeType = null;

        if (n.getScope() != null) {

            scopeType = (SymbolType) n.getScope().getSymbolData();

        }
        SymbolType[] argsType = argsType(n.getArgs());

        symbolTable.lookUpSymbolForRead(n.getName(), n, scopeType, argsType, ReferenceType.METHOD);

        List<Type> typeargs = n.getTypeArgs();

        if (typeargs != null) {
            for (Type type : typeargs) {
                type.accept(this, arg);
            }
        }

    }

    @Override
    public void visit(FieldAccessExpr n, A arg) {

        if (n.getScope() != null) {

            if (!arg.containsKey(VISITOR_SCOPE_PROCESSOR)) {
                n.getScope().accept(this, arg);
            }
            SymbolType scopeType = (SymbolType) n.getScope().getSymbolData();

            lookupSymbol(n.getField(), arg, n, scopeType, ReferenceType.VARIABLE);

        } else {

            lookupSymbol(n.getField(), arg, n, null, ReferenceType.VARIABLE);
        }
    }

    public void visit(AssignExpr n, A arg) {
        AccessType old = (AccessType) arg.get(AccessType.ACCESS_TYPE);
        arg.put(AccessType.ACCESS_TYPE, AccessType.WRITE);
        n.getTarget().accept(expressionTypeAnalyzer, arg);
        arg.put(AccessType.ACCESS_TYPE, AccessType.READ);
        n.getValue().accept(expressionTypeAnalyzer, arg);
        arg.put(AccessType.ACCESS_TYPE, old);
        n.setSymbolData(n.getTarget().getSymbolData());
    }

    public void visit(VariableDeclarationExpr n, A arg) {

        Type type = n.getType();
        SymbolType st = (SymbolType) type.getSymbolData();
        if (st == null) {
            type.accept(expressionTypeAnalyzer, arg);
            st = (SymbolType) type.getSymbolData();
            if (st == null) {
                throw new NoSuchExpressionTypeException(
                        "The type of " + type.toString() + "(" + type.getClass().getName() + ") at ["
                                + type.getBeginLine() + "," + type.getBeginColumn() + "] is not found.");
            }
        }
        List<SymbolAction> actions = null;
        if (actionProvider != null) {
            actions = actionProvider.getActions(n);
        }
        List<VariableDeclarator> vars = n.getVars();
        for (VariableDeclarator vd : vars) {
            SymbolType aux = st.clone();
            if (vd.getId().getArrayCount() > 0) {
                aux.setArrayCount(vd.getId().getArrayCount());
            }
            symbolTable.pushSymbol(vd.getId().getName(), ReferenceType.VARIABLE, aux, vd, actions);
            Expression expr = vd.getInit();
            if (expr != null && !(n.getParentNode() instanceof ExpressionStmt)) { // e.g
                // TryStmt
                expr.accept(expressionTypeAnalyzer, arg);
            }
        }
    }

    @Override
    public void visit(ExpressionStmt n, A arg) {
        n.getExpression().accept(expressionTypeAnalyzer, arg);
    }

    @Override
    public void visit(AssertStmt n, A arg) {
        n.getCheck().accept(expressionTypeAnalyzer, arg);
        if (n.getMessage() != null) {
            n.getMessage().accept(this, arg);
        }
    }

    @Override
    public void visit(DoStmt n, A arg) {
        n.getBody().accept(this, arg);
        n.getCondition().accept(expressionTypeAnalyzer, arg);
        SymbolData sd = n.getBody().getSymbolData();
        n.setSymbolData(sd);
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, A arg) {
        if (!n.isThis()) {
            if (n.getExpr() != null) {
                n.getExpr().accept(expressionTypeAnalyzer, arg);
                n.setSymbolData(n.getExpr().getSymbolData());
            }
        }
        // resolve constructor symbol data
        n.accept(expressionTypeAnalyzer, arg);

        if (n.getTypeArgs() != null) {
            for (Type t : n.getTypeArgs()) {
                t.accept(this, arg);
            }
        }
        if (n.getArgs() != null) {
            for (Expression e : n.getArgs()) {
                e.accept(expressionTypeAnalyzer, arg);
            }
        }

    }

    @Override
    public void visit(ForeachStmt n, A arg) {
        symbolTable.pushScope();
        n.getVariable().accept(expressionTypeAnalyzer, arg);
        n.getIterable().accept(expressionTypeAnalyzer, arg);
        n.getBody().accept(this, arg);
        SymbolData sd = n.getBody().getSymbolData();
        n.setSymbolData(sd);
        symbolTable.popScope();
    }

    @Override
    public void visit(ForStmt n, A arg) {
        symbolTable.pushScope();
        if (n.getInit() != null) {
            for (Expression e : n.getInit()) {
                e.accept(expressionTypeAnalyzer, arg);
            }
        }
        if (n.getCompare() != null) {
            n.getCompare().accept(expressionTypeAnalyzer, arg);
        }
        if (n.getUpdate() != null) {
            for (Expression e : n.getUpdate()) {
                e.accept(expressionTypeAnalyzer, arg);
            }
        }
        n.getBody().accept(this, arg);
        SymbolData sd = n.getBody().getSymbolData();
        n.setSymbolData(sd);
        symbolTable.popScope();
    }

    @Override
    public void visit(IfStmt n, A arg) {
        n.getCondition().accept(expressionTypeAnalyzer, arg);
        n.getThenStmt().accept(this, arg);
        SymbolData symData = n.getThenStmt().getSymbolData();
        n.setSymbolData(symData);
        if (n.getElseStmt() != null) {
            n.getElseStmt().accept(this, arg);
            if (symData != null) {
                symData = symData.merge(n.getElseStmt().getSymbolData());
            }
            n.setSymbolData(symData);
        }

    }

    @Override
    public void visit(ReturnStmt n, A arg) {
        if (n.getExpr() != null) {
            n.getExpr().accept(expressionTypeAnalyzer, arg);
            n.setSymbolData(n.getExpr().getSymbolData());
        } else {
            n.setSymbolData(new SymbolType(void.class));
        }
    }

    @Override
    public void visit(SwitchEntryStmt n, A arg) {
        if (n.getLabel() != null) {
            n.getLabel().accept(expressionTypeAnalyzer, arg);
        }
        if (n.getStmts() != null) {
            SymbolData sd = null;
            for (Statement s : n.getStmts()) {
                s.accept(this, arg);
                if (sd == null) {
                    sd = s.getSymbolData();
                } else {
                    sd = sd.merge(s.getSymbolData());
                }
            }
            n.setSymbolData(sd);
        }
    }

    @Override
    public void visit(SwitchStmt n, A arg) {
        n.getSelector().accept(expressionTypeAnalyzer, arg);
        if (n.getEntries() != null) {
            SymbolData sd = null;
            for (SwitchEntryStmt e : n.getEntries()) {
                e.accept(this, arg);
                if (sd == null) {
                    sd = e.getSymbolData();
                } else {
                    sd = sd.merge(e.getSymbolData());
                }
            }
        }
    }

    @Override
    public void visit(SynchronizedStmt n, A arg) {
        n.getExpr().accept(expressionTypeAnalyzer, arg);
        n.getBlock().accept(this, arg);
        n.setSymbolData(n.getBlock().getSymbolData());
    }

    @Override
    public void visit(ThrowStmt n, A arg) {
        n.getExpr().accept(expressionTypeAnalyzer, arg);
        n.setSymbolData(null);
    }

    @Override
    public void visit(WhileStmt n, A arg) {
        n.getCondition().accept(expressionTypeAnalyzer, arg);
        n.getBody().accept(this, arg);
        n.setSymbolData(n.getBody().getSymbolData());
    }

    private void lookupSymbol(String name, A arg, SymbolReference n, SymbolType scope, ReferenceType... referenceType) {
        AccessType atype = (AccessType) arg.get(AccessType.ACCESS_TYPE);
        if (atype != null) {
            if (atype.equals(AccessType.WRITE)) {
                symbolTable.lookUpSymbolForWrite(name, n, scope, null, referenceType);
            } else {
                symbolTable.lookUpSymbolForRead(name, n, scope, null, referenceType);
            }
        } else {
            symbolTable.lookUpSymbolForRead(name, n, scope, null, referenceType);
        }
    }

    public void visit(NameExpr n, A arg) {
        lookupSymbol(n.toString(), arg, n, null, ReferenceType.VARIABLE, ReferenceType.ENUM_LITERAL,
                ReferenceType.TYPE);
    }

    @Override
    public void visit(MethodReferenceExpr n, A arg) {

        SymbolType scopeType = null;

        if (n.getScope() == null) {
            scopeType = symbolTable.getType("this", ReferenceType.VARIABLE);
        } else {
            scopeType = (SymbolType) n.getScope().getSymbolData();
            if (scopeType == null) {
                n.getScope().accept(expressionTypeAnalyzer, arg);
                scopeType = (SymbolType) n.getScope().getSymbolData();
            }
        }
        SymbolType[] argsType = (SymbolType[]) n.getReferencedArgsSymbolData();

        symbolTable.lookUpSymbolForRead(n.getIdentifier(), n, scopeType, argsType, ReferenceType.METHOD);

    }

    @Override
    public void visit(TypeParameter n, A arg) {
        super.visit(n, arg);
        symbolTable.lookUpSymbolForRead(n.getName(), null, ReferenceType.TYPE);

    }

    @Override
    public void visit(SuperExpr n, A arg) {
        Expression classExpr = n.getClassExpr();
        Symbol<?> s = null;
        if (classExpr == null) {
            s = symbolTable.lookUpSymbolForRead("super", null, ReferenceType.VARIABLE);

        } else {
            classExpr.accept(this, arg);
            SymbolData sd = classExpr.getSymbolData();
            Class<?> aux = sd.getClazz().getSuperclass();
            if (aux == null) {
                aux = Object.class;
            }
            s = symbolTable.lookUpSymbolForRead(aux.getCanonicalName(), null, ReferenceType.TYPE);

        }
        if (n.getSymbolData() == null && s != null) {
            n.setSymbolData(s.getType());
        }
    }

    @Override
    public void visit(ThisExpr n, A arg) {

        Expression classExpr = n.getClassExpr();
        Symbol<?> s = null;
        if (classExpr == null) {
            s = symbolTable.lookUpSymbolForRead("this", null, ReferenceType.VARIABLE);
            if (n.getSymbolData() == null && s != null) {
                n.setSymbolData(s.getType());
            }
        } else {
            classExpr.accept(this, arg);
            SymbolData sd = classExpr.getSymbolData();
            n.setSymbolData(sd);
        }

    }

    @Override
    public void visit(WildcardType n, A arg) {
        n.accept(expressionTypeAnalyzer, arg);
    }

    @Override
    public void visit(VoidType n, A arg) {
        n.accept(expressionTypeAnalyzer, arg);
    }

    @Override
    public void visit(org.walkmod.javalang.ast.type.ReferenceType n, A arg) {
        n.accept(expressionTypeAnalyzer, arg);
    }

    @Override
    public void visit(PrimitiveType n, A arg) {
        n.accept(expressionTypeAnalyzer, arg);
    }

    @Override
    public void visit(ClassOrInterfaceType n, A arg) {
        n.accept(expressionTypeAnalyzer, arg);
    }

    @Override
    public void visit(FieldDeclaration n, A arg) {
        super.visit(n, arg);
        n.accept(expressionTypeAnalyzer, arg);
    }

}
