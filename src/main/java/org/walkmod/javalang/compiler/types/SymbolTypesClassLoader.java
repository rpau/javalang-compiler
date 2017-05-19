/*
 * Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 * 
 * Walkmod is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Walkmod. If
 * not, see <http://www.gnu.org/licenses/>.
 */
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
        primitiveClasses.put("void", void.class);
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
            if (name == null) {
                return null;
            }
            return loadClass(t.getName());
        } else {
            return primClass;
        }
    }
}
