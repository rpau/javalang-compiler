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

public class ResultBuilderFromCallGenerics implements
		Builder<Map<String, SymbolType>> {

	private List<Type> generics = null;
	private Method method = null;
	private SymbolType scope = null;
	private SymbolTable symbolTable;

	public ResultBuilderFromCallGenerics(List<Type> generics, Method method) {
		this.generics = generics;
		this.method = method;
	}

	public ResultBuilderFromCallGenerics(SymbolType scope, Method method,
			SymbolTable symbolTable) {
		this.scope = scope;
		this.symbolTable = symbolTable;
		this.method = method;
	}

	@Override
	public Map<String, SymbolType> build(Map<String, SymbolType> typeMapping)
			throws Exception {
		if (generics != null) {
			SymbolType[] syms = ASTSymbolTypeResolver.getInstance().valueOf(
					generics);
			SymbolType scope = new SymbolType();
			scope.setParameterizedTypes(Arrays.asList(syms));
			updateTypeMapping(method.getGenericReturnType(), typeMapping,
					scope, null, true);
		} else if (scope != null) {
			String symbolName = scope.getClazz().getName();
			if (scope.getClazz().isMemberClass()) {
				symbolName = scope.getClazz().getCanonicalName();
			}
			Map<String, SymbolType> params = null;
			Symbol<?> s = symbolTable
					.findSymbol(symbolName, ReferenceType.TYPE);
			if (s != null) {
				Scope scope = s.getInnerScope();

				if (scope != null) {
					Class<?> clazz = this.scope.getClazz();
					if (clazz != null) {
						clazz = clazz.getSuperclass();
					}
					if (method != null
							&& clazz != null
							&& method.getDeclaringClass().isAssignableFrom(
									clazz)) {
						// we need to find for the super type params to resolve
						// the method
						Symbol<?> superSymbol = scope.findSymbol("super");
						if (superSymbol != null) {
							scope = superSymbol.getInnerScope();
						}

					}
				}

				if (scope != null) {

					params = scope.getTypeParams();
					if (params != null) {
						typeMapping.putAll(params);
					}
				}
			}

			List<SymbolType> paramTypes = scope.getParameterizedTypes();

			if (paramTypes != null) {
				updateTypeMapping(method.getDeclaringClass(), typeMapping,
						scope, params, false);
			}

		}

		return typeMapping;
	}

	private void updateTypeMapping(java.lang.reflect.Type type,
			Map<String, SymbolType> typeMapping, SymbolType parameterizedType,
			Map<String, SymbolType> scopeMapping, boolean genericArgs) {
		if (parameterizedType != null) {
			if (type instanceof TypeVariable) {
				TypeVariable<?> tv = (TypeVariable<?>) type;
				String vname = tv.getName();
				SymbolType existingSymbol = typeMapping.get(vname);
				if (existingSymbol != null) {
					typeMapping.put(vname, existingSymbol.refactor(
							vname,
							parameterizedType,
							genericArgs
									|| (scopeMapping != null && scopeMapping
											.containsKey(vname))));

				} else {
					typeMapping.put(vname, parameterizedType);
				}

				java.lang.reflect.Type[] bounds = tv.getBounds();
				List<SymbolType> paramBounds = parameterizedType.getBounds();
				if (paramBounds != null) {
					for (int i = 0; i < bounds.length; i++) {
						updateTypeMapping(bounds[i], typeMapping,
								paramBounds.get(i), scopeMapping, genericArgs);
					}
				}

			} else if (type instanceof WildcardType) {
				WildcardType wildcard = (WildcardType) type;
				java.lang.reflect.Type[] bounds = wildcard.getUpperBounds();
				List<SymbolType> paramBounds = parameterizedType.getBounds();
				if (paramBounds != null) {
					for (int i = 0; i < bounds.length; i++) {
						updateTypeMapping(bounds[i], typeMapping,
								paramBounds.get(i), scopeMapping, genericArgs);
					}
				}
				bounds = wildcard.getLowerBounds();
				paramBounds = parameterizedType.getLowerBounds();
				if (paramBounds != null) {
					for (int i = 0; i < bounds.length; i++) {
						updateTypeMapping(bounds[i], typeMapping,
								paramBounds.get(i), scopeMapping, genericArgs);
					}
				}

			} else if (type instanceof ParameterizedType) {
				ParameterizedType paramType = (ParameterizedType) type;
				java.lang.reflect.Type[] typeArgs = paramType
						.getActualTypeArguments();
				List<SymbolType> paramTypeParams = parameterizedType
						.getParameterizedTypes();
				if (paramTypeParams != null) {

					for (int i = 0; i < typeArgs.length; i++) {
						SymbolType st = null;
						if (i < paramTypeParams.size()) {
							st = paramTypeParams.get(i);
						}
						updateTypeMapping(typeArgs[i], typeMapping, st,
								scopeMapping, genericArgs);
					}
				}

			} else if (type instanceof GenericArrayType) {
				GenericArrayType arrayType = (GenericArrayType) type;
				SymbolType st = parameterizedType.clone();
				st.setArrayCount(parameterizedType.getArrayCount() - 1);

				updateTypeMapping(arrayType.getGenericComponentType(),
						typeMapping, st, scopeMapping, genericArgs);

			} else if (type instanceof Class) {

				Class<?> clazz = (Class<?>) type;
				java.lang.reflect.Type[] tparams = clazz.getTypeParameters();
				List<SymbolType> paramTypeParams = parameterizedType
						.getParameterizedTypes();
				if (paramTypeParams != null) {
					for (int i = 0; i < tparams.length; i++) {
						SymbolType st = null;
						if (i < paramTypeParams.size()) {
							st = paramTypeParams.get(i);
						}
						updateTypeMapping(tparams[i], typeMapping, st,
								scopeMapping, genericArgs);
					}
				}
			}
		}
	}
}
