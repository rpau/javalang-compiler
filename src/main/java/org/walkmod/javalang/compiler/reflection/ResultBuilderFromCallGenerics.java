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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Scope;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class ResultBuilderFromCallGenerics implements Builder<SymbolTable> {

   private List<Type> generics = null;
   private Method method = null;
   private SymbolType scope = null;
   private SymbolTable symbolTable;

   public ResultBuilderFromCallGenerics(List<Type> generics, Method method) {
      this.generics = generics;
      this.method = method;
   }

   public ResultBuilderFromCallGenerics(SymbolType scope, Method method, SymbolTable symbolTable) {
      this.scope = scope;
      this.symbolTable = symbolTable;
      this.method = method;
   }

   @Override
   public SymbolTable build(SymbolTable genericsSymbolTable) throws Exception {

      if (generics != null) {
         SymbolType[] syms = ASTSymbolTypeResolver.getInstance().valueOf(generics);
         SymbolType scope = new SymbolType();
         scope.setParameterizedTypes(Arrays.asList(syms));
         genericsSymbolTable.pushScope();
         updateTypeMapping(method.getGenericReturnType(), genericsSymbolTable, scope, true);
      } else if (scope != null) {
         String symbolName = scope.getClazz().getName();
         if (scope.getClazz().isMemberClass()) {
            symbolName = scope.getClazz().getCanonicalName();
         }
         Symbol<?> s = symbolTable.findSymbol(symbolName);
         if (s != null) {
            Scope scope = s.getInnerScope();

            if (scope != null) {
               Class<?> clazz = this.scope.getClazz();
               if (clazz != null) {
                  clazz = clazz.getSuperclass();
               }
               if (method != null && clazz != null && method.getDeclaringClass().isAssignableFrom(clazz)) {
                  // we need to find for the super type params to resolve
                  // the method
                  Symbol<?> superSymbol = scope.findSymbol("super");
                  if (superSymbol != null) {
                     scope = superSymbol.getInnerScope();
                  }

               }
            }

            if (scope != null) {
               Map<String, SymbolType> typeParams = scope.getTypeParams();

               Scope newScope = new Scope();
               for (String key : typeParams.keySet()) {
                  newScope.addSymbol(key, typeParams.get(key).clone(), null);
               }

               genericsSymbolTable.pushScope(newScope);
            }
         }

         List<SymbolType> paramTypes = scope.getParameterizedTypes();

         if (paramTypes != null) {
            updateTypeMapping(method.getDeclaringClass(), genericsSymbolTable, scope, false);
         }

         Scope newScope = new Scope();
         genericsSymbolTable.pushScope(newScope);
         TypeVariable<?>[] typeParams = method.getTypeParameters();
         for (int i = 0; i < typeParams.length; i++) {
            genericsSymbolTable.pushSymbol(typeParams[i].getName(), ReferenceType.TYPE_PARAM,
                  SymbolType.valueOf(typeParams[i], null), null);
         }
      } else {
         Scope newScope = new Scope();
         genericsSymbolTable.pushScope(newScope);
      }

      return genericsSymbolTable;
   }

   private void updateTypeMapping(java.lang.reflect.Type type, SymbolTable genericsSymbolTable,
         SymbolType parameterizedType, boolean genericArgs) {
      if (parameterizedType != null) {
         if (type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            String vname = tv.getName();
            Symbol<?> s = genericsSymbolTable.findSymbol(vname);
            if (s != null) {
               boolean isInTheTopScope = genericsSymbolTable.getScopes().peek().findSymbol(vname) != null;
               SymbolType refactor = s.getType().refactor(vname, parameterizedType, genericArgs || isInTheTopScope);
               s.setType(refactor);

            } else {
               genericsSymbolTable.pushSymbol(vname, ReferenceType.TYPE, parameterizedType, null);

            }

            java.lang.reflect.Type[] bounds = tv.getBounds();
            List<SymbolType> paramBounds = parameterizedType.getBounds();
            if (paramBounds != null) {
               for (int i = 0; i < bounds.length; i++) {
                  // to avoid recursive type declarations Enum<E>
                  if (!parameterizedType.equals(paramBounds.get(i))) {
                     updateTypeMapping(bounds[i], genericsSymbolTable, paramBounds.get(i), genericArgs);
                  }
               }
            }

         } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            java.lang.reflect.Type[] bounds = wildcard.getUpperBounds();
            List<SymbolType> paramBounds = parameterizedType.getBounds();
            if (paramBounds != null) {
               for (int i = 0; i < bounds.length; i++) {
                  updateTypeMapping(bounds[i], genericsSymbolTable, paramBounds.get(i), genericArgs);
               }
            }
            bounds = wildcard.getLowerBounds();
            paramBounds = parameterizedType.getLowerBounds();
            if (paramBounds != null) {
               for (int i = 0; i < bounds.length; i++) {
                  updateTypeMapping(bounds[i], genericsSymbolTable, paramBounds.get(i), genericArgs);
               }
            }

         } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
            List<SymbolType> paramTypeParams = parameterizedType.getParameterizedTypes();
            if (paramTypeParams != null) {

               for (int i = 0; i < typeArgs.length; i++) {
                  SymbolType st = null;
                  if (i < paramTypeParams.size()) {
                     st = paramTypeParams.get(i);
                  }
                  updateTypeMapping(typeArgs[i], genericsSymbolTable, st, genericArgs);
               }
            }

         } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            SymbolType st = parameterizedType.clone();
            st.setArrayCount(parameterizedType.getArrayCount() - 1);

            updateTypeMapping(arrayType.getGenericComponentType(), genericsSymbolTable, st, genericArgs);

         } else if (type instanceof Class) {

            Class<?> clazz = (Class<?>) type;
            java.lang.reflect.Type[] tparams = clazz.getTypeParameters();
            List<SymbolType> paramTypeParams = parameterizedType.getParameterizedTypes();
            if (paramTypeParams != null) {
               for (int i = 0; i < tparams.length; i++) {
                  SymbolType st = null;
                  if (i < paramTypeParams.size()) {
                     st = paramTypeParams.get(i);
                  }
                  updateTypeMapping(tparams[i], genericsSymbolTable, st, genericArgs);
               }
            }
         }
      }
   }
}
