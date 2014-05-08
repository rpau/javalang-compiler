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

import org.walkmod.javalang.ParseException;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.expr.BooleanLiteralExpr;
import org.walkmod.javalang.ast.expr.CharLiteralExpr;
import org.walkmod.javalang.ast.expr.DoubleLiteralExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.LiteralExpr;
import org.walkmod.javalang.ast.expr.LongLiteralExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NullLiteralExpr;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.ReferenceType;
import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.ast.type.PrimitiveType.Primitive;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.exceptions.InvalidImportException;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;



public class TypeTable implements List<ImportDeclaration> {

	private List<ImportDeclaration> imports = new LinkedList<ImportDeclaration>();

	private static Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();

	private static Map<String, LiteralExpr> defaultValues = new HashMap<String, LiteralExpr>();

	private static Map<String, String> wrapperClasses = new HashMap<String, String>();

	private static Map<String, Integer> matrixTypePosition;

	private static boolean[][] compatibilityMatrix;

	private String currentPackage;

	private String currentClassSimpleName;

	static {
		primitiveClasses.put("boolean", boolean.class);
		primitiveClasses.put("int", int.class);
		primitiveClasses.put("long", long.class);
		primitiveClasses.put("double", double.class);
		primitiveClasses.put("char", char.class);
		primitiveClasses.put("float", float.class);
		primitiveClasses.put("short", short.class);
		primitiveClasses.put("byte", byte.class);

		defaultValues.put("int", new IntegerLiteralExpr("0"));
		defaultValues.put("boolean", new BooleanLiteralExpr(false));
		defaultValues.put("float", new IntegerLiteralExpr("0"));
		defaultValues.put("long", new LongLiteralExpr("0"));
		defaultValues.put("double", new DoubleLiteralExpr("0"));
		defaultValues.put("char", new CharLiteralExpr(""));
		defaultValues.put("short", new IntegerLiteralExpr("0"));
		defaultValues.put("byte", new IntegerLiteralExpr("0"));

		matrixTypePosition = new HashMap<String, Integer>();
		matrixTypePosition.put("byte", 0);
		matrixTypePosition.put("java.lang.Byte", 0);
		matrixTypePosition.put("short", 1);
		matrixTypePosition.put("char", 2);
		matrixTypePosition.put("java.lang.Character", 2);
		matrixTypePosition.put("int", 3);
		matrixTypePosition.put("java.lang.Integer", 3);
		matrixTypePosition.put("long", 4);
		matrixTypePosition.put("java.lang.Long", 4);
		matrixTypePosition.put("float", 5);
		matrixTypePosition.put("java.lang.Float", 5);
		matrixTypePosition.put("double", 6);
		matrixTypePosition.put("java.lang.Double", 6);
		matrixTypePosition.put("boolean", 7);
		matrixTypePosition.put("java.lang.Boolean", 7);
		matrixTypePosition.put("String", 8);
		matrixTypePosition.put("java.lang.String", 8);
		matrixTypePosition.put("java.lang.Object", 9);

		wrapperClasses.put("java.lang.Byte", "byte");
		wrapperClasses.put("java.lang.Character", "char");
		wrapperClasses.put("java.lang.Integer", "int");
		wrapperClasses.put("java.lang.Long", "long");
		wrapperClasses.put("java.lang.Float", "float");
		wrapperClasses.put("java.lang.Double", "double");
		wrapperClasses.put("java.lang.Boolean", "boolean");

		compatibilityMatrix = new boolean[][] {
				{ true, true, true, true, true, true, true, false, false, true },
				{ false, true, false, true, true, true, true, false, false,
						true },
				{ false, false, true, true, true, true, true, false, false,
						true },
				{ false, false, false, true, true, true, true, false, false,
						true },
				{ false, false, false, false, true, true, true, false, false,
						true },
				{ false, false, false, false, false, true, true, false, false,
						true },
				{ false, false, false, false, false, false, true, false, false,
						true },
				{ false, false, false, false, false, false, false, true, false,
						true },
				{ false, false, false, false, false, false, false, false, true,
						true },
				{ false, false, false, false, false, false, false, false,
						false, true } };

	}

	public TypeTable() {
	}

