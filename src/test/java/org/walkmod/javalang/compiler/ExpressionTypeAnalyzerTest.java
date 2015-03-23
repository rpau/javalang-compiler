package org.walkmod.javalang.compiler;

import java.beans.Expression;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.SourceVersion;

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.expr.ArrayAccessExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.UnaryExpr;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.ExpressionTypeAnalyzer;
import org.walkmod.javalang.visitors.SemanticVisitorAdapter;

public class ExpressionTypeAnalyzerTest extends SemanticTest {

	private ExpressionTypeAnalyzer<Map<String, Object>> expressionAnalyzer;

	@Override
	public CompilationUnit compile(String code) throws Exception {
		CompilationUnit cu = super.compile(code);
		SemanticVisitorAdapter<Map<String, Object>> semanticVisitor = 
				new SemanticVisitorAdapter<Map<String, Object>>();
		semanticVisitor.setSymbolTable(getSymbolTable());
		expressionAnalyzer = new ExpressionTypeAnalyzer<Map<String, Object>>(
				getTypeTable(), getSymbolTable(), semanticVisitor);
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
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(
				Expression.class, "a.length");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("int", type.getName());
	}

	@Test
	public void testArithmeticExpressionsWithEqualTypes() throws Exception {
		compile("public class A {}");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		symTable.pushSymbol("a", ReferenceType.VARIABLE, new SymbolType(
				int.class), null);
		symTable.pushSymbol("b", ReferenceType.VARIABLE, new SymbolType(
				int.class), null);
		BinaryExpr expr = (BinaryExpr) ASTManager
				.parse(Expression.class, "a+b");

		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("int", type.getName());
	}

	private SymbolType eval(Class<?> op1, Class<?> op2, String op)
			throws Exception {
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		symTable.pushSymbol("a", ReferenceType.VARIABLE, new SymbolType(op1),
				null);
		symTable.pushSymbol("b", ReferenceType.VARIABLE, new SymbolType(op2),
				null);
		BinaryExpr expr = (BinaryExpr) ASTManager.parse(Expression.class, "a "
				+ op + " b");

		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		symTable.popScope();
		return type;
	}

	private SymbolType eval(Class<?> op1, String op) throws Exception {
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		symTable.pushSymbol("a", ReferenceType.VARIABLE, new SymbolType(op1),
				null);

		UnaryExpr expr = (UnaryExpr) ASTManager.parse(Expression.class, op
				+ " a ");

		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		symTable.popScope();
		return type;
	}

	@Test
	public void testArithmeticExpressionsWithNonEqualTypes() throws Exception {
		compile("public class A {}");
		String[] ops = new String[] { "+", "-", "*", "/", "%" };
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
		String[] ops = new String[] { "&&", "||", "&", "|" };
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
		ArrayAccessExpr expr = (ArrayAccessExpr) ASTManager.parse(
				Expression.class, "a[0]");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		BinaryExpr expr = (BinaryExpr) ASTManager.parse(Expression.class,
				"\"Hello\"+a");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(
				Expression.class, "a.name");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("java.lang.String", type.getName());
	}

	@Test
	public void testEnumAccess() throws Exception {
		compile("public enum A { OPEN, CLOSE }");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		symTable.pushSymbol("A", ReferenceType.TYPE, new SymbolType(
				getClassLoader().loadClass("A")), null);
		FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(
				Expression.class, "A.OPEN");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class, "a.toString()");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class, "a.foo(\"hello\")");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class, "a.foo(\"hello\")");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class,
				"a.foo(1,\"hello\",\"hello\",\"hello\",\"hello\")");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
			FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(
					Expression.class, "a.bar");
			HashMap<String, Object> ctx = new HashMap<String, Object>();
			expressionAnalyzer.visit(expr, ctx);
			SymbolType type = (SymbolType) ctx
					.get(ExpressionTypeAnalyzer.TYPE_KEY);
			Assert.assertNotNull(type);
			Assert.assertEquals("java.util.List", type.getName());
			Assert.assertNotNull(type.getParameterizedTypes());
			Assert.assertEquals("java.lang.Integer", type
					.getParameterizedTypes().get(0).getName());
		}
	}

	@Test
	public void testRawTypes() throws Exception {
		compile("import java.util.List; import java.util.LinkedList; public class A { List bar = new LinkedList(); }");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
		symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
		FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(
				Expression.class, "a.bar");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("java.util.List", type.getName());
		Assert.assertNull(type.getParameterizedTypes());

	}

	@Test
	public void testBoundedTypeParameters() throws Exception {
		compile("public class A<T> { private T t;  public T get(){ return t; }}");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
		symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class, "a.get()");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("java.lang.Object", type.getName());
	}

	@Test
	public void testMultipleBoundParameters() throws Exception {
		compile("import java.io.Serializable;"
				+ "import java.io.Closeable;"
				+ "import java.io.IOException;"
				+ "public class A implements Closeable, Serializable {"
				+ " private Object t;  "
				+ " public <T extends Serializable & Closeable> T set(T a){ return a; }"
				+ " @Override public void close() throws IOException{}}");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
		symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class, "a.set(a)");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("A", type.getName());

		expr = (MethodCallExpr) ASTManager.parse(Expression.class,
				"a.set(a).close()");
		ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
	}

	@Test
	public void testGenericMethods() throws Exception {
		compile("import java.util.ArrayList; import java.io.Serializable;"
				+ " public class A { public static <T> T pick(T a1, T a2) { return a2; }}");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
		symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class, "A.pick(\"d\", new ArrayList<String>())");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("java.io.Serializable", type.getName());
	}

	@Test
	public void testGenericMethodsExplicitTypeInvocation() throws Exception {
		compile("import java.util.ArrayList; import java.io.Serializable;"
				+ " public class A { public static <T> T pick(T a1, T a2) { return a2; }}");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
		symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class,
				"A.<Serializable>pick(\"d\", new ArrayList<String>())");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
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
		ObjectCreationExpr expr = (ObjectCreationExpr) ASTManager.parse(
				Expression.class, "new A(a)");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("A", type.getName());
	}

	@Test
	public void testTargetInference() throws Exception {

		compile("import java.util.Collections; "
				+ "import java.util.List; "
				+ "public class A { public static void processStringList(List<String> stringList) {}}");
		SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		symTable.pushSymbol("A", ReferenceType.TYPE, st, null);

		MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
				Expression.class,
				"A.processStringList(Collections.emptyList());");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("void", type.getName());
	}

	@Test
	public void testLambdaExpressions() throws Exception {
		if (SourceVersion.latestSupported().ordinal() >= 8) {
			compile("import java.util.LinkedList; public class A{ public String getName() { return \"hello\"; }}");
			SymbolType st = new SymbolType(getClassLoader().loadClass(
					"java.util.LinkedList"));
			List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();
			parameterizedTypes.add(new SymbolType(getClassLoader().loadClass(
					"A")));
			st.setParameterizedTypes(parameterizedTypes);
			SymbolTable symTable = getSymbolTable();
			symTable.pushScope();
			symTable.pushSymbol("a", ReferenceType.TYPE, st, null);
			MethodCallExpr expr = (MethodCallExpr) ASTManager.parse(
					Expression.class,
					"a.stream().filter(p->p.getName().equals(\"hello\"))");
			HashMap<String, Object> ctx = new HashMap<String, Object>();
			expressionAnalyzer.visit(expr, ctx);
			SymbolType type = (SymbolType) ctx
					.get(ExpressionTypeAnalyzer.TYPE_KEY);
			Assert.assertNotNull(type);
		}
	}

}
