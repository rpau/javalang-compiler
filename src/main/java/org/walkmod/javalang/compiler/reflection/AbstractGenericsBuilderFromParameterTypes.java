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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.NullLiteralExpr;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.exceptions.InvalidTypeException;

public abstract class AbstractGenericsBuilderFromParameterTypes {
    private Map<String, SymbolType> typeMapping;

    private SymbolTable typeParamsSymbolTable;

    private List<Expression> args;

    private SymbolType[] typeArgs;

    private Type[] types;

    private SymbolTable symTable;

    public AbstractGenericsBuilderFromParameterTypes(Map<String, SymbolType> typeMapping, List<Expression> args,
            SymbolType[] typeArgs, SymbolTable symTable) {
        this.typeMapping = typeMapping;
        this.args = args;
        this.typeArgs = typeArgs;
        this.symTable = symTable;
    }

    public AbstractGenericsBuilderFromParameterTypes(Map<String, SymbolType> typeMapping, SymbolTable symTable) {
        this.typeMapping = typeMapping;
        this.symTable = symTable;
    }

    public SymbolTable getSymbolTable() {
        return symTable;
    }

    public AbstractGenericsBuilderFromParameterTypes() {}

    public void setArgs(List<Expression> args) {
        this.args = args;
    }

    public List<Expression> getArgs() {
        return args;
    }

    public void setTypeArgs(SymbolType[] typeArgs) {
        this.typeArgs = typeArgs;
    }

    private SymbolType getType(ClassExpr classExpr) {
        final SymbolData symbolData = classExpr.getType().getSymbolData();
        if (symbolData instanceof SymbolType) {
            return (SymbolType) symbolData;
        } else {
            String name = classExpr.getType().toString();
            SymbolType type = symTable.getType(name, ReferenceType.TYPE);
            if (type == null) {
                Class<?> clazz;
                try {
                    clazz = TypesLoaderVisitor.getClassLoader().loadClass(name);

                    String className = clazz.getName();
                    type = new SymbolType(className);
                } catch (ClassNotFoundException e) {
                    // a name expression could be "org.walkmod.A" and this node
                    // could be "org.walkmod"

                }
            }
            return type;
        }
    }

    public SymbolTable getTypeParamsSymbolTable() {
        if (typeParamsSymbolTable == null) {
            typeParamsSymbolTable = new SymbolTable();
            // we store in the symbol table the generics of the parameterized
            // types of the implicit object
            Scope methodCallScope = new Scope();
            typeParamsSymbolTable.pushScope(methodCallScope);
            Map<String, SymbolType> typeMapping = getTypeMapping();
            if (typeMapping != null) {
                Set<String> parameterizedTypeNames = typeMapping.keySet();
                for (String typeName : parameterizedTypeNames) {
                    SymbolType st = typeMapping.get(typeName).cloneAsTypeVariable(typeName);
                    typeParamsSymbolTable.pushSymbol(typeName, ReferenceType.TYPE, st, null);
                }
            }
        }
        return typeParamsSymbolTable;
    }

    public void loadTypeMappingFromTypeArgs() throws Exception {

        loadTypeMappingFromTypeArgs(getTypeParamsSymbolTable());

    }

    public void loadTypeMappingFromTypeArgs(SymbolTable symbolTable) throws Exception {
        symbolTable.pushScope();
        java.lang.reflect.Type[] types = getTypes();

        int pos = 0;
        for (Type type : types) {
            if (type instanceof ParameterizedType) {

                Type aux = ((ParameterizedType) type).getRawType();
                if (aux instanceof Class) {
                    if (((Class<?>) aux).getName().equals("java.lang.Class")) {
                        Type[] targs = ((ParameterizedType) type).getActualTypeArguments();
                        for (Type targ : targs) {
                            String letter = targ.toString();
                            if (!"?".equals(letter)) {
                                if (pos < args.size()) {
                                    Expression e = args.get(pos);
                                    // String className = "";
                                    if (e instanceof ClassExpr) {

                                        SymbolType eType = getType(((ClassExpr) e));

                                        // Is this sufficient and correct?
                                        final String pureLetter = letter.replace("? extends ", "");
                                        symbolTable.pushSymbol(pureLetter, ReferenceType.TYPE, eType, e);

                                    } else {
                                        if (!(e instanceof NullLiteralExpr)) {
                                            List<SymbolData> params = e.getSymbolData().getParameterizedTypes();
                                            if (params != null && !params.isEmpty()) {
                                                SymbolType eType = (SymbolType) params.get(0);
                                                symbolTable.pushSymbol(letter, ReferenceType.TYPE, eType, e);
                                            }
                                        }
                                    }
                                } else {
                                    symbolTable.pushSymbol(letter, ReferenceType.TYPE, new SymbolType(Object.class),
                                            null);
                                }
                            }
                        }
                    }
                }
            }
            pos++;
        }
    }

    public void build() throws Exception {
        buildTypeParamsTypes();
        closeTypeMapping();
    }

    public void buildTypeParamsTypes() throws Exception {

        loadTypeMappingFromTypeArgs();

        for (int i = 0; i < types.length && i < typeArgs.length; i++) {
            typeMappingUpdate(types[i], typeArgs[i]);
        }
    }

    public void closeTypeMapping() {
        ArrayList<Symbol<?>> symbols = typeParamsSymbolTable.findSymbolsByType();
        ListIterator<Symbol<?>> it = symbols.listIterator(symbols.size());
        while (it.hasPrevious()) {
            Symbol<?> s = it.previous();
            typeMapping.put(s.getName(), s.getType());
        }
    }

