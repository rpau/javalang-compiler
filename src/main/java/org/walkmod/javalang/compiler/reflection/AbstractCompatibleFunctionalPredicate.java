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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.PreviousPredicateAware;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public abstract class AbstractCompatibleFunctionalPredicate<T> implements PreviousPredicateAware {

    private VoidVisitor<T> typeResolver;
    private List<Expression> args;
    private T ctx = null;
    private SymbolType scope;
    private Map<String, SymbolType> typeMapping;

    private Class<?>[] params;
    private boolean isVarArgs;
    private SymbolTable symTable;
    private SymbolType[] calculatedTypeArgs;

    private AbstractCompatibleArgsPredicate previousPredicate;

    public AbstractCompatibleFunctionalPredicate(SymbolType scope, VoidVisitor<T> typeResolver, List<Expression> args,
            T ctx, SymbolTable symTable, AbstractCompatibleArgsPredicate previousPredicate,
            SymbolType[] calculatedTypeArgs) {
        this.typeResolver = typeResolver;
        this.args = args;
        this.ctx = ctx;
        this.scope = scope;
        this.symTable = symTable;
        this.previousPredicate = previousPredicate;
        this.calculatedTypeArgs = calculatedTypeArgs;
    }

    private void createEquivalenceMapping(java.lang.reflect.Type classToInspect,
            java.lang.reflect.Type receivedParameter, Map<String, List<String>> equivalences) {

        if (classToInspect instanceof Class<?>) {
            Class<?> clazz1 = (Class<?>) classToInspect;
            TypeVariable<?>[] params1 = clazz1.getTypeParameters();
            if (receivedParameter instanceof Class<?>) {
                Class<?> clazz2 = (Class<?>) receivedParameter;
                TypeVariable<?>[] params2 = clazz2.getTypeParameters();
                for (int i = 0; i < params1.length; i++) {
                    createEquivalenceMapping(params1[i], params2[i], equivalences);
                }
            } else if (receivedParameter instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) receivedParameter;
                java.lang.reflect.Type[] typeArgs = ptype.getActualTypeArguments();
                for (int i = 0; i < params1.length; i++) {
                    createEquivalenceMapping(params1[i], typeArgs[i], equivalences);
                }
            }
        } else if (classToInspect instanceof TypeVariable) {
            TypeVariable<?> tv1 = (TypeVariable<?>) classToInspect;
            if (receivedParameter instanceof TypeVariable) {
                TypeVariable<?> tv2 = (TypeVariable<?>) receivedParameter;
                List<String> list = equivalences.get(tv2.getName());
                if (list == null) {
                    list = new LinkedList<String>();
                }
                list.add(tv1.getName());
                equivalences.put(tv2.getName(), list);
            } else if (receivedParameter instanceof WildcardType) {
                WildcardType wildcard = (WildcardType) receivedParameter;
                java.lang.reflect.Type[] bounds = wildcard.getUpperBounds();
                for (int i = 0; i < bounds.length; i++) {
                    createEquivalenceMapping(classToInspect, bounds[i], equivalences);
                }
                bounds = wildcard.getLowerBounds();
                for (int i = 0; i < bounds.length; i++) {
                    createEquivalenceMapping(classToInspect, bounds[i], equivalences);
                }
            }
        }

    }

    public Map<String, SymbolType> createMapping(Class<?> classToInspect, java.lang.reflect.Type interfaceToInspect,
            SymbolType inferredArg, Class<?> declaringClass) throws Exception {
        Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();

        // infers the type mapping of the scope
        GenericsBuilderFromClassParameterTypes builder =
                new GenericsBuilderFromClassParameterTypes(typeMapping, scope, symTable);
        builder.build(declaringClass);
        typeMapping = builder.getTypeMapping();

        // we want to infer the type mapping of the class that contains the
        // method that
        // is represented by a lambda expression using the type variables that
        // appear in the generic parameter
        Map<String, SymbolType> update = new HashMap<String, SymbolType>();

        Map<String, List<String>> equivalences = new HashMap<String, List<String>>();
        createEquivalenceMapping(classToInspect, interfaceToInspect, equivalences);
        Set<String> keys = equivalences.keySet();

        for (String key : keys) {
            if (typeMapping.containsKey(key)) {
                List<String> list = equivalences.get(key);
                for (String key2 : list) {
                    update.put(key2, typeMapping.get(key));
                }
            }
        }

        keys = typeMapping.keySet();
        for (String key : keys) {
            if (!equivalences.containsKey(key)) {
                update.put(key, typeMapping.get(key));
            }
        }

        if (inferredArg != null) {
            // acording the template variables of the representative class,
            // which are the corresponding SymbolTypes?
            Map<String, SymbolType> paramsTypeMapping = new HashMap<String, SymbolType>();
            SymbolType.valueOf(classToInspect, inferredArg, paramsTypeMapping, null);

            // we delete the inferred Object classes
            Map<String, SymbolType> subset = new HashMap<String, SymbolType>();
            keys = paramsTypeMapping.keySet();
            for (String key : keys) {
                SymbolType st1 = paramsTypeMapping.get(key);
                Class<?> clazz = st1.getClazz();
                if (!Object.class.equals(clazz)) {
                    subset.put(key, st1);
                }

            }
            // we override the values defined by the generics that the scope
            // gives.
            update.putAll(subset);

        }

        return update;
    }

    public boolean filter(LambdaExpr lambda, Class<?> interfaceToInspect,
            java.lang.reflect.Type equivalentTypeToInspect, SymbolType inferredArg, Class<?> declaringClass)
            throws Exception {

        boolean found = false;

        Method[] methods = interfaceToInspect.getMethods();

        ArrayFilter<Method> filter = new ArrayFilter<Method>(methods);

        Map<String, SymbolType> typeMapping =
                createMapping(interfaceToInspect, equivalentTypeToInspect, inferredArg, declaringClass);
        CompatibleLambdaArgsPredicate predArgs = new CompatibleLambdaArgsPredicate(lambda);
        predArgs.setTypeMapping(typeMapping);

        filter.appendPredicate(new LambdaParamsTypeResolver(lambda, typeResolver, typeMapping))
                .appendPredicate(new AbstractMethodsPredicate()).appendPredicate(predArgs).appendPredicate(
                        new CompatibleLambdaResultPredicate<T>(lambda, typeResolver, ctx, typeMapping, symTable));
        found = filter.filterOne() != null;
        return found;
    }

    public boolean filter(MethodReferenceExpr methodRef, Class<?> interfaceToInspect,
            java.lang.reflect.Type equivalentTypeToInspect, SymbolType inferredArg, Class<?> declaringClass)
            throws Exception {

        boolean found = false;
        Map<String, SymbolType> aux =
                createMapping(interfaceToInspect, equivalentTypeToInspect, inferredArg, declaringClass);
        if (typeMapping != null) {
            aux.putAll(typeMapping);
        }
        CompatibleMethodReferencePredicate<T, Method> predArgs =
                new CompatibleMethodReferencePredicate<T, Method>(methodRef, typeResolver, ctx, aux, symTable);

        Set<Method> methodsSet =
                MethodInspector.getVisibleMethods(interfaceToInspect, symTable.getType("this").getClazz());
        Method[] methods = new Method[methodsSet.size()];
        methodsSet.toArray(methods);

        ArrayFilter<Method> filter = new ArrayFilter<Method>(methods);
        filter.appendPredicate(new AbstractMethodsPredicate());
        filter.appendPredicate(predArgs);
        found = filter.filterOne() != null;

        return found;
    }

    public boolean filter(Method method) throws Exception {
        return filter(method.getGenericParameterTypes(), method.getDeclaringClass());
    }

    public boolean filter(Constructor<?> method) throws Exception {
        return filter(method.getGenericParameterTypes(), method.getDeclaringClass());
    }

    public boolean filter(java.lang.reflect.Type[] genericTypes, Class<?> declaringClass) throws Exception {

        boolean found = false;
        boolean containsLambda = false;
        SymbolType[] inferredTypes = null;
        if (previousPredicate != null) {
            inferredTypes = previousPredicate.getInferredMethodArgs();
        }
        if (args != null && !args.isEmpty()) {
            Iterator<Expression> it = args.iterator();
            int i = 0;
            while (it.hasNext() && !found) {
                SymbolType argType = null;
                if (inferredTypes != null) {
                    argType = inferredTypes[i];
                }
                Expression current = it.next();
                if (current instanceof LambdaExpr || current instanceof MethodReferenceExpr) {

                    containsLambda = true;
                    Class<?> interfaceToInspect = null;

                    if (inferredTypes[i].getClazz().isInterface()) {
                        interfaceToInspect = inferredTypes[i].getClazz();

                    } else if (isVarArgs && i == params.length - 1) {
                        Class<?> componentType = inferredTypes[i].getClazz();
                        if (componentType.isInterface()) {
                            interfaceToInspect = componentType;
                        }
                    }
                    if (interfaceToInspect != null) {
                        if (current instanceof LambdaExpr) {
                            found = filter((LambdaExpr) current, interfaceToInspect, genericTypes[i], argType,
                                    declaringClass);
                            if (found) {
                                calculatedTypeArgs[i] = (SymbolType) current.getSymbolData();
                            }
                        } else {
                            found = filter((MethodReferenceExpr) current, interfaceToInspect, genericTypes[i], argType,
                                    declaringClass);
                            if (found) {
                                calculatedTypeArgs[i] = (SymbolType) current.getSymbolData();
                            }
                        }
                    }

                }
                if (i < params.length - 1) {
                    i++;
                }

            }
        }

        return (found && containsLambda) || !containsLambda;
    }

    public void setTypeMapping(Map<String, SymbolType> typeMapping) {
        this.typeMapping = typeMapping;
    }

    public Class<?>[] getParams() {
        return params;
    }

    public void setParams(Class<?>[] params) {
        this.params = params;
    }

    public boolean isVarArgs() {
        return isVarArgs;
    }

    public void setVarArgs(boolean isVarArgs) {
        this.isVarArgs = isVarArgs;
    }

    public void setPreviousPredicate(Predicate<?> pred) {
        if (pred instanceof AbstractCompatibleArgsPredicate) {
            this.previousPredicate = (AbstractCompatibleArgsPredicate) pred;
        }
    }

}
