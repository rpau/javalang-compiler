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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class GenericsBuilderFromMethodParameterTypes extends
		AbstractGenericsBuilderFromParameterTypes implements
		TypeMappingBuilder<Method> {

	public GenericsBuilderFromMethodParameterTypes(
			Map<String, SymbolType> typeMapping, List<Expression> args,
			SymbolType[] typeArgs) {
		super(typeMapping, args, typeArgs);
	}

	public GenericsBuilderFromMethodParameterTypes() {
	}

	@Override
	public Method build(Method method) throws Exception {
		setTypes(method.getGenericParameterTypes());
		super.build();
		return method;
	}

}
