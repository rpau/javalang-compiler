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
package org.walkmod.javalang.compiler.types;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.body.VariableDeclaratorId;
import org.walkmod.javalang.ast.expr.ArrayAccessExpr;
import org.walkmod.javalang.ast.expr.ArrayCreationExpr;
import org.walkmod.javalang.ast.expr.ArrayInitializerExpr;
import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr.Operator;
import org.walkmod.javalang.ast.expr.BooleanLiteralExpr;
import org.walkmod.javalang.ast.expr.CastExpr;
import org.walkmod.javalang.ast.expr.CharLiteralExpr;
import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.ConditionalExpr;
import org.walkmod.javalang.ast.expr.DoubleLiteralExpr;
import org.walkmod.javalang.ast.expr.EnclosedExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.InstanceOfExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralMinValueExpr;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.expr.LongLiteralExpr;
import org.walkmod.javalang.ast.expr.LongLiteralMinValueExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.NormalAnnotationExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.QualifiedNameExpr;
import org.walkmod.javalang.ast.expr.SingleMemberAnnotationExpr;
import org.walkmod.javalang.ast.expr.StringLiteralExpr;
import org.walkmod.javalang.ast.expr.SuperExpr;
import org.walkmod.javalang.ast.expr.ThisExpr;
import org.walkmod.javalang.ast.expr.TypeExpr;
import org.walkmod.javalang.ast.expr.UnaryExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.ExplicitConstructorInvocationStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.stmt.SwitchEntryStmt;
import org.walkmod.javalang.ast.stmt.SwitchStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.IntersectionType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.PrimitiveType.Primitive;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.VoidType;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.CompositeBuilder;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.reflection.CompatibleArgsPredicate;
import org.walkmod.javalang.compiler.reflection.CompatibleConstructorArgsPredicate;
import org.walkmod.javalang.compiler.reflection.CompatibleFunctionalConstructorPredicate;
import org.walkmod.javalang.compiler.reflection.CompatibleFunctionalMethodPredicate;
import org.walkmod.javalang.compiler.reflection.ConstructorInspector;
import org.walkmod.javalang.compiler.reflection.FieldInspector;
import org.walkmod.javalang.compiler.reflection.GenericsBuilderFromConstructorParameterTypes;
import org.walkmod.javalang.compiler.reflection.GenericsBuilderFromMethodParameterTypes;
import org.walkmod.javalang.compiler.reflection.InvokableMethodsPredicate;
import org.walkmod.javalang.compiler.reflection.MethodInspector;
import org.walkmod.javalang.compiler.reflection.MethodsByNamePredicate;
import org.walkmod.javalang.compiler.reflection.SymbolDataOfMethodReferenceBuilder;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.AnonymousClassUtil;
import org.walkmod.javalang.compiler.symbols.MethodSymbol;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.symbols.SymbolVisitorAdapter;
import org.walkmod.javalang.exceptions.InvalidTypeException;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

/**
 * Resolve the type of a node (symbol or expression) using the symbol table.
 * See SymbolVisitorAdapter for symbol resolution.
 * The original idea is separating the type resolution from the symbol resolution. 
 */
public class TypeVisitorAdapter<A extends Map<String, Object>> extends VoidVisitorAdapter<A> {

    public static final String IMPLICIT_PARAM_TYPE = "implicit_param_type";

    private SymbolTable symbolTable;

    private static Logger LOG = Logger.getLogger(TypeVisitorAdapter.class);

    private VoidVisitorAdapter<A> semanticVisitor;

    public TypeVisitorAdapter(SymbolTable symbolTable) {
        this(symbolTable, null);
    }

    public TypeVisitorAdapter(SymbolTable symbolTable, VoidVisitorAdapter<A> semanticVisitor) {

        this.symbolTable = symbolTable;
        this.semanticVisitor = semanticVisitor;
    }

    @Override
    public void visit(ArrayAccessExpr n, A arg) {
        n.getName().accept(this, arg);
        n.getIndex().accept(this, arg);
        SymbolType arrayType = (SymbolType) n.getName().getSymbolData();
        SymbolType newType = new SymbolType(arrayType.getName());
        newType.setParameterizedTypes(arrayType.getParameterizedTypes());
        newType.setArrayCount(arrayType.getArrayCount() - 1);

        n.setSymbolData(newType);
    }

    @Override
    public void visit(ArrayCreationExpr n, A arg) {
        SymbolType arrayType = ASTSymbolTypeResolver.getInstance().valueOf(n.getType());
        arrayType.setArrayCount(n.getArrayCount() > 0 ? n.getArrayCount() : 1);
        n.setSymbolData(arrayType);
        ArrayInitializerExpr expr = n.getInitializer();
        if (expr != null) {
            expr.accept(this, arg);
        }
        List<Expression> dimensions = n.getDimensions();
        if (dimensions != null) {
            for (Expression dimension : dimensions) {
                dimension.accept(this, arg);
            }
        }

        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
    }

