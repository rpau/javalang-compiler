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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleMethodReferencePredicate<A, T extends Executable>
		extends CompatibleArgsPredicate<T> implements Predicate<T> {

	private MethodReferenceExpr expression = null;

	private VoidVisitor<A> typeResolver;

	private A ctx;

	private SymbolType sd;

	private List<Method> methodCallCandidates = null;

	private SymbolTable symTable;

	public CompatibleMethodReferencePredicate(MethodReferenceExpr expression,
			VoidVisitor<A> typeResolver, A ctx,
			Map<String, SymbolType> mapping, SymbolTable symTable) {
		this.expression = expression;
		this.typeResolver = typeResolver;
		this.symTable = symTable;
		this.ctx = ctx;
		setTypeMapping(mapping);
	}

	@Override
	public boolean filter(Executable elem) throws Exception {
		Executable equivalentMethod = null;

		sd = (SymbolType) expression.getScope().getSymbolData();
		if (sd == null) {
			expression.getScope().accept(typeResolver, ctx);
			sd = (SymbolType) expression.getScope().getSymbolData();

			if (!expression.getIdentifier().equals("new")) {

				ArrayFilter<Method> filter = new ArrayFilter<Method>(sd
						.getClazz().getMethods());

				filter.appendPredicate(new MethodsByNamePredicate(expression
						.getIdentifier()));
				methodCallCandidates = filter.filter();
			}

		}
		boolean found = false;
		if (!expression.getIdentifier().equals("new")) {
			Iterator<Method> it = methodCallCandidates.iterator();

			while (it.hasNext() && !found) {

				Method md = it.next();
				int mdParameterCount = md.getParameterTypes().length;
				int elemParameterCount = elem.getParameterTypes().length;

				FunctionalGenericsBuilder<MethodReferenceExpr> builder = new FunctionalGenericsBuilder<MethodReferenceExpr>(
						md, typeResolver, getTypeMapping());
				builder.build(expression);
				SymbolType[] args = builder.getArgs();
				if (!Modifier.isStatic(md.getModifiers())) {
					// the implicit parameter is an argument of the invisible
					// lambda

					if (mdParameterCount == elemParameterCount - 1) {
						SymbolType[] staticArgs = new SymbolType[args.length + 1];
						for (int i = 0; i < args.length; i++) {
							staticArgs[i + 1] = args[i];
						}
						staticArgs[0] = (SymbolType) sd;
						args = staticArgs;
						setTypeArgs(args);
						found = super.filter(elem);

					} else {

						Expression scope = expression.getScope();
						
						SymbolType stype = (SymbolType) scope.getSymbolData();
						boolean isField = stype.getField() != null;
						boolean isVariable = false;
						if(!isField){
							String name = scope.toString();
							isVariable = (symTable.findSymbol(name, ReferenceType.VARIABLE) != null);
						}
						// it is a variable
						if ((isField || isVariable)
								&& mdParameterCount == elemParameterCount) {
							setTypeArgs(args);
							found = super.filter(elem);

						}
					}

				} else if (mdParameterCount == elemParameterCount) {
					setTypeArgs(args);
					found = super.filter(elem);
				}
				if (found) {
					equivalentMethod = md;
				}

			}
		} else {
			Constructor<?>[] constructors = sd.getClazz().getConstructors();

			for (int i = 0; i < constructors.length && !found; i++) {

				FunctionalGenericsBuilder<MethodReferenceExpr> builder = new FunctionalGenericsBuilder<MethodReferenceExpr>(
						constructors[i], typeResolver, getTypeMapping());
				builder.build(expression);

				SymbolType[] args = builder.getArgs();
				setTypeArgs(args);

				found = super.filter(elem);
				if (found) {
					equivalentMethod = constructors[i];
				}
			}
		}
		if (found && elem instanceof Method) {

			Map<String, SymbolType> mapping = getTypeMapping();
			SymbolType realResultType = null;
			if (equivalentMethod instanceof Method) {

				// R apply(T t); -> Quote::parse ( T= String y R = Quote)
				realResultType = SymbolType.valueOf((Method) equivalentMethod,
						mapping);

				java.lang.reflect.Type genericReturnType = ((Method) elem)
						.getGenericReturnType();
				resolveTypeMapping(genericReturnType, realResultType, mapping);
			}
			realResultType = SymbolType.valueOf(elem.getDeclaringClass(),
					mapping);
			expression.setSymbolData(realResultType);
			
			SymbolType st = SymbolType.valueOf((Method) elem, mapping);
			expression.setReferencedMethodSymbolData(st);
			expression.setReferencedArgsSymbolData(getTypeArgs());
		}
		return found;
	}

	private void resolveTypeMapping(java.lang.reflect.Type type,
			SymbolType reference, Map<String, SymbolType> mapping) {

		if (type instanceof TypeVariable) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
			SymbolType st = mapping.get(tv.getName());
			if (st == null || "java.lang.Object".equals(st.getName())) {
				mapping.put(tv.getName(), reference);
			} else {
				mapping.put(tv.getName(), (SymbolType) reference.merge(st));
			}
		}
	}
}
