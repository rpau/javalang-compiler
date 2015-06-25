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

import org.walkmod.javalang.compiler.symbols.SymbolType;

public class CompatibleArgsPredicate<T extends Executable> extends AbstractCompatibleArgsPredicate
		implements TypeMappingPredicate<T> {

	public CompatibleArgsPredicate() {
	}

	public CompatibleArgsPredicate(SymbolType[] typeArgs) {
		super(typeArgs);
	}

	@Override
	public boolean filter(Executable method) throws Exception {
		setVarAgs(method.isVarArgs());
		setGenericParameterTypes(method.getGenericParameterTypes());
		setParameterTypesLenght(method.getParameterTypes().length);
		return super.filter();
	}

}
