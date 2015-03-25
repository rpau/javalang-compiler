package org.walkmod.javalang.compiler.actions;

import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;

public class LoadTypeParamsAction implements SymbolAction {

	@Override
	public void execute(Symbol symbol, SymbolTable table, SymbolEvent event) {
		if (event.equals(SymbolEvent.PUSH)) {
			Node n = symbol.getLocation();
			if (n != null) {
				if (n instanceof ClassOrInterfaceDeclaration) {
					ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) symbol
							.getLocation();
					List<TypeParameter> typeParams = declaration
							.getTypeParameters();
					if (typeParams != null && !typeParams.isEmpty()) {
						SymbolType thisType = symbol.getType();
						List<SymbolType> parameterizedTypes = new LinkedList<SymbolType>();

						for (TypeParameter tp : typeParams) {
							List<ClassOrInterfaceType> typeBounds = tp
									.getTypeBound();
							List<SymbolType> bounds = new LinkedList<SymbolType>();
							SymbolType st = null;
							if (typeBounds != null) {
								for (ClassOrInterfaceType type : typeBounds) {
									bounds.add(TypeTable.getInstance().valueOf(
											type));
								}
								st = new SymbolType(bounds);
								table.pushSymbol(tp.getName(),
										ReferenceType.TYPE, st, tp);
							} else {
								st = new SymbolType(Object.class);
							}

							st.setTemplateVariable(true);
							parameterizedTypes.add(st);
						}
						thisType.setParameterizedTypes(parameterizedTypes);
					}
				}
			}
		}
	}

}
