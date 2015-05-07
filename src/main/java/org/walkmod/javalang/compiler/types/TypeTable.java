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
package org.walkmod.javalang.compiler.types;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.ModifierSet;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.QualifiedNameExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.PrimitiveType.Primitive;
import org.walkmod.javalang.ast.type.ReferenceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.VoidType;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

public class TypeTable<T> extends VoidVisitorAdapter<T> {

	private Map<String, String> typeTable = new HashMap<String, String>();

	private Set<String> typeNames = new HashSet<String>();

	private String contextName = null;

	private String packageName = null;

	private ClassLoader classLoader = Thread.currentThread()
			.getContextClassLoader();

	private static Set<String> defaultJavaLangClasses = new HashSet<String>();

	private static Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();

	private static JarFile SDKJar;

	private static TypeTable<Map<String, Object>> instance = null;

	private String mainJavaClassFile = null;

	private Set<String> staticPgkImports = null;

	private Map<String, String> innerClasses = null;

	private boolean useSymbolTable = true;

	private TypeTable() {

		for (String defaultType : defaultJavaLangClasses) {
			typeNames.add(defaultType);
			typeTable.put(getKeyName(defaultType, false), defaultType);
		}
	}

	private String getKeyName(String name, boolean imported) {
		int index = name.lastIndexOf(".");
		String simpleName = name;
		if (index != -1) {
			simpleName = name.substring(index + 1);
		}
		if (imported) {
			index = simpleName.lastIndexOf("$");
			if (index != -1) {
				simpleName = simpleName.substring(index + 1);
			}
		} else {
			simpleName = simpleName.replaceAll("\\$", ".");
		}
		return simpleName;
	}

	public void setUseSymbolTable(boolean useSymbolTable) {
		this.useSymbolTable = useSymbolTable;
	}

	public static TypeTable<Map<String, Object>> getInstance() {
		if (instance == null) {
			instance = new TypeTable<Map<String, Object>>();
		}
		return instance;
	}

	public void setClassLoader(ClassLoader cl) {
		this.classLoader = cl;
	}

	public Map<String, String> getTypeTable() {
		return typeTable;
	}

	public Set<String> findTypesByPrefix(String namePrefix) {
		Set<String> result = new HashSet<String>();
		for (String type : typeNames) {
			if (type.startsWith(namePrefix)) {
				result.add(type);
			}
		}
		Set<String> keys = innerClasses.keySet();
		for (String type : keys) {
			if (type.startsWith(namePrefix)) {
				result.add(innerClasses.get(type));
			}
		}
		return result;
	}

	public String getSimpleName(String type) {
		int dotIndex = type.lastIndexOf('.');
		int dollarIndex = type.lastIndexOf('$');
		int index = dotIndex;
		if (dollarIndex > 0) {
			index = dollarIndex;
		}
		if (index > 0) {
			return type.substring(index + 1);
		}
		return type;
	}

	

	public String getContext(TypeDeclaration type) {
		String name = type.getName();

		if (contextName != null && !contextName.equals("")) {
			if (packageName != null && packageName.equals(contextName)) {
				name = contextName + "." + name;
			} else {
				name = contextName/* .replace("$", ".") */+ "$" + name;
			}
		}
		if (mainJavaClassFile == null) {
			if (ModifierSet.isPublic(type.getModifiers())) {
				mainJavaClassFile = type.getName();
			}
		}

		if (typeNames.add(name)) {
			typeTable.put(getKeyName(name, false), name);
		}
		return name;
	}

	public void visit(ClassOrInterfaceDeclaration type, T context) {

		String name = getContext(type);
		String oldCtx = contextName;
		contextName = name;
		super.visit(type, context);
		contextName = oldCtx;
	}

	public void visit(EnumDeclaration type, T context) {

		String name = getContext(type);
		String oldCtx = contextName;
		contextName = name;
		super.visit(type, context);
		contextName = oldCtx;
	}

	public void visit(AnnotationDeclaration type, T context) {

		String name = getContext(type);
		String oldCtx = contextName;
		contextName = name;
		super.visit(type, context);
		contextName = oldCtx;
	}