	/**
	 * Adds an import declaration. When exists another imported class with the
	 * same name has been imported, an {@link InvalidImportException} is thrown.
	 * 
	 */
	@Override
	public boolean add(ImportDeclaration id) throws InvalidImportException {
		// verificar que no hay otro import que acabe con el mismo nombre

		String packName = null;

		if (!id.isAsterisk()) {
			String importName = id.getName().toString();
			int dotIdx = importName.lastIndexOf(".");
			if (dotIdx != -1) {
				packName = importName.substring(0, dotIdx);
			} else {
				packName = importName;
			}
		}
		String idName = id.getName().toString();

		for (ImportDeclaration current : imports) {

			if (!current.isAsterisk()) {
				// compare the simple name
				if (current.getName().getName().equals(id.getName().getName())) {

					// its exactly the same import
					if (current.getName().equals(id.getName())) {
						return false;
					} else {
						// it is invalid to add an import with the same simple
						// name
						throw new InvalidImportException();
					}
				} else if (current.getName().toString().endsWith(idName)) {
					// it is an inner class
					return false;
				}
			} else {
				// retrieving the complete import name (package)
				String packageName = current.getName().toString();

				// comparing if it is already included
				if ((packName != null && packageName.equals(packName)) ||
				// its exactly the same package
						(id.getName().toString().equals(packageName))) {

					return false;
				}

				String importedClass = packageName + "."
						+ id.getName().getName();

				try {
					Class.forName(importedClass, false, this.getClass()
							.getClassLoader());

					// exists another class with the same name imported
					throw new InvalidImportException();

				} catch (ClassNotFoundException e) {
					// do nothing
				}

			}

		}
		return imports.add(id);
	}

	public void clear() {
		imports = new LinkedList<ImportDeclaration>();
	}

	@Override
	public int size() {
		return imports.size();
	}

	@Override
	public boolean isEmpty() {
		return imports.isEmpty();
	}

	@Override
	public boolean contains(Object o) {

		if (o instanceof ImportDeclaration) {

			ImportDeclaration element = (ImportDeclaration) o;

			String elementName = element.getName().toString();
			String packageN = elementName;

			if (packageN.contains(".")) {
				packageN = packageN.substring(0, elementName.lastIndexOf("."));
			}

			if (elementName.startsWith("java.lang")) {
				return true;
			}

			for (ImportDeclaration id : imports) {
				String importName = id.getName().toString();
				if (importName.equals(elementName) ||
				// it is an inner class
						importName.endsWith(packageN)) {
					return true;
				} else {
					if (id.isAsterisk()) {
						String packageName = id.getName().toString();

						// are in the same root package
						if (elementName.startsWith(packageName)) {
							// are in the same level
							String substring = elementName
									.substring(packageName.length());
							if (!substring.contains(".")) {
								return true;
							}

						}
					}
				}
			}
		}

		return false;
	}

	@Override
	public Iterator<ImportDeclaration> iterator() {
		return imports.iterator();
	}

	@Override
	public Object[] toArray() {
		return imports.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return imports.toArray(a);
	}

	@Override
	public boolean remove(Object o) {
		return imports.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		boolean result = false;
		for (Object o : c) {
			result = result && contains(o);
		}
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends ImportDeclaration> c) {
		boolean result = true;
		if (c != null) {
			for (ImportDeclaration id : c) {
				result = result && imports.add(id);
			}
		}
		return result;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return imports.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return imports.retainAll(c);
	}

	public static boolean isCompatible(Class<?> fromClass, Class<?> toClass) {
		if (fromClass == null || toClass == null) {
			return true;
		}
		if (matrixTypePosition.containsKey(fromClass.getName())
				&& matrixTypePosition.containsKey(toClass.getName())) {
			return compatibilityMatrix[matrixTypePosition.get(fromClass
					.getName())][matrixTypePosition.get(toClass.getName())];
		} else {
			return toClass.isAssignableFrom(fromClass);
		}

	}

	public static MethodCallExpr getPrimitiveMethodCallExpr(Class<?> clazz,
			Expression scope) {

		if (wrapperClasses.containsKey(clazz.getName())) {
			String basicType = wrapperClasses.get(clazz.getName());
			String methodCallExpr = scope.toString() + "." + basicType
					+ "Value()";

			try {
				return (MethodCallExpr) ASTManager.parse(MethodCallExpr.class,
						methodCallExpr);

			} catch (ParseException e) {
				throw new RuntimeException(e.getCause());
			}

		}
		throw new RuntimeException("The clazz " + clazz.getName()
				+ " is not a basic type");
	}

