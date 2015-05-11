/*
 Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 
Walkmod is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Walkmod is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.compiler.actions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.PackageDeclaration;
import org.walkmod.javalang.compiler.symbols.MethodSymbol;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class LoadStaticImportsAction extends SymbolAction {

	private String pkgName = null;

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
		Node n = symbol.getLocation();
		if (n instanceof ImportDeclaration) {
			ImportDeclaration id = (ImportDeclaration) n;
			if (id.isStatic()) {
				Class<?> clazz = symbol.getType().getClazz();
				Method[] methods = clazz.getDeclaredMethods();
				Package pkg = clazz.getPackage();
				String importPkgName = "";
				if (pkg != null) {
					importPkgName = pkg.getName();
				}
				if (pkgName == null) {
					CompilationUnit cu = (CompilationUnit) id.getParentNode();
					PackageDeclaration pkgDeclaration = cu.getPackage();
					if (pkgDeclaration != null) {
						pkgName = pkgDeclaration.getName().toString();
					} else {
						pkgName = "";
					}
				}
				for (Method m : methods) {
					if (id.isAsterisk()
							|| id.getName().getName().equals(m.getName())) {
						int modifiers = m.getModifiers();
						if (Modifier.isStatic(modifiers)) {

							boolean isVisible = Modifier.isPublic(modifiers)
									|| (!Modifier.isPrivate(modifiers) && importPkgName
											.equals(pkgName));

							if (isVisible) {
								Class<?> returnClass = m.getReturnType();
								Class<?>[] params = m.getParameterTypes();
								SymbolType[] args = null;
								if (params.length > 0) {
									args = new SymbolType[params.length];
									int i = 0;
									for (Class<?> param : params) {
										args[i] = SymbolType.valueOf(param,
												null);
										i++;
									}
								}

								SymbolType st = new SymbolType(returnClass);
								MethodSymbol method = new MethodSymbol(
										m.getName(), st, n, symbol.getType(),
										args, true, m.isVarArgs(), m,
										(List<SymbolAction>) null);
								table.pushSymbol(method);
							}
						}
					}
				}
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					if (id.isAsterisk()
							|| id.getName().getName().equals(field.getName())) {

						int modifiers = field.getModifiers();
						if (Modifier.isStatic(modifiers)) {

							boolean isVisible = Modifier.isPublic(modifiers)
									|| (!Modifier.isPrivate(modifiers) && importPkgName
											.equals(pkgName));
							if (isVisible) {
								Class<?> type = field.getType();
								SymbolType st = new SymbolType(type);
								table.pushSymbol(field.getName(),
										ReferenceType.VARIABLE, st, n);
							}
						}
					}
				}
			}
		}

	}
}
