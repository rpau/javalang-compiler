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

import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.compiler.symbols.MethodSymbol;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class LoadStaticImportsAction extends SymbolAction {

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
		Node n = symbol.getLocation();
		if (n instanceof ImportDeclaration) {
			ImportDeclaration id = (ImportDeclaration) n;
			if (id.isStatic()) {
				Class<?> clazz = symbol.getType().getClazz();
				Method[] methods = clazz.getMethods();
				for (Method m : methods) {
					if (id.isAsterisk()
							|| id.getName().getName().equals(m.getName())) {
						if (Modifier.isStatic(m.getModifiers())
								&& Modifier.isPublic(m.getModifiers())) {
							Class<?> returnClass = m.getReturnType();
							Class<?>[] params = m.getParameterTypes();
							SymbolType[] args = null;
							if (params.length > 0) {
								args = new SymbolType[params.length];
								int i = 0;
								for (Class<?> param : params) {
									args[i] = new SymbolType(param);
									i++;
								}
							}

							SymbolType st = new SymbolType(returnClass);
							MethodSymbol method = new MethodSymbol(m.getName(),
									st, n, symbol.getType(), args, true,
									(List<SymbolAction>) null);
							table.pushSymbol(method);
						}
					}
				}
				Field[] fields = clazz.getFields();
				for (Field field : fields) {
					if (id.isAsterisk()
							|| id.getName().getName().equals(field.getName())) {
						if (Modifier.isStatic(field.getModifiers())
								&& Modifier.isPublic(field.getModifiers())) {
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