	public void visit(ImportDeclaration id, T context) {

		String className = null;

		if (!id.isAsterisk()) {
			if (!id.isStatic()) {
				String typeName = id.getName().toString();
				addType(typeName, true);
				className = typeName;
			} else {
				className = ((QualifiedNameExpr) id.getName()).getQualifier()
						.toString();
			}
		} else {
			if (classLoader != null) {
				String typeName = id.getName().toString();
				loadClassesFromPackage(typeName);
				className = typeName;
			}
		}
		if (id.isStatic()) {
			int index = className.lastIndexOf(".");
			if (index != -1) {
				if (className.substring(0, index).equals(packageName)) {
					staticPgkImports.add(className.substring(index + 1));
				}

			} else if (packageName.equals("")) {
				staticPgkImports.add(className);
			}

		}

	}

	private void loadNestedClasses(Class<?> clazz, boolean imported) {
		Class<?>[] innerClasses = clazz.getDeclaredClasses();
		if (innerClasses != null) {
			for (int i = 0; i < innerClasses.length; i++) {
				if (!Modifier.isPrivate(innerClasses[i].getModifiers())) {
					String fullName = innerClasses[i].getName();

					if (typeNames.add(fullName)) {
						typeTable.put(getKeyName(fullName, imported), fullName);

					}
				}
			}

		}
	}

	private void addType(String name, boolean imported) {
		if (classLoader != null && name != null) {
			try {
				Class<?> clazz = Class.forName(name, false, classLoader);
				if (!Modifier.isPrivate(clazz.getModifiers())
						&& !clazz.isAnonymousClass()) {

					if (typeNames.add(name)) {
						typeTable.put(getKeyName(name, imported), name);
						if (clazz.isMemberClass()) {
							String cname = clazz.getCanonicalName();
							if (cname != null) {
								innerClasses.put(cname, name);
								Package pkg = clazz.getPackage();
								if (pkg != null) {
									if (pkg.getName().equals(packageName)) {
										typeTable.put(clazz.getSimpleName(),
												name);
									}
								}
							}
						}

						loadNestedClasses(clazz, imported);

					}

				}
			} catch (ClassNotFoundException e) {
				loadInnerClass(name, imported);
			} catch (IncompatibleClassChangeError e2) {
				int index = name.lastIndexOf("$");
				if (index != -1) {
					addType(name.substring(0, index), imported);
				}
			}

		}
	}

	private void loadInnerClass(String name, boolean imported) {
		int index = name.lastIndexOf(".");
		if (index != -1) {
			// it is an inner class?
			String preffix = name.substring(0, index);
			String suffix = name.substring(index + 1);

			String internalName = preffix + "$" + suffix;

			try {
				Class<?> clazz = Class
						.forName(internalName, false, classLoader);

				if (!Modifier.isPrivate(clazz.getModifiers())) {
					String keyName = getKeyName(internalName, imported);
					if (!innerClasses.containsKey(name)) {

						typeNames.add(internalName);
						// <A.B, com.foo.A$B>
						typeTable.put(keyName, internalName);
						// com.foo.A.B
						innerClasses.put(name, internalName);
						loadNestedClasses(clazz, imported);
					}
				}
			} catch (ClassNotFoundException e1) {
				throw new RuntimeException("The referenced class "
						+ internalName + " does not exists");
			} catch (IncompatibleClassChangeError e2) {
				// existent bug of the JVM
				// http://bugs.java.com/view_bug.do?bug_id=7003595
				index = internalName.lastIndexOf("$");
				if (index != -1) {
					addType(internalName.substring(0, index), imported);
				}
			}

		} else {
			throw new RuntimeException("The referenced class " + name
					+ " does not exists");
		}
	}

