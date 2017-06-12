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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.exceptions.InvalidTypeException;

public class ResultBuilderFromGenerics implements Builder<SymbolTable> {

    private List<Type> generics = null;
    private Method method = null;
    private Class<?> clazz = null;
    private SymbolType scope = null;
    private SymbolTable symbolTable;

    public ResultBuilderFromGenerics(List<Type> generics, Method method) {
        this.generics = generics;
        this.method = method;
    }

    public ResultBuilderFromGenerics(SymbolType scope, Method method, SymbolTable symbolTable) {
        this.scope = scope;
        this.symbolTable = symbolTable;
        this.method = method;
        if (method != null) {
            clazz = method.getDeclaringClass();
        }
    }

    public ResultBuilderFromGenerics(SymbolType scope, Class<?> clazz, SymbolTable symbolTable) {
        this.scope = scope;
        this.symbolTable = symbolTable;
        this.clazz = clazz;
    }

    @Override
    public SymbolTable build(SymbolTable genericsSymbolTable) throws Exception {

		if (generics != null) {
			SymbolType[] syms = ASTSymbolTypeResolver.getInstance().valueOf(generics);
			genericsSymbolTable.pushScope();
			updateTypeMappings(Arrays.asList(syms),method.getTypeParameters(), true, genericsSymbolTable,new HashSet<String>());
		} else if (scope != null) {
			String symbolName = scope.getClazz().getName();
			if (scope.getClazz().isMemberClass()) {
				symbolName = scope.getClazz().getCanonicalName();
			}
			Symbol<?> s = symbolTable.findSymbol(symbolName, ReferenceType.TYPE);
			if (s != null) {
				Scope scope = s.getInnerScope();
				if (scope != null) {
					Map<String, SymbolType> typeParams = scope.getTypeParams();

                    Scope newScope = new Scope();
                    for (String key : typeParams.keySet()) {
                        SymbolType aux = typeParams.get(key).cloneAsTypeVariable(key);
                        newScope.addSymbol(key, aux, null);
                    }

                    genericsSymbolTable.pushScope(newScope);
                }
            }

            updateTypeMapping(clazz, genericsSymbolTable, scope, false, new HashSet<String>());

            Scope newScope = new Scope();
            genericsSymbolTable.pushScope(newScope);

            if (method != null) {

                TypeVariable<?>[] typeParams = method.getTypeParameters();
                for (int i = 0; i < typeParams.length; i++) {
                    SymbolType aux = SymbolType.typeVariableOf(typeParams[i]);
                    genericsSymbolTable.pushSymbol(typeParams[i].getName(), ReferenceType.TYPE_PARAM, aux, null);
                }
            }

        } else {
            Scope newScope = new Scope();
            genericsSymbolTable.pushScope(newScope);
        }

        return genericsSymbolTable;
    }

    private void updateTypeMapping(java.lang.reflect.Type type, SymbolTable genericsSymbolTable,
            SymbolType parameterizedType, final boolean genericArgs, Set<String> processedTypeVariables)
            throws InvalidTypeException {
        if (parameterizedType != null) {
            if (type instanceof TypeVariable) {
                TypeVariable<?> tv = (TypeVariable<?>) type;
                String vname = tv.getName();
                if (!processedTypeVariables.contains(vname)) {
                    processedTypeVariables.add(vname);
                    Symbol<?> s = genericsSymbolTable.findSymbol(vname);
                    if (s != null) {
                        boolean isInTheTopScope = genericsSymbolTable.getScopes().peek().findSymbol(vname) != null;
                        SymbolType st = s.getType();
                        if (st != null) {
                            SymbolType refactor = s.getType().refactorToTypeVariable(vname, parameterizedType,
                                    genericArgs || isInTheTopScope);
                            s.setType(refactor);
                        } else {
                            s.setType(parameterizedType);
                        }

                    } else {
                        genericsSymbolTable.pushSymbol(vname, ReferenceType.TYPE, parameterizedType, null);

                    }

                    java.lang.reflect.Type[] bounds = tv.getBounds();
                    List<SymbolType> paramBounds = parameterizedType.getBounds();
                    if (paramBounds != null) {
                        for (int i = 0; i < paramBounds.size() && i < bounds.length; i++) {

                            updateTypeMapping(bounds[i], genericsSymbolTable, paramBounds.get(i), genericArgs,
                                    processedTypeVariables);

                        }
                    }
                }

            } else if (type instanceof WildcardType) {
                WildcardType wildcard = (WildcardType) type;
                java.lang.reflect.Type[] bounds = wildcard.getUpperBounds();
                List<SymbolType> paramBounds = parameterizedType.getBounds();
                if (paramBounds != null) {
                    for (int i = 0; i < bounds.length; i++) {
                        updateTypeMapping(bounds[i], genericsSymbolTable, paramBounds.get(i), genericArgs,
                                processedTypeVariables);
                    }
                }
                bounds = wildcard.getLowerBounds();
                paramBounds = parameterizedType.getLowerBounds();
                if (paramBounds != null) {
                    for (int i = 0; i < bounds.length; i++) {
                        updateTypeMapping(bounds[i], genericsSymbolTable, paramBounds.get(i), genericArgs,
                                processedTypeVariables);
                    }
                }

			} else if (type instanceof ParameterizedType) {
				updateTypeMappings(parameterizedType.getParameterizedTypes(), ( (ParameterizedType) type).getActualTypeArguments(), genericArgs, genericsSymbolTable,processedTypeVariables);


            } else if (type instanceof GenericArrayType) {
                GenericArrayType arrayType = (GenericArrayType) type;
                SymbolType st = parameterizedType.clone();
                st.setArrayCount(parameterizedType.getArrayCount() - 1);

                updateTypeMapping(arrayType.getGenericComponentType(), genericsSymbolTable, st, genericArgs,
                        processedTypeVariables);

            } else if (type instanceof Class) {

                Class<?> clazz = (Class<?>) type;
                Map<String, SymbolType> updateMapping = new LinkedHashMap<String, SymbolType>();

                SymbolType rewrittenType = SymbolType.valueOf(clazz, parameterizedType, updateMapping, null);
                java.lang.reflect.TypeVariable[] tparams = clazz.getTypeParameters();
                List<SymbolType> paramTypes = rewrittenType.getParameterizedTypes();
                if (paramTypes != null) {
                    Iterator<SymbolType> it = paramTypes.iterator();
                    HashSet<String> processed = new HashSet<String>();
                    for (int i = 0; i < tparams.length && it.hasNext(); i++) {
                        SymbolType st = it.next();
                        if (st != null) {
                            updateTypeMapping(tparams[i], genericsSymbolTable,
                                    st.cloneAsTypeVariable(tparams[i].getName()), true, processed);
                        }
                    }
                }

            }
        }
    }

	private void updateTypeMappings(final List<SymbolType> paramTypeParams, final java.lang.reflect.Type[] typeArgs, boolean genericArgs, SymbolTable genericsSymbolTable, Set<String> processedTypeVariables) throws InvalidTypeException {
		if (paramTypeParams != null) {

            for (int i = 0; i < typeArgs.length; i++) {
                SymbolType st = null;
                if (i < paramTypeParams.size()) {
                    st = paramTypeParams.get(i);
                }
                updateTypeMapping(typeArgs[i], genericsSymbolTable, st, genericArgs, processedTypeVariables);
            }
        }
	}
}
