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

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.PrimitiveType.Primitive;
import org.walkmod.javalang.ast.type.ReferenceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.VoidType;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;
import org.walkmod.javalang.visitors.GenericVisitorAdapter;

public class ASTSymbolTypeResolver extends GenericVisitorAdapter<SymbolType, List<TypeParameter>>
        implements SymbolTypeResolver<Type> {

    private SymbolTable symbolTable = null;

    private static ASTSymbolTypeResolver instance = null;

    private Map<String, SymbolType> mapping = null;

    private ASTSymbolTypeResolver() {

    }

    public ASTSymbolTypeResolver(Map<String, SymbolType> mapping, SymbolTable symbolTable) {
        this.mapping = mapping;
        this.symbolTable = symbolTable;
    }

    public static ASTSymbolTypeResolver getInstance() {
        if (instance == null) {
            instance = new ASTSymbolTypeResolver();
        }
        return instance;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public SymbolType visit(PrimitiveType n, List<TypeParameter> arg) {
        final SymbolType result;
        Primitive pt = n.getType();
        if (pt.equals(Primitive.Boolean)) {
            result = new SymbolType(boolean.class.getName());
        } else if (pt.equals(Primitive.Char)) {
            result = new SymbolType(char.class.getName());
        } else if (pt.equals(Primitive.Double)) {
            result = new SymbolType(double.class.getName());
        } else if (pt.equals(Primitive.Float)) {
            result = new SymbolType(float.class.getName());
        } else if (pt.equals(Primitive.Int)) {
            result = new SymbolType(int.class.getName());
        } else if (pt.equals(Primitive.Long)) {
            result = new SymbolType(long.class.getName());
        } else if (pt.equals(Primitive.Short)) {
            result = new SymbolType(short.class.getName());
        } else if (pt.equals(Primitive.Byte)) {
            result = new SymbolType(byte.class.getName());
        } else {
            throw new IllegalArgumentException("unexpected primitive type: " + pt);
        }
        return result;
    }

    @Override
    public SymbolType visit(ClassOrInterfaceType type, List<TypeParameter> arg) {
        SymbolType result = null;

        String name = type.getName();
        ClassOrInterfaceType scope = type.getScope();
        Node parent = type.getParentNode();
        boolean isObjectCreationCtxt = (parent != null && parent instanceof ObjectCreationExpr);
        isObjectCreationCtxt = isObjectCreationCtxt && ((ObjectCreationExpr) parent).getScope() != null;
        if (scope == null && !isObjectCreationCtxt) {

            if (arg != null) {
                Iterator<TypeParameter> it = arg.iterator();
                while (it.hasNext() && result == null) {
                    TypeParameter next = it.next();
                    if (next.getName().equals(name)) {
                        List<ClassOrInterfaceType> bounds = next.getTypeBound();
                        if (bounds == null || bounds.isEmpty()) {
                            result = SymbolType.typeVariableOf(name, Object.class);
                        } else {
                            List<SymbolType> params = new LinkedList<SymbolType>();
                            for (ClassOrInterfaceType bound : bounds) {
                                params.add(bound.accept(this, arg));
                            }
                            result = SymbolType.typeVariableOf(name, params);
                        }
                    }
                }
            }
            if (result == null) {

                // it can be resolved through the symbol table (imports,
                // generics, sibling/children inner classes, package
                // classes)
                result = symbolTable.getType(name, org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
                        org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE_PARAM);
                if (result != null) {
                    result = result.clone();
                } else {
                    SymbolType thisType =
                            symbolTable.getType("this", org.walkmod.javalang.compiler.symbols.ReferenceType.VARIABLE);
                    if (thisType != null) {
                        Class<?> clazz = thisType.getClazz();
                        // we look for a declared class in one of our super
                        // classes
                        Class<?> superClass = clazz.getSuperclass();
                        Class<?> nestedClass =
                                ClassInspector.findClassMember(thisType.getClazz().getPackage(), name, superClass);

                        // this is an inner class? If so, we look for a nested
                        // class
                        // in one of our parent classes
                        while (clazz.isMemberClass() && nestedClass == null) {
                            clazz = clazz.getDeclaringClass();
                            nestedClass = ClassInspector.findClassMember(clazz.getPackage(), name, clazz);
                        }
                        // this is an anonymous class? If so, we look for a
                        // nested
                        // class in the enclosing class
                        while (clazz.isAnonymousClass() && nestedClass == null) {
                            clazz = clazz.getEnclosingClass();
                            nestedClass = ClassInspector.findClassMember(clazz.getPackage(), name, clazz);
                            while (clazz.isMemberClass() && nestedClass == null) {
                                clazz = clazz.getDeclaringClass();
                                nestedClass = ClassInspector.findClassMember(clazz.getPackage(), name, clazz);
                            }
                        }
                        if (nestedClass != null) {
                            result = new SymbolType(nestedClass);
                        }

                    }
                }
            }

        } else {
            // it is a fully qualified name or a inner class (>1 hop)

            String scopeName = "";
            String parentName = "";
            if (isObjectCreationCtxt) {
                SymbolData sd = ((ObjectCreationExpr) parent).getScope().getSymbolData();
                Class<?> ctxClass = sd.getClazz();
                if (ctxClass.isAnonymousClass()) {
                    ctxClass = ctxClass.getSuperclass();
                }
                parentName = ctxClass.getName() + "$";
            }
            ClassOrInterfaceType ctxt = type;
            while (ctxt.getScope() != null) {
                ctxt = (ClassOrInterfaceType) ctxt.getScope();
                if (ctxt.getSymbolData() != null) {
                    scopeName = ctxt.getName() + "$" + scopeName;
                } else {
                    scopeName = ctxt.getName() + "." + scopeName;
                }
            }
            scopeName = parentName + scopeName;

            String innerClassName = name;
            if (scopeName.length() > 1) {
                innerClassName = scopeName.substring(0, scopeName.length() - 1) + "$" + name;
            }
            String fullName = scopeName + name;

            result = symbolTable.getType(innerClassName, org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
                    org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE_PARAM);
            if (result == null) {
                result = symbolTable.getType(fullName, org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
                        org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE_PARAM);
                if (result == null) {
                    // in the code appears B.C
                    SymbolType scopeType = null;
                    if (type.getScope() != null) {
                        scopeType = type.getScope().accept(this, arg);
                    }
                    if (scopeType != null) {
                        result = symbolTable.getType(scopeType.getClazz().getCanonicalName() + "." + name,
                                org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE);
                        if (result == null) {
                            SymbolType thisType = symbolTable.getType("this");
                            if (thisType != null) {
                                Class<?> resolvedClass = ClassInspector
                                        .findClassMember(thisType.getClazz().getPackage(), name, scopeType.getClazz());
                                result = new SymbolType(resolvedClass.getName());
                                result.setClazz(resolvedClass);
                            } else {
                                result = new SymbolType(scopeType.getName() + "$" + name);
                            }
                        }
                    } else {

                        try {
                            TypesLoaderVisitor.getClassLoader().loadClass(fullName);
                        } catch (ClassNotFoundException e) {
                            return null;
                        }
                        // it is a type that has not previously imported
                        result = new SymbolType(fullName);
                    }
                }
            }

        }

        if (type.getTypeArgs() != null) {
            if (result == null) {
                result = new SymbolType();
            }
            List<SymbolType> typeArgs = new LinkedList<SymbolType>();

            for (Type typeArg : type.getTypeArgs()) {
                SymbolType aux = valueOf(typeArg);
                if (aux == null) {
                    aux = SymbolType.typeVariableOf(typeArg.toString(), Object.class);
                }
                typeArgs.add(aux);
            }
            if (!typeArgs.isEmpty()) {
                result.setParameterizedTypes(typeArgs);
            }
        } else {
            if (result != null) {
                result.setParameterizedTypes(null);
            }
        }
        if (mapping != null && result != null) {
            String letter = result.getTemplateVariable();
            if (letter != null) {
                mapping.put(letter, result);
            } else {
                mapping.put(result.getName(), result);
            }
        }
        return result;
    }

    @Override
    public SymbolType visit(VoidType n, List<TypeParameter> arg) {
        return new SymbolType(Void.class.getName());
    }

    @Override
    public SymbolType visit(WildcardType n, List<TypeParameter> arg) {
        SymbolType result = null;
        if (n.toString().equals("?")) {
            result = new SymbolType("java.lang.Object");
        } else {
            List<SymbolType> upperBounds = null;
            List<SymbolType> lowerBounds = null;
            ReferenceType extendsRef = n.getExtends();
            ReferenceType superRef = n.getSuper();
            if (extendsRef != null) {

                SymbolType aux = extendsRef.accept(this, arg);
                if (aux != null) {
                    upperBounds = new LinkedList<SymbolType>();
                    upperBounds.add(aux);
                }

            } else {

                SymbolType aux = superRef.accept(this, arg);
                if (aux != null) {
                    lowerBounds = new LinkedList<SymbolType>();
                    lowerBounds.add(aux);
                }
            }
            if (upperBounds != null || lowerBounds != null) {
                result = new SymbolType(upperBounds, lowerBounds);
            }

        }
        return result;
    }

    public SymbolType visit(ReferenceType n, List<TypeParameter> arg) {
        Type containerType = n.getType();
        SymbolType result = null;
        if (containerType instanceof PrimitiveType) {
            result = SymbolType.classValueOf(containerType.accept(this, arg).getName(), n.getArrayCount());
        } else if (containerType instanceof ClassOrInterfaceType) {
            result = SymbolType.cloneAsArrayOrNull(containerType.accept(this, arg), n.getArrayCount());
        }
        return result;
    }

    @Override
    public SymbolType valueOf(Type parserType) {
        return valueOf(parserType, (List<TypeParameter>) null);
    }

    public SymbolType valueOf(Type parserType, List<TypeParameter> tps) {
        return parserType.accept(this, tps);
    }

    @Override
    public SymbolType[] valueOf(List<Type> nodes) {
        if (nodes == null) {
            return new SymbolType[0];
        }
        SymbolType[] result = new SymbolType[nodes.size()];
        int i = 0;
        for (Type node : nodes) {
            result[i] = valueOf(node);
            i++;
        }
        return result;
    }

}
