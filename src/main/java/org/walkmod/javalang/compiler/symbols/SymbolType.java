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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.walkmod.javalang.ast.ConstructorSymbolData;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.types.TypeNotFoundException;
import org.walkmod.javalang.compiler.types.Types;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.compiler.JavaLangCompilerContracts;
import org.walkmod.javalang.exceptions.InvalidTypeException;

import static java.util.Arrays.asList;

public class SymbolType implements SymbolData, MethodSymbolData, FieldSymbolData, ConstructorSymbolData {

    /**
     * Marker to aid symbol resolution.
     * Incomplete, avoid making public.
     */
    private enum Marker {
        None,
        AnonymousClass,
        EnumConstantClass,
        /**
         * @see {@link #markDisabledCode()}
         */
        DisabledAnonymousClass
    }

    private final Marker marker;

    private String name;

    private List<SymbolType> upperBounds = null;

    private List<SymbolType> lowerBounds = null;

    private List<SymbolType> parameterizedTypes;

    private int arrayCount = 0;

    private String typeVariable = null;

    private Class<?> clazz;

    private Method method = null;

    private Field field = null;

    private Constructor<?> constructor = null;

    private SymbolType(Marker marker, String name) {
        this.marker = marker;
        this.name = name;
        if (marker == null) {
            throw new IllegalArgumentException("marker");
        }
    }

    public SymbolType() {
        this(Marker.None, null);
    }

    private SymbolType(List<SymbolType> upperBounds, String typeVariable) {
        this(upperBounds, (List<SymbolType>) null);
        this.typeVariable = typeVariable;
    }

    public SymbolType(List<SymbolType> upperBounds) {
        this(upperBounds, (List<SymbolType>) null);
    }

    private SymbolType(int arrayCount, List<SymbolType> upperBounds, List<SymbolType> lowerBounds,
            String typeVariable) {
        this(upperBounds, lowerBounds);
        this.typeVariable = typeVariable;
        this.arrayCount = arrayCount;
    }

    public SymbolType(List<SymbolType> upperBounds, List<SymbolType> lowerBounds) {
        this(Marker.None, name(upperBounds, lowerBounds));
        this.upperBounds = upperBounds;
        this.lowerBounds = lowerBounds;
        if (upperBounds != null) {
            if (!upperBounds.isEmpty()) {
                clazz = upperBounds.get(0).getClazz();
            }
        } else if (lowerBounds != null) {
            clazz = Object.class;
        }
    }

    private static String name(List<SymbolType> upperBounds, List<SymbolType> lowerBounds) {
        String name = null;
        if (upperBounds != null) {
            if (!upperBounds.isEmpty()) {
                name = upperBounds.get(0).getName();
            }
        } else if (lowerBounds != null) {
            name = "java.lang.Object";
        }
        return name;
    }

    public SymbolType(String name, List<SymbolType> upperBounds) {
        this(Marker.None, name);
        if (upperBounds != null) {
            this.upperBounds = upperBounds;
            if (!upperBounds.isEmpty()) {
                clazz = upperBounds.get(0).getClazz();
            }
        }
    }

    private SymbolType(String name, int arrayCount, List<SymbolType> upperBounds, List<SymbolType> lowerBounds,
            String typeVariable) {
        this(name, upperBounds, lowerBounds);
        this.typeVariable = typeVariable;
        this.arrayCount = arrayCount;
    }

    public SymbolType(String name, List<SymbolType> upperBounds, List<SymbolType> lowerBounds) {
        this(Marker.None, name);
        this.lowerBounds = lowerBounds;
        if (upperBounds != null) {
            this.upperBounds = upperBounds;
            if (!upperBounds.isEmpty()) {
                clazz = upperBounds.get(0).getClazz();
            }
        }
    }

    private SymbolType(Class<?> clazz, String typeVariable) {
        this(clazz);
        this.typeVariable = typeVariable;
    }

    public SymbolType(Class<?> clazz) {
        this(Marker.None, clazz.getName());
        setClazz(clazz);
        setParameterizedTypes(resolveGenerics(clazz));
        setArrayCount(resolveDimmensions(clazz));
    }

    private SymbolType(String name, String typeVariable) {
        this(Marker.None, name);
        this.typeVariable = typeVariable;
    }

    public SymbolType(String name) {
        this(Marker.None, name);
    }

    private SymbolType(String name, int arrayCount) {
        this(Marker.None, name);
        this.arrayCount = arrayCount;
    }

    private int resolveDimmensions(Class<?> clazz) {
        if (clazz.isArray()) {
            Class<?> component = clazz.getComponentType();
            return resolveDimmensions(component) + 1;
        }
        return 0;
    }

    public boolean hasBounds() {
        return upperBounds != null;
    }

    public List<SymbolType> getBounds() {
        return upperBounds;
    }

