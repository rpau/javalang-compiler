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
package org.walkmod.javalang.compiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ParseException;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.expr.*;
import org.walkmod.javalang.compiler.symbols.*;
import org.walkmod.javalang.compiler.types.TypeVisitorAdapter;
import org.walkmod.javalang.exceptions.NoSuchExpressionTypeException;
import org.walkmod.javalang.test.SemanticTest;

import javax.lang.model.SourceVersion;
import java.beans.Expression;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TypeVisitorAdapterTest extends SemanticTest {

    private TypeVisitorAdapter<Map<String, Object>> expressionAnalyzer;

    public void populateSemantics() throws Exception {}

    @Override
    public CompilationUnit compile(String... code) throws Exception {
        return compile(false, code);
    }

    public CompilationUnit compile(boolean useTypeTable, String... code) throws Exception {

        CompilationUnit cu = super.compile(code);
        initTypes();
        SymbolVisitorAdapter<Map<String, Object>> semanticVisitor = new SymbolVisitorAdapter<Map<String, Object>>();
        semanticVisitor.setSymbolTable(getSymbolTable());

        expressionAnalyzer = new TypeVisitorAdapter<Map<String, Object>>(getSymbolTable(), semanticVisitor);

        semanticVisitor.setExpressionTypeAnalyzer(expressionAnalyzer);
        return cu;
    }

    @Test
    public void testVariableType() throws Exception {
        compile("public class A {}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(int.class);
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        NameExpr n = new NameExpr("a");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(n, ctx);
        SymbolType type = (SymbolType) n.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("int", type.getName());
    }

    @Test
    public void testArrayLength() throws Exception {
        compile("public class A {}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(char.class);
        st.setArrayCount(1);
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, "a.length");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("int", type.getName());
    }

    @Test
    public void testArithmeticExpressionsWithEqualTypes() throws Exception {
        compile("public class A {}");

        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        symTable.pushSymbol("a", ReferenceType.VARIABLE, new SymbolType(int.class), null);
        symTable.pushSymbol("b", ReferenceType.VARIABLE, new SymbolType(int.class), null);
        BinaryExpr expr = (BinaryExpr) ASTManager.parse(Expression.class, "a+b");

        HashMap<String, Object> ctx = new HashMap<String, Object>();

        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("int", type.getName());
    }

    private SymbolType eval(Class<?> op1, Class<?> op2, String op) throws Exception {
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        symTable.pushSymbol("a", ReferenceType.VARIABLE, new SymbolType(op1), null);
        symTable.pushSymbol("b", ReferenceType.VARIABLE, new SymbolType(op2), null);
        BinaryExpr expr = (BinaryExpr) ASTManager.parse(Expression.class, "a " + op + " b");

        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        symTable.popScope();
        return type;
    }

    private SymbolType eval(Class<?> op1, String op) throws Exception {
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        symTable.pushSymbol("a", ReferenceType.VARIABLE, new SymbolType(op1), null);

        UnaryExpr expr = (UnaryExpr) ASTManager.parse(Expression.class, op + " a ");

        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        symTable.popScope();
        return type;
    }

    @Test
    public void testArithmeticExpressionsWithNonEqualTypes() throws Exception {
        compile("public class A {}");
        String[] ops = new String[] {"+", "-", "*", "/", "%"};
        for (String op : ops) {
            SymbolType type = eval(int.class, double.class, op);
            Assert.assertNotNull(type);
            Assert.assertEquals("double", type.getName());
            type = eval(int.class, long.class, op);
            Assert.assertNotNull(type);
            Assert.assertEquals("long", type.getName());
            type = eval(int.class, float.class, op);
            Assert.assertNotNull(type);
            Assert.assertEquals("float", type.getName());
            type = eval(int.class, byte.class, op);
            Assert.assertNotNull(type);
            Assert.assertEquals("int", type.getName());
            type = eval(int.class, short.class, op);
            Assert.assertNotNull(type);
            Assert.assertEquals("int", type.getName());
        }
    }

    @Test
    public void testBooleanOps() throws Exception {
        compile("public class A {}");
        String[] ops = new String[] {"&&", "||", "&", "|"};
        for (String op : ops) {
            SymbolType type = eval(boolean.class, boolean.class, op);
            Assert.assertNotNull(type);
            Assert.assertEquals("boolean", type.getName());
        }
        SymbolType type = eval(boolean.class, "!");
        Assert.assertNotNull(type);
        Assert.assertEquals("boolean", type.getName());
    }

    @Test
    public void testArrayContent() throws Exception {
        compile("public class A {}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(char.class);
        st.setArrayCount(1);
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        ArrayAccessExpr expr = (ArrayAccessExpr) ASTManager.parse(Expression.class, "a[0]");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("char", type.getName());
    }

    @Test
    public void testStringConcatenation() throws Exception {
        compile("public class A {}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(int.class);
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        BinaryExpr expr = (BinaryExpr) ASTManager.parse(Expression.class, "\"Hello\"+a");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testFieldAccess() throws Exception {
        compile("public class A { String name; }");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, "a.name");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testEnumAccess() throws Exception {
        compile("public enum A { OPEN, CLOSE }");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        symTable.pushSymbol("A", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("A")), null);
        FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, "A.OPEN");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("A", type.getName());
    }

    @Test
    public void testMethodWithoutArgsAccess() throws Exception {
        compile("public class A { }");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.toString()");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testMethodInference() throws Exception {
        compile("public class A { public String foo(String bar) {return bar;} }");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.foo(\"hello\")");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testMethodInferenceWithSubClassesAsArgument() throws Exception {
        compile("public class A { public String foo(Object bar) {return bar.toString();} }");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.foo(\"hello\")");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testMethodWithDynamicArgs() throws Exception {
        compile("public class A { public String foo(int bar, String... others) {return bar+\"hello+\";} }");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr =
                (MethodCallExpr) ASTManager.parse(Expression.class, "a.foo(1,\"hello\",\"hello\",\"hello\",\"hello\")");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testDiamond() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 7) {
            compile("import java.util.List; import java.util.LinkedList; public class A{ List<Integer> bar = new LinkedList<>(); }");
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
            symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
            FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, "a.bar");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("java.util.List", type.getName());
            Assert.assertNotNull(type.getParameterizedTypes());
            Assert.assertEquals("java.lang.Integer", type.getParameterizedTypes().get(0).getName());
        }
    }

    @Test
    public void testRawTypes() throws Exception {
        compile("import java.util.List; import java.util.LinkedList;"
                + " public class A { List bar = new LinkedList(); List<Object> foo = new LinkedList<Object>();}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);

        SymbolType qualified = getSymbolType("a.foo");
        Assert.assertNotNull(qualified);
        Assert.assertEquals("java.util.List", qualified.getName());
        Assert.assertNotNull(qualified.getParameterizedTypes());
        Assert.assertEquals("java.lang.Object", qualified.getParameterizedTypes().get(0).getName());

        FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, "a.bar");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.util.List", type.getName());
        Assert.assertNull(type.getParameterizedTypes());
    }

    private SymbolType getSymbolType(final String name) throws ParseException {
        FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, name);
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        return (SymbolType) expr.getSymbolData();
    }

    @Test
    public void testBoundedTypeParameters() throws Exception {
        compile("public class A<T> { private T t;  public T get(){ return t; }}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.get()");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.Object", type.getName());
    }

    @Test
    public void testMultipleBoundParameters() throws Exception {
        compile("import java.io.Serializable;" + "import java.io.Closeable;" + "import java.io.IOException;"
                + "public class A implements Closeable, Serializable {" + " private Object t;  "
                + " public <T extends Serializable & Closeable> T set(T a){ return a; }"
                + " @Override public void close() throws IOException{}}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.set(a)");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("A", type.getName());

        expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.set(a).close()");
        ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
    }

    @Test
    public void testGenericMethods() throws Exception {
        compile("import java.util.ArrayList; import java.io.Serializable;"
                + " public class A { public static <T> T pick(T a1, T a2) { return a2; }}");
        SymbolTable symTable = getSymbolTable();
        ASTSymbolTypeResolver.getInstance().setSymbolTable(symTable);
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

        MethodCallExpr expr =
                (MethodCallExpr) ASTManager.parse(Expression.class, "A.pick(\"d\", new ArrayList<String>())");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.io.Serializable", type.getName());
    }

    @Test
    public void testGenericMethodOverload() throws Exception {
        compile("import java.util.*;\n" + " public class A {\n" + "    public static void f1(Set<Integer> col) {}\n"
                + "\n" + "    public static void f1(Collection<String> col) {}\n" + "\n"
                /*
                + "    public static void f1usage() {\n"
                + "        // select first\n"
                + "        f1(new HashSet<Integer>());\n"
                + "        // select second\n"
                + "        f1(new ArrayList<String>());\n"
                + "        // select second\n"
                + "        f1(new HashSet<String>());\n"
                + "    }\n"
                */
                + "}");
        SymbolTable symTable = getSymbolTable();
        ASTSymbolTypeResolver.getInstance().setSymbolTable(symTable);
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

        assertResolvedToMethod(expressionAnalyzer, "A.f1(new HashSet<Integer>())",
                "public static void A.f1(java.util.Set)");
        assertResolvedToMethod(expressionAnalyzer, "A.f1(new ArrayList<String>())",
                "public static void A.f1(java.util.Collection)");
        assertResolvedToMethod(expressionAnalyzer, "A.f1(new HashSet<String>())",
                "public static void A.f1(java.util.Collection)");
    }

    @Test
    public void testUnboundGenericMethodReturnIsNotValid() throws Exception {
        /**
         * javalang-compiler primary concern is providing symbol/type/semantic information for valid java code.
         * this test is different because it asserts how it fails for invalid code.
         * If the error messages do change the test needs to be adjusted.
         */
        final String code = "" + " public class A {\n" + "    public static <T> T anyObject() {\n"
                + "        return null;\n" + "    }" + "    public static void fs(String s) {}\n"
                + "    public static void fi(Integer i) {}\n" + "    public static void usage() { /**/; } \n" + "}";
        final String okstring = "A.fs(\"s\")";
        final String okint = "A.fi(1)";
        final String failstring = "A.fs(anyObject())";
        final String failint = "A.fi(anyObject())";

        // assert that expressions okstring/okint are valid
        Assert.assertNotNull(populateSemantics(compile(code.replace("/**/", okstring))));
        Assert.assertNotNull(populateSemantics(compile(code.replace("/**/", okint))));

        // assert that expressions failstring/failint are not valid
        try {
            populateSemantics(compile(code.replace("/**/", failstring)));
            fail();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("The method call A.fs(anyObject()) is not resolved.");
        }
        try {
            populateSemantics(compile(code.replace("/**/", failint)));
            fail();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("The method call A.fi(anyObject()) is not resolved.");
        }

        compile(code);
        SymbolTable symTable = getSymbolTable();
        ASTSymbolTypeResolver.getInstance().setSymbolTable(symTable);
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("A", ReferenceType.TYPE, st, null);
        symTable.pushSymbol("this", ReferenceType.VARIABLE, st, null);

        // now tests on expression level

        // test non-generic cases as base line
        assertResolvedToMethod(expressionAnalyzer, okstring, "public static void A.fs(java.lang.String)");
        assertResolvedToMethod(expressionAnalyzer, okint, "public static void A.fi(java.lang.Integer)");
        // now the real test
        try {
            assertResolvedToMethod(expressionAnalyzer, failstring, "*unused*");
            fail();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("The method call A.fs(anyObject()) is not resolved.");
        }
        try {
            assertResolvedToMethod(expressionAnalyzer, failint, "*unused*");
            fail();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("The method call A.fi(anyObject()) is not resolved.");
        }
    }

    private static void assertResolvedToMethod(TypeVisitorAdapter<Map<String, Object>> expressionAnalyzer,
            final String expression, final String expectedMethod) throws Exception {
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, expression);
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals(expectedMethod, type.getMethod().toString());
    }

    @Test
    public void testGenericMethodsExplicitTypeInvocation() throws Exception {
        compile("import java.util.ArrayList; import java.io.Serializable;"
                + " public class A { public static <T> T pick(T a1, T a2) { return a2; }}");
        SymbolTable symTable = getSymbolTable();
        ASTSymbolTypeResolver.getInstance().setSymbolTable(symTable);
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                "A.<Serializable>pick(\"d\", new ArrayList<String>())");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.io.Serializable", type.getName());
    }

    @Test
    public void testGenericsInConstructors() throws Exception {
        compile("public class A<X> { <T extends A> A(T t) {}}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        ObjectCreationExpr expr = (ObjectCreationExpr) ASTManager.parse(Expression.class, "new A(a)");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("A", type.getName());
    }

    @Test
    public void testTargetInference() throws Exception {

        compile("import java.util.Collections; " + "import java.util.List; "
                + "public class A { public static void processStringList(List<String> stringList) {}}");
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

        MethodCallExpr expr =
                (MethodCallExpr) ASTManager.parse(Expression.class, "A.processStringList(Collections.emptyList());");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("void", type.getName());
    }

    //@Test: TODO: https://docs.oracle.com/javase/tutorial/java/generics/genTypeInference.html
    public void testTargetInference2() throws Exception {

        compile("import java.util.Collections; " + "import java.util.List; "
                + "public class A { public static void processAList(List<A> stringList) {}}");
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

        MethodCallExpr expr =
                (MethodCallExpr) ASTManager.parse(Expression.class, "A.processAList(Collections.emptyList());");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("void", type.getName());
    }

    // TODO4: Las declaraciones no tienen su tipo (metodos y campos)

    @Test
    public void testComplexArrayContent() throws Exception {
        compile("public class A {}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(char.class);
        st.setArrayCount(2);
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        ArrayAccessExpr expr = (ArrayAccessExpr) ASTManager.parse(Expression.class, "a[0]");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("char", type.getName());
        Assert.assertEquals(1, type.getArrayCount());

        expr = (ArrayAccessExpr) ASTManager.parse(Expression.class, "a[0][0]");

        ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("char", type.getName());
        Assert.assertEquals(0, type.getArrayCount());
    }

    @Test
    public void testSimpleLambdaExpressions() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {

            compile("import java.util.LinkedList; public class A{ public String getName() { return \"hello\"; }}");
            SymbolType st = new SymbolType(getClassLoader().loadClass("java.util.LinkedList"));
            List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();
            parameterizedTypes.add(new SymbolType(getClassLoader().loadClass("A")));
            st.setParameterizedTypes(parameterizedTypes);
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("a", ReferenceType.TYPE, st, null);
            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "a.stream().filter(p->p.getName().equals(\"hello\"))");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("java.util.stream.Stream", type.getName());
            Assert.assertEquals("A", type.getParameterizedTypes().get(0).getName());
        }
    }

    @Test
    public void testAmbiguousLambdaExpressions() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String BCode = "public interface B { public String execute(); } ";
            String CCode = "public interface C { public int execute(); }";

            compile("public class A{ public void run(B b){} public void run(C c){} " + BCode + CCode + "}");
            SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("a", ReferenceType.TYPE, st, null);

            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.run(()->1+2)");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$C", type.getMethod().getParameterTypes()[0].getName());
        }
    }

    @Test
    public void testLambdaExpressionsWithExplicitArgs() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String BCode = "public interface B { public int execute(int c); } ";

            compile("public class A{ public void run(B b){} " + BCode + "}");
            SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("a", ReferenceType.TYPE, st, null);

            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.run((int d)->d+1)");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$B", type.getMethod().getParameterTypes()[0].getName());
        }
    }

    @Test
    public void testMethodCallWithMultipleLambdaArgs() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String BCode = "public interface B { public int execute(int c); } ";

            compile("public class A{ public void run(B b, B c){} " + BCode + "}");
            SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("a", ReferenceType.TYPE, st, null);

            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.run((int d)->d+1, d->d)");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$B", type.getMethod().getParameterTypes()[0].getName());
        }
    }

    @Test
    public void testLambdaExpressionsWithBody() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String BCode = "public interface B { public int execute(int c); } ";

            compile("public class A{ public void run(B b){} " + BCode + "}");
            SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("a", ReferenceType.TYPE, st, null);

            MethodCallExpr expr =
                    (MethodCallExpr) ASTManager.parse(Expression.class, "a.run((int d)->{ return d+1; })");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$B", type.getMethod().getParameterTypes()[0].getName());

            expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "a.run((int d)->{ if(d > 0) {return 1;} else {return 0;} })");
            ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$B", type.getMethod().getParameterTypes()[0].getName());

            expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "a.run((int d)->{  if (true) return 12; else { int result = 15;for (int i = 1; i < 10; i++) result *= i; return result;}})");
            ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$B", type.getMethod().getParameterTypes()[0].getName());
        }
    }

    @Test
    public void testLambdaExpressionsWithVoidResult() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String BCode = "public interface B { public void execute(int c); } ";

            compile("public class A{ public void run(B b){} " + BCode + "}");
            SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("a", ReferenceType.TYPE, st, null);

            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.run((int d)->{ return; })");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$B", type.getMethod().getParameterTypes()[0].getName());

            expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "a.run((int d)->{ System.out.println(\"hello\"); })");
            ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("A$B", type.getMethod().getParameterTypes()[0].getName());
        }

    }

    @Test
    public void testMethodReferenceWithStaticMethod() throws Exception {

        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String codeBase =
                    "public class Person{ int age; public static int compareByAge(Person p1, Person p2){ return p2.age -p1.age; }}";
            String codeMain = "import java.util.Arrays; " + codeBase;
            compile(codeMain);
            SymbolType st = new SymbolType(getClassLoader().loadClass("Person"));
            st.setArrayCount(1);
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("rosterAsArray", ReferenceType.TYPE, st, null);
            symTable.pushSymbol("this", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("Person")), null);

            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "Arrays.sort(rosterAsArray, Person::compareByAge)");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("sort", type.getMethod().getName());
            MethodReferenceExpr arg1 = (MethodReferenceExpr) expr.getArgs().get(1);

            SymbolType methodType = (SymbolType) arg1.getReferencedMethodSymbolData();

            Assert.assertNotNull(methodType);
            Assert.assertEquals("compare", methodType.getMethod().getName());
        }
        // https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html
    }

    @Test
    public void testMethodReferenceOfAParticularObject() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            String comparator =
                    "public class ComparisonProvider { public int compareByAge(Person p1, Person p2) { return p2.age -p1.age;}}";

            String codeBase = "public class Person{ public ComparisonProvider myComparisonProvider; int age; "
                    + comparator + " }";

            String codeMain = "import java.util.Arrays; " + codeBase;
            compile(codeMain);
            Class<?> clazz = getClassLoader().loadClass("Person");
            SymbolType st = new SymbolType(clazz);
            st.setArrayCount(1);
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("rosterAsArray", ReferenceType.TYPE, st, null);
            symTable.pushSymbol("this", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("Person")), null);
            st = new SymbolType(getClassLoader().loadClass("Person$ComparisonProvider"));
            st.setField(clazz.getField("myComparisonProvider"));

            symTable.pushSymbol("myComparisonProvider", ReferenceType.VARIABLE, st, null);

            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "Arrays.sort(rosterAsArray, myComparisonProvider::compareByAge)");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("sort", type.getMethod().getName());
            MethodReferenceExpr arg1 = (MethodReferenceExpr) expr.getArgs().get(1);

            SymbolType methodType = (SymbolType) arg1.getReferencedMethodSymbolData();
            Assert.assertNotNull(methodType);
            Assert.assertEquals("compare", methodType.getMethod().getName());
        }
    }

    @Test
    public void testMethodReferencesToAnArrayItem() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            compile("import java.util.Arrays; public class A{}");
            SymbolType st = new SymbolType(String.class);
            st.setArrayCount(1);
            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();
            symTable.pushSymbol("stringArray", ReferenceType.TYPE, st, null);
            symTable.pushSymbol("this", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("A")), null);
            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "Arrays.sort(stringArray, String::compareToIgnoreCase)");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("sort", type.getMethod().getName());
            MethodReferenceExpr arg1 = (MethodReferenceExpr) expr.getArgs().get(1);

            SymbolType methodType = (SymbolType) arg1.getReferencedMethodSymbolData();
            Assert.assertNotNull(methodType);
            Assert.assertEquals("compare", methodType.getMethod().getName());
        }
    }

    @Test
    public void testMethodReferencesToConstructors() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {
            compile("import java.util.HashSet; public class A{ public static void foo(B b) {} public interface B{ public Object get();}}");
            SymbolTable symTable = getSymbolTable();
            symTable.pushSymbol("this", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("A")), null);
            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "A.foo(HashSet::new)");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("foo", type.getMethod().getName());
            MethodReferenceExpr arg1 = (MethodReferenceExpr) expr.getArgs().get(0);

            SymbolType methodType = (SymbolType) arg1.getReferencedMethodSymbolData();
            Assert.assertNotNull(methodType);
            Assert.assertEquals("get", methodType.getMethod().getName());
        }
    }

    @Test
    public void testMethodReferencesToConstructors2() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {

            String method = "public static " + "<T, SOURCE extends Collection<T>, DEST extends Collection<T>>"
                    + " DEST transferElements( SOURCE sourceCollection, Supplier<DEST> collectionFactory)"
                    + " { return collectionFactory.get(); } ";

            String interfaceSupplier = "public interface Supplier<T>{ public T get(); }";

            compile("import java.util.*; public class A{ " + method + interfaceSupplier + "}");

            MethodCallExpr expr =
                    (MethodCallExpr) ASTManager.parse(Expression.class, "A.transferElements(roster, HashSet::new)");

            SymbolType st = new SymbolType(java.util.Collection.class);
            List<SymbolType> paramTypes = new LinkedList<SymbolType>();
            paramTypes.add(new SymbolType(String.class));
            st.setParameterizedTypes(paramTypes);

            SymbolTable symTable = getSymbolTable();
            symTable.pushScope();

            // roster is a Collection<String>
            symTable.pushSymbol("this", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("A")), null);
            symTable.pushSymbol("roster", ReferenceType.TYPE, st, null);

            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("transferElements", type.getMethod().getName());
            MethodReferenceExpr arg1 = (MethodReferenceExpr) expr.getArgs().get(1);

            SymbolType methodType = (SymbolType) arg1.getReferencedMethodSymbolData();
            Assert.assertNotNull(methodType);
            Assert.assertEquals("get", methodType.getMethod().getName());
            Assert.assertEquals("A$Supplier", arg1.getSymbolData().getName());

        }
    }

    @Test
    public void testMethodReferencesToConstructors3() throws Exception {
        if (SourceVersion.latestSupported().ordinal() >= 8) {

            String method = "public static " + "<T, SOURCE extends Collection<T>, DEST extends Collection<T>>"
                    + " DEST transferElements( SOURCE sourceCollection, Supplier<DEST> collectionFactory)"
                    + " { return collectionFactory.get(); } ";

            String interfaceSupplier = "public interface Supplier<T>{ public T get(); }";

            compile("import java.util.*; public class A{ " + method + interfaceSupplier + "}");

            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "A.transferElements(roster, HashSet<Person>::new)");

            SymbolType st = new SymbolType(java.util.Collection.class);
            List<SymbolType> paramTypes = new LinkedList<SymbolType>();
            paramTypes.add(new SymbolType(String.class));
            st.setParameterizedTypes(paramTypes);

            SymbolTable symTable = getSymbolTable();
            ASTSymbolTypeResolver.getInstance().setSymbolTable(symTable);
            symTable.pushScope();
            // roster is a Collection<String>
            symTable.pushSymbol("this", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("A")), null);
            symTable.pushSymbol("roster", ReferenceType.TYPE, st, null);

            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            SymbolType type = (SymbolType) expr.getSymbolData();
            Assert.assertNotNull(type);
            Assert.assertEquals("transferElements", type.getMethod().getName());
            MethodReferenceExpr arg1 = (MethodReferenceExpr) expr.getArgs().get(1);
            SymbolType methodType = (SymbolType) arg1.getSymbolData();
            Assert.assertNotNull(methodType);
            Assert.assertEquals("get", arg1.getReferencedMethodSymbolData().getMethod().getName());
            Assert.assertEquals("java.lang.String", arg1.getReferencedMethodSymbolData().getName());

            Assert.assertEquals("A$Supplier", methodType.getName());
        }
    }

    @Test
    public void testDynamicArgsWithoutArguments() throws Exception {
        compile("public class A { public String foo(String... others) {return \"hello+\";} }");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.foo()");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testMethodsOverwriting() throws Exception {
        compile("public class A{ public Object get() { return null; } public class B extends A{ public String get(){ return \"hello\"; }}}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A$B"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class, "a.get()");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expressionAnalyzer.visit(expr, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testFieldsOverwriting() throws Exception {
        compile("public class A { private Object name; public class B{ private String name; }}");
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("A$B"));
        symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
        org.walkmod.javalang.ast.expr.Expression expr =
                (org.walkmod.javalang.ast.expr.Expression) ASTManager.parse(Expression.class, "a.name");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expr.accept(expressionAnalyzer, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("java.lang.String", type.getName());
    }

    @Test
    public void testReferencesToInnerClasses() throws Exception {
        compile(true, "public class OuterClass { public class InnerClass { } }");

        SymbolTable symTable = getSymbolTable();
        ASTSymbolTypeResolver.getInstance().setSymbolTable(symTable);
        symTable.pushScope();
        SymbolType st = new SymbolType(getClassLoader().loadClass("OuterClass"));
        symTable.pushSymbol("outerObject", ReferenceType.VARIABLE, st, null);
        org.walkmod.javalang.ast.expr.Expression expr = (org.walkmod.javalang.ast.expr.Expression) ASTManager
                .parse(Expression.class, "outerObject.new InnerClass()");
        HashMap<String, Object> ctx = new HashMap<String, Object>();
        expr.accept(expressionAnalyzer, ctx);
        SymbolType type = (SymbolType) expr.getSymbolData();
        Assert.assertNotNull(type);
        Assert.assertEquals("OuterClass$InnerClass", type.getName());
    }

    @Test
    public void testTargetInferenceShouldFail() throws Exception {

        compile("import java.util.Collections; " + "import java.util.List; "
                + "public class A { public static <T> void processStringList(T[] args) {}}");
        SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
        SymbolTable symTable = getSymbolTable();
        symTable.pushScope();
        symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

        try {
            MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(Expression.class,
                    "A.processStringList(Collections.emptyList());");
            HashMap<String, Object> ctx = new HashMap<String, Object>();
            expressionAnalyzer.visit(expr, ctx);
            fail("should not be reached: type=" + expr.getSymbolData().getMethod());
        } catch (NoSuchExpressionTypeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage()
                    .contains("Ops! The method call A.processStringList(Collections.emptyList()) is not resolved"));
        }
    }

}
