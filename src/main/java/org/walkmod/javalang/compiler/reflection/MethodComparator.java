package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;

import org.walkmod.javalang.compiler.types.Types;

public class MethodComparator implements Comparator<Method> {
	
	
	private static boolean isAssignable(Class<?> clazz2, Class<?> clazz1) {
		boolean isMethod2First = true;
		boolean isAssignable = Types.isAssignable(clazz2, clazz1);
		if (!isAssignable) {
			if (Types.isAssignable(clazz1, clazz2)) {
				isMethod2First = false;
			} else {
				int h2 = ClassInspector.getClassHierarchyHeight(clazz2);
				int h1 = ClassInspector.getClassHierarchyHeight(clazz1);
				isMethod2First = h2 > h1;
			}
		} else {
			isMethod2First = true;
		}
		return isMethod2First;
	}
	
	@Override
	public int compare(Method method1, Method method2) {
		Parameter[] params1 = method1.getParameters();
		Parameter[] params2 = method2.getParameters();

		if (params1.length < params2.length) {
			return -1;
		} else if (params1.length > params2.length) {
			return 1;
		} else {
			boolean isMethod2First = true;

			try {
				for (int i = 0; i < params1.length && isMethod2First; i++) {

					Class<?> clazz2 = params2[i].getType();
					Class<?> clazz1 = params1[i].getType();

					if (i == params1.length - 1) {
						boolean isVarArgs1 = method1.isVarArgs();
						boolean isVarArgs2 = method2.isVarArgs();
						if ((isVarArgs1 && isVarArgs2)
								|| (!isVarArgs1 && !isVarArgs2)) {
							isMethod2First = isAssignable(clazz2, clazz1);

						} else {

							isMethod2First = method1.isVarArgs()
									&& !method2.isVarArgs();

						}
					} else {
						isMethod2First = isAssignable(clazz2, clazz1);
					}

				}
				if (isMethod2First) {
					return 1;
				} else {
					return -1;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}
}