	public static boolean isPrimitiveWrapperClass(Class<?> clazz) {
		return wrapperClasses.containsKey(clazz.getName());
	}

	public static boolean isCompatible(Class<?>[] fromClasses,
			Class<?>[] toClasses) {

		if (fromClasses.length == toClasses.length) {
			boolean assignable = true;
			for (int i = 0; i < fromClasses.length && assignable; i++) {
				assignable = isCompatible(fromClasses[i], toClasses[i]);
			}
			return assignable;
		}
		return false;
	}

	public org.walkmod.javalang.compiler.Type valueOf(java.lang.reflect.Type type,
			Map<String, org.walkmod.javalang.compiler.Type> typeMapping) {

		org.walkmod.javalang.compiler.Type returnType = null;

		if (type instanceof Class<?>) {
			Class<?> aux = ((Class<?>) type);
			returnType = new org.walkmod.javalang.compiler.Type(aux.getName());
			if (aux.isArray()) {
				returnType.setArrayCount(1);
				returnType.setName(aux.getComponentType().getName());
			}

		} else if (type instanceof TypeVariable) {

			String variableName = ((TypeVariable<?>) type).getName();
			org.walkmod.javalang.compiler.Type aux = typeMapping.get(variableName);

			if (aux == null) {
				aux = new org.walkmod.javalang.compiler.Type(Object.class.getName());
				return aux;
			} else {
				return aux;
			}

		} else if (type instanceof ParameterizedType) {
			Class<?> auxClass = (Class<?>) ((ParameterizedType) type)
					.getRawType();

			java.lang.reflect.Type[] types = ((ParameterizedType) type)
					.getActualTypeArguments();

			returnType = new org.walkmod.javalang.compiler.Type(auxClass.getName());

			if (types != null) {
				List<org.walkmod.javalang.compiler.Type> params = new LinkedList<org.walkmod.javalang.compiler.Type>();
				returnType.setParameterizedTypes(params);
				for (java.lang.reflect.Type t : types) {
					org.walkmod.javalang.compiler.Type param = typeMapping.get(t
							.toString());
					params.add(param);
				}
			}

		} else if (type instanceof GenericArrayType) {
			// method.getReturnType();(
			returnType = new org.walkmod.javalang.compiler.Type(valueOf(
					((GenericArrayType) type).getGenericComponentType(),
					typeMapping).getName());

			returnType.setArrayCount(1);

		} else {
			throw new RuntimeException("Unssuported Java Type "
					+ type.toString());
		}
		return returnType;

	}

