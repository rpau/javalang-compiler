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
package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.ast.expr.SuperExpr;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleMethodReferencePredicate<A, T> extends CompatibleArgsPredicate<T> implements Predicate<T> {

    private MethodReferenceExpr expression = null;

    private VoidVisitor<A> typeResolver;

    private A ctx;

    private SymbolType sd;

    private List<Method> methodCallCandidates = null;

    private SymbolTable symTable;

    private Class<?> thisClass;

    private Method[] methodsArray = null;

    public CompatibleMethodReferencePredicate(MethodReferenceExpr expression, VoidVisitor<A> typeResolver, A ctx,
            Map<String, SymbolType> mapping, SymbolTable symTable) {
        this.expression = expression;
        this.typeResolver = typeResolver;
        this.symTable = symTable;
        this.ctx = ctx;
        setTypeMapping(mapping);
        thisClass = symTable.getType("this").getClazz();
    }

    @Override
    public boolean filter(T elem) throws Exception {
        int elemParameterCount = 0;
        Type[] genericParameterTypes = null;
        Class<?> declaringClass = null;
        if (elem instanceof Method) {
            elemParameterCount = ((Method) elem).getParameterTypes().length;
            genericParameterTypes = ((Method) elem).getGenericParameterTypes();
            declaringClass = ((Method) elem).getDeclaringClass();
        } else if (elem instanceof Constructor) {
            elemParameterCount = ((Constructor<?>) elem).getParameterTypes().length;
            genericParameterTypes = ((Constructor<?>) elem).getGenericParameterTypes();
            declaringClass = ((Constructor<?>) elem).getDeclaringClass();
        } else {
            return false;
        }
        Object equivalentMethod = null;

        sd = (SymbolType) expression.getScope().getSymbolData();
        if (sd == null) {
            expression.getScope().accept(typeResolver, ctx);
            sd = (SymbolType) expression.getScope().getSymbolData();

            if (!expression.getIdentifier().equals("new")) {

                if (methodsArray == null) {
                    Set<Method> methods = MethodInspector.getVisibleMethods(sd.getClazz(), thisClass);
                    methodsArray = new Method[methods.size()];
                    methods.toArray(methodsArray);
                    ExecutableSorter sorter = new ExecutableSorter();

                    List<Method> sortedMethods = sorter.sort(methodsArray, null);
                    sortedMethods.toArray(methodsArray);
                }

                ArrayFilter<Method> filter = new ArrayFilter<Method>(methodsArray);

                filter.appendPredicate(new MethodsByNamePredicate(expression.getIdentifier()));
                methodCallCandidates = filter.filter();
            }

        }
        boolean found = false;
        if (!expression.getIdentifier().equals("new")) {
            Iterator<Method> it = methodCallCandidates.iterator();

            while (it.hasNext() && !found) {

                Method md = it.next();
                int mdParameterCount = md.getParameterTypes().length;

                Map<String, SymbolType> typeMapping = getTypeMapping();
                FunctionalGenericsBuilder<MethodReferenceExpr> builder =
                        new FunctionalGenericsBuilder<MethodReferenceExpr>(md, typeResolver, typeMapping);
                builder.build(expression);
                SymbolType[] args = builder.getArgs();
                if (!Modifier.isStatic(md.getModifiers())) {

                    if (mdParameterCount == elemParameterCount - 1) {
                        // may be, just taking into account the type variables,
                        // it matches
                        SymbolType[] genericArgs = new SymbolType[genericParameterTypes.length];
                        boolean allAreGenerics = true;
                        for (int i = 0; i < genericParameterTypes.length && allAreGenerics; i++) {
                            if (genericParameterTypes[i] instanceof TypeVariable<?>) {
                                TypeVariable<?> td = (TypeVariable<?>) genericParameterTypes[i];
                                genericArgs[i] = typeMapping.get(td.getName());
                                allAreGenerics = genericArgs[i] != null;
                            } else {
                                allAreGenerics = false;
                            }
                        }
                        if (allAreGenerics) {
                            setTypeArgs(genericArgs);
                            found = super.filter(elem);
                        } else {
                            // the implicit parameter is an argument of the
                            // invisible
                            // lambda
                            SymbolType[] staticArgs = new SymbolType[args.length + 1];
                            for (int i = 0; i < args.length; i++) {
                                staticArgs[i + 1] = args[i];
                            }
                            staticArgs[0] = (SymbolType) sd;
                            args = staticArgs;
                            setTypeArgs(args);
                            found = super.filter(elem);
                        }

                    } else {

                        Expression scope = expression.getScope();

                        SymbolType stype = (SymbolType) scope.getSymbolData();
                        boolean isField = stype.getField() != null;
                        boolean isVariable = false;
                        boolean isSuper = scope instanceof SuperExpr;
                        if (!isField && !isSuper) {
                            String name = scope.toString();
                            isVariable = (symTable.findSymbol(name, ReferenceType.VARIABLE) != null);
                        }
                        // it is a variable
                        if ((isField || isVariable || isSuper) && mdParameterCount == elemParameterCount) {
                            setTypeArgs(args);
                            found = super.filter(elem);

                        }
                    }

                } else if (mdParameterCount == elemParameterCount) {
                    setTypeArgs(args);
                    found = super.filter(elem);
                }
                if (found) {
                    equivalentMethod = md;
                }

            }
        } else {
            Constructor<?>[] constructors = sd.getClazz().getDeclaredConstructors();

            for (int i = 0; i < constructors.length && !found; i++) {

                FunctionalGenericsBuilder<MethodReferenceExpr> builder =
                        new FunctionalGenericsBuilder<MethodReferenceExpr>(constructors[i], typeResolver,
                                getTypeMapping());
                builder.build(expression);

                SymbolType[] args = builder.getArgs();
                setTypeArgs(args);

                found = super.filter(elem);
                if (found) {
                    equivalentMethod = constructors[i];
                }
            }
        }
        if (found && elem instanceof Method) {

            Map<String, SymbolType> mapping = getTypeMapping();
            SymbolType realResultType = null;
            if (equivalentMethod instanceof Method) {

                // R apply(T t); -> Quote::parse ( T= String y R = Quote)
                realResultType = SymbolType.valueOf((Method) equivalentMethod, mapping);

                java.lang.reflect.Type genericReturnType = ((Method) elem).getGenericReturnType();
                resolveTypeMapping(genericReturnType, realResultType, mapping);
            }
            realResultType = SymbolType.valueOf(declaringClass, mapping);
            expression.setSymbolData(realResultType);

            SymbolType st = SymbolType.valueOf((Method) elem, mapping);
            expression.setReferencedMethodSymbolData(st);
            expression.setReferencedArgsSymbolData(getTypeArgs());
        }
        return found;
    }

    private void resolveTypeMapping(java.lang.reflect.Type type, SymbolType reference,
            Map<String, SymbolType> mapping) {

        if (type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            SymbolType st = mapping.get(tv.getName());
            if (st == null || "java.lang.Object".equals(st.getName())) {
                mapping.put(tv.getName(), reference);
            } else {
                mapping.put(tv.getName(), (SymbolType) reference.merge(st));
            }
        }
    }
}