    private List<SymbolType> resolveGenerics(Class<?> clazz) {
        List<SymbolType> result = null;
        TypeVariable<?>[] typeParams = clazz.getTypeParameters();
        if (typeParams.length > 0) {

            for (TypeVariable<?> td : typeParams) {
                Type[] bounds = td.getBounds();
                for (Type bound : bounds) {
                    try {
                        SymbolType st = valueOf(bound, null);
                        if (result == null) {
                            result = new LinkedList<>();
                        }
                        result.add(st);
                    } catch (InvalidTypeException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return result;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getName() {
        return name;
    }

    /** @deprecated do not use directly but via specialized constructors of factory methods */
    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    public List<SymbolType> getLowerBounds() {
        return lowerBounds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SymbolType> getParameterizedTypes() {
        if (parameterizedTypes == null) {
            if (upperBounds != null && !upperBounds.isEmpty()) {
                List<SymbolType> params = upperBounds.get(0).getParameterizedTypes();
                if (params != null && name != null) {
                    Iterator<SymbolType> it = params.iterator();
                    List<SymbolType> aux = null;
                    while (it.hasNext()) {
                        SymbolType st = it.next();

                        // if (!name.equals(st.getName())) {
                        if (aux == null) {
                            aux = new LinkedList<>();
                        }
                        aux.add(st);

                        // }
                    }
                    return aux;
                }
                return null;

            }
        }
        return parameterizedTypes;
    }

    public void setParameterizedTypes(List<SymbolType> parameterizedTypes) {
        /*
           this invariant is considered to be correct but some code needs to be fixed that
           breaks the invariant before general use.
         */
        if (JavaLangCompilerContracts.CHECK_EXPERIMENTAL_INVARIANT_ENABLED) {
            if (parameterizedTypes != null) {
                final Class<?> clazz = getClazz();
                if (clazz != null) {
                    final TypeVariable<? extends Class<?>>[] tps = clazz.getTypeParameters();
                    if (tps.length != parameterizedTypes.size()) {
                        throw new IllegalArgumentException("[" + this + "]: symbol type invariant violation"
                            + "\n    # of type arguments (" + parameterizedTypes.size() + ")"
                                + " does not match # of type parameters (" + tps.length + ")"
                                + "\n        args  : " + parameterizedTypes
                                + "\n        params: " + asList(tps)
                        );
                    }
                }
            }
        }
        this.parameterizedTypes = parameterizedTypes != null
                ? Collections.unmodifiableList(new ArrayList<>(parameterizedTypes))
                : null;
    }

    public int getArrayCount() {
        return arrayCount;
    }

    public void setArrayCount(int arrayCount) {
        this.arrayCount = arrayCount;
    }

    public boolean isTemplateVariable() {
        return typeVariable != null;
    }

    /** @deprecated use factory methods "templateVariableOf" instead */
    public void setTemplateVariable(String templateVariable) {
        this.typeVariable = templateVariable;
    }

    public String getTemplateVariable() {
        return typeVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SymbolType) {
            SymbolType aux = (SymbolType) o;
            String auxName = aux.getName();
            boolean equalName = name != null && auxName != null && name.equals(auxName);
            equalName = equalName
                    || (isTemplateVariable() && aux.isTemplateVariable() && typeVariable.equals(aux.typeVariable));
            return equalName && arrayCount == aux.getArrayCount();
        }
        return false;
    }

    private Map<String, SymbolType> getTypeMappingVariables() {
        Map<String, SymbolType> result = null;
        List<SymbolType> paramTypes = getParameterizedTypes();
        if (paramTypes != null) {
            TypeVariable<?>[] vars = getClazz().getTypeParameters();
            result = new HashMap<>();
            for (int i = 0; i < vars.length; i++) {
                result.put(vars[i].getName(), paramTypes.get(i));
            }
        }
        return result;
    }

    private boolean isArrayCountCompatible(SymbolType other) {
        boolean isCompatible = true;
        if (!isObjectClass(getClazz()) || getArrayCount() > 0) {
            isCompatible = getArrayCount() == other.getArrayCount()
                    || (getArrayCount() < other.getArrayCount() && isObjectClass(getClazz()));
        }
        return isCompatible;
    }

    private boolean isLowerBoundsCompatible(SymbolType other) {
        boolean isCompatible = true;
        if (lowerBounds != null) {
            Iterator<SymbolType> it = lowerBounds.iterator();
            while (it.hasNext() && isCompatible) {
                isCompatible = other.isCompatible(it.next());
            }
        }
        return isCompatible;
    }

    private boolean isParameterizedTypesCompatible(SymbolType other) {
        boolean isCompatible = true;
        List<SymbolType> otherParams = other.getParameterizedTypes();
        if (parameterizedTypes != null && otherParams != null) {
            Set<Type> paramTypes = ClassInspector.getEquivalentParametrizableClasses(other.getClazz());
            Iterator<Type> paramTypesIt = paramTypes.iterator();
            boolean found = false;
            Set<Integer> recursiveParamDefs = new HashSet<>();
            try {
                Map<String, SymbolType> otherMap = other.getTypeMappingVariables();
                Class<?> clazz = getClazz();
                TypeVariable<?>[] tvs = clazz.getTypeParameters();

                for (int i = 0; i < tvs.length; i++) {
                    Type[] bounds = tvs[i].getBounds();
                    if (bounds.length == 1) {
                        Class<?> classToCompare = null;
                        if (bounds[0] instanceof Class<?>) {
                            classToCompare = (Class<?>) bounds[0];
                        } else if (bounds[0] instanceof ParameterizedType) {
                            classToCompare = (Class<?>) ((ParameterizedType) bounds[0]).getRawType();
                        }
                        if (classToCompare != null) {
                            if (name.equals(classToCompare.getName())) {
                                recursiveParamDefs.add(i);
                            }
                        }
                    }
                }
                int i = 0;
                while (paramTypesIt.hasNext() && !found) {
                    Type currentType = paramTypesIt.next();
                    SymbolType st = SymbolType.valueOf(currentType, otherMap);

                    found = Types.isCompatible(st.getClazz(), getClazz());
                    if (isCompatible) {
                        otherParams = st.getParameterizedTypes();
                        Iterator<SymbolType> it = parameterizedTypes.iterator();
                        if (otherParams != null) {
                            Iterator<SymbolType> otherIt = otherParams.iterator();
                            while (it.hasNext() && found && otherIt.hasNext()) {
                                SymbolType thisType = it.next();
                                SymbolType otherType = otherIt.next();
                                if (!recursiveParamDefs.contains(i)) {
                                    found = thisType.isCompatible(otherType);
                                }
                                i++;
                            }
                        }
                    }

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            isCompatible = found;
        } else {
            isCompatible = other.isUndefinedTemplateVariable();
            if (!isCompatible) {
                List<Class<?>> boundClasses = other.getBoundClasses();

                Iterator<Class<?>> itBounds = boundClasses.iterator();
                while (itBounds.hasNext() && !isCompatible) {
                    if (getArrayCount() < other.getArrayCount()) {
                        if (!isObjectClass(getClazz())) {
                            isCompatible = Types.isCompatible(itBounds.next(), getClazz());
                        } else {
                            itBounds.next();
                            isCompatible = true;
                        }
                    } else {
                        isCompatible = Types.isCompatible(itBounds.next(), getClazz());
                    }
                }

            }

        }
        return isCompatible;
    }

    private boolean isObjectClass(final Class<?> clazz) {
        return Object.class.equals(clazz);
    }

    private boolean isUndefinedTemplateVariable() {
        return isTemplateVariable() && getName() == null;
    }

    private boolean isUpperBoundsCompatible(SymbolType other) {
        boolean isCompatible = true;
        if (upperBounds != null) {
            Iterator<SymbolType> it = upperBounds.iterator();

            while (it.hasNext() && isCompatible) {
                SymbolType bound = it.next();
                List<SymbolType> bounds = null;

                if (other != null) {
                    bounds = other.getBounds();
                }
                if (bounds == null) {
                    bounds = new LinkedList<>();
                    bounds.add(other);
                }
                Iterator<SymbolType> otherIt = bounds.iterator();
                boolean found = false;
                while (otherIt.hasNext() && !found) {
                    found = bound.isCompatible(otherIt.next());
                }
                isCompatible = found;
            }

        }
        return isCompatible;
    }

    public boolean isCompatible(SymbolType other) {
        boolean isCompatible = true;

        if (!isTemplateVariable()) {

            if (other != null) {

                isCompatible = isUpperBoundsCompatible(other) && isLowerBoundsCompatible(other)
                        && isParameterizedTypesCompatible(other) && isArrayCountCompatible(other);

            }

        } else {
            // WARNING: we need to resolve its value afterwards. We just can check array counts
            isCompatible = isArrayCountCompatible(other);
        }

        return isCompatible;
    }

    /**
     * Is symbol for anonymous class that has been either being loaded successfully
     * or being detected as disabled code on load?
     */
    public boolean isLoadedAnonymousClass() {
        return marker == Marker.AnonymousClass || marker == Marker.DisabledAnonymousClass;
    }

    public Class<?> getClazz() {
        if (clazz == null) {
            try {
                clazz = TypesLoaderVisitor.getClassLoader().loadClass(this);
            } catch (ClassNotFoundException e) {
                throw new TypeNotFoundException("Error resolving the class for " + name, e.getCause());
            }

        }
        return clazz;
    }

    public List<Class<?>> getBoundClasses() {
        List<Class<?>> compatibleClasses = new LinkedList<>();
        if (hasBounds()) {
            List<SymbolType> bounds = getBounds();
            for (SymbolType bound : bounds) {
                compatibleClasses.add(bound.getClazz());
            }

        } else {
            Class<?> clazz = getClazz();
            if (clazz != null) {
                compatibleClasses.add(clazz);
            }
        }
        return compatibleClasses;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        addString(result, new ArrayDeque<SymbolType>());
        return result.toString();
    }

    private void addString(StringBuffer result, Deque<SymbolType> visited) {
        result.append(name);
        if (parameterizedTypes != null && !visited.contains(this)) {
            visited.push(this);
            result.append("<");
            Iterator<? extends SymbolData> it = parameterizedTypes.iterator();
            while (it.hasNext()) {
                SymbolType next = (SymbolType) it.next();
                next.addString(result, visited);
                if (it.hasNext()) {
                    result.append(", ");
                }
            }
            result.append(">");
            visited.pop();
        }
        for (int i = 0; i < arrayCount; i++) {
            result.append("[]");
        }
    }

    /** clone with name replaced */
    public SymbolType withName(String name) {
        return clone(marker, name, arrayCount, typeVariable, null, null);
    }

    public SymbolType clone() {
        return clone(null, null);
    }

    public SymbolType cloneAsTypeVariable(String typeVariable) {
        return clone(marker, name != null ? name : name(upperBounds, lowerBounds), arrayCount, typeVariable, null, null);
    }

    public static SymbolType cloneAsArrayOrNull(/* Nullable */ SymbolType type, final int arrayCount) {
        return type != null ? type.cloneAsArray(arrayCount) : null;
    }

    public SymbolType cloneAsArray(int arrayCount) {
        return clone(marker, name, arrayCount, typeVariable, null, null);
    }

    public SymbolType cloneAsArray(String name, int arrayCount) {
        return clone(marker, name, arrayCount, typeVariable, null, null);
    }

    /**
     * If code is disabled via conditional compilation the symbol information
     * is typically incomplete because no class has been created.
     * (For conditional compilation see JLS 14.21. Unreachable Statements,
     * http://docs.oracle.com/javase/specs/jls/se8/html/jls-14.html#jls-14.21)
     */
    public SymbolType markDisabledCode() {
        if (marker != Marker.None && marker != Marker.DisabledAnonymousClass) {
            throw new IllegalStateException("disabled code marker not supported for symbol marked with " + marker);
        }
        return clone(Marker.DisabledAnonymousClass, name, arrayCount, typeVariable, null, null);
    }


    private SymbolType clone(final Stack<SymbolType> parent, final Stack<SymbolType> created) {
        return clone(marker, name, arrayCount, typeVariable, parent, created);
    }

    private SymbolType clone(final Marker marker, final String name, final int arrayCount, final String typeVariable,
                             Stack<SymbolType> parent, Stack<SymbolType> created) {
        SymbolType result = new SymbolType(marker, name);
        result.setClazz(clazz);
        result.setArrayCount(arrayCount);
        result.setField(field);
        result.setMethod(method);
        result.typeVariable = typeVariable;
        if (parent == null) {
            parent = new Stack<>();
            created = new Stack<>();
        }
        Iterator<SymbolType> it = parent.iterator();
        Iterator<SymbolType> it2 = created.iterator();
        boolean found = false;
        while (it.hasNext() && !found) {
            SymbolType next = it.next();
            SymbolType created2 = it2.next();
            if (next == this) {
                result = created2;
                found = true;
            }
        }
        if (!found) {
            parent.push(this);
            created.push(result);
            if (parameterizedTypes != null) {
                List<SymbolType> list = new LinkedList<>();

                for (SymbolData type : parameterizedTypes) {
                    list.add(((SymbolType) type).clone(parent, created));
                }
                result.setParameterizedTypes(list);
            } else {
                result.setParameterizedTypes(null);
            }
            if (upperBounds != null) {
                List<SymbolType> list = new LinkedList<>();
                for (SymbolData type : upperBounds) {
                    list.add(((SymbolType) type).clone(parent, created));
                }
                result.upperBounds = list;
            }
            if (lowerBounds != null) {
                List<SymbolType> list = new LinkedList<>();
                for (SymbolData type : lowerBounds) {
                    list.add(((SymbolType) type).clone(parent, created));
                }
                result.lowerBounds = list;
            }
            parent.pop();
            created.pop();
        }
        return result;
    }

    private static void loadTypeParams(Iterator<SymbolType> it, Type[] typeParams, List<SymbolType> parameterizedTypes,
            Map<String, SymbolType> typeMapping, Map<String, SymbolType> auxMap) throws InvalidTypeException {

        if (parameterizedTypes != null) {
            it = parameterizedTypes.iterator();
        }
        for (Type typeParam : typeParams) {
            SymbolType ref = null;
            if (it != null && it.hasNext()) {
                ref = it.next();
            }

            valueOf(typeParam, ref, auxMap, typeMapping);

        }

    }

    private SymbolType getParameterizedType(String variableName, Set<SymbolType> visited) {
        if (method != null) {
            return null;
        }
        if (variableName.equals(typeVariable)) {
            return this;
        } else {

            if (parameterizedTypes != null) {

                Iterator<SymbolType> itV = visited.iterator();
                boolean found = false;
                while (itV.hasNext()) {
                    found = itV.next() == this;
                }
                if (!found) {
                    visited.add(this);

                    Iterator<SymbolType> it = parameterizedTypes.iterator();

                    SymbolType result = null;
                    while (it.hasNext() && result == null) {
                        SymbolType next = it.next();
                        if (next != this) {

                            SymbolType elem = next.getParameterizedType(variableName, visited);
                            if (elem != null) {
                                result = elem;
                            }

                        }
                    }
                    return result;
                }
                return null;
            }
            return null;
        }
    }


    /**
     * Build symbol representing an anonymous class.
     */
    public static SymbolType anonymousClassOf(String name) {
        return new SymbolType(Marker.AnonymousClass, name);
    }

    /**
     * Build a simple class based symbol probably as an array.
     */
    public static SymbolType classValueOf(final String className, final int arrayCount) {
        return new SymbolType(className, arrayCount);
    }

    /**
     * Build symbol representing an enum constant class.
     */
    public static SymbolType enumConstantOf(String name) {
        return new SymbolType(Marker.EnumConstantClass, name);
    }

    /**
     * Builds a symbol for a type variable from a (Java class) name.
     */
    public static SymbolType typeVariableOf(final String typeVariable, final String name) {
        return new SymbolType(name, typeVariable);
    }

    /**
     * Builds a symbol for a type variable from a Java class.
     */
    public static SymbolType typeVariableOf(final String typeVariable, final Class<Object> clazz) {
        return new SymbolType(clazz, typeVariable);
    }

    /**
     * Builds a symbol for a type variable from a list of upper bounds.
     */
    public static SymbolType typeVariableOf(final String typeVariable, List<SymbolType> upperBounds) {
        return new SymbolType(upperBounds, typeVariable);
    }

    /**
     * Builds a symbol for a type variable.
     */
    private static SymbolType typeVariableOf(String typeVariable, final String name, final int arrayCount,
            final List<SymbolType> upperBounds, final List<SymbolType> lowerBounds) {
        return new SymbolType(name, arrayCount, upperBounds, lowerBounds, typeVariable);
    }

    /**
     * Builds a symbol for a type variable.
     */
    private static SymbolType typeVariableOf(final String typeVariable, final int arrayCount,
            List<SymbolType> upperBounds, List<SymbolType> lowerBounds) {
        return new SymbolType(arrayCount, upperBounds, lowerBounds, typeVariable);
    }

    /**
     * Builds a symbol for a type variable from a TypeVariable.
     */
    public static SymbolType typeVariableOf(TypeVariable<?> typeVariable) throws InvalidTypeException {
        return valueOf(typeVariable, null).cloneAsTypeVariable(typeVariable.getName());
    }

    /**
    * Builds a symbol type from a Java type.
    *
    * @param type
    *           type to convert
    * @param arg
    *           reference class to take into account if the type is a generic variable.
    * @param updatedTypeMapping
    *           place to put the resolved generic variables.
    * @param typeMapping
    *           reference type mapping for generic variables.
    * @return the representative symbol type
    * @throws InvalidTypeException
    */
    public static SymbolType valueOf(Type type, SymbolType arg, Map<String, SymbolType> updatedTypeMapping,
            Map<String, SymbolType> typeMapping) throws InvalidTypeException {
        if (typeMapping == null) {
            typeMapping = Collections.emptyMap();
        }

        SymbolType returnType = null;
        if (type instanceof Class<?>) {
            returnType = valueOfClass((Class<?>) type, arg, updatedTypeMapping, typeMapping);
        } else if (type instanceof TypeVariable) {
            return valueOfTypeVariable((TypeVariable<?>) type, arg, updatedTypeMapping, typeMapping);
        } else if (type instanceof ParameterizedType) {
            returnType = valueOfParameterizedType((ParameterizedType) type, arg, updatedTypeMapping, typeMapping);
        } else if (type instanceof GenericArrayType) {
            returnType = valueOfGenericArrayType((GenericArrayType) type, arg, updatedTypeMapping, typeMapping);
        } else if (type instanceof WildcardType) {
            returnType = valueOfWildcardType((WildcardType) type, arg, updatedTypeMapping, typeMapping);
        }
        return returnType;

    }

    private static SymbolType valueOfClass(Class<?> type, SymbolType arg, Map<String, SymbolType> updatedTypeMapping,
            Map<String, SymbolType> typeMapping) throws InvalidTypeException {
        Class<?> aux = type;
        int arrayCount = 0;
        while (aux.isArray()) {
            arrayCount++;
            aux = aux.getComponentType();
        }
        final SymbolType returnType = new SymbolType(aux.getName());
        returnType.setArrayCount(arrayCount);
        Type[] typeParams = aux.getTypeParameters();
        if (typeParams.length > 0) {

            List<SymbolType> params = new LinkedList<>();
            List<SymbolType> implParams = new LinkedList<>();
            boolean isParameterizedImplementation = false;
            List<SymbolType> parameterizedTypes = null;
            Iterator<SymbolType> it = null;
            if (arg != null) {

                Class<?> argClass = arg.getClazz();
                parameterizedTypes = arg.getParameterizedTypes();
                if (parameterizedTypes != null) {
                    it = parameterizedTypes.iterator();

                }
                List<Type> implementations = ClassInspector.getInterfaceOrSuperclassImplementations(argClass, aux);

                Iterator<Type> itTypes = implementations.iterator();
                Type[] typeParamsAux;
                Map<String, SymbolType> auxMap = new HashMap<>(typeMapping);
                while (itTypes.hasNext()) {
                    Type implementation = itTypes.next();
                    if (implementation instanceof ParameterizedType) {
                        ParameterizedType ptype = (ParameterizedType) implementation;
                        Map<String, SymbolType> typeMappingVars = arg.getTypeMappingVariables();

                        Type[] targuments = ptype.getActualTypeArguments();
                        for (Type targument : targuments) {

                            SymbolType st;
                            if (targument instanceof TypeVariable) {
                                String name = ((TypeVariable<?>) targument).getName();
                                if (typeMappingVars != null && typeMappingVars.containsKey(name)) {
                                    st = typeMappingVars.get(name);
                                } else if (it != null && it.hasNext()) {
                                    st = it.next();
                                } else {
                                    st = new SymbolType(Object.class);
                                }
                            } else {

                                st = SymbolType.valueOf(targument, auxMap);
                            }
                            if (st != null) {
                                implParams.add(st);
                            }
                        }
                        isParameterizedImplementation = true;
                        it = implParams.iterator();
                        params = implParams;
                        implParams = new LinkedList<>();
                        parameterizedTypes = params;
                    } else if (implementation instanceof Class<?>) {
                        Class<?> auxClass = (Class<?>) implementation;
                        boolean isRecursiveThenOmit = type.getName().equals(auxClass.getName());
                        if (!isRecursiveThenOmit) {
                            typeParamsAux = auxClass.getTypeParameters();
                            loadTypeParams(it, typeParamsAux, parameterizedTypes, typeMapping, auxMap);
                        }
                    }
                }

            }
            if (!isParameterizedImplementation) {

                if (parameterizedTypes != null) {
                    it = parameterizedTypes.iterator();
                }
                for (Type typeParam : typeParams) {
                    SymbolType ref = null;
                    if (it != null && it.hasNext()) {
                        ref = it.next();
                    }
                    boolean isRecursiveThenOmit = false;

                    if (typeParam instanceof TypeVariable) {
                        TypeVariable<?> tv = (TypeVariable<?>) typeParam;
                        Type[] types = tv.getBounds();
                        if (types.length == 1) {
                            if (types[0] instanceof Class<?>) {
                                isRecursiveThenOmit = type.getName().equals(((Class<?>) types[0]).getName());
                            }
                        }
                    }
                    if (!isRecursiveThenOmit) {
                        SymbolType tp = valueOf(typeParam, ref, updatedTypeMapping, typeMapping);
                        if (arg != null || !Object.class.getName().equals(tp.getName())) {
                            if (tp != null) {
                                params.add(tp);
                            }
                        }
                    } else {
                        params.add(returnType);
                    }
                }

            }
            if (!params.isEmpty()) {
                returnType.setParameterizedTypes(params);
            }
        }
        return returnType;
    }

    private static SymbolType valueOfTypeVariable(TypeVariable<?> type, SymbolType arg,
            Map<String, SymbolType> updatedTypeMapping, Map<String, SymbolType> typeMapping)
            throws InvalidTypeException {
        SymbolType returnType;
        String variableName = type.getName();
        SymbolType aux = typeMapping.get(variableName);

        if (aux == null) {

            Type[] bounds = type.getBounds();

            if (arg != null) {

                returnType = arg.getParameterizedType(variableName, new HashSet<SymbolType>());

                Class<?> argClazz = arg.getClazz();

                if (returnType != null && argClazz != null
                        && argClazz.getName().equals(returnType.getClazz().getName())) {

                    arg = returnType;
                }
                returnType = typeVariableOf(variableName, arg.getName(), arg.getArrayCount(), arg.getBounds(),
                        arg.getLowerBounds());
                Map<String, SymbolType> auxMap = new HashMap<>(typeMapping);
                auxMap.put(variableName, returnType);

                for (Type bound : bounds) {
                    valueOf(bound, arg, updatedTypeMapping, auxMap);
                }

                returnType.setParameterizedTypes(arg.getParameterizedTypes());

            } else {
                if (bounds.length == 0) {
                    returnType = typeVariableOf(variableName, "java.lang.Object");
                } else {
                    List<SymbolType> boundsList = new LinkedList<>();
                    SymbolType initReturnType = typeVariableOf(variableName, boundsList);
                    Map<String, SymbolType> auxMap = new HashMap<>(typeMapping);
                    auxMap.put(variableName, initReturnType);
                    for (Type bound : bounds) {
                        SymbolType st = valueOf(bound, null, updatedTypeMapping, auxMap);
                        if (st != null) {
                            boundsList.add(st);
                        }
                    }
                    if (boundsList.isEmpty()) {
                        returnType = typeVariableOf(variableName, "java.lang.Object");
                    } else if (bounds.length == 1) {
                        returnType = boundsList.get(0).cloneAsTypeVariable(variableName);
                    } else {
                        returnType = typeVariableOf(variableName, boundsList);
                    }
                }
            }
            if (!updatedTypeMapping.containsKey(variableName)) {

                updatedTypeMapping.put(variableName, returnType);
                return returnType;
            } else {
                SymbolType previousSymbol = updatedTypeMapping.get(variableName);
                String returnTypeName = returnType.getName();
                if (!"java.lang.Object".equals(returnTypeName)) {
                    returnType = (SymbolType) previousSymbol.merge(returnType);
                    if (returnType != null) {
                        previousSymbol.setClazz(returnType.getClazz());
                    }
                    updatedTypeMapping.put(variableName, previousSymbol);
                }
                return updatedTypeMapping.get(variableName);
            }

        } else {
            return aux;
        }
    }

    private static SymbolType valueOfParameterizedType(ParameterizedType type, SymbolType arg,
            Map<String, SymbolType> updatedTypeMapping, Map<String, SymbolType> typeMapping)
            throws InvalidTypeException {
        final Class<?> auxClass = (Class<?>) type.getRawType();

        final Type[] types = type.getActualTypeArguments();

        final SymbolType returnType = new SymbolType(auxClass.getName());

        if (types != null) {

            List<SymbolType> params = new LinkedList<>();

            List<SymbolType> paramTypes = null;
            if (arg != null) {
                paramTypes = arg.getParameterizedTypes();
            }
            int i = 0;
            TypeVariable<?>[] tvs = auxClass.getTypeParameters();
            for (Type t : types) {

                String label = t.toString();

                SymbolType param = typeMapping.get(label);
                if (param != null) {
                    String name = param.getName();
                    if (name != null) {
                        Class<?> aux = Types.getWrapperClass(name);
                        if (aux != null) {
                            param.setName(aux.getName());
                            param.setClazz(aux);
                        }
                    }
                    params.add(param);
                } else {
                    SymbolType argToAnalyze = null;
                    if (paramTypes != null && paramTypes.size() > i) {
                        argToAnalyze = paramTypes.get(i);
                    }
                    boolean validParameterizedType = true;
                    if (t instanceof TypeVariable<?>) {
                        Type[] bounds = ((TypeVariable<?>) t).getBounds();
                        validParameterizedType = !(bounds.length == 1 && bounds[0] == type);
                    }

                    SymbolType st = null;
                    if (validParameterizedType) {

                        st = valueOf(t, argToAnalyze, updatedTypeMapping, typeMapping);
                        if (st != null) {
                            if (st.isTemplateVariable() && st.getTemplateVariable().equals("?")) {
                                List<SymbolType> bounds = st.getBounds();
                                if (bounds != null && bounds.size() == 1) {
                                    if ("java.lang.Object".equals(bounds.get(0).getName())) {
                                        if (tvs.length > i) {
                                            SymbolType templateVarType =
                                                    valueOf(tvs[i], null, updatedTypeMapping, typeMapping);
                                            bounds = new LinkedList<>();
                                            bounds.add(templateVarType);
                                            st = typeVariableOf("?", bounds);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (st != null) {
                        String name = st.getName();
                        if (name != null) {
                            Class<?> aux = Types.getWrapperClass(name);
                            if (aux != null) {
                                st.setName(aux.getName());
                                st.setClazz(aux);
                            }
                        }
                    }
                    if (st == null) {
                        st = new SymbolType("java.lang.Object");
                    }

                    params.add(st);

                }

                i++;
            }
            returnType.setParameterizedTypes(!params.isEmpty() ? params : null);
        }
        return returnType;
    }

    private static SymbolType valueOfGenericArrayType(GenericArrayType type, SymbolType arg,
            Map<String, SymbolType> updatedTypeMapping, Map<String, SymbolType> typeMapping)
            throws InvalidTypeException {
        SymbolType returnType;
        if (arg != null) {
            if (arg.getArrayCount() > 0) {
                arg = arg.clone();
                arg.setArrayCount(arg.getArrayCount() - 1);
            } else {
                arg = null;
            }
        }
        SymbolType st = valueOf(type.getGenericComponentType(), arg, updatedTypeMapping, typeMapping);

        returnType = st.clone();
        returnType.setArrayCount(returnType.getArrayCount() + 1);
        return returnType;
    }

    private static SymbolType valueOfWildcardType(final WildcardType wt, SymbolType arg,
            Map<String, SymbolType> updatedTypeMapping, Map<String, SymbolType> typeMapping)
            throws InvalidTypeException {
        Type[] types = wt.getUpperBounds();

        List<SymbolType> upperBounds = null;
        List<SymbolType> lowerBounds = null;
        if (types != null && types.length > 0) {
            upperBounds = new LinkedList<>();
            for (Type type : types) {
                SymbolType st = valueOf(type, arg, updatedTypeMapping, typeMapping);
                if (st != null) {
                    upperBounds.add(st);
                }
            }
            if (upperBounds.isEmpty()) {
                upperBounds = null;
            }

        }
        types = wt.getLowerBounds();
        if (types != null && types.length > 0) {
            lowerBounds = new LinkedList<>();
            for (Type type : types) {

                SymbolType st = valueOf(type, arg, updatedTypeMapping, typeMapping);
                if (st != null) {
                    lowerBounds.add(st);
                }

            }
            if (lowerBounds.isEmpty()) {
                lowerBounds = null;
            }
        }
        SymbolType returnType = null;
        if (upperBounds != null || lowerBounds != null) {
            returnType = typeVariableOf(wt.toString(), arg != null ? arg.getArrayCount() : 0, upperBounds, lowerBounds);
        }
        return returnType;
    }

    public static SymbolType valueOf(Type type, Map<String, SymbolType> typeMapping) throws InvalidTypeException {

        return valueOf(type, null, new HashMap<String, SymbolType>(), typeMapping);

    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public static SymbolType valueOf(Method method, Map<String, SymbolType> typeMapping)
            throws ClassNotFoundException, InvalidTypeException {
        java.lang.reflect.Type type;
        if (typeMapping == null) {
            typeMapping = new HashMap<>();
            type = method.getGenericReturnType();
        } else {
            TypeVariable<Method>[] tvs = method.getTypeParameters();
            type = method.getGenericReturnType();
            if (tvs.length > 0) {
                for (TypeVariable<Method> tv : tvs) {
                    Type[] bounds = tv.getBounds();
                    List<SymbolType> boundsList = new LinkedList<>();
                    for (Type bound : bounds) {
                        SymbolType st = valueOf(bound, typeMapping);
                        if (st != null) {
                            boundsList.add(st);
                        }
                    }
                    SymbolType st = typeMapping.get(tv.getName());
                    if (st == null) {
                        if (boundsList.size() == 1) {
                            typeMapping.put(tv.getName(), boundsList.get(0));

                        } else {
                            typeMapping.put(tv.getName(), new SymbolType(boundsList));
                        }
                    }
                }
            }
        }
        SymbolType st = SymbolType.valueOf(type, typeMapping);
        st.method = method;

        return st;
    }

    public void setField(Field field) {
        this.field = field;
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public SymbolData merge(SymbolData other) {
        SymbolType result;
        if (other == null || equals(other)) {
            result = this;
        } else {
            if (other.getArrayCount() == getArrayCount()) {

                List<? extends Class<?>> bounds =
                        ClassInspector.intersectRawTypes(getBoundClasses(), other.getBoundClasses());
                if (bounds.isEmpty()) {
                    result = null;
                } else if (bounds.size() == 1) {
                    result = new SymbolType(bounds.get(0));
                } else {
                    List<SymbolType> boundsList = new LinkedList<>();
                    for (Class<?> bound : bounds) {
                        boundsList.add(new SymbolType(bound));
                    }
                    result = new SymbolType(boundsList);
                }
                if (result != null) {
                    if (lowerBounds != null) {
                        result.lowerBounds = new LinkedList<>();
                        for (SymbolType st : lowerBounds) {
                            result.lowerBounds.add(st.clone());
                        }
                    }
                    result.arrayCount = other.getArrayCount();
                }
            } else {
                result = new SymbolType(Object.class);
            }
        }
        return result;
    }

    public void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    public boolean belongsToAnonymousClass() {
        return belongsToAnonymous(getClazz());
    }

    private boolean belongsToAnonymous(Class<?> clazz) {
        if (clazz == null || isObjectClass(clazz)) {
            return false;
        }
        return clazz.isAnonymousClass() || belongsToAnonymous(clazz.getDeclaringClass());
    }

    @Override
    public List<Class<?>> getLowerBoundClasses() {
        List<Class<?>> upperBoundClasses = new LinkedList<>();
        if (lowerBounds != null) {
            for (SymbolType bound : lowerBounds) {
                upperBoundClasses.add(bound.getClazz());
            }

        }
        return upperBoundClasses;
    }

    private SymbolType refactor_rec(String variable, SymbolType st, boolean dynamicVar) {
        if (variable.equals(typeVariable) && dynamicVar) {
            return st;
        } else {
            SymbolType aux;
            if (this.parameterizedTypes != null) {
                aux = this.clone();
                List<SymbolType> parameterizedTypes = new LinkedList<>();
                for (SymbolType param : this.parameterizedTypes) {
                    param = param.refactor_rec(variable, st, dynamicVar);
                    parameterizedTypes.add(param);
                }
                aux.setParameterizedTypes(parameterizedTypes);
            } else {
                aux = this;

            }
            return aux;
        }
    }

    public SymbolType refactorToTypeVariable(String typeVariable, SymbolType st, boolean dynamicVar) {
        SymbolType refactor = refactor(typeVariable, st, dynamicVar);
        refactor.setTemplateVariable(typeVariable);
        return refactor;
    }

    public SymbolType refactor(String variable, SymbolType st, boolean dynamicVar) {
        if (variable.equals(typeVariable) && dynamicVar) {
            return st;
        } else {
            SymbolType aux;
            if (this.parameterizedTypes != null) {
                aux = this.clone();
                List<SymbolType> parameterizedTypes = new LinkedList<>();
                for (SymbolType param : this.parameterizedTypes) {
                    param = param.refactor_rec(variable, st, dynamicVar);
                    parameterizedTypes.add(param);
                }
                aux.setParameterizedTypes(parameterizedTypes);
            } else {
                if (isObjectClass(getClazz())) {
                    aux = st;
                } else {
                    aux = this;
                }
            }
            return aux;
        }
    }

    public SymbolType refactor(Map<String, SymbolType> mapping) {
        return refactor(mapping, true);
    }

    public SymbolType refactor(Map<String, SymbolType> mapping, boolean dynamicVar) {
        SymbolType result = this;
        if (mapping != null) {
            Set<String> keys = mapping.keySet();
            for (String key : keys) {
                result = result.refactor(key, mapping.get(key), dynamicVar);
            }
        }

        return result;
    }

    public static <T extends SymbolData> Class<?>[] toClassArray(T[] args) {

        Class<?>[] argClasses;
        int params = 0;
        if (args != null) {
            params = args.length;
        }
        argClasses = new Class<?>[params];
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    argClasses[i] = args[i].getClazz();
                }
            }
        }
        return argClasses;
    }
}