    private void typeMappingUpdate(Type type, SymbolType typeArg) throws InvalidTypeException {
        if (type instanceof TypeVariable) {
            String name = ((TypeVariable<?>) type).getName();
            Symbol<?> s = typeParamsSymbolTable.findSymbol(name, ReferenceType.TYPE, ReferenceType.TYPE_PARAM);

            SymbolType st = null;
            if (s != null) {
                st = s.getType();
            }
            if ((s == null || st == null)
                    || (st.isTemplateVariable() && s.getReferenceType().equals(ReferenceType.TYPE_PARAM))
                    || (Object.class.equals(st.getClazz()) && !s.getReferenceType().equals(ReferenceType.TYPE))) {
                typeParamsSymbolTable.pushSymbol(name, ReferenceType.TYPE, typeArg, null);
            } else {
                if (s.getReferenceType().equals(ReferenceType.TYPE)) {
                    SymbolType aux = (SymbolType) st.merge(typeArg);
                    s.setType(aux);
                } else {
                    // it is a type param, so it is not part of the implicit
                    // object
                    typeParamsSymbolTable.pushSymbol(name, ReferenceType.TYPE, typeArg, null);
                }
            }

        } else if (type instanceof ParameterizedType) {

            ParameterizedType paramType = (ParameterizedType) type;
            Type[] args = paramType.getActualTypeArguments();
            if (typeArg != null) {

                Map<String, SymbolType> updateMapping = new LinkedHashMap<String, SymbolType>();

                // class A<T> implements B<String,T>, if type is B<K,M>, we need
                // to know that K is String and M whatever has the typeArg
                SymbolType rewrittenType = SymbolType.valueOf(paramType.getRawType(), typeArg, updateMapping, null);

                List<SymbolType> callParams = typeArg.getParameterizedTypes();
                String name = rewrittenType.getName();
                if (name == null || !"java.lang.Class".equals(name)) {

                    List<SymbolType> rewrittenParams = rewrittenType.getParameterizedTypes();
                    if (rewrittenParams != null) {
                        Iterator<SymbolType> itP = rewrittenParams.iterator();
                        Iterator<SymbolType> auxIt = null;
                        if (callParams != null) {
                            auxIt = callParams.iterator();

                        }
                        int i = 0;
                        while (itP.hasNext() && i < args.length) {
                            SymbolType st = itP.next();
                            if ((st.getName() == null || Object.class.equals(st.getClazz()))) {
                                if (auxIt != null && auxIt.hasNext()) {
                                    // it is defined by the parameters of the
                                    // implicit object.Eg: (List<String> a =
                                    // null;
                                    // a.add("hello"));
                                    typeMappingUpdate(args[i], auxIt.next());
                                } else {
                                    typeMappingUpdate(args[i], st);
                                }
                            } else {
                                // it is defined by the rewritten type eg: (A
                                // implements B<String>) => st = String
                                typeMappingUpdate(args[i], st);
                            }
                            i++;
                        }
                    }
                }

            }

        } else if (type instanceof WildcardType) {
            if (typeArg != null) {
                WildcardType wildcardType = (WildcardType) type;

                // ? super T -> upper is '?'. So it is Object
                Type[] upper = wildcardType.getUpperBounds();
                List<SymbolType> bounds = typeArg.getBounds();
                if (bounds == null) {
                    bounds = new LinkedList<SymbolType>();
                    bounds.add(typeArg);
                }

                for (int i = 0; i < upper.length; i++) {
                    typeMappingUpdate(upper[i], bounds.get(i));
                }
                // ? super T -> lower is 'T'. The type can contain lower bounds
                // or is a resolved type
                Type[] lower = wildcardType.getLowerBounds();
                List<SymbolType> lowerBounds = typeArg.getLowerBounds();
                if (lowerBounds != null) {
                    bounds = lowerBounds;
                }
                if (bounds != null) {
                    for (int i = 0; i < lower.length; i++) {
                        typeMappingUpdate(lower[i], bounds.get(i));
                    }
                }
            }
        } else if (type instanceof GenericArrayType) {
            if (typeArg != null) {
                GenericArrayType arrayType = (GenericArrayType) type;
                SymbolType aux = typeArg.clone();
                if (typeArg.getArrayCount() > 0) {
                    aux.setArrayCount(typeArg.getArrayCount() - 1);
                }

                typeMappingUpdate(arrayType.getGenericComponentType(), aux);
            }
        } else if (type instanceof Class<?>) {
            if (typeArg != null) {
                Class<?> clazz = (Class<?>) type;
                Map<String, SymbolType> updateMapping = new LinkedHashMap<String, SymbolType>();

                // class A<T> implements B<String,T>, if type is B<K,M>, we need
                // to know that K is String and M whatever has the typeArg
                SymbolType rewrittenType = SymbolType.valueOf(clazz, typeArg, updateMapping, null);
                TypeVariable[] tvs = clazz.getTypeParameters();
                List<SymbolType> paramTypes = rewrittenType.getParameterizedTypes();
                if (paramTypes != null) {
                    Iterator<SymbolType> it = paramTypes.iterator();
                    for (int i = 0; i < tvs.length && it.hasNext(); i++) {
                        SymbolType st = it.next();
                        if (st != null) {
                            typeMappingUpdate(tvs[i], st);
                        }
                    }
                }
            }
        }
    }

    public void setTypeMapping(Map<String, SymbolType> typeMapping) {
        this.typeMapping = typeMapping;
    }

    public Map<String, SymbolType> getTypeMapping() {
        return typeMapping;
    }

    public void setTypes(Type[] types) {
        this.types = types;
    }

    public Type[] getTypes() {
        return types;
    }

}