	public org.walkmod.javalang.compiler.Type valueOf(Type parserType) {

		org.walkmod.javalang.compiler.Type result = new org.walkmod.javalang.compiler.Type();

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
				String importName = name;
				if (!importName.contains(".")) {
					importName = "." + importName;
				}
				for (ImportDeclaration i : imports) {

					if (i.getName().toString().endsWith(importName)) {
						result.setName(i.getName().toString());
						break;
					} else {
						int endIdx = name.lastIndexOf(".");
						if (endIdx != -1) {
							String aux = name.substring(0, endIdx);
							// the last part of the name corresponds to an inner
							// class
							if (i.getName().toString().endsWith(aux)) {
								result.setName(i.getName().toString() + "$"
										+ name.substring(endIdx + 1));
								break;
							}
						}
					}
				}
				try {
					Class.forName("java.lang." + name);
					result.setName("java.lang." + name);

				} catch (ClassNotFoundException e) {

					if (result.getName() == null) {
						result.setName(name);
					}
				}
				if (type.getTypeArgs() != null) {

					List<org.walkmod.javalang.compiler.Type> typeArgs = new LinkedList<org.walkmod.javalang.compiler.Type>();

					for (Type typeArg : type.getTypeArgs()) {
						org.walkmod.javalang.compiler.Type aux = valueOf(typeArg);
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

			for (ImportDeclaration i : imports) {
				if (i.getName().toString().endsWith(name)) {
					result.setName(i.getName().toString());

					break;
				}
			}
			try {
				Class.forName("java.lang." + name);
				result.setName("java.lang." + name);

			} catch (ClassNotFoundException e) {

				if (result.getName() == null) {
					result.setName(name);
				}
			}
			if (type.getTypeArgs() != null) {

				List<org.walkmod.javalang.compiler.Type> typeArgs = new LinkedList<org.walkmod.javalang.compiler.Type>();

				for (Type typeArg : type.getTypeArgs()) {
					org.walkmod.javalang.compiler.Type aux = valueOf(typeArg);
					typeArgs.add(aux);
				}
				result.setParameterizedTypes(typeArgs);
			}

		} else if (parserType instanceof WildcardType) {
			if (((WildcardType) parserType).toString().equals("?")) {
				result.setName("java.lang.Object");
			}
		}

		return result;
	}

	/**
	 * This method returns the return type of a method in compilation time. When
	 * Java generics is applied, the scope type information is necessary to
	 * determine the return type of some collections methods.
	 * 
	 * @param method
	 * @param symbol
	 * @return
	 * @throws ClassNotFoundException
	 */
	public org.walkmod.javalang.compiler.Type getMethodType(Method method,
			Map<String, org.walkmod.javalang.compiler.Type> typeMapping)
			throws ClassNotFoundException {

		// mirem el tipus de resultat
		java.lang.reflect.Type type = method.getGenericReturnType();

		return valueOf(type, typeMapping);
	}

	/**
	 * This method returns a method(public, protected or private) which appears
	 * in the selected class or any of its parent (interface or superclass) or
	 * container classes (when the selected class is an inner class) which
	 * matches with the name and whose parameters are compatible with the
	 * selected typeArgs.
	 * 
	 * @param clazz
	 * @param methodName
	 * @param typeArgs
	 * @return
	 */
	public Method getMethod(Class<?> clazz, String methodName,
			Class<?>... typeArgs) {

		int numParams = typeArgs == null ? 0 : typeArgs.length;
		Method[] classMethods = clazz.getMethods();

		for (Method method : classMethods) {
			if (method.getName().equals(methodName)) {
				if (method.getParameterTypes().length == numParams) {
					boolean isCompatible = true;
					Class<?>[] methodParameterTypes = method
							.getParameterTypes();
					for (int i = 0; i < methodParameterTypes.length; i++) {
						isCompatible = isCompatible(typeArgs[i],
								methodParameterTypes[i]);
						if (!isCompatible)
							break;
					}
					if (isCompatible) {
						return method;
					}
				}
			}
		}

		classMethods = clazz.getDeclaredMethods();

		for (Method method : classMethods) {
			if (method.getName().equals(methodName)) {
				if (method.getParameterTypes().length == numParams) {
					boolean isCompatible = true;
					Class<?>[] methodParameterTypes = method
							.getParameterTypes();
					for (int i = 0; i < methodParameterTypes.length; i++) {
						isCompatible = isCompatible(typeArgs[i],
								methodParameterTypes[i]);
						if (!isCompatible)
							break;
					}
					if (isCompatible) {
						return method;
					}
				}
			}
		}
		Method result = null;
		if (clazz.isMemberClass()) {
			result = getMethod(clazz.getEnclosingClass(), methodName, typeArgs);
		}
		if (result == null && clazz.getSuperclass() != null) {

			return getMethod(clazz.getSuperclass(), methodName, typeArgs);
		}

		return result;

	}

	public Class<?> getJavaClass(String className)
			throws ClassNotFoundException {

		if (className == null) {
			return null;
		}
		if (primitiveClasses.containsKey(className)) {
			return primitiveClasses.get(className);
		}
		Class<?> result = null;
		try {
			if (className.contains(".") || currentPackage == null) {
				result = Class.forName(className, false, this.getClass()
						.getClassLoader());
			} else if (currentPackage != null) {
				result = Class.forName(currentPackage + "." + className, false,
						this.getClass().getClassLoader());
			}

		} catch (ClassNotFoundException e2) {

			boolean finish = false;
			Class<?> parent = null;
			String completeName = currentClassSimpleName;
			if (currentPackage != null) {
				if (completeName.contains("$")) {
					completeName = completeName.substring(0,
							completeName.indexOf("$"));
				}
				completeName = currentPackage + "." + completeName;
			}

			while (!finish) {
				try {
					// check if it is an inner class of any super class
					result = Class.forName(completeName + "$" + className,
							false, this.getClass().getClassLoader());

				} catch (ClassNotFoundException e3) {
					parent = Class.forName(completeName, false,
							this.getClass().getClassLoader()).getSuperclass();
					if (parent != null) {
						completeName = parent.getName();
					}
				} finally {
					finish = (result != null || parent == null);
				}
			}
			if (result == null) {
				// check the import declarations
				for (ImportDeclaration i : imports) {
					// complete import name
					if (i.getName().toString().endsWith(className)) {
						String importName = i.getName().toString();

						try {

							result = Class.forName(importName, false, this
									.getClass().getClassLoader());

						} catch (ClassNotFoundException e) {
							// looking for an inner class
							int lastIdx = importName.lastIndexOf(".");
							importName = importName.substring(0, lastIdx) + "$"
									+ importName.substring(lastIdx + 1);
							result = Class.forName(importName, false, this
									.getClass().getClassLoader());
						}
						return result;
					} else if (i.isAsterisk()) {
						String importName = i.getName().toString();

						if (!className.contains(".")) { // package name is
														// included
							importName = importName + "." + className;
							try {
								result = Class.forName(importName, false, this
										.getClass().getClassLoader());

								if (result != null) {
									return result;
								}

							} catch (ClassNotFoundException e) {

							}
						}
					}
				}

				className = "java.lang." + className;
				try {
					result = Class.forName(className, false, this.getClass()
							.getClassLoader());
				} catch (ClassNotFoundException e) {
					throw e;
				}
			}

		}
		return result;

	}

	public String getFullName(ClassOrInterfaceType type) {

		if (type.getScope() != null) {
			return getFullName(type.getScope());
		}

		for (ImportDeclaration i : imports) {
			if (i.getName().toString().endsWith(type.getName())) {
				return i.getName().toString();
			}
		}
		return type.getName();
	}

	public String getPackage(ClassOrInterfaceType type) {

		String fullName = getFullName(type);

		int dotIndex = fullName.lastIndexOf(".");

		if (dotIndex == -1) {
			return "";
		}
		return fullName.substring(0, dotIndex);
	}

	public static LiteralExpr getDefaultValue(Class<?> t) {
		String type = t.getName();
		if (wrapperClasses.containsKey(type)) {
			type = wrapperClasses.get(type);

		}
		if (defaultValues.containsKey(type)) {
			return defaultValues.get(type);
		}

		return new NullLiteralExpr();
	}

	public String getCurrentPackage() {
		return currentPackage;
	}

	public void setCurrentPackage(String currentPackage) {
		this.currentPackage = currentPackage;
	}

	public String getCurrentClassSimpleName() {
		return currentClassSimpleName;
	}

	public void setCurrentClassSimpleName(String currentClassSimpleName) {
		this.currentClassSimpleName = currentClassSimpleName;
	}

	public Class<?> getJavaClass(Type t) throws ClassNotFoundException {

		return getJavaClass(valueOf(t));
	}

	public Class<?> getJavaClass(org.walkmod.javalang.compiler.Type type)
			throws ClassNotFoundException {
		if (type != null && !type.getName().equals("void")
				&& !type.getName().startsWith("[")) {
			String clazzName = type.getName();
			if (type.getArrayCount() != 0) {
				// it is an array
				Class<?> aux = getJavaClass(clazzName);
				return Array.newInstance(aux, 1).getClass();
			}

			return getJavaClass(clazzName);

		} else {
			return null;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends ImportDeclaration> c) {

		return imports.addAll(index, c);
	}

	@Override
	public ImportDeclaration get(int index) {
		return imports.get(index);
	}

	@Override
	public ImportDeclaration set(int index, ImportDeclaration element) {

		return imports.set(index, element);
	}

	@Override
	public void add(int index, ImportDeclaration element) {
		imports.add(index, element);
	}

	@Override
	public ImportDeclaration remove(int index) {
		return imports.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return imports.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {

		return imports.lastIndexOf(o);
	}

	@Override
	public ListIterator<ImportDeclaration> listIterator() {
		return imports.listIterator();
	}

	@Override
	public ListIterator<ImportDeclaration> listIterator(int index) {

		return imports.listIterator(index);
	}

	@Override
	public List<ImportDeclaration> subList(int fromIndex, int toIndex) {

		return imports.subList(fromIndex, toIndex);
	}

	public List<ImportDeclaration> toImportList() {
		return imports;
	}

}
