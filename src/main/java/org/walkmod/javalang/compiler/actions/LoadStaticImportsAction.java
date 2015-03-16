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
import org.walkmod.javalang.compiler.symbols.SymbolEvent;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class LoadStaticImportsAction implements SymbolAction {

	@Override
	public void execute(Symbol symbol, SymbolTable table, SymbolEvent event)
			throws Exception {

		Node n = symbol.getLocation();
		if (n instanceof ImportDeclaration) {
			ImportDeclaration id = (ImportDeclaration) n;
			if (id.isStatic()) {
				Class<?> clazz = symbol.getClass();
				Method[] methods = clazz.getMethods();
				for (Method m : methods) {
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
						MethodSymbol method = new MethodSymbol(m.getName(), st,
								n, symbol.getType(), args,
								(List<SymbolAction>) null);
						table.pushSymbol(method);
					}
				}
				Field[] fields = clazz.getFields();
				for (Field field : fields) {
					if (Modifier.isStatic(field.getModifiers())
							&& Modifier.isPublic(field.getModifiers())) {
						Class<?> type = field.getType();
						SymbolType st = new SymbolType(type);
						table.pushSymbol(field.getName(),
								ReferenceType.VARIABLE, st, null);
					}
				}
			}
		}

	}
}
