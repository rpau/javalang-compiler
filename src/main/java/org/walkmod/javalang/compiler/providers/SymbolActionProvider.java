package org.walkmod.javalang.compiler.providers;

import java.util.List;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.AnnotationMemberDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.EmptyTypeDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.MultiTypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.symbols.SymbolAction;

/**
 * Provides a way to setup a set of SymbolAction for every type of symbol
 * (method, variable, type, field, enum, annotations), the global scope
 * (compilation unit) and an specific scope(BlockStmt)
 * 
 * @author rpau
 *
 */
public interface SymbolActionProvider {

	public List<SymbolAction> getActions(ClassOrInterfaceDeclaration n);

	public List<SymbolAction> getActions(Parameter n);

	public List<SymbolAction> getActions(ClassOrInterfaceType n);

	public List<SymbolAction> getActions(MultiTypeParameter n);

	public List<SymbolAction> getActions(VariableDeclarator n);

	public List<SymbolAction> getActions(ImportDeclaration n);

	public List<SymbolAction> getActions(MethodDeclaration n);

	public List<SymbolAction> getActions(FieldDeclaration n);

	public List<SymbolAction> getActions(EnumDeclaration n);

	public List<SymbolAction> getActions(EnumConstantDeclaration n);

	public List<SymbolAction> getActions(AnnotationDeclaration n);

	public List<SymbolAction> getActions(AnnotationMemberDeclaration n);

	public List<SymbolAction> getActions(ConstructorDeclaration n);

	public List<SymbolAction> getActions(CompilationUnit n);

	public List<SymbolAction> getActions(EmptyTypeDeclaration n);

	public List<SymbolAction> getActions(TypeDeclaration n);

	public List<SymbolAction> getActions(BlockStmt n);

	public List<SymbolAction> getActions(VariableDeclarationExpr n);
}
