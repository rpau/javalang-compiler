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
    private Map<String, Class<?>> cache = new HashMap<String, Class<?>>();

    public SymbolTypesClassLoader(ClassLoader parent) {
        super(parent);
        cache.put("boolean", boolean.class);
        cache.put("int", int.class);
        cache.put("long", long.class);
        cache.put("double", double.class);
        cache.put("char", char.class);
        cache.put("float", float.class);
        cache.put("short", short.class);
        cache.put("byte", byte.class);
        cache.put("void", void.class);
    }

    public Class<?> loadClass(Type t) throws ClassNotFoundException {

        return ASTSymbolTypeResolver.getInstance().valueOf(t).getClazz();
    }

    public Class<?> loadClass(SymbolType t) throws ClassNotFoundException {
        String name = t.getName();
        Class<?> cachedClass = cache.get(name);
        if (cachedClass == null) {
            if (name == null) {
                return null;
            }
            Class<?> clazz = loadClass(t.getName());
            cache.put(t.getName(), clazz);
            return clazz;
        } else {
            return cachedClass;
        }
    }
}
