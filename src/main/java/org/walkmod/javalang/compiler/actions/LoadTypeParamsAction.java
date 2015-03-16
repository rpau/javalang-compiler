package org.walkmod.javalang.compiler.actions;

import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

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
							SymbolType st = new SymbolType(tp.getName());
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
