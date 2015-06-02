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

import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class LoadTypeParamsAction extends SymbolAction {

	@Override
	public void doPush(Symbol<?> symbol, SymbolTable table) {

		Node n = symbol.getLocation();
		if (n != null) {
			if (n instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) symbol
						.getLocation();
				List<TypeParameter> typeParams = declaration
						.getTypeParameters();
				SymbolType thisType = symbol.getType();

				load(table, typeParams, thisType);
				Map<String, SymbolType> localTypeParams = table.getScopes()
						.peek().getLocalTypeParams();

				if (!declaration.isInterface()) {
					List<ClassOrInterfaceType> extendsList = declaration
							.getExtends();
					if (extendsList != null) {
						for (ClassOrInterfaceType type : extendsList) {
							SymbolType superType = ASTSymbolTypeResolver
									.getInstance().valueOf(type);
							List<SymbolType> params = superType
									.getParameterizedTypes();
							if (params != null) {
								// extends a parameterizable type
								TypeVariable<?>[] tps = superType.getClazz()
										.getTypeParameters();
								for (int i = 0; i < tps.length; i++) {
									params.get(i).setTemplateVariable(
											tps[i].getName());
									if (localTypeParams == null
											|| !localTypeParams
													.containsKey(tps[i]
															.getName())) {
										table.pushSymbol(tps[i].getName(),
												ReferenceType.TYPE_PARAM,
												params.get(i), null);
									}

								}
							}
						}
					}
				}

			}
		}

	}

	public void load(SymbolTable table, List<TypeParameter> typeParams,
			SymbolType thisType) {
		if (typeParams != null && !typeParams.isEmpty()) {

			List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();

			for (TypeParameter tp : typeParams) {
				List<ClassOrInterfaceType> typeBounds = tp.getTypeBound();
				List<SymbolType> bounds = new LinkedList<SymbolType>();
				SymbolType st = null;
				if (typeBounds != null) {
					for (ClassOrInterfaceType type : typeBounds) {
						SymbolType paramType = ASTSymbolTypeResolver
								.getInstance().valueOf(type);
						if (paramType == null) {
							paramType = new SymbolType(Object.class);
						}
						bounds.add(paramType);
					}
					st = new SymbolType(bounds);

				} else {
					st = new SymbolType(Object.class);
				}
				st.setTemplateVariable(tp.getName());
				table.pushSymbol(tp.getName(), ReferenceType.TYPE_PARAM, st, tp);

				parameterizedTypes.add(st);
			}
			if (thisType != null && !parameterizedTypes.isEmpty()) {
				thisType.setParameterizedTypes(parameterizedTypes);
			}
		}

	}

}
