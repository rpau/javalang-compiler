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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.ast.SymbolDefinition;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.QualifiedNameExpr;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.actions.LoadStaticImportsAction;
import org.walkmod.javalang.compiler.providers.SymbolActionProvider;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;

/**
 * Inserts into the symbol table the set of types that can be used from an
 * specific class. These types can be resolved through the package, import
 * declarations or inner classes.
 * 
 * Each type is loaded into the symbol table for all possible simple names that
 * can solve this type.
 * 
 * 
 * @author rpau
 *
 */
public class TypesLoaderVisitor<T> extends VoidVisitorAdapter<T> {

	private String contextName = null;

	private String packageName = null;

	private static SymbolTypesClassLoader classLoader = new SymbolTypesClassLoader(
			Thread.currentThread().getContextClassLoader());

	private static Set<String> defaultJavaLangClasses = new HashSet<String>();

	private static Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();

	private static JarFile SDKJar;

	private SymbolTable symbolTable = null;

	private List<SymbolAction> actions;

	private SymbolActionProvider actionProvider = null;

	private Node startingNode = null;

	public TypesLoaderVisitor(SymbolTable symbolTable,
			SymbolActionProvider actionProvider, List<SymbolAction> actions) {
		this.symbolTable = symbolTable;
		this.actions = actions;
		this.actionProvider = actionProvider;
		for (String defaultType : primitiveClasses.keySet()) {
			SymbolType st = new SymbolType(primitiveClasses.get(defaultType));
			symbolTable.pushSymbol(defaultType,
					org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
					st, null, actions);

		}
		for (String defaultType : defaultJavaLangClasses) {

			SymbolType st = new SymbolType(defaultType);
			symbolTable.pushSymbol(getSymbolName(defaultType, false),
					org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
					st, null, actions);
		}
	}

