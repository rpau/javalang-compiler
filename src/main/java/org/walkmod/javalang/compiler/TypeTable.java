package org.walkmod.javalang.compiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.PrimitiveType.Primitive;
import org.walkmod.javalang.ast.type.ReferenceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.WildcardType;
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

	public TypeTable() {

		for (String defaultType : defaultJavaLangClasses) {
			typeNames.add(defaultType);
			typeTable.put(
					defaultType.substring(defaultType.lastIndexOf(".") + 1),
					defaultType);
		}
	}

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
				name = contextName + "." + name;
			} else {
				name = contextName.replace("$", ".") + "$" + name;
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
				addType(typeName);
			}
		} else {
			if (classLoader != null) {

				String typeName = id.getName().toString();

				loadClassesFromPackage(typeName);
			}
		}
	}

	private void addType(String name) {
		if (classLoader != null && name != null) {
			try {
				Class<?> clazz = Class.forName(name, false, classLoader);
				if (typeNames.add(name)) {
					typeTable.put(clazz.getSimpleName(), name);
				}
				Class<?>[] innerClasses = clazz.getDeclaredClasses();
				if (innerClasses != null) {
					for (int i = 0; i < innerClasses.length; i++) {
						if (typeNames.add(innerClasses[i].getName())) {

							typeTable.put(innerClasses[i].getSimpleName(),
									innerClasses[i].getName());

						}
					}

				}
			} catch (ClassNotFoundException e) {
				int index = name.lastIndexOf(".");
				if (index != -1) {
					// it is an inner class?
					name = name.substring(0, index) + "$"
							+ name.substring(index + 1);
					try {
						Class<?> clazz = Class
								.forName(name, false, classLoader);
						if (typeNames.add(name)) {
							typeTable.put(clazz.getSimpleName(), name);
						}
					} catch (ClassNotFoundException e1) {
						throw new RuntimeException("The referenced class "
								+ name + " does not exists", e);
					}

				} else {
					throw new RuntimeException("The referenced class " + name
							+ " does not exists", e);
				}
			}

		}
	}
	
	private void loadClassesFromJar(JarFile jar, String directory ){		
		
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();

			int index = name.indexOf(directory);

			if (index != -1 && name.endsWith(".class") 
					&& name.lastIndexOf("/") == directory.length()) {

				name = name.replaceAll("/", ".");
				name = name.substring(0, name.length() - 6);

				addType(name);
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
							String name = packageName + "." + simpleName;
							addType(name);
						}
					}
				}
			}
		}

	}

	static {
		// static block to resolve java.lang package classes
		String[] bootPath = System.getProperties().get("sun.boot.class.path")
				.toString().split(";");
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
			packageName = contextName;
			loadClassesFromPackage(packageName);

		}
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
			if (type.getArrayCount() > 0) {
				Class<?> aux = loadClass(clazzName);
				return Array.newInstance(aux, 1).getClass();
			}

			return loadClass(clazzName);

		} else {
			
			return null;
		}
	}

	public Class<?> loadClass(Type t) throws ClassNotFoundException {

		Class<?> result = loadClass(valueOf(t));
		if(result == null){
			throw new ClassNotFoundException("The class "+t.toString()+" is not found");
		}
		return result;
	}

	public SymbolType valueOf(Type parserType) {

		SymbolType result = new SymbolType();

		if (parserType instanceof ReferenceType) {

			Type containerType = (Type) ((ReferenceType) parserType).getType();

			result.setArrayCount(((ReferenceType) parserType).getArrayCount());

			if (containerType instanceof PrimitiveType) {
				result.setName(valueOf(containerType).getName());

			} else if (containerType instanceof ClassOrInterfaceType) {

				ClassOrInterfaceType type = (ClassOrInterfaceType) containerType;

				String name = type.getName();

				// it is a generic collections parameter
				if (name.length() == 1) {

					name = "java.lang.Object";

				}

				while (type.getScope() != null) {
					type = (ClassOrInterfaceType) type.getScope();
					name = type.getName() + "." + name;
				}
				if (typeNames.contains(name)) {
					result.setName(name);
				} else {
					result.setName(typeTable.get(name));
				}
				if (result.getName() == null) {
					// it is a fully qualified name
					result.setName(name);
				}

				if (type.getTypeArgs() != null) {

					List<SymbolType> typeArgs = new LinkedList<SymbolType>();

					for (Type typeArg : type.getTypeArgs()) {
						SymbolType aux = valueOf(typeArg);
						typeArgs.add(aux);
					}
					result.setParameterizedTypes(typeArgs);
				}
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
			ClassOrInterfaceType type = ((ClassOrInterfaceType) parserType);
			String name = type.getName();

			while (type.getScope() != null) {
				type = (ClassOrInterfaceType) type.getScope();
				name = type.getName() + "." + name;
			}

			if (typeNames.contains(name)) {
				result.setName(name);
			} else {
				result.setName(typeTable.get(name));
			}
			if (result.getName() == null) {
				result.setName(name);
			}

			if (type.getTypeArgs() != null) {

				List<SymbolType> typeArgs = new LinkedList<SymbolType>();

				for (Type typeArg : type.getTypeArgs()) {
					SymbolType aux = valueOf(typeArg);
					typeArgs.add(aux);
				}
				result.setParameterizedTypes(typeArgs);
			}

		} else if (parserType instanceof WildcardType) {
			if (((WildcardType) parserType).toString().equals("?")) {
				result.setName("java.lang.Object");
			} else {
				result = valueOf(((WildcardType) parserType).getExtends());
			}
		}
		if (result.getName() == null) {
			throw new RuntimeException("The type " + parserType.toString()
					+ " cannot be resolved");
		}

		return result;
	}

	public String getFullName(ClassOrInterfaceType type) {

		if (type.getScope() != null) {
			return getFullName(type.getScope());
		}

		if (typeNames.contains(type.getName())) {
			return type.getName();
		} else {
			return typeTable.get(type.getName());
		}

	}

	public String getFullName(ClassOrInterfaceDeclaration type) {
		return typeTable.get(type.getName());
	}

	public void clear() {
		typeNames.clear();
		typeTable.clear();

		for (String defaultType : defaultJavaLangClasses) {
			typeNames.add(defaultType);
			typeTable.put(
					defaultType.substring(defaultType.lastIndexOf(".") + 1),
					defaultType);
		}
	}

}