    @Override
    public void visit(ArrayInitializerExpr n, A arg) {

        if (n.getValues() != null) {

            List<Expression> values = n.getValues();
            SymbolType st = null;
            for (Expression expr : values) {
                expr.accept(this, arg);
                SymbolData sd = expr.getSymbolData();
                if (st == null && sd != null) {
                    st = (SymbolType) sd;
                    st = st.clone();

                } else if (sd != null) {
                    st = (SymbolType) st.merge(sd);
                }

            }
            if (values != null && !values.isEmpty() && st != null) {
                st.setArrayCount(st.getArrayCount() + 1);
            }

            n.setSymbolData(st);
        }
    }

    @Override
    public void visit(AssignExpr n, A arg) {
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
    }

    @Override
    public void visit(BinaryExpr n, A arg) {

        n.getLeft().accept(this, arg);
        SymbolType leftType = (SymbolType) n.getLeft().getSymbolData();

        n.getRight().accept(this, arg);
        SymbolType rightType = (SymbolType) n.getRight().getSymbolData();

        SymbolType resultType = leftType;

        Class<?> leftClazz = null;
        if (leftType != null) {
            leftClazz = leftType.getClazz();
        }
        Class<?> rightClazz = null;
        if (rightType != null) {
            rightClazz = rightType.getClazz();
        }

        if (Types.isCompatible(leftClazz, rightClazz)) {
            resultType = rightType;
        }

        Operator op = n.getOperator();
        if (op.equals(Operator.plus)) {

            if (leftType.getName().equals("java.lang.String")) {
                resultType = leftType;
            } else if (rightType.getName().equals("java.lang.String")) {
                resultType = rightType;
            }
        }

        if (op.equals(Operator.equals) || op.equals(Operator.notEquals) || op.equals(Operator.greater)
                || op.equals(Operator.greaterEquals) || op.equals(Operator.less) || op.equals(Operator.lessEquals)) {
            resultType = new SymbolType(boolean.class);
        }

        n.setSymbolData(resultType);

    }

    public void visit(UnaryExpr n, A arg) {
        super.visit(n, arg);
        SymbolData sd = n.getExpr().getSymbolData();
        n.setSymbolData(sd);
    }

    @Override
    public void visit(BooleanLiteralExpr n, A arg) {
        n.setSymbolData(new SymbolType("boolean"));
    }

    @Override
    public void visit(CastExpr n, A arg) {
        super.visit(n, arg);
        n.setSymbolData(n.getType().getSymbolData());
    }

    @Override
    public void visit(CharLiteralExpr n, A arg) {
        n.setSymbolData(new SymbolType("char"));
    }

    @Override
    public void visit(ClassExpr n, A arg) {
        n.getType().accept(this, arg);

        SymbolType st = (SymbolType) n.getType().getSymbolData();
        SymbolType aux = new SymbolType("java.lang.Class");
        List<SymbolType> args = new LinkedList<SymbolType>();
        args.add(st.clone());
        aux.setParameterizedTypes(args);
        n.setSymbolData(aux);
    }

    @Override
    public void visit(ConditionalExpr n, A arg) {

        super.visit(n, arg);
        SymbolData thenData = n.getThenExpr().getSymbolData();
        SymbolData elseData = n.getElseExpr().getSymbolData();

        if (elseData == null || thenData == null) {
            if (elseData == null) {
                n.setSymbolData(thenData);
            } else {
                n.setSymbolData(elseData);
            }
        } else {
            n.setSymbolData(thenData.merge(elseData));
        }

    }