	/**
	 * Infers the simple name to be pushed into the symbol table
	 * 
	 * @param name
	 *            full name of a given class
	 * @param imported
	 *            if it appears as an import declaration. It is important,
	 *            because the simple name is the part after the $ symbol if it
	 *            is an inner class.
	 * @return the simple name to be pushed into the symbol table
	 */
	private String getSymbolName(String name, boolean imported) {
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
			String[] splittedString = simpleName.split("\\$\\d");
			String aux = splittedString[splittedString.length - 1];

			if (!aux.equals("")) {
				if (aux.charAt(0) == '$') {
					simpleName = aux.substring(1);
				} else {
					simpleName = aux;
				}
			}
			simpleName = simpleName.replaceAll("\\$", ".");
		}
		return simpleName;
	}

	public void setClassLoader(ClassLoader cl) {
		classLoader = new SymbolTypesClassLoader(cl);
	}

	public static SymbolTypesClassLoader getClassLoader() {
		return classLoader;
	}

	private SymbolType buildSymbolType(TypeDeclaration type) {

		SymbolType st = (SymbolType) type.getSymbolData();
		if (st != null) {
			return st;
		}
		String name = type.getName();
		Node node = type.getParentNode();

		if (node instanceof SymbolDataAware<?>) {
			st = (SymbolType) ((SymbolDataAware<?>) node).getSymbolData();

		}
		if (st != null) {
			name = st.getName() + "$" + name;
		} else {
			if (contextName != null && !contextName.equals("")) {
				if (packageName != null && packageName.equals(contextName)) {
					name = contextName + "." + name;
				} else {
					name = contextName + "$" + name;
				}
			}
		}
		st = new SymbolType(name);
		type.setSymbolData(st);
		return st;
	}

	private void pushCanonicalName(Symbol<?> added, SymbolType st) {
		String fullName = st.getName().replaceAll("\\$", ".");
		Symbol<?> importedPkgSymbol = symbolTable.findSymbol(fullName,
				ReferenceType.TYPE);
		if (importedPkgSymbol == null) {
			Symbol<?> aux = symbolTable.pushSymbol(fullName,
					ReferenceType.TYPE, added.getType(), added.getLocation());
			aux.setInnerScope(added.getInnerScope());
		} else if (importedPkgSymbol.getLocation() == null) {
			importedPkgSymbol.setInnerScope(added.getInnerScope());
		}
	}

	private <K extends Node & SymbolDefinition> void overrideSymbol(
			Symbol<K> symbol, K location, SymbolType st, Scope scope) {
		symbol.setLocation(location);
		symbol.setInnerScope(scope);
		symbol.setType(st);
	}

	private String getInnerName(SymbolType st) {
		String preffix = ((SymbolDataAware<?>) startingNode).getSymbolData()
				.getName();
		return getSymbolName(st.getName().substring(preffix.length() + 1),
				false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String getContext(TypeDeclaration type, T context) {

		List<SymbolAction> actions = new LinkedList<SymbolAction>();

		if (actionProvider != null) {
			actions.addAll(actionProvider.getActions(type));
		}

		SymbolType st = buildSymbolType(type);
		String keyName = getSymbolName(st.getName(), false);

		Symbol oldSymbol = symbolTable.findSymbol(keyName, ReferenceType.TYPE);
		Symbol added = null;
		if (oldSymbol == null || !oldSymbol.getType().equals(st)) {
			added = new Symbol(keyName, st, type,
					ReferenceType.TYPE, actions);
			added.setInnerScope(new Scope(added));
			symbolTable.pushSymbol(added, true);

		} else {

			Node location = oldSymbol.getLocation();
			if ((location == null || location instanceof ImportDeclaration)
					&& !st.belongsToAnonymousClass()) {
				added = oldSymbol;
				overrideSymbol(added, type, st, new Scope(added));

			} else {
				if (startingNode != null && startingNode != type) {

					String innerClassName = getInnerName(st);
					added = new Symbol(innerClassName, st, type,
							ReferenceType.TYPE, actions);
					added.setInnerScope(oldSymbol.getInnerScope());
					symbolTable.pushSymbol(added, true);

				}
			}

		}
		if (added != null && !st.belongsToAnonymousClass()) {
			pushCanonicalName(added, st);
		}
		return st.getName();
	}

	public void visit(ClassOrInterfaceDeclaration type, T context) {
		boolean restore = false;
		if (startingNode == null) {
			startingNode = type;
			restore = true;
		}
		String name = getContext(type, context);
		String oldCtx = contextName;
		contextName = name;
		List<BodyDeclaration> members = type.getMembers();
		processMembers(members, context);
		contextName = oldCtx;
		if (restore) {
			startingNode = null;
		}
	}

	public void visit(EnumDeclaration type, T context) {
		boolean restore = false;
		if (startingNode == null) {
			startingNode = type;
			restore = true;
		}
		String name = getContext(type, context);
		String oldCtx = contextName;
		contextName = name;
		List<BodyDeclaration> members = type.getMembers();
		processMembers(members, context);
		contextName = oldCtx;
		if (restore) {
			startingNode = null;
		}
	}

	public void visit(AnnotationDeclaration type, T context) {
		boolean restore = false;
		if (startingNode == null) {
			startingNode = type;
			restore = true;
		}
		String name = getContext(type, context);
		String oldCtx = contextName;
		contextName = name;
		List<BodyDeclaration> members = type.getMembers();
		processMembers(members, context);
		contextName = oldCtx;
		if (restore) {
			startingNode = null;
		}
	}

	public void processMembers(List<BodyDeclaration> members, T context) {
		if (members != null) {
			for (BodyDeclaration bd : members) {
				if (bd instanceof TypeDeclaration) {
					bd.accept(this, context);
				}
			}
		}
	}

	public void visit(ObjectCreationExpr n, T context) {
		boolean restore = false;
		if (startingNode == null) {
			startingNode = n;
			restore = true;
		}
		List<BodyDeclaration> members = n.getAnonymousClassBody();
		if (members != null) {
			SymbolType st = symbolTable.getType("this", ReferenceType.VARIABLE);
			n.setSymbolData(st);
			String name = st.getName();
			String oldCtx = contextName;
			contextName = name;
			processMembers(members, context);
			contextName = oldCtx;
		}
		if (restore) {
			startingNode = null;
		}
	}

	public void visit(ImportDeclaration id, T context) {

		List<SymbolAction> actions = new LinkedList<SymbolAction>();
		if (id.isStatic()) {
			actions.add(new LoadStaticImportsAction());
		}
		if (actionProvider != null) {
			actions.addAll(actionProvider.getActions(id));
		}

		if (!id.isAsterisk()) {
			if (!id.isStatic()) {
				String typeName = id.getName().toString();
				addType(typeName, true, id, actions);
			} else {
				String typeName = id.getName().toString();
				QualifiedNameExpr type = (QualifiedNameExpr) id.getName();
				String className = type.getQualifier().toString();
				Class<?> result = null;
				boolean finish = false;
				while (result == null && !finish) {
					try {
						result = Class.forName(className, false, classLoader);

						finish = true;
					} catch (ClassNotFoundException e) {
						int index = className.lastIndexOf('.');
						if (index != -1) {
							String aux = className.substring(0, index) + "$"
									+ className.substring(index + 1);
							className = aux;
						} else {
							finish = true;
						}
					}

				}
				if (result != null) {
					symbolTable.pushSymbol(typeName, ReferenceType.TYPE,
							new SymbolType(result), id, actions, true);
				} else {
					throw new RuntimeException("Invalid static import "
							+ type.getQualifier().toString());
				}
			}
		} else {
			if (classLoader != null) {
				String typeName = id.getName().toString();

				if (!id.isStatic()) {
					loadClassesFromPackage(typeName, actions);
				} else {

					symbolTable.pushSymbol(typeName, ReferenceType.TYPE,
							new SymbolType(typeName), id, actions, true);
				}

			}
		}

	}

	private void loadNestedClasses(Class<?> clazz, boolean imported, Node node) {
		Class<?>[] innerClasses = clazz.getDeclaredClasses();
		if (innerClasses != null) {
			for (int i = 0; i < innerClasses.length; i++) {
				if (!Modifier.isPrivate(innerClasses[i].getModifiers())) {
					String fullName = innerClasses[i].getName();
					SymbolType st = new SymbolType(innerClasses[i]);
					symbolTable
							.pushSymbol(
									getSymbolName(fullName, imported),
									org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
									st, node, true);
				}
			}

		}
	}

	private void addType(String name, boolean imported, Node node,
			List<SymbolAction> actions) {
		if (classLoader != null && name != null) {
			try {
				Class<?> clazz = Class.forName(name, false, classLoader);
				if (!Modifier.isPrivate(clazz.getModifiers())
						&& !clazz.isAnonymousClass()) {

					SymbolType st = new SymbolType(clazz);
					symbolTable
							.pushSymbol(
									getSymbolName(name, imported),
									org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
									st, node, actions, true);

					if (clazz.isMemberClass()) {
						String cname = clazz.getCanonicalName();
						if (cname != null) {
							symbolTable
									.pushSymbol(
											cname,
											org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
											st, node, actions, true);

							Package pkg = clazz.getPackage();
							if (pkg != null) {
								if (pkg.getName().equals(packageName) && node != null) {

									symbolTable
											.pushSymbol(
													clazz.getSimpleName(),
													org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
													st, node, actions, true);
								}
							}
						}

						loadNestedClasses(clazz, imported, node);

					}

				}
			} catch (ClassNotFoundException e) {
				loadInnerClass(name, imported, node, actions);
			} catch (IncompatibleClassChangeError e2) {
				int index = name.lastIndexOf("$");
				if (index != -1) {
					addType(name.substring(0, index), imported, node, actions);
				}
			}

		}
	}

	private void loadInnerClass(String name, boolean imported, Node node,
			List<SymbolAction> actions) {
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
					String keyName = getSymbolName(internalName, imported);
					SymbolType st = new SymbolType(clazz);
					Symbol<?> pushedSymbol = symbolTable
							.pushSymbol(
									keyName,
									org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE,
									st, node, actions, true);
					if (pushedSymbol != null) {
						loadNestedClasses(clazz, imported, node);
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
					addType(internalName.substring(0, index), imported, node,
							actions);
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

				String[] split = name.split("\\$\\d");
				if (split.length == 1) {
					addType(name, false, null, actions);
				}

			}
		}
	}

	private void loadClassesFromPackage(String packageName,
			List<SymbolAction> actions) {

		URL[] urls = ((URLClassLoader) classLoader.getParent()).getURLs();
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

							String[] split = resource.getName().split("\\$\\d");
							if (split.length == 1) {
								addType(name, false, null, actions);
							}

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
							String[] split = name.split("\\$\\d");
							if (split.length == 1) {
								defaultJavaLangClasses.add(split[0]);
							}
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

		packageName = contextName;
		loadClassesFromPackage(packageName, actions);
		if (cu.getImports() != null) {
			for (ImportDeclaration i : cu.getImports()) {
				i.accept(this, context);
			}
		}
		if (cu.getTypes() != null) {
			for (TypeDeclaration typeDeclaration : cu.getTypes()) {
				typeDeclaration.accept(this, context);
			}
		}
		startingNode = null;
	}

	public Class<?> loadClass(Type t) throws ClassNotFoundException {

		Class<?> result = ASTSymbolTypeResolver.getInstance().valueOf(t)
				.getClazz();
		if (result == null) {
			throw new ClassNotFoundException("The class " + t.toString()
					+ " is not found");
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
		return symbolTable
				.findSymbol(
						name,
						org.walkmod.javalang.compiler.symbols.ReferenceType.TYPE)
				.getType().getName();
	}

	public void clear() {
		packageName = null;
		contextName = null;
		startingNode = null;
	}

}