	private void loadClassesFromJar(JarFile jar, String directory) {

		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();

			int index = name.indexOf(directory);

			if (index != -1 && name.endsWith(".class")
					&& name.lastIndexOf("/") == directory.length()) {

				name = name.replaceAll("/", ".");
				name = name.substring(0, name.length() - 6);
				addType(name, false);
			}
		}
	}

	private void loadClassesFromPackage(String packageName) {

		URL[] urls = ((URLClassLoader) classLoader).getURLs();
		String directory = packageName.replaceAll("\\.", "/");

		loadClassesFromJar(SDKJar, directory);

		for (URL url : urls) {
			File file = new File(url.getFile());

			if (!file.isDirectory() && file.canRead()) {
				// it is a jar file
				JarFile jar = null;
				try {
					jar = new JarFile(file);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				loadClassesFromJar(jar, directory);

			} else if (file.isDirectory() && file.canRead()) {
				File aux = new File(file, directory);
				if (aux.exists() && aux.isDirectory()) {
					File[] contents = aux.listFiles();
					for (File resource : contents) {
						if (resource.getName().endsWith(".class")) {
							String simpleName = resource.getName().substring(0,
									resource.getName().lastIndexOf(".class"));
							String name = simpleName;
							if (!"".equals(packageName)) {
								name = packageName + "." + simpleName;
							}
							addType(name, false);
						}
					}
				}
			}
		}

	}

	static {
		// static block to resolve java.lang package classes
		String[] bootPath = System.getProperties().get("sun.boot.class.path")
				.toString().split(Character.toString(File.pathSeparatorChar));
		for (String lib : bootPath) {
			if (lib.endsWith("rt.jar")) {
				File f = new File(lib);
				try {
					JarFile jar = new JarFile(f);
					SDKJar = jar;
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						String name = entry.getName();

						int index = name.indexOf("java/lang/");

						if (index != -1
								&& name.lastIndexOf("/") == "java/lang/"
										.length() - 1) {

							name = name.replaceAll("/", ".");
							name = name.substring(0, name.length() - 6);

							defaultJavaLangClasses.add(name);
						}
					}

				} catch (IOException e) {
					throw new RuntimeException(
							"The java.lang classes cannot be loaded",
							e.getCause());
				}
			}
		}
	}

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

	@Override
	public void visit(CompilationUnit cu, T context) {

		if (cu.getPackage() != null) {
			contextName = cu.getPackage().getName().toString();

		} else {
			contextName = "";
		}
		staticPgkImports = new HashSet<String>();
		innerClasses = new HashMap<String, String>();
		packageName = contextName;
		loadClassesFromPackage(packageName);
		super.visit(cu, context);
	}

	public Class<?> loadClass(String className) throws ClassNotFoundException {
		if (className == null) {
			return null;
		}
		if (typeNames.contains(className)) {
			return Class.forName(className, false, classLoader);
		} else {
			String fullName = typeTable.get(className);
			if (fullName != null) {
				return Class.forName(fullName, false, classLoader);
			} else {
				Class<?> clazz = primitiveClasses.get(className);
				if (clazz == null) {
					try {
						clazz = Class.forName(className, false, classLoader);
					} catch (ClassNotFoundException e) {
						if (!className.contains("$")) {
							String[] innerClassName = className.split("\\.");
							if (innerClassName.length >= 2) {
								int index = className.lastIndexOf('.');
								if (index < className.length() - 1) {
									String preffix = className.substring(0,
											index);
									if (!typeNames.contains(preffix)) {
										preffix = typeTable.get(preffix);
									}
									if (preffix == null) {
										throw e;
									} else {
										className = preffix
												+ "$"
												+ className
														.substring(index + 1);
										return Class.forName(className, false,
												classLoader);
									}
								}
							}
						} else {
							throw e;
						}

					}

					return clazz;
				} else {
					return clazz;
				}
			}
		}
	}

	public Class<?> loadClass(SymbolType type) throws ClassNotFoundException {
		if (type != null && !type.getName().equals("void")
				&& !type.getName().startsWith("[")
				&& !type.isTemplateVariable()) {
			String clazzName = type.getName();
			return loadClass(clazzName);

		} else {

			return null;
		}
	}

	public Class<?> loadClass(Type t, SymbolTable st)
			throws ClassNotFoundException {

		Class<?> result = loadClass(valueOf(t, st));
		if (result == null) {
			throw new ClassNotFoundException("The class " + t.toString()
					+ " is not found");
		}
		return result;
	}

	private SymbolType resolve(ClassOrInterfaceType type, SymbolTable st) {
		// ClassOrInterfaceType type = (ClassOrInterfaceType) containerType;
		SymbolType result = null;
		if (type == null) {
			return result;
		}
		String name = type.getName();
		ClassOrInterfaceType scope = type.getScope();
		Node parent = type.getParentNode();
		boolean isObjectCreationCtxt = (parent != null && parent instanceof ObjectCreationExpr);
		isObjectCreationCtxt = isObjectCreationCtxt
				&& ((ObjectCreationExpr) parent).getScope() != null;
		if (scope == null && useSymbolTable && !isObjectCreationCtxt) {
			// it can be resolved through the symbol table (imports,
			// generics, sibling/children inner classes, package
			// classes)
			result = st.getType(name,
					org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE);
			if (result != null) {
				result = result.clone();
			} else {
				SymbolType thisType = st
						.getType(
								"this",
								org.walkmod.javalang.compiler.symbols.ReferenceType.VARIABLE);
				if (thisType != null) {
					Class<?> clazz = thisType.getClazz();
					// we look for a declared class in one of our super classes
					Class<?> superClass = clazz.getSuperclass();
					Class<?> nestedClass = ClassInspector.findClassMember(
							thisType.getClazz().getPackage(), name, superClass);

					// this is an inner class? If so, we look for a nested class
					// in one of our parent classes
					while (clazz.isMemberClass() && nestedClass == null) {
						clazz = clazz.getDeclaringClass();
						nestedClass = ClassInspector.findClassMember(
								clazz.getPackage(), name, clazz);
					}
					// this is an anonymous class? If so, we look for a nested
					// class in the enclosing class
					while (clazz.isAnonymousClass() && nestedClass == null) {
						clazz = clazz.getEnclosingClass();
						nestedClass = ClassInspector.findClassMember(
								clazz.getPackage(), name, clazz);
						while (clazz.isMemberClass() && nestedClass == null) {
							clazz = clazz.getDeclaringClass();
							nestedClass = ClassInspector.findClassMember(
									clazz.getPackage(), name, clazz);
						}
					}
					if (nestedClass != null) {
						result = new SymbolType(nestedClass);
					}

				}
			}

		} else {
			// it is a fully qualified name or a inner class (>1 hop)

			String scopeName = "";
			if (isObjectCreationCtxt) {
				scopeName = ((ObjectCreationExpr) parent).getScope()
						.getSymbolData().getName()
						+ ".";
			}
			ClassOrInterfaceType ctxt = type;
			while (ctxt.getScope() != null) {
				ctxt = (ClassOrInterfaceType) ctxt.getScope();
				scopeName = ctxt.getName() + "." + scopeName;
			}

			String innerClassName = name;
			if (scopeName.length() > 1) {
				innerClassName = scopeName.substring(0, scopeName.length() - 1)
						+ "$" + name;
			}
			String fullName = scopeName + name;
			if (innerClasses.containsKey(innerClassName)) {
				// fully qualified inner class name
				result = new SymbolType();
				result.setName(innerClassName);
			} else if (typeNames.contains(fullName)) {
				// fully qualified class name
				result = new SymbolType();
				result.setName(fullName);
			} else {
				// nested inner class >1 hop A.B.C
				String aux = typeTable.get(fullName);
				if (aux == null) {
					// in the code appears B.C
					SymbolType scopeType = resolve(ctxt.getScope(), st);
					if (scopeType != null) {
						result = new SymbolType();
						result.setName(scopeType.getName() + "$" + name);
					} else {
						result = new SymbolType();
						// it is a type that has not previously imported
						result.setName(fullName);
					}
				} else {
					result = new SymbolType();
					result.setName(aux);
				}
			}

		}

		if (type.getTypeArgs() != null) {
			if (result == null) {
				result = new SymbolType();
			}
			List<SymbolType> typeArgs = new LinkedList<SymbolType>();

			for (Type typeArg : type.getTypeArgs()) {
				SymbolType aux = valueOf(typeArg, st);
				if (aux == null) {
					aux = new SymbolType(Object.class);
				}
				typeArgs.add(aux);
			}
			result.setParameterizedTypes(typeArgs);
		}
		return result;
	}

	public SymbolType valueOf(Type parserType, SymbolTable st) {

		SymbolType result = new SymbolType();

		if (parserType instanceof ReferenceType) {

			Type containerType = (Type) ((ReferenceType) parserType).getType();

			if (containerType instanceof PrimitiveType) {
				result.setName(valueOf(containerType, st).getName());

			} else if (containerType instanceof ClassOrInterfaceType) {

				result = resolve((ClassOrInterfaceType) containerType, st);

			}
			if (result != null) {
				result.setArrayCount(((ReferenceType) parserType)
						.getArrayCount());
			}

		} else if (parserType instanceof PrimitiveType) {

			Primitive pt = ((PrimitiveType) parserType).getType();

			if (pt.equals(Primitive.Boolean)) {
				result.setName(primitiveClasses.get("boolean").getName());

			} else if (pt.equals(Primitive.Char)) {
				result.setName(primitiveClasses.get("char").getName());
			} else if (pt.equals(Primitive.Double)) {
				result.setName(primitiveClasses.get("double").getName());
			} else if (pt.equals(Primitive.Float)) {
				result.setName(primitiveClasses.get("float").getName());
			} else if (pt.equals(Primitive.Int)) {
				result.setName(primitiveClasses.get("int").getName());
			} else if (pt.equals(Primitive.Long)) {
				result.setName(primitiveClasses.get("long").getName());
			} else if (pt.equals(Primitive.Short)) {
				result.setName(primitiveClasses.get("short").getName());
			} else if (pt.equals(Primitive.Byte)) {
				result.setName(primitiveClasses.get("byte").getName());
			}

		} else if (parserType instanceof ClassOrInterfaceType) {

			result = resolve((ClassOrInterfaceType) parserType, st);

		} else if (parserType instanceof WildcardType) {
			if (((WildcardType) parserType).toString().equals("?")) {
				result.setName("java.lang.Object");
			} else {
				ReferenceType extendsRef = ((WildcardType) parserType)
						.getExtends();
				ReferenceType superRef = ((WildcardType) parserType).getSuper();
				if (extendsRef != null) {
					result = valueOf(extendsRef, st);
				} else {
					result = valueOf(superRef, st);
				}
			}
		} else if (parserType instanceof VoidType) {
			result.setName(Void.class.getName());
		}
		if (result != null && result.getName() == null) {
			throw new RuntimeException("The type " + parserType.toString()
					+ " cannot be resolved");
		}

		return result;
	}

	public String getFullName(TypeDeclaration type) {
		String name = type.getName();
		Node parentNode = type.getParentNode();
		// if it is an inner class, we build the unique name
		while (parentNode instanceof TypeDeclaration) {
			name = ((TypeDeclaration) parentNode).getName() + "." + name;
			parentNode = parentNode.getParentNode();
		}
		return typeTable.get(name);
	}

	public void clear() {
		typeNames.clear();
		typeTable.clear();
		packageName = null;
		contextName = null;
		for (String defaultType : defaultJavaLangClasses) {
			typeNames.add(defaultType);
			typeTable.put(getKeyName(defaultType, false), defaultType);
		}
	}

	public Set<String> getPackageClasses() {
		Set<String> pkgClasses = new HashSet<String>();
		String name = packageName;
		if (name == null) {
			name = "";
		}
		Set<Entry<String, String>> entries = this.typeTable.entrySet();
		Iterator<Entry<String, String>> it = entries.iterator();
		while (it.hasNext()) {
			Entry<String, String> entry = it.next();
			String fullName = entry.getValue();
			int index = fullName.lastIndexOf(".");
			String pckName = "";
			if (index != -1) {
				pckName = fullName.substring(0, index);

			}
			if (pckName.equals(name) && mainJavaClassFile != null
					&& !mainJavaClassFile.equals(entry.getKey())) {
				pkgClasses.add(entry.getKey());
			}
		}

		return pkgClasses;
	}

	public Set<String> getStaticPackageClasses() {

		return staticPgkImports;
	}

}
