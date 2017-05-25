package org.walkmod.javalang.compiler.test.assertj;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.FieldSymbolData;
import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.IfStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.stmt.TypeDeclarationStmt;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.symbols.SymbolType;

/**
 * Experimental AssertJ alike assertions of javalang AST.
 *
 * If the experiment is successful the classes and methods
 * may be generated from AST classes.
 *
 * Subject to change without notice.
 */
@SuppressWarnings("WeakerAccess")
public class AstAssertions {
    public static CompilationUnitAssert assertThat(CompilationUnit cu) {
        return new CompilationUnitAssert(cu);
    }

    public static BodyDeclarationAssert assertThat(BodyDeclaration actual) {
        return new BodyDeclarationAssert(actual);
    }

    public static ClassOrInterfaceDeclarationAssert assertThat(ClassOrInterfaceDeclaration actual) {
        return new ClassOrInterfaceDeclarationAssert(actual);
    }

    public static TypeDeclarationAssert assertThat(TypeDeclaration actual) {
        return new TypeDeclarationAssert(actual);
    }

    public static ConstructorDeclarationAssert assertThat(ConstructorDeclaration actual) {
        return new ConstructorDeclarationAssert(actual);
    }

    public static MethodDeclarationAssert assertThat(MethodDeclaration actual) {
        return new MethodDeclarationAssert(actual);
    }

    public static FieldDeclarationAssert assertThat(FieldDeclaration actual) {
        return new FieldDeclarationAssert(actual);
    }

    public static BlockStmtAssert assertThat(BlockStmt actual) {
        return new BlockStmtAssert(actual);
    }

    public static StatementAssert assertThat(Statement actual) {
        return new StatementAssert(actual);
    }

    public static TypeDeclarationStmtAssert assertThat(TypeDeclarationStmt actual) {
        return new TypeDeclarationStmtAssert(actual);
    }

    public static ExpressionStmtAssert assertThat(ExpressionStmt actual) {
        return new ExpressionStmtAssert(actual);
    }

    public static IfStmtAssert assertThat(IfStmt actual) {
        return new IfStmtAssert(actual);
    }

    public static ReturnStmtAssert assertThat(ReturnStmt actual) {
        return new ReturnStmtAssert(actual);
    }

    public static ExpressionAssert assertThat(Expression actual) {
        return new ExpressionAssert(actual);
    }

    public static BinaryExprAssert assertThat(BinaryExpr actual) {
        return new BinaryExprAssert(actual);
    }

    public static IntegerLiteralExprAssert assertThat(IntegerLiteralExpr actual) {
        return new IntegerLiteralExprAssert(actual);
    }

    public static NameExprAssert assertThat(NameExpr actual) {
        return new NameExprAssert(actual);
    }

    public static ObjectCreationExprAssert assertThat(ObjectCreationExpr actual) {
        return new ObjectCreationExprAssert(actual);
    }

    public static AssignExprAssert assertThat(AssignExpr actual) {
        return new AssignExprAssert(actual);
    }

    public static MethodCallExprAssert assertThat(MethodCallExpr actual) {
        return new MethodCallExprAssert(actual);
    }

    public static VariableDeclarationExprAssert assertThat(VariableDeclarationExpr actual) {
        return new VariableDeclarationExprAssert(actual);
    }

    public static TypeAssert assertThat(Type actual) {
        return new TypeAssert(actual);
    }

    // non AST nodes

    public static SymbolDataAssert<?, ?> assertThat(SymbolData actual) {
        return new SymbolDataAssert(actual);
    }

    public static MethodSymbolDataAssert assertThat(MethodSymbolData actual) {
        return new MethodSymbolDataAssert(actual);
    }

    public static FieldSymbolDataAssert assertThat(FieldSymbolData actual) {
        return new FieldSymbolDataAssert(actual);
    }

    public static SymbolTypeAssert assertThat(SymbolType actual) {
        return new SymbolTypeAssert(actual);
    }
}
