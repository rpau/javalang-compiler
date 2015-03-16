package org.walkmod.javalang.compiler.symbols;

import java.util.List;

import org.walkmod.javalang.ast.Node;

public class MethodSymbol extends Symbol {

	private SymbolType scope;

	private SymbolType[] args;

	public MethodSymbol(String name, SymbolType type, Node location) {
		this(name, type, location, null, null, null);
	}

	public MethodSymbol(String name, SymbolType type, Node location,
			List<SymbolAction> actions) {
		this(name, type, location, null, null, actions);
	}

	public MethodSymbol(String name, SymbolType type, Node location,
			SymbolType scope, SymbolType[] args, List<SymbolAction> actions) {
		super(name, type, location, ReferenceType.METHOD, actions);

		this.args = args;
		this.scope = scope;

		if (args == null) {
			args = new SymbolType[0];
		}
	}

	public SymbolType[] getArgs() {
		return args;
	}

	public boolean hasCompatibleSignature(SymbolType scope,
			SymbolType[] otherArgs) {
		boolean sameScope = (scope == null && this.scope == null);
		if (scope != null && this.scope != null) {
			sameScope = this.scope.isCompatible(scope);
		}
		if (sameScope) {
			if (otherArgs == null && args == null) {
				return true;
			}
			if (otherArgs == null && args.length == 0) {
				return true;
			}
			if (args == null && otherArgs.length == 0) {
				return true;
			}
			boolean sameNumberOfArgs = otherArgs.length == args.length;
			if (!sameNumberOfArgs) {
				return false;
			}
			boolean sameArgs = true;
			for (int i = 0; i < args.length && sameArgs; i++) {
				sameArgs = (otherArgs[i] == null /*
												 * the expression to call the
												 * method is a
												 * NullExpressionLiteral
												 */
				|| (args[i] != null && otherArgs[i] != null && args[i]
						.isCompatible(otherArgs[i])));
			}
			return sameArgs;
		}
		return false;
	}

	public SymbolType getScope() {
		return scope;
	}

	@Override
	public boolean equals(Object o) {

		if (o instanceof MethodSymbol) {
			MethodSymbol other = (MethodSymbol) o;
			boolean sameName = getName().equals(((MethodSymbol) o).getName());
			if (!sameName) {
				return false;
			}
			return hasCompatibleSignature(other.getScope(), other.getArgs());
		}
		return false;
	}

}
