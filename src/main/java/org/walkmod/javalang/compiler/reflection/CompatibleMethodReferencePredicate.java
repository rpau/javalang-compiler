package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.compiler.ArrayFilter;
import org.walkmod.javalang.compiler.Predicate;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeTable;
import org.walkmod.javalang.visitors.VoidVisitor;

public class CompatibleMethodReferencePredicate<A> extends
		CompatibleArgsPredicate implements Predicate<Method> {

	private MethodReferenceExpr expression = null;

	private VoidVisitor<A> typeResolver;

	private A ctx;

	private SymbolType sd;

	private List<Method> methodCallCandidates = null;

	public CompatibleMethodReferencePredicate(MethodReferenceExpr expression,
			VoidVisitor<A> typeResolver, A ctx, Map<String, SymbolType> mapping) {
		this.expression = expression;
		this.typeResolver = typeResolver;
		this.ctx = ctx;
		setTypeMapping(mapping);
	}

	@Override
	public boolean filter(Method elem) throws Exception {

		sd = (SymbolType)expression.getScope().getSymbolData();
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

				FunctionalGenericsBuilder<MethodReferenceExpr> builder = new FunctionalGenericsBuilder<MethodReferenceExpr>(
						md, typeResolver, getTypeMapping());
				builder.build(expression);
				SymbolType[] args = builder.getArgs();
				if (!Modifier.isStatic(md.getModifiers())) {
					// the implicit parameter is an argument of the invisible
					// lambda
					if (md.getParameterCount() == elem.getParameterCount() - 1) {
						SymbolType[] staticArgs = new SymbolType[args.length + 1];
						for (int i = 0; i < args.length; i++) {
							staticArgs[i+1] = args[i];
						}
						staticArgs[0] = (SymbolType) sd;
						args = staticArgs;
						setTypeArgs(args);
						found = super.filter(elem);
					} else {
						
						String typeName = expression.getScope().toString();
						String fullName = TypeTable.getInstance().getTypeTable().get(typeName);
						// it is a variable
						if (fullName == null
								&& md.getParameterCount() == elem
										.getParameterCount()) {
							setTypeArgs(args);
							found = super.filter(elem);
						}
					}

				} else if (md.getParameterCount() == elem.getParameterCount()) {
					setTypeArgs(args);
					found = super.filter(elem);
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
			}
		}
		if (found) {
			SymbolType st = SymbolType.valueOf(elem, getTypeMapping());
			expression.setSymbolData(st);
		}
		return found;
	}
}
