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
import java.lang.reflect.Method;
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

public class ExecutableSorter<T extends Executable> implements Comparator<T> {

   public List<T> sort(T[] methods) {
      Map<String, List<T>> map = new HashMap<String, List<T>>();

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
            ListIterator<T> li = sortedList.listIterator(sortedList.size());
            T method = it.next();
            boolean inserted = false;
            if (sortedList.isEmpty()) {
               sortedList.add(method);
            } else {
               int pos = sortedList.size() - 1;
               while (!inserted && li.hasPrevious()) {
                  T previous = li.previous();
                  if (compare(method, previous) == 1) {
                     sortedList.add(pos + 1, method);
                     inserted = true;
                  }
                  pos--;
               }
               if (!inserted) {
                  sortedList.add(0, method);
               }
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

               Class<?> clazz2 = params2[i].getType();
               Class<?> clazz1 = params1[i].getType();

               if (i == params1.length - 1) {
                  boolean isVarArgs1 = method1.isVarArgs();
                  boolean isVarArgs2 = method2.isVarArgs();
                  if ((isVarArgs1 && isVarArgs2) || (!isVarArgs1 && !isVarArgs2)) {
                     isMethod2First = ClassInspector.isAssignable(clazz2, clazz1);

                  } else {

                     isMethod2First = method1.isVarArgs() && !method2.isVarArgs();

                  }
               } else {
                  if (clazz2.isArray() && clazz1.isArray()) {
                     clazz2 = clazz2.getComponentType();
                     clazz1 = clazz1.getComponentType();
                  }
                  isMethod2First = ClassInspector.isAssignable(clazz2, clazz1);
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
