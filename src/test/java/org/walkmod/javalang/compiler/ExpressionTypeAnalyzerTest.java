package org.walkmod.javalang.compiler;

import java.beans.Expression;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.expr.ArrayAccessExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.UnaryExpr;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.ExpressionTypeAnalyzer;

public class ExpressionTypeAnalyzerTest extends SemanticTest {

	private ExpressionTypeAnalyzer<Map<String, Object>> expressionAnalyzer;

	@Override
	public CompilationUnit compile(String code) throws Exception {
		CompilationUnit cu = super.compile(code);
		expressionAnalyzer = new ExpressionTypeAnalyzer<Map<String, Object>>(
				getTypeTable(), getSymbolTable());
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
		Assert.assertEquals(type.getName(), "int");
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
		Assert.assertEquals(type.getName(), "int");
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
		Assert.assertEquals(type.getName(), "int");
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
		Assert.assertEquals(type.getName(), "char");
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
		FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, "a.name");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("java.lang.String", type.getName());
	}
	
	@Test
	public void testEnumAccess() throws Exception{
		compile("public enum A { OPEN, CLOSE }");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		symTable.pushSymbol("A", ReferenceType.TYPE, new SymbolType(getClassLoader().loadClass("A")), null);
		FieldAccessExpr expr = (FieldAccessExpr) ASTManager.parse(Expression.class, "A.OPEN");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("A", type.getName());
	}
	
	@Test
	public void testMethodWithoutArgsAccess() throws Exception{
		compile("public class A { }");
		SymbolTable symTable = getSymbolTable();
		symTable.pushScope();
		SymbolType st = new SymbolType(getClassLoader().loadClass("A"));
		symTable.pushSymbol("a", ReferenceType.VARIABLE, st, null);
		MethodCallExpr expr = (MethodCallExpr)ASTManager.parse(Expression.class, "a.toString()");
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		expressionAnalyzer.visit(expr, ctx);
		SymbolType type = (SymbolType) ctx.get(ExpressionTypeAnalyzer.TYPE_KEY);
		Assert.assertNotNull(type);
		Assert.assertEquals("java.lang.String", type.getName());
	}
	
	

}
