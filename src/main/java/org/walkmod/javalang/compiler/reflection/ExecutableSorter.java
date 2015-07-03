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
package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.walkmod.javalang.compiler.symbols.MethodSymbol;

public class ExecutableSorter<T extends Executable> implements Comparator<T> {

	private Class<?>[] args = null;

	public List<T> sort(T[] methods, Class<?>[] args) {
		Map<String, List<T>> map = new HashMap<String, List<T>>();
		this.args = args;
		LinkedList<T> result = new LinkedList<T>();
		for (T method : methods) {
			List<T> aux = map.get(method.getName());
			if (aux == null) {
				aux = new LinkedList<T>();
				map.put(method.getName(), aux);
			}
			aux.add(method);
		}

		Set<String> entries = map.keySet();
		for (String entry : entries) {
			List<T> aux = map.get(entry);

			ArrayList<T> sortedList = new ArrayList<T>();
			Iterator<T> it = aux.iterator();
			while (it.hasNext()) {
				Iterator<T> li = sortedList.iterator();
				T method = it.next();
				boolean inserted = false;
				if (sortedList.isEmpty()) {
					sortedList.add(method);
				} else {
					int pos = 0;

					while (!inserted && li.hasNext()) {
						T previous = li.next();
						if (compare(method, previous) == -1) {
							// sortedList.add(pos, method);
							inserted = true;
						} else {
							pos++;
						}
					}

					sortedList.add(pos, method);

				}
			}

			result.addAll(sortedList);
		}

		return result;
	}

	@Override
	public int compare(T method1, T method2) {
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
					Class<?> arg = null;
					if (args != null && i < args.length) {
						arg = args[i];
					}

					while (arg != null && arg.isArray()) {
						arg = arg.getComponentType();
					}

					Class<?> clazz2 = params2[i].getType();
					Class<?> clazz1 = params1[i].getType();
					while (clazz2.isArray() && clazz1.isArray()) {
						clazz2 = clazz2.getComponentType();
						clazz1 = clazz1.getComponentType();

					}

					if (i == params1.length - 1) {
						boolean isVarArgs1 = method1.isVarArgs();
						boolean isVarArgs2 = method2.isVarArgs();
						if ((isVarArgs1 && isVarArgs2)
								|| (!isVarArgs1 && !isVarArgs2)) {
							isMethod2First = ClassInspector.isMoreSpecficFor(
									clazz2, clazz1, arg);

						} else {

							isMethod2First = method1.isVarArgs()
									&& !method2.isVarArgs();

						}
					} else {
						isMethod2First = ClassInspector.isMoreSpecficFor(
								clazz2, clazz1, arg);
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
