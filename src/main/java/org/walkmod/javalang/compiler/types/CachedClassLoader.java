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


import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CachedClassLoader extends ClassLoader {

    public static final Map<String, Class<?>> PRIMITIVES;

    static {

        Map<String, Class<?>> aux = new HashMap<>();
        // static block to resolve primitive classes
        aux.put("boolean", boolean.class);
        aux.put("int", int.class);
        aux.put("long", long.class);
        aux.put("double", double.class);
        aux.put("char", char.class);
        aux.put("float", float.class);
        aux.put("short", short.class);
        aux.put("byte", byte.class);
        aux.put("void", void.class);
        PRIMITIVES = Collections.unmodifiableMap(aux);
    }

    private Map<String, Class<?>> cache = new HashMap<String, Class<?>>();

    public CachedClassLoader(IndexedURLClassLoader parent) {
        super(parent);

        cache.putAll(PRIMITIVES);

    }

    public Class<?> loadClass(Type t) throws ClassNotFoundException {
        return ASTSymbolTypeResolver.getInstance().valueOf(t).getClazz();
    }


    public List<String> getPackageContents(String packageName) {
        return ((IndexedURLClassLoader)getParent()).getPackageClasses(packageName);
    }

    public List<String> getSDKContents(String packageName) {
        return ((IndexedURLClassLoader)getParent()).getSDKContents(packageName);
    }

    public Class<?> loadClass(SymbolData t) throws ClassNotFoundException {
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
