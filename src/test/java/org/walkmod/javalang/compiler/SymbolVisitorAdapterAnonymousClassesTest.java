package org.walkmod.javalang.compiler;

import org.junit.Assert;
import org.junit.Test;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.test.assertj.AstAssertions;
import org.walkmod.javalang.compiler.test.assertj.TypeDeclarationAssert;

public class SymbolVisitorAdapterAnonymousClassesTest extends SymbolVisitorAdapterTestSupport {

	@Test
	public void testAnonymousClassNameGenerationBug() throws Exception {
		// the bug incremented the anonymous counter twice, so the CompilationUnit could not be created
		// because the class could not be resolved
		String code = ""
				+ "public final class AProvider {\n"
				+ "    public final static class Factory {\n"
				+ "        protected Object[] create() {\n"
				+ "            return new Object[] {\n"
				+ "                    new java.io.Serializable() {\n"
				+ "                    }\n"
				+ "            };\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		CompilationUnit cu = run(code);
		Assert.assertNotNull(cu);
	}

	@Test
	public void testAnonymousClassNameGenerationInConstructorBug() throws Exception {
		// the bug incremented the anonymous counter twice, so the CompilationUnit could not be created
		// because the class could not be resolved
		String code = ""
				+ "public final class A {\n"
				+ "    public static class Base { Base(Object o) {} }\n"
				+ "    public static class Usage extends Base {\n"
				+ "        Usage() {\n"
				+ "            super(new java.io.Serializable() {});\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		CompilationUnit cu = run(code);
		Assert.assertNotNull(cu);
	}

    @Test
    public void testConditionalCompilationEliminatesAnonymousClasses() throws Exception {
		// Note: The anonymous class counter is incremented for disabled code but no class is generated.
        // JLS 14.21. Unreachable Statements, http://docs.oracle.com/javase/specs/jls/se8/html/jls-14.html#jls-14.21
        String code = ""
				+ "import java.io.Serializable;\n"
				+ "public class U {\n"
                + "  protected void set(Serializable s) { }\n"
                + "  private static final boolean ENABLED = false;\n"
				+ "  private static final Boolean ENABLED2 = false;\n"
				+ "  private static final boolean DISABLED = true;\n"
				+ "  private static final Boolean DISABLED2 = true;\n"
                + "  protected void resolvedBefore() { set(new Serializable() {}); }\n"
                + "  protected void deadCode() {\n"
				+ "   if (ENABLED) { set(new  Serializable() {}); }\n"
				+ "   if (ENABLED2) { set(new  Serializable() {}); }\n"
				+ "   if (!DISABLED) { set(new  Serializable() {}); }\n"
				+ "   if (!DISABLED2) { set(new  Serializable() {}); }\n"
				+ "   if (false && Math.sin(0) > -2) { set(new  Serializable() {}); }\n"
				+ "   if (false || (ENABLED && Math.sin(0) > -2)) { set(new  Serializable() {}); }\n"
				+ "   if (false == true) { set(new  Serializable() {}); }\n"
				+ "   if (false != false) { set(new  Serializable() {}); }\n"
				+ "   if (1 > 2) { set(new  Serializable() {}); }\n"
				+ "   if (1 > 2L) { set(new  Serializable() {}); }\n"
				+ "   if (1L > 2) { set(new  Serializable() {}); }\n"
				+ "   if (1L > 2L) { set(new  Serializable() {}); }\n"
				+ "   if (1.0 > 2.0) { set(new  Serializable() {}); }\n"
				+ "   if ('a' > 'b') { set(new  Serializable() {}); }\n"
				+ " }\n"
                + "  protected void resolvedAfter() { set(new Serializable() {}); }\n"
				+ "}";
        CompilationUnit cu = run(code);
        // if conditional compilation condition is not properly detected a class not found exception is thrown.
		final TypeDeclarationAssert clazz = AstAssertions.assertThat(cu)
				.types().item(0)
				.hasName("U")
				;
		clazz.members().item(5).asMethodDeclaration()
				.hasName("resolvedBefore")
				.body().stmts().item(0).asExpressionStmt().expression().asMethodCallExpr()
				.hasName("set").args().item(0)
				.symbolData().name().isEqualTo("U$1");

		// 14 (disabled) anonymous classes in between
		clazz.members().item(7).asMethodDeclaration()
				.hasName("resolvedAfter")
				.body().stmts().item(0).asExpressionStmt().expression().asMethodCallExpr()
				.hasName("set").args().item(0)
				.symbolData().name().isEqualTo("U$16");
	}

	@Test
	public void testConditionalCompilationFallsBackToBaseType() throws Exception {
		// Note: The anonymous class counter is incremented for disabled code but no class is generated.
		// JLS 14.21. Unreachable Statements, http://docs.oracle.com/javase/specs/jls/se8/html/jls-14.html#jls-14.21
		// The symbol should at least be bound by the base class.
		String code = ""
				+ "import java.io.Serializable;\n"
				+ "public class U {\n"
				+ "  protected void set(Serializable s) { }\n"
				+ "  private static final boolean ENABLED = false;\n"
				+ "  protected void resolvedBefore() { set(new Serializable() {}); }\n"
				+ "  protected void deadCode() {\n"
				+ "   if (ENABLED) { set(new  Serializable() {}); }\n"
				+ " }\n"
				+ "  protected void resolvedAfter() { set(new Serializable() {}); }\n"
				+ "}";
		CompilationUnit cu = run(code);
		final TypeDeclarationAssert clazz = AstAssertions.assertThat(cu)
				.types().item(0)
				.hasName("U")
				;
		clazz.members().item(2).asMethodDeclaration()
				.hasName("resolvedBefore")
				.body().stmts().item(0).asExpressionStmt().expression().asMethodCallExpr()
				.hasName("set").args().item(0)
				.symbolData().name().isEqualTo("U$1");
		clazz.members().item(4).asMethodDeclaration()
				.hasName("resolvedAfter")
				.body().stmts().item(0).asExpressionStmt().expression().asMethodCallExpr()
				.hasName("set").args().item(0)
				.symbolData().name().isEqualTo("U$3");

		// no class could be loaded but the symbol type should at least resolve to
        // the base class
		clazz.members().item(3).asMethodDeclaration()
				.hasName("deadCode")
				.body().stmts().item(0).asIfStmt()
				.thenStmt().asBlockStmt().stmts().item(0).asExpressionStmt()
				.expression().asMethodCallExpr()
				.hasName("set").args().item(0)
				.symbolData().name().isEqualTo("java.io.Serializable");
	}

	@Test
	public void testNestedMultipleAnonymousClasses() throws Exception {
		String otherIteratorCode = "public interface OtherIterator{ public void expand(); } "; // $1$1
		String adaptedIteratorCode =
				"import java.util.Iterator; public abstract class AdaptedIterator<T> implements Iterator<T>{ public AdaptedIterator(OtherIterator aux){} public abstract String adapt(); public T next() { return null; } public void remove(){} public boolean hasNext() { return false; } }"; // $1$2
		String iteratorCode =
				"public Iterator<String> iterator() { return new AdaptedIterator<String>( new OtherIterator() { public void expand() {} }){ public String adapt() { return null; }}; }";
		String code =
				"import java.util.Iterator; public class NestedMultipleAnonymousClasses { public Iterable<String> list() { return new Iterable<String>() { "
						+ iteratorCode + "}; } }";
		CompilationUnit cu = run(code, adaptedIteratorCode, otherIteratorCode);
		Assert.assertNotNull(cu);
	}
}
