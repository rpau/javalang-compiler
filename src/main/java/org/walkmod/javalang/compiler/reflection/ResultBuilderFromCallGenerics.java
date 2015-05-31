package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.type.Type;
import org.walkmod.javalang.compiler.Builder;
import org.walkmod.javalang.compiler.symbols.ASTSymbolTypeResolver;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class ResultBuilderFromCallGenerics implements
		Builder<Map<String, SymbolType>> {

	private List<Type> generics = null;
	private Method method = null;
	private SymbolType scope = null;

	public ResultBuilderFromCallGenerics(List<Type> generics, Method method) {
		this.generics = generics;
		this.method = method;
	}

	public ResultBuilderFromCallGenerics(SymbolType scope) {
		this.scope = scope;
	}

	@Override
	public Map<String, SymbolType> build(Map<String, SymbolType> typeMapping)
			throws Exception {
		if (generics != null) {
			SymbolType[] syms = ASTSymbolTypeResolver.getInstance().valueOf(
					generics);
			SymbolType scope = new SymbolType();
			scope.setParameterizedTypes(Arrays.asList(syms));
			updateTypeMapping(method.getGenericReturnType(), typeMapping, scope);
		} else if (scope != null) {
			List<SymbolType> paramTypes = scope.getParameterizedTypes();
			if (paramTypes != null) {
				updateTypeMapping(scope.getClazz(), typeMapping, scope);
			}
		}

		return typeMapping;
	}

	private void updateTypeMapping(java.lang.reflect.Type type,
			Map<String, SymbolType> typeMapping, SymbolType parameterizedType) {
		if (parameterizedType != null) {
			if (type instanceof TypeVariable) {
				TypeVariable<?> tv = (TypeVariable<?>) type;
				String vname = tv.getName();

				if (!typeMapping.containsKey(vname)
						|| typeMapping.get(vname).getClazz()
								.equals(Object.class)) {

					typeMapping.put(vname, parameterizedType);

				}

				java.lang.reflect.Type[] bounds = tv.getBounds();
				List<SymbolType> paramBounds = parameterizedType.getBounds();
				if (paramBounds != null) {
					for (int i = 0; i < bounds.length; i++) {
						updateTypeMapping(bounds[i], typeMapping,
								paramBounds.get(i));
					}
				}

			} else if (type instanceof WildcardType) {
				WildcardType wildcard = (WildcardType) type;
				java.lang.reflect.Type[] bounds = wildcard.getUpperBounds();
				List<SymbolType> paramBounds = parameterizedType.getBounds();
				if (paramBounds != null) {
					for (int i = 0; i < bounds.length; i++) {
						updateTypeMapping(bounds[i], typeMapping,
								paramBounds.get(i));
					}
				}
				bounds = wildcard.getLowerBounds();
				paramBounds = parameterizedType.getLowerBounds();
				if (paramBounds != null) {
					for (int i = 0; i < bounds.length; i++) {
						updateTypeMapping(bounds[i], typeMapping,
								paramBounds.get(i));
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
						updateTypeMapping(typeArgs[i], typeMapping, st);
					}
				}

			} else if (type instanceof GenericArrayType) {
				GenericArrayType arrayType = (GenericArrayType) type;
				SymbolType st = parameterizedType.clone();
				st.setArrayCount(parameterizedType.getArrayCount() - 1);

				updateTypeMapping(arrayType.getGenericComponentType(),
						typeMapping, st);

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
						updateTypeMapping(tparams[i], typeMapping, st);
					}
				}
			}
		}
	}
}
