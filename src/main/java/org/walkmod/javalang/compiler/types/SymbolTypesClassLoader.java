package org.walkmod.javalang.compiler.types;

import java.util.HashMap;
import java.util.Map;

import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class SymbolTypesClassLoader extends ClassLoader {
	private static Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();

	static {
		// static block to resolve primitive classes
		primitiveClasses.put("boolean", boolean.class);
		primitiveClasses.put("int", int.class);
		primitiveClasses.put("long", long.class);
		primitiveClasses.put("double", double.class);
		primitiveClasses.put("char", char.class);
		primitiveClasses.put("float", float.class);
		primitiveClasses.put("short", short.class);
		primitiveClasses.put("byte", byte.class);
	}

	public SymbolTypesClassLoader(ClassLoader parent) {
		super(parent);
	}

	public Class<?> loadClass(Type t) throws ClassNotFoundException {

		return ASTSymbolTypeResolver.getInstance().valueOf(t).getClazz();
	}

	public Class<?> loadClass(SymbolType t) throws ClassNotFoundException {
		String name = t.getName();
		Class<?> primClass = primitiveClasses.get(name);
		if (primClass == null) {
			return loadClass(t.getName());
		} else {
			return primClass;
		}
	}
}
