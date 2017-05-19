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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.reflection.FieldInspector;
import org.walkmod.javalang.compiler.reflection.GenericBuilderFromGenericClasses;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.InvalidTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class LoadFieldDeclarationsAction extends SymbolAction {

    private SymbolActionProvider actionProvider;

    public LoadFieldDeclarationsAction(SymbolActionProvider actionProvider) {

        this.actionProvider = actionProvider;
    }

    @Override
    public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {

        if (symbol.getName().equals("this")) {
            Node node = symbol.getLocation();
            if (node instanceof TypeDeclaration || node instanceof ObjectCreationExpr
                    || node instanceof EnumConstantDeclaration) {
                node.accept(new FieldsPopulator(table), table.getScopes().peek());
            }

        }
    }

    private class FieldsPopulator extends VoidVisitorAdapter<Scope> {

        private SymbolTable table;

        public FieldsPopulator(SymbolTable table) {
            this.table = table;
        }

        @Override
        public void visit(ObjectCreationExpr o, Scope scope) {
            if (!scope.hasFieldsLoaded()) {
                table.pushScope(scope);
                List<ClassOrInterfaceType> types = new LinkedList<ClassOrInterfaceType>();
                types.add(o.getType());
                loadExtendsOrImplements(types);
                loadFields(o.getAnonymousClassBody(), scope);
                table.popScope(true);
            }
        }

        @Override
        public void visit(EnumConstantDeclaration o, Scope scope) {
            if (!scope.hasFieldsLoaded()) {
                table.pushScope(scope);
                loadFields(o.getClassBody(), scope);
                table.popScope(true);
            }
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Scope scope) {
            if (!scope.hasFieldsLoaded()) {
                table.pushScope(scope);

                loadExtendsOrImplements(n.getExtends());
                loadExtendsOrImplements(n.getImplements());
                loadFields(n.getMembers(), scope);
                table.popScope(true);
            }

        }

        @Override
        public void visit(EnumDeclaration n, Scope scope) {
            if (!scope.hasFieldsLoaded()) {
                table.pushScope(scope);
                loadExtendsOrImplements(n.getImplements());
                loadFields(n.getMembers(), scope);
                table.popScope(true);
            }
        }

        @Override
        public void visit(AnnotationDeclaration n, Scope scope) {
            if (!scope.hasFieldsLoaded()) {
                table.pushScope(scope);
                loadFields(n.getMembers(), scope);
                table.popScope(true);
            }
        }

        public void loadExtendsOrImplements(List<ClassOrInterfaceType> extendsList) {
            if (extendsList != null) {
                Symbol<?> superSym = table.findSymbol("super");
                Map<String, SymbolType> typeParams = null;
                Scope superScope = null;
                if (superSym != null) {
                    superScope = superSym.getInnerScope();

                    if (superScope != null) {
                        typeParams = superScope.getTypeParams();
                    }
                }

                for (ClassOrInterfaceType type : extendsList) {
                    String name = type.getName();
                    ClassOrInterfaceType scope = type.getScope();
                    if (scope != null) {
                        name = scope.toString() + "." + name;
                    }
                    Symbol<?> s = table.findSymbol(name, ReferenceType.TYPE);

                    if (s != null) {
                        Scope inner = s.getInnerScope();
                        Object location = s.getLocation();
                        if (location != null && location instanceof TypeDeclaration) {
                            ((TypeDeclaration) location).accept(this, inner);
                            List<SymbolType> parameters = s.getType().getParameterizedTypes();
                            if (parameters != null && typeParams != null) {
                                List<Symbol<?>> symbols = inner.getSymbolsByType(ReferenceType.VARIABLE);
                                table.pushScope(superScope);
                                for (Symbol<?> sym : symbols) {
                                    String symName = sym.getName();
                                    if (!"this".equals(symName) && !"super".equals(symName)) {
                                        SymbolType refactorized = sym.getType().refactor(typeParams);

                                        table.pushSymbol(symName, ReferenceType.VARIABLE, refactorized,
                                                sym.getLocation());
                                    }
                                }
                                table.popScope(true);
                            }

                        } else {
                            Class<?> clazz = s.getType().getClazz();
                            loadFieldsFromClass(clazz, typeParams);
                        }
                    } else {
                        Class<?> clazz = null;

                        if (type.getParentNode() instanceof ObjectCreationExpr) {
                            ObjectCreationExpr parent = (ObjectCreationExpr) type.getParentNode();
                            clazz = parent.getSymbolData().getClazz().getSuperclass();
                        } else {
                            SymbolType stype = ASTSymbolTypeResolver.getInstance().valueOf(type);
                            clazz = stype.getClazz();

                        }
                        loadFieldsFromClass(clazz, typeParams);
                    }
                }
            }
        }

        private void loadFieldsFromClass(Class<?> clazz, Map<String, SymbolType> typeParams) {
            Set<Field> fields = FieldInspector.getNonPrivateFields(clazz);
            List<SymbolType> params = table.getType("this").getParameterizedTypes();

            for (Field field : fields) {
                try {
                    Map<String, SymbolType> typeMapping = null;
                    if (typeParams == null) {
                        if (!clazz.getName().equals(field.getDeclaringClass().getName())) {

                            List<java.lang.reflect.Type> types = ClassInspector
                                    .getInterfaceOrSuperclassImplementations(clazz, field.getDeclaringClass());
                            if (!types.isEmpty()) {
                                java.lang.reflect.Type firstType = types.iterator().next();
                                if (firstType instanceof ParameterizedType) {
                                    typeMapping = new HashMap<String, SymbolType>();
                                    ParameterizedType ptype = (ParameterizedType) firstType;
                                    java.lang.reflect.Type[] args = ptype.getActualTypeArguments();
                                    TypeVariable<?>[] tv = field.getDeclaringClass().getTypeParameters();
                                    for (int i = 0; i < tv.length && i < args.length; i++) {
                                        typeMapping.put(tv[i].getName(), SymbolType.valueOf(args[i], null));
                                    }
                                }
                            }
                        }
                    } else {
                        typeMapping = new HashMap<String, SymbolType>();

                        GenericBuilderFromGenericClasses builder =
                                new GenericBuilderFromGenericClasses(field.getDeclaringClass(), params);
                        builder.build(typeMapping);
                        typeMapping.putAll(typeParams);
                    }

                    table.pushSymbol(field.getName(), ReferenceType.VARIABLE,
                            SymbolType.valueOf(field.getGenericType(), typeMapping), null,

                            true);
                } catch (InvalidTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void loadFields(List<BodyDeclaration> members, Scope scope) {
            if (!scope.hasFieldsLoaded()) {
                if (members != null) {

                    for (BodyDeclaration member : members) {
                        if (member instanceof FieldDeclaration) {

                            FieldDeclaration fd = (FieldDeclaration) member;
                            Type type = fd.getType();
                            List<SymbolAction> actions = null;

                            Symbol<?> root = table.getScopes().peek().getRootSymbol();
                            if (root != null) {
                                Node location = root.getLocation();
                                if (location != null) {
                                    if (location == member.getParentNode()) {
                                        if (actionProvider != null) {
                                            actions = actionProvider.getActions(fd);
                                        }
                                    }
                                }
                            }

                            SymbolType resolvedType = ASTSymbolTypeResolver.getInstance().valueOf(type);

                            if (resolvedType == null) {
                                resolvedType = new SymbolType(Object.class);
                            }
                            type.setSymbolData(resolvedType);

                            for (VariableDeclarator var : fd.getVariables()) {
                                SymbolType symType = resolvedType.clone();
                                if (symType.getArrayCount() == 0) {
                                    symType.setArrayCount(var.getId().getArrayCount());
                                }

                                table.pushSymbol(var.getId().getName(), ReferenceType.VARIABLE, symType, var, actions,
                                        true);

                            }

                        }
                    }

                }
                scope.setHasFieldsLoaded(true);
            }
        }
    }
}
