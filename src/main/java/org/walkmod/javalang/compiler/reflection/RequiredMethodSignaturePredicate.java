package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.CollectionFilter;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class RequiredMethodSignaturePredicate implements TypeMappingPredicate<Method> {

	private CollectionFilter<Class<?>> resultTypeFilters;

	private GenericsBuilderFromArgs b2 = new GenericsBuilderFromArgs();

	private List<Expression> argumentValues;

	private Map<String, SymbolType> typeMapping;


	public RequiredMethodSignaturePredicate() {
	}

	public RequiredMethodSignaturePredicate(
			CollectionFilter<Class<?>> resultTypeFilters,
			List<Expression> argumentValues,
			Map<String, SymbolType> typeMapping) {
		this.argumentValues = argumentValues;
		this.typeMapping = typeMapping;
		this.resultTypeFilters = resultTypeFilters;
	}
	
	public void setResultTypeFilters(
			CollectionFilter<Class<?>> resultTypeFilters) {
		this.resultTypeFilters = resultTypeFilters;
	}

	public void setArgumentValues(List<Expression> argumentValues) {
		this.argumentValues = argumentValues;
	}

	@Override
	public void setTypeMapping(Map<String, SymbolType> typeMapping) {
		this.typeMapping = typeMapping;
	}

		

	@Override
	public boolean filter(Method method) throws Exception {

		if (resultTypeFilters != null) {
			b2.setMethod(method);
			b2.setArgumentValues(argumentValues);
			
			typeMapping = b2
					.build(new HashMap<String, SymbolType>(typeMapping));
			SymbolType result = SymbolType.valueOf(method, typeMapping);
			List<Class<?>> classes = result.getBoundClasses();
			resultTypeFilters.setElements(classes);
			return resultTypeFilters.filterOne() != null;

		}

		return false;
	}


}
