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

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.reflection.MethodInspector;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.MethodSymbol;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeVisitorAdapter;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.visitors.GenericVisitorAdapter;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class LoadMethodDeclarationsAction extends SymbolAction {

    private SymbolActionProvider actionProvider;
    private TypeVisitorAdapter<?> expressionTypeAnalyzer;

    public LoadMethodDeclarationsAction(SymbolActionProvider actionProvider,
            TypeVisitorAdapter<?> expressionTypeAnalyzer) {

        this.actionProvider = actionProvider;
        this.expressionTypeAnalyzer = expressionTypeAnalyzer;
    }

    private void pushMethod(SymbolType st, SymbolTable table, MethodDeclaration md) throws Exception {
        Scope sc = new Scope();
        table.pushScope(sc);
        LoadTypeParamsAction action = new LoadTypeParamsAction();
        action.load(table, md.getTypeParameters(), null);
        Type type = md.getType();
        SymbolType resolvedType = ASTSymbolTypeResolver.getInstance().valueOf(type);
        if (resolvedType == null) {
            resolvedType = new SymbolType(Object.class);
        } else {
            resolvedType.setClazz(TypesLoaderVisitor.getClassLoader().loadClass(resolvedType));
        }
        type.setSymbolData(resolvedType);

        List<Parameter> params = md.getParameters();
        List<TypeParameter> tps = md.getTypeParameters();
        SymbolType[] args = null;
        boolean hasDynamicArgs = false;
        if (params != null) {
            args = new SymbolType[params.size()];
            for (int i = 0; i < args.length; i++) {
                Parameter currentParam = params.get(i);

                args[i] = ASTSymbolTypeResolver.getInstance().valueOf(currentParam.getType(), tps);
                int arrayCount = currentParam.getId().getArrayCount();
                if (arrayCount > 0) {
                    args[i].setArrayCount(args[i].getArrayCount() + arrayCount);
                }
                params.get(i).getType().setSymbolData(args[i]);
                if (i == args.length - 1) {
                    hasDynamicArgs = params.get(i).isVarArgs();
                    if (hasDynamicArgs) {
                        args[i] = args[i].clone();
                        args[i].setArrayCount(args[i].getArrayCount() + 1);
                    }
                }

            }
        }

        List<SymbolAction> actions = null;

        if (actionProvider != null) {
            actions = actionProvider.getActions(md);
        }

        MethodSymbol method = new MethodSymbol(md.getName(), resolvedType, md, st, args, false, hasDynamicArgs,
                (Method) null, actions);
        sc.setRootSymbol(method);
        method.setInnerScope(sc);

        md.accept(expressionTypeAnalyzer, null);
        table.popScope(true);
        MethodSymbolData msd = md.getSymbolData();
        if (msd == null) {
            throw new RuntimeException("Ops! The following method can't be solved: " + md.toString());
        }
        method.setReferencedMethod(msd.getMethod());
        table.pushSymbol(method, true);
    }

    private void pushConstructor(SymbolType st, SymbolTable table, ConstructorDeclaration md) throws Exception {
        Type type = new ClassOrInterfaceType(md.getName());
        SymbolType resolvedType = ASTSymbolTypeResolver.getInstance().valueOf(type);
        type.setSymbolData(resolvedType);

        List<Parameter> params = md.getParameters();
        List<TypeParameter> tps = md.getTypeParameters();

        SymbolType[] args = null;
        boolean hasDynamicArgs = false;
        if (params != null) {
            args = new SymbolType[params.size()];
            for (int i = 0; i < args.length; i++) {

                args[i] = ASTSymbolTypeResolver.getInstance().valueOf(params.get(i).getType(), tps);

                params.get(i).getType().setSymbolData(args[i]);
                if (i == args.length - 1) {
                    hasDynamicArgs = params.get(i).isVarArgs();
                }
            }
        }
        List<SymbolAction> actions = null;
        if (actionProvider != null) {
            actions = actionProvider.getActions(md);
        }
        MethodSymbol method = new MethodSymbol(md.getName(), resolvedType, md, st, args, false, hasDynamicArgs,
                (java.lang.reflect.Constructor<?>) null, actions);

        Scope scope = new Scope(method);
        method.setInnerScope(scope);
        table.pushScope(scope);
        md.accept(expressionTypeAnalyzer, null);
        table.popScope(true);

        method.setReferencedConstructor(md.getSymbolData().getConstructor());

        table.pushSymbol(method);
    }

    @Override
    public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
        if (symbol.getName().equals("this")) {
            Node node = symbol.getLocation();
            MethodsPopulator populator = new MethodsPopulator(table);
            node.accept(populator, table.getScopes().peek());
        }

    }

    private class MethodsPopulator extends VoidVisitorAdapter<Scope> {

        private SymbolTable table;

        public MethodsPopulator(SymbolTable table) {
            this.table = table;
        }

        @Override
        public void visit(ObjectCreationExpr o, Scope scope) {
            if (!scope.hasMethodsLoaded()) {
                table.pushScope(scope);
                List<ClassOrInterfaceType> types = new LinkedList<ClassOrInterfaceType>();
                types.add(o.getType());
                loadExtendsOrImplements(types);
                loadMethods(o.getAnonymousClassBody(), scope);

                table.popScope(true);
            }
        }

        @Override
        public void visit(EnumConstantDeclaration o, Scope scope) {
            if (!scope.hasMethodsLoaded()) {
                table.pushScope(scope);
                loadMethods(o.getClassBody(), scope);
                table.popScope(true);
            }
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Scope scope) {
            if (!scope.hasMethodsLoaded()) {
                table.pushScope(scope);
                loadExtendsOrImplements(n.getExtends());
                loadMethods(n.getMembers(), scope);
                if (!n.isInterface()) {
                    loadExtendsOrImplements(n.getImplements());
                }
                table.popScope(true);
            }
        }

        private void loadMethods(List<BodyDeclaration> members, Scope scope) {
            if (!scope.hasMethodsLoaded() && members != null) {
                try {
                    for (BodyDeclaration member : members) {
                        if (member instanceof ConstructorDeclaration) {
                            ConstructorDeclaration cd = (ConstructorDeclaration) member;
                            pushConstructor((SymbolType) ((SymbolDataAware<?>) member.getParentNode()).getSymbolData(),
                                    table, cd);
                        }
                        if (member instanceof MethodDeclaration) {
                            MethodDeclaration md = (MethodDeclaration) member;
                            pushMethod((SymbolType) ((SymbolDataAware<?>) member.getParentNode()).getSymbolData(),
                                    table, md);

                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error loading methods in a given scope", e);
                }
                scope.setHasMethodsLoaded(true);
            }
        }

        @Override
        public void visit(EnumDeclaration n, Scope scope) {
            table.pushScope(scope);
            loadMethods(n.getMembers(), scope);
            table.popScope(true);
        }

        @Override
        public void visit(AnnotationDeclaration n, Scope scope) {
            table.pushScope(scope);
            loadMethods(n.getMembers(), scope);
            table.popScope(true);
        }

        class NameBuilder extends GenericVisitorAdapter<String, Object> {
            public String visit(ClassOrInterfaceType n, Object arg) {
                String result = "";

                ClassOrInterfaceType scope = n.getScope();
                if (scope != null) {
                    result = scope.accept(this, arg) + ".";
                }

                return result + n.getName();
            }
        }

        private NameBuilder nameBuilder = new NameBuilder();

        private void loadMethods(Collection<Method> methods, List<SymbolType> params, SymbolType st) {
            for (Method method : methods) {
                Map<String, SymbolType> parameterTypes = null;
                try {
                    java.lang.reflect.Type[] genericParameterTypes = method.getGenericParameterTypes();
                    SymbolType[] methodArgs = new SymbolType[genericParameterTypes.length];
                    if (params != null) {
                        parameterTypes = new HashMap<String, SymbolType>();

                        TypeVariable<?>[] typeParams = method.getDeclaringClass().getTypeParameters();
                        for (int i = 0; i < params.size() && i < typeParams.length; i++) {
                            SymbolType.valueOf(typeParams[i], params.get(i), parameterTypes, null);
                        }
                    }

                    for (int i = 0; i < genericParameterTypes.length; i++) {
                        methodArgs[i] = SymbolType.valueOf(genericParameterTypes[i], parameterTypes);

                    }

                    SymbolType returnType = SymbolType.valueOf(method, parameterTypes);
                    MethodSymbol methodSymbol = new MethodSymbol(method.getName(), returnType, null, st, methodArgs,
                            false, method.isVarArgs(), method, null);

                    table.pushSymbol(methodSymbol);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void loadExtendsOrImplements(List<ClassOrInterfaceType> extendsList) {
            if (extendsList != null) {
                for (ClassOrInterfaceType type : extendsList) {
                    String name = type.accept(nameBuilder, null);
                    Symbol<?> s = table.findSymbol(name, ReferenceType.TYPE);
                    if (s == null) {
                        SymbolType st = ASTSymbolTypeResolver.getInstance().valueOf(type);
                        if (st == null || st.getClazz() == null) {
                            throw new RuntimeException("Error resolving  " + name);
                        }
                        name = st.getClazz().getCanonicalName();
                        s = table.findSymbol(name, ReferenceType.TYPE);
                    }

                    if (s != null) {
                        Object location = s.getLocation();
                        boolean isInterfaceImplementation = false;
                        if (location != null && location instanceof TypeDeclaration) {

                            if (s.getType().getClazz().isInterface()) {
                                Node parent = type.getParentNode();
                                if (parent != null) {
                                    if (parent instanceof ClassOrInterfaceDeclaration) {
                                        ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) parent;

                                        isInterfaceImplementation = !decl.isInterface();
                                    }
                                }
                            }
                            if (!isInterfaceImplementation) {
                                TypeDeclaration superClass = ((TypeDeclaration) location);
                                superClass.accept(this, s.getInnerScope());
                            } else {
                                // we need to load in the same scope the
                                // "default" methods
                                SymbolType typeArg = ASTSymbolTypeResolver.getInstance().valueOf(type);

                                Node parent = type.getParentNode();
                                if (parent instanceof SymbolDataAware) {
                                    SymbolDataAware<?> ctxt = (SymbolDataAware<?>) parent;
                                    Set<Method> methods = MethodInspector.getInhertitedDefaultMethods(
                                            typeArg.getClazz(), ctxt.getSymbolData().getClazz());
                                    if (!methods.isEmpty()) {
                                        Scope aux = s.getInnerScope();

                                        TypeDeclaration superClass = ((TypeDeclaration) location);

                                        superClass.accept(this, aux);

                                        Iterator<Method> it = methods.iterator();

                                        while (it.hasNext()) {
                                            Method current = it.next();
                                            Symbol<?> sType = table.findSymbol(
                                                    current.getDeclaringClass().getCanonicalName(), ReferenceType.TYPE);
                                            if (sType != null) {
                                                aux = sType.getInnerScope();
                                                if (aux != null) {
                                                    Class<?>[] argClasses = current.getParameterTypes();
                                                    SymbolType[] args = new SymbolType[argClasses.length];
                                                    for (int i = 0; i < args.length; i++) {
                                                        args[i] = new SymbolType(argClasses[i]);
                                                    }
                                                    Symbol<?> sym = aux.findSymbol(current.getName(), null, args, null,
                                                            ReferenceType.METHOD);

                                                    if (sym != null) {
                                                        sym.getType().setMethod(current);
                                                        table.pushSymbol(sym);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        } else {
                            Class<?> clazz = s.getType().getClazz();
                            Set<Method> methods = null;

                            if (!isInterfaceImplementation) {
                                methods = MethodInspector.getInheritedMethods(clazz);
                                Symbol<?> parent = table.findSymbol("super", ReferenceType.VARIABLE);
                                if (parent == null) {
                                    parent = table.pushSymbol("super", ReferenceType.TYPE, new SymbolType(clazz), null);
                                }

                                Scope parentScope = parent.getInnerScope();
                                if (parentScope == null) {
                                    parentScope = new Scope(parent);
                                    parent.setInnerScope(parentScope);
                                }

                                SymbolType typeArg = ASTSymbolTypeResolver.getInstance().valueOf(type);

                                List<SymbolType> params = typeArg.getParameterizedTypes();

                                table.pushScope(parentScope);
                                loadMethods(methods, params, s.getType());
                                table.popScope(true);
                            } else {
                                SymbolType typeArg = ASTSymbolTypeResolver.getInstance().valueOf(type);
                                List<SymbolType> params = typeArg.getParameterizedTypes();
                                methods = MethodInspector.getInhertitedDefaultMethods(typeArg.getClazz(), clazz);
                                loadMethods(methods, params, s.getType());
                            }

                        }
                    }
                }
            }
        }
    }
}