    @Override
    public void visit(EnclosedExpr n, A arg) {
        super.visit(n, arg);
        n.setSymbolData(n.getInner().getSymbolData());
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, A arg) {

        if (n.getTypeArgs() != null) {
            for (Type t : n.getTypeArgs()) {
                t.accept(this, arg);
            }
        }
        Node p = n.getParentNode();
        while (p != null && !(p instanceof ConstructorDeclaration)) {
            p = p.getParentNode();
        }

        if (p != null) {
            final SymbolType st;
            final ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) p;
            if (n.isThis()) {
                st = (SymbolType) constructorDeclaration.getSymbolData();
            } else {
                final ClassOrInterfaceDeclaration parentNode =
                        (ClassOrInterfaceDeclaration) constructorDeclaration.getParentNode();
                final List<ClassOrInterfaceType> anExtends = parentNode.getExtends();
                st = anExtends != null && !anExtends.isEmpty() ? (SymbolType) anExtends.get(0).getSymbolData() : null;
            }
            if (st != null) {
                resolveConstructor(n, n.getArgs(), st, arg);
            }
        }
    }

    @Override
    public void visit(DoubleLiteralExpr n, A arg) {
        String value = n.getValue();
        String typeName = "double";
        if (value != null) {
            char lastChar = value.charAt(value.length() - 1);
            if (Character.toLowerCase(lastChar) == 'f') {
                typeName = "float";
            }
        }
        n.setSymbolData(new SymbolType(typeName));
    }

    @Override
    public void visit(FieldAccessExpr n, A arg) {

        n.getScope().accept(this, arg);

        SymbolType scopeType = (SymbolType) n.getScope().getSymbolData();

        Class<?> c = null;

        try {
            if (scopeType == null) {
                try {
                    c = TypesLoaderVisitor.getClassLoader().loadClass(n.toString());
                    if (c != null) {
                        scopeType = new SymbolType(c);
                        symbolTable.lookUpSymbolForRead(c.getSimpleName(), null, ReferenceType.TYPE);
                        n.setSymbolData(scopeType);

                    }
                } catch (ClassNotFoundException e) {

                } catch (NoClassDefFoundError e) {

                }
            } else {
                SymbolType fieldType = null;
                if (n.getScope() instanceof ThisExpr) {
                    fieldType = symbolTable.findSymbol(n.getField(), scopeType, null, ReferenceType.VARIABLE).getType();
                } else {
                    fieldType = FieldInspector.findFieldType(symbolTable, scopeType, n.getField());
                }
                n.setSymbolData(fieldType);

            }

        } catch (Exception e) {
            throw new NoSuchExpressionTypeException("Error evaluating a type expression in " + n.toString(), e);

        }
        if (semanticVisitor != null) {
            arg.put(SymbolVisitorAdapter.VISITOR_SCOPE_PROCESSOR, this);
            n.accept(semanticVisitor, arg);
            arg.remove(SymbolVisitorAdapter.VISITOR_SCOPE_PROCESSOR);
        }
    }

    @Override
    public void visit(InstanceOfExpr n, A arg) {
        super.visit(n, arg);
        n.setSymbolData(new SymbolType("boolean"));
    }

    @Override
    public void visit(IntegerLiteralExpr n, A arg) {
        n.setSymbolData(new SymbolType("int"));
    }

    @Override
    public void visit(IntegerLiteralMinValueExpr n, A arg) {
        n.setSymbolData(new SymbolType("int"));
    }

    @Override
    public void visit(LongLiteralExpr n, A arg) {
        n.setSymbolData(new SymbolType("long"));
    }

    @Override
    public void visit(LongLiteralMinValueExpr n, A arg) {
        n.setSymbolData(new SymbolType("long"));
    }

    @Override
    public void visit(MethodCallExpr n, A arg) {
        try {
            final ArgTypes argTypes = argTypes(n.getArgs(), arg);
            final SymbolType[] symbolTypes = argTypes.symbolTypes;
            final boolean hasFunctionalExpressions = argTypes.hasFunctionalExpressions;

            SymbolType scope = null;

            if (n.getScope() != null) {

                n.getScope().accept(this, arg);

                scope = (SymbolType) n.getScope().getSymbolData();

                if (scope == null) {
                    throw new RuntimeException("Ops! Error discovering the type of " + n.getScope().toString());
                } else {
                    LOG.debug("scope: (" + n.getScope().toString() + ")" + scope.getName() + " method " + n.toString());
                }
            }
            if (scope != null && "sun.misc.Unsafe".equals(scope.getName()) && n.getName().equals("getUnsafe")) {
                n.setSymbolData(scope);
                return;
            }

            List<Predicate<?>> preds = null;
            if (hasFunctionalExpressions) {
                preds = new LinkedList<Predicate<?>>();
                preds.add(new CompatibleFunctionalMethodPredicate<A>(scope, this, n.getArgs(), arg, symbolTable, null,
                        symbolTypes));
            }
            // for static imports
            Symbol<?> s = symbolTable.findSymbol(n.getName(), scope, symbolTypes, preds, ReferenceType.METHOD);
            boolean lookUpMethodByReflection = (s == null);

            if (s != null) {

                MethodSymbol methodSymbol = (MethodSymbol) s;

                Method m = methodSymbol.getReferencedMethod();
                SymbolType methodScope = new SymbolType(m.getDeclaringClass());
                if (scope == null // is static import
                        || methodScope.isCompatible(scope)) { // is a method
                    // inside the CU
                    if (MethodInspector.isGeneric(m)) {
                        // it is may return a parameterized type
                        if (scope == null) {
                            SymbolType st = symbolTable.getType("this", ReferenceType.VARIABLE);
                            if (!methodScope.isCompatible(st)) {
                                // it is an parent method call from an inner
                                // class
                                scope = methodScope;
                            }
                        }

                        Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
                        GenericsBuilderFromMethodParameterTypes builder = new GenericsBuilderFromMethodParameterTypes(
                                typeMapping, n.getArgs(), scope, symbolTypes, n.getTypeArgs(), symbolTable);

                        builder.build(m);

                        SymbolType aux = SymbolType.valueOf(m, typeMapping);

                        n.setSymbolData(aux);

                    } else {
                        SymbolType result = s.getType().clone();
                        result.setMethod(m);
                        n.setSymbolData(result);
                    }
                } else {
                    lookUpMethodByReflection = true;
                }
            }

            if (lookUpMethodByReflection) {
                if (scope == null) {
                    scope = symbolTable.getType("this", ReferenceType.VARIABLE);
                    LOG.debug("scope (this): " + scope.getName() + " method " + n.toString());
                }

                Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
                // it should be initialized after resolving the method

                ArrayFilter<Method> filter = new ArrayFilter<Method>(null);
                CompatibleArgsPredicate pred = new CompatibleArgsPredicate(symbolTypes);
                filter.appendPredicate(new MethodsByNamePredicate(n.getName()))
                        .appendPredicate(new InvokableMethodsPredicate()).appendPredicate(pred);
                if (hasFunctionalExpressions) {
                    filter.appendPredicate(new CompatibleFunctionalMethodPredicate<A>(scope, this, n.getArgs(), arg,
                            symbolTable, pred, symbolTypes));
                }
                CompositeBuilder<Method> builder = new CompositeBuilder<Method>();
                builder.appendBuilder(new GenericsBuilderFromMethodParameterTypes(typeMapping, n.getArgs(), scope,
                        symbolTypes, n.getTypeArgs(), symbolTable));

                SymbolType st = MethodInspector.findMethodType(scope, symbolTypes, filter, builder, typeMapping);

                n.setSymbolData(st);

                SymbolDataOfMethodReferenceBuilder<A> typeBuilder =
                        new SymbolDataOfMethodReferenceBuilder<A>(typeMapping, this, arg);
                typeBuilder.build(n);

            }
            if (semanticVisitor != null) {
                n.accept(semanticVisitor, arg);
            }

        } catch (ClassNotFoundException e) {
            throw new NoSuchExpressionTypeException(e);

        } catch (Exception e) {
            throw new NoSuchExpressionTypeException(e);
        }

    }

    @Override
    public void visit(IntersectionType n, A arg) {
        super.visit(n, arg);
        List<org.walkmod.javalang.ast.type.ReferenceType> bounds = n.getBounds();
        SymbolData sd = null;
        if (bounds != null) {
            List<SymbolType> boundsTypes = new LinkedList<SymbolType>();
            for (org.walkmod.javalang.ast.type.ReferenceType bound : bounds) {
                SymbolType aux = (SymbolType) bound.getSymbolData();
                if (aux != null) {
                    boundsTypes.add(aux);
                }
            }
            sd = new SymbolType(boundsTypes);
        }
        n.setSymbolData(sd);
    }

    @Override
    public void visit(NameExpr n, A arg) {
        SymbolType type = symbolTable.getType(n.getName(), ReferenceType.VARIABLE, ReferenceType.ENUM_LITERAL,
                ReferenceType.TYPE);
        Node parentNode = n.getParentNode();

        if (parentNode instanceof SwitchEntryStmt) {

            SwitchStmt stmt = (SwitchStmt) parentNode.getParentNode();
            SymbolType scope = (SymbolType) stmt.getSelector().getSymbolData();
            if (scope.getClazz().isEnum()) {
                type = FieldInspector.findFieldType(symbolTable, scope, n.getName());
            }
        }
        if (type == null) {
            Class<?> clazz = null;
            try {
                clazz = TypesLoaderVisitor.getClassLoader().loadClass(n.getName());

                String className = clazz.getName();
                type = new SymbolType(className);

            } catch (ClassNotFoundException e) {
                // a name expression could be "org.walkmod.A" and this node
                // could be "org.walkmod"

            }
        }
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
        n.setSymbolData(type);

    }

    @Override
    public void visit(ObjectCreationExpr n, A arg) {

        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
        }
        if (n.getTypeArgs() != null) {
            for (Type t : n.getTypeArgs()) {
                t.accept(this, arg);
            }
        }
        n.getType().accept(this, arg);

        if (!AnonymousClassUtil.isAnonymousClass(n) || AnonymousClassUtil.needsSymbolData(n)) {
            SymbolType st = (SymbolType) n.getType().getSymbolData();
            resolveConstructor(n, n.getArgs(), st, arg);
        }

        // we need to update the symbol table
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }

    }

    private ArgTypes argTypes(List<Expression> args, A arg) {
        boolean hasFunctionalExpressions = false;
        SymbolType[] symbolTypes = null;
        if (args != null) {

            symbolTypes = new SymbolType[args.size()];
            int i = 0;

            for (Expression e : args) {
                if (!(e instanceof LambdaExpr) && !(e instanceof MethodReferenceExpr)) {
                    e.accept(this, arg);
                    SymbolType argType = null;
                    if (e instanceof ObjectCreationExpr) {
                        ObjectCreationExpr aux = (ObjectCreationExpr) e;
                        argType = (SymbolType) aux.getType().getSymbolData();
                    } else {
                        argType = (SymbolType) e.getSymbolData();
                    }
                    symbolTypes[i] = argType;
                } else {
                    hasFunctionalExpressions = true;
                }
                i++;
            }
        } else {
            symbolTypes = new SymbolType[0];
        }
        return new ArgTypes(symbolTypes, hasFunctionalExpressions);
    }

    private void resolveConstructor(SymbolDataAware<SymbolData> n, final List<Expression> args, SymbolType st, A arg) {
        final ArgTypes argTypes = argTypes(args, arg);
        final SymbolType[] symbolTypes = argTypes.symbolTypes;
        final boolean hasFunctionalExpressions = argTypes.hasFunctionalExpressions;

        Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
        CompatibleConstructorArgsPredicate pred = new CompatibleConstructorArgsPredicate(symbolTypes);
        ArrayFilter<Constructor<?>> filter = new ArrayFilter<Constructor<?>>(null);
        filter.appendPredicate(pred);

        if (hasFunctionalExpressions) {
            filter.appendPredicate(new CompatibleFunctionalConstructorPredicate<A>(st, this, args, arg, symbolTable,
                    pred, symbolTypes));
        }
        CompositeBuilder<Constructor<?>> builder = new CompositeBuilder<Constructor<?>>();
        builder.appendBuilder(
                new GenericsBuilderFromConstructorParameterTypes(typeMapping, args, symbolTypes, symbolTable));

        try {
            SymbolType aux = ConstructorInspector.findConstructor(st, symbolTypes, filter, builder, typeMapping);
            n.setSymbolData(aux);
        } catch (Exception e) {
            throw new NoSuchExpressionTypeException(e);
        }
    }

    @Override
    public void visit(QualifiedNameExpr n, A arg) {
        StringBuilder name = new StringBuilder(n.getName());
        NameExpr aux = n.getQualifier();
        while (aux != null) {
            name.append(".").append(aux.getName());
            if (aux instanceof QualifiedNameExpr) {
                aux = ((QualifiedNameExpr) aux).getQualifier();
            } else {
                aux = null;
            }
        }
        SymbolType type = new SymbolType(name.toString());
        n.setSymbolData(type);
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
    }

    @Override
    public void visit(StringLiteralExpr n, A arg) {
        n.setSymbolData(new SymbolType("java.lang.String"));
    }

    @Override
    public void visit(SuperExpr n, A arg) {
        Expression classExpr = n.getClassExpr();
        if (classExpr == null) {
            n.setSymbolData(symbolTable.getType("super", ReferenceType.VARIABLE));
        } else {
            classExpr.accept(this, arg);
            SymbolType st = (SymbolType) classExpr.getSymbolData();

            Symbol<?> sType = symbolTable.findSymbol(st.getName(), ReferenceType.TYPE);
            boolean useReflection = true;
            if (sType != null && sType.getInnerScope() != null) {
                Scope scope = sType.getInnerScope();
                sType = scope.findSymbol("super");
                if (sType != null) {
                    st = sType.getType().clone();
                    useReflection = false;
                }
            }
            if (useReflection) {
                Class<?> superClass = st.getClazz().getSuperclass();
                if (superClass == null) {
                    superClass = Object.class;
                }
                st = new SymbolType(superClass);
            }
            n.setSymbolData(st);
        }
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
    }

    @Override
    public void visit(ThisExpr n, A arg) {
        Expression classExpr = n.getClassExpr();
        if (classExpr == null) {
            n.setSymbolData(symbolTable.getType("this", ReferenceType.VARIABLE));
        } else {
            classExpr.accept(this, arg);
            SymbolType st = (SymbolType) classExpr.getSymbolData();
            n.setSymbolData(st.clone());
        }
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
    }

    @Override
    public void visit(TypeExpr n, A arg) {
        super.visit(n, arg);
        n.setSymbolData(n.getType().getSymbolData());
    }

    @Override
    public void visit(MethodReferenceExpr n, A arg) {

        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }

    }

    public void visit(NormalAnnotationExpr n, A arg) {
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
        n.getName().accept(this, arg);
        n.setSymbolData(n.getSymbolData());
    }

    public void visit(MarkerAnnotationExpr n, A arg) {
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
        n.getName().accept(this, arg);
        n.setSymbolData(n.getSymbolData());
    }

    public void visit(SingleMemberAnnotationExpr n, A arg) {
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
        n.getName().accept(this, arg);
        n.setSymbolData(n.getSymbolData());
    }

    @Override
    public void visit(TypeParameter n, A arg) {
        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(LambdaExpr n, A arg) {
        if (semanticVisitor != null) {
            Scope previous = null;
            Symbol<?> ctxt = null;
            Method md = null;

            Node parent = n.getParentNode();
            if (parent instanceof VariableDeclarator || parent instanceof ReturnStmt) {
                Stack<Scope> scopes = symbolTable.getScopes();
                int j = scopes.size() - 1;
                while (ctxt == null && j >= 0) {
                    previous = scopes.get(j);
                    ctxt = previous.getRootSymbol();
                    j--;
                }

            }

            symbolTable.pushScope();
            List<Parameter> params = n.getParameters();
            if (params != null) {
                int i = 0;
                SymbolType[] args = new SymbolType[params.size()];
                java.lang.reflect.Type[] classes = null;
                for (Parameter p : params) {
                    if (md == null && ctxt != null) {
                        if (p.getType() == null && classes == null) {
                            Class<?> clazz = ctxt.getType().getClazz();
                            md = MethodInspector.getLambdaMethod(clazz, params.size());
                            Map<String, SymbolType> typeMapping = new HashMap<String, SymbolType>();
                            try {
                                SymbolType.valueOf(clazz, ctxt.getType(), typeMapping, null);

                                ClassInspector.updateTypeMappingOfInterfaceSubclass(clazz, md.getDeclaringClass(),
                                        typeMapping);

                                classes = md.getGenericParameterTypes();
                                for (int j = 0; j < classes.length; j++) {
                                    args[j] = SymbolType.valueOf(classes[i], typeMapping);
                                }
                            } catch (InvalidTypeException e) {
                                throw new NoSuchExpressionTypeException(e);
                            }
                        }

                        p.setSymbolData(args[i]);

                    }
                    p.accept(semanticVisitor, arg);
                    i++;
                }
            }
            Statement stmt = n.getBody();
            stmt.accept(semanticVisitor, arg);
            if (stmt instanceof ExpressionStmt) {
                stmt.setSymbolData(((ExpressionStmt) stmt).getExpression().getSymbolData());
            }
            symbolTable.popScope();
        }
    }

    @Override
    public void visit(ClassOrInterfaceType n, A arg) {
        super.visit(n, arg);

        String typeName = n.getName();
        ClassOrInterfaceType scope = n.getScope();

        SymbolType type = null;
        if (scope != null) {
            SymbolData data = scope.getSymbolData();
            if (data == null) {
                typeName = scope.toString() + "." + typeName;

            } else {
                typeName = data.getClazz().getCanonicalName() + "." + typeName;
            }
            if (n.getSymbolData() == null) {
                // we try to look the type into the symbol table
                Symbol<?> s = symbolTable.lookUpSymbolForRead(typeName, n, ReferenceType.TYPE, ReferenceType.VARIABLE);
                if (s != null) {
                    type = s.getType().clone();
                } else {
                    // if we don't find it, it is a full type name
                    Class<?> clazz = null;
                    if (data != null) {
                        // it is an inner class
                        typeName = data.getName() + "$" + n.getName();
                    }

                    if (data == null) {
                        try {
                            clazz = TypesLoaderVisitor.getClassLoader().loadClass(typeName);
                        } catch (ClassNotFoundException e) {
                        } catch (NoClassDefFoundError e2) {

                        }
                    }
                    if (clazz == null && data != null) {
                        SymbolType st = symbolTable.getType("this");

                        // there is no import nor a inner class inside the CU
                        // unit. We need to load it by reflection
                        clazz = ClassInspector.findClassMember(st.getClazz().getPackage(), n.getName(),
                                data.getClazz());
                    }
                    if (data != null && clazz == null) {
                        type = FieldInspector.findFieldType(symbolTable, (SymbolType) data, n.getName());
                        if (type == null) {
                            throw new NoSuchExpressionTypeException(
                                    "Ops! The class " + n.toString() + " can't be resolved", null);
                        }
                    }

                    if (clazz != null) {
                        type = new SymbolType(clazz);
                    }
                }
            }

        } else {

            if (n.getSymbolData() == null) {
                Symbol<?> s = null;
                Node parentNode = n.getParentNode();
                if (parentNode instanceof ObjectCreationExpr) {
                    ObjectCreationExpr expr = (ObjectCreationExpr) parentNode;
                    Expression grandParent = expr.getScope();
                    if (grandParent != null) {
                        Class<?> clazz = grandParent.getSymbolData().getClazz();
                        if (clazz.isAnonymousClass()) {

                            clazz = clazz.getSuperclass();
                        }
                        Symbol<?> parentSymbol = symbolTable.findSymbol(clazz.getCanonicalName(), ReferenceType.TYPE);
                        if (parentSymbol != null) {
                            Scope innerScope = parentSymbol.getInnerScope();
                            if (innerScope != null) {
                                symbolTable.pushScope(innerScope);

                                s = symbolTable.lookUpSymbolForRead(typeName, n, ReferenceType.TYPE_PARAM,
                                        ReferenceType.TYPE, ReferenceType.VARIABLE);

                                symbolTable.popScope();
                            }
                        }
                        if (s == null) {
                            typeName = clazz.getName() + "$" + typeName;
                            type = new SymbolType(typeName);
                        }
                    } else {
                        s = symbolTable.lookUpSymbolForRead(typeName, n, ReferenceType.TYPE_PARAM, ReferenceType.TYPE,
                                ReferenceType.VARIABLE);
                    }

                } else {
                    s = symbolTable.lookUpSymbolForRead(typeName, n, ReferenceType.TYPE_PARAM, ReferenceType.TYPE,
                            ReferenceType.VARIABLE);
                }
                if (s != null) {
                    type = s.getType().clone();

                } else {

                    if (type == null) {
                        type = ASTSymbolTypeResolver.getInstance().valueOf(n);
                    }

                }
            }
        }
        if (type != null) {
            List<Type> args = n.getTypeArgs();

            if (args != null && !args.isEmpty()) {
                List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();
                TypeVariable<?>[] vars = type.getClazz().getTypeParameters();
                int idx = 0;
                for (Type currentArg : args) {
                    SymbolType st = (SymbolType) currentArg.getSymbolData();
                    if (st == null) {
                        if (currentArg.toString().equals("?")) {

                            boolean found = false;

                            TypeVariable<?> var = vars[idx];
                            java.lang.reflect.Type[] bounds = var.getBounds();
                            List<SymbolType> varTypes = new LinkedList<SymbolType>();
                            if (bounds.length > 0) {
                                for (int i = 0; i < bounds.length && !found; i++) {
                                    try {
                                        varTypes.add(SymbolType.valueOf(bounds[i], null));
                                    } catch (InvalidTypeException e) {
                                        throw new NoSuchExpressionTypeException(e);
                                    }
                                }
                            } else {
                                varTypes.add(new SymbolType(Object.class));
                            }
                            st = new SymbolType(varTypes);

                            idx++;
                        } else {
                            st = new SymbolType(Object.class);
                        }
                    }
                    parameterizedTypes.add(st);
                }
                type.setParameterizedTypes(parameterizedTypes);
            } else {
                type.setParameterizedTypes(null);

            }
            n.setSymbolData(type);
        }
    }

    @Override
    public void visit(PrimitiveType n, A arg) {
        super.visit(n, arg);
        Primitive type = n.getType();
        if (type.equals(Primitive.Boolean)) {
            n.setSymbolData(new SymbolType("boolean"));
        } else if (type.equals(Primitive.Byte)) {
            n.setSymbolData(new SymbolType("byte"));
        } else if (type.equals(Primitive.Char)) {
            n.setSymbolData(new SymbolType("char"));
        } else if (type.equals(Primitive.Double)) {
            n.setSymbolData(new SymbolType("double"));
        } else if (type.equals(Primitive.Float)) {
            n.setSymbolData(new SymbolType("float"));
        } else if (type.equals(Primitive.Int)) {
            n.setSymbolData(new SymbolType("int"));
        } else if (type.equals(Primitive.Long)) {
            n.setSymbolData(new SymbolType("long"));
        } else if (type.equals(Primitive.Short)) {
            n.setSymbolData(new SymbolType("short"));
        }
    }

    @Override
    public void visit(org.walkmod.javalang.ast.type.ReferenceType n, A arg) {
        super.visit(n, arg);
        SymbolType newType = null;
        SymbolType st = (SymbolType) n.getType().getSymbolData();
        if (st != null) {
            newType = st.clone();
            newType.setArrayCount(n.getArrayCount());
        }
        n.setSymbolData(newType);
    }

    @Override
    public void visit(VoidType n, A arg) {
        super.visit(n, arg);
        n.setSymbolData(new SymbolType(void.class));
    }

    @Override
    public void visit(WildcardType n, A arg) {
        super.visit(n, arg);
        Type superType = n.getSuper();
        if (superType != null) {
            n.setSymbolData(superType.getSymbolData());
        } else {
            Type extendsType = n.getExtends();
            if (extendsType != null) {
                n.setSymbolData(extendsType.getSymbolData());
            }
        }
    }

    public void visit(VariableDeclarationExpr n, A arg) {

        if (semanticVisitor != null) {
            n.accept(semanticVisitor, arg);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(VariableDeclarator n, A arg) {
        n.getId().accept(this, arg);

        Expression init = n.getInit();
        if (init != null) {
            Symbol<?> aux = symbolTable.findSymbol(n.getId().getName(), ReferenceType.VARIABLE);
            Scope innerscope = new Scope(aux);
            aux.setInnerScope(innerscope);
            symbolTable.pushScope(innerscope);
            if (init instanceof LambdaExpr || init instanceof MethodReferenceExpr) {
                ArrayFilter<Method> filter = new ArrayFilter<Method>(null);
                SymbolType scope = symbolTable.getType(n.getId().getName(), ReferenceType.VARIABLE);
                filter.appendPredicate(
                        new CompatibleFunctionalMethodPredicate<A>(scope, this, null, arg, symbolTable, null, null));
                SymbolData sd = null;
                try {
                    sd = MethodInspector.findMethodType(scope, null, filter, null, null);
                } catch (Exception e) {
                    throw new NoSuchExpressionTypeException(e);
                }
                if (init instanceof LambdaExpr) {
                    init.setSymbolData(sd);
                } else {
                    init.setSymbolData(scope);
                    MethodReferenceExpr methodRef = (MethodReferenceExpr) init;
                    methodRef.accept(this, arg);
                }
            } else {
                init.accept(this, arg);
            }
            symbolTable.popScope();
        }
    }

    private SymbolType[] transformParams(List<Parameter> params) {
        int argsSize = 0;

        if (params != null) {
            argsSize = params.size();
        }
        SymbolType[] typeArgs = null;
        if (params != null) {
            typeArgs = new SymbolType[argsSize];
            Iterator<Parameter> it = params.iterator();
            for (int i = 0; i < typeArgs.length; i++) {
                Parameter param = it.next();
                if (param.getSymbolData() == null) {
                    param.getType().accept(this, null);
                    typeArgs[i] = (SymbolType) param.getType().getSymbolData();
                    VariableDeclaratorId id = param.getId();
                    if (id != null) {
                        int arrayCount = id.getArrayCount();
                        if (arrayCount > 0) {
                            typeArgs[i].setArrayCount(typeArgs[i].getArrayCount() + arrayCount);
                        }
                    }
                    if (param.isVarArgs()) {
                        typeArgs[i].setArrayCount(typeArgs[i].getArrayCount() + 1);
                    }

                }

            }
        }
        return typeArgs;
    }

    @Override
    public void visit(MethodDeclaration n, A arg) {

        SymbolType[] typeArgs = transformParams(n.getParameters());

        ArrayFilter<Method> filter = new ArrayFilter<Method>(null);
        filter.appendPredicate(new MethodsByNamePredicate(n.getName())).appendPredicate(new InvokableMethodsPredicate())
                .appendPredicate(new CompatibleArgsPredicate(typeArgs));
        Map<String, SymbolType> typeMapping = symbolTable.getTypeParams();

        try {
            final SymbolType scope = symbolTable.getType("this", ReferenceType.VARIABLE);
            SymbolType st = MethodInspector.findMethodType(scope, typeArgs, filter, null, typeMapping);

            if (st == null) {
                throw new NoSuchExpressionTypeException("Error locating method " + n.getName() + " with type args "
                        + (typeArgs == null ? "[]" : Arrays.asList(typeArgs)) + " and type params " + typeMapping
                        + " for parameters " + n.getParameters() + " in current class scope "
                        + scope
                );
            }
            SymbolType typeData = (SymbolType) n.getType().getSymbolData();
            SymbolType methodType = typeData.clone();
            methodType.setMethod(st.getMethod());
            n.setSymbolData(methodType);
        } catch (Exception e) {
            throw new NoSuchExpressionTypeException("Error resolving the signature of the method " + n.getName()
                    + " at [" + n.getBeginLine() + ", " + n.getBeginColumn() + "]", e);
        }

    }

    @Override
    public void visit(ConstructorDeclaration n, A arg) {
        SymbolType[] typeArgs = transformParams(n.getParameters());
        ArrayFilter<Constructor<?>> filter = new ArrayFilter<Constructor<?>>(null);
        filter.appendPredicate(new CompatibleConstructorArgsPredicate(typeArgs));
        try {
            SymbolType st = ConstructorInspector.findConstructor(symbolTable.getType("this", ReferenceType.VARIABLE),
                    typeArgs, filter);
            n.setSymbolData(st);
        } catch (Exception e) {
            throw new NoSuchExpressionTypeException(e);
        }
    }

    @Override
    public void visit(FieldDeclaration n, A arg) {
        List<VariableDeclarator> vds = n.getVariables();
        List<FieldSymbolData> result = new LinkedList<FieldSymbolData>();
        SymbolType thisType = symbolTable.getType("this", ReferenceType.VARIABLE);
        for (VariableDeclarator vd : vds) {
            String name = vd.getId().getName();
            SymbolType st = symbolTable.getType(name, ReferenceType.VARIABLE).clone();
            try {
                st.setField(thisType.getClazz().getDeclaredField(name));
            } catch (Exception e) {
                throw new NoSuchExpressionTypeException(
                        "Ops! We can't find the field " + name + " in " + thisType.getClazz().getName(), e);
            }
            result.add(st);
        }
        n.setFieldsSymbolData(result);
    }

    private static class ArgTypes {
        private final SymbolType[] symbolTypes;
        private final boolean hasFunctionalExpressions;

        private ArgTypes(SymbolType[] symbolTypes, boolean hasFunctionalExpressions) {
            this.symbolTypes = symbolTypes;
            this.hasFunctionalExpressions = hasFunctionalExpressions;
        }
    }
}
