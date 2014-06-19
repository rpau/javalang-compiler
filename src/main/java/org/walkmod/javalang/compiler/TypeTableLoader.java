/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
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
package org.walkmod.javalang.compiler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class TypeTableLoader<T> extends VoidVisitorAdapter<T> {

	private Map<String, String> typeTable;

	private Set<String> typeNames;

	private String contextName = null;

	private String packageName = null;

	private ClassLoader classLoader = null;

	public void setClassLoader(ClassLoader cl) {
		this.classLoader = cl;
	}

	public Map<String, String> getTypeTable() {
		return typeTable;
	}

	public void visit(ClassOrInterfaceDeclaration type, T context) {
		String name = type.getName();
		if (contextName != null) {
			if (packageName.equals(contextName)) {
				name = contextName + "." + type;
			} else {
				name = contextName.replace("$", ".") + "$" + type;
			}
		}
		if (typeNames.add(name)) {
			typeTable.put(type.getName(), name);
		}
		String oldCtx = contextName;
		contextName = name;
		super.visit(type, context);
		contextName = oldCtx;
	}

	public void visit(ImportDeclaration id, T context) {
		if (!id.isAsterisk()) {
			if (!id.isStatic()) {
				String typeName = id.getName().toString();
				if (typeNames.add(typeName)) {
					typeTable.put(id.getName().getName(), typeName);
				}
			}
		} else {
			if (classLoader != null) {
				String typeName = id.getName().toString();
				Reflections reflections = new Reflections(typeName,
						new SubTypesScanner(false));
				Set<Class<?>> types = reflections.getSubTypesOf(Object.class);
				if (types != null) {
					for (Class<?> resource : types) {
						typeNames.add(resource.getName());
						typeTable.put(resource.getSimpleName(),
								resource.getName());
					}
				}
			}
		}
	}

	@Override
	public void visit(CompilationUnit cu, T context) {
		typeNames = new HashSet<String>();
		typeTable = new HashMap<String, String>();
		if (cu.getPackage() != null) {
			contextName = cu.getPackage().getName().toString();
			packageName = contextName;
		}
		super.visit(cu, context);
	}
}
