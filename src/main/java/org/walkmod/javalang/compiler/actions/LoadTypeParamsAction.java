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
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;

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
								bounds.add(TypeTable.getInstance()
										.valueOf(type));
							}
							st = new SymbolType(bounds);

						} else {
							st = new SymbolType(Object.class);
						}
						table.pushSymbol(tp.getName(), ReferenceType.TYPE, st,
								tp);
						st.setTemplateVariable(true);
						parameterizedTypes.add(st);
					}
					thisType.setParameterizedTypes(parameterizedTypes);
				}
			}

		}
	}

}
