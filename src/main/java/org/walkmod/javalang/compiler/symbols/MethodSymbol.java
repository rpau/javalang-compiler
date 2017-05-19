/*
 * Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 * 
 * Walkmod is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Walkmod. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.walkmod.javalang.compiler.symbols;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.walkmod.javalang.ast.Node;

public class MethodSymbol extends Symbol {

    private SymbolType scope;

    private SymbolType[] args;

    private boolean hasDynamicArgs = false;

    private Method referencedMethod = null;

    private Constructor<?> referencedConstructor = null;

    public MethodSymbol(String name, SymbolType type, Node location, boolean staticallyImported, boolean hasDynamicArgs,
            Method referencedMethod) {
        this(name, type, location, null, null, staticallyImported, hasDynamicArgs, referencedMethod, null);
    }

    public MethodSymbol(String name, SymbolType type, Node location, boolean staticallyImported, boolean hasDynamicArgs,
            Constructor<?> referencedConstructor) {
        this(name, type, location, null, null, staticallyImported, hasDynamicArgs, referencedConstructor, null);
    }

    public MethodSymbol(String name, SymbolType type, Node location, boolean staticallyImported, boolean hasDynamicArgs,
            List<SymbolAction> actions, Method referencedMethod) {
        this(name, type, location, null, null, staticallyImported, hasDynamicArgs, referencedMethod, actions);
    }

    public MethodSymbol(String name, SymbolType type, Node location, boolean staticallyImported, boolean hasDynamicArgs,
            List<SymbolAction> actions, Constructor<?> referencedConstructor) {
        this(name, type, location, null, null, staticallyImported, hasDynamicArgs, referencedConstructor, actions);
    }

    public MethodSymbol(String name, SymbolType type, Node location, SymbolType scope, SymbolType[] args,
            boolean staticallyImported, boolean hasDynamicArgs, Constructor<?> referencedConstructor,
            List<SymbolAction> actions) {
        super(name, type, location, ReferenceType.METHOD, staticallyImported, actions);

        this.args = args;
        this.scope = scope;

        this.hasDynamicArgs = hasDynamicArgs;
        this.referencedConstructor = referencedConstructor;
        if (args == null) {
            args = new SymbolType[0];
        }
    }

    public MethodSymbol(String name, SymbolType type, Node location, SymbolType scope, SymbolType[] args,
            boolean staticallyImported, boolean hasDynamicArgs, Method referencedMethod, List<SymbolAction> actions) {
        super(name, type, location, ReferenceType.METHOD, staticallyImported, actions);

        this.args = args;
        this.scope = scope;
        this.hasDynamicArgs = hasDynamicArgs;
        this.referencedMethod = referencedMethod;
        if (args == null) {
            args = new SymbolType[0];
        }
    }

    public void setReferencedMethod(Method method) {
        this.referencedMethod = method;
    }

    public void setReferencedConstructor(Constructor referencedConstructor) {
        this.referencedConstructor = referencedConstructor;
    }

    public MethodSymbol buildTypeParameters(Map<String, SymbolType> typeParams) {
        MethodSymbol result = null;
        if (referencedMethod != null && typeParams != null) {
            result = new MethodSymbol(getName(), getType(), getLocation(), isStaticallyImported(), isVarArgs(),
                    getReferencedMethod());
            try {
                SymbolType aux = SymbolType.valueOf(getReferencedMethod(), typeParams);
                result.setType(aux);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        } else {
            return this;
        }

    }

    public SymbolType[] getArgs() {
        return args;
    }

    public boolean hasCompatibleSignature(SymbolType scope, SymbolType[] otherArgs) {
        boolean sameScope = (scope == null || this.scope == null) || staticallyImported;
        if (scope != null && this.scope != null && !staticallyImported) {
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
            int otherLenght = 0;
            int argsLenght = 0;
            if (otherArgs != null) {
                otherLenght = otherArgs.length;
            }
            if (args != null) {
                argsLenght = args.length;
            }

            boolean sameNumberOfArgs = otherLenght == argsLenght;
            if (!hasDynamicArgs) {
                if (!sameNumberOfArgs) {
                    return false;
                }
                boolean sameArgs = true;
                for (int i = 0; i < args.length && sameArgs; i++) {
                    sameArgs = (otherArgs[i] == null
                            || (args[i] != null && otherArgs[i] != null && args[i].isCompatible(otherArgs[i])));
                }
                return sameArgs;
            } else {
                if (otherLenght < argsLenght - 1) {
                    return false;
                }
                boolean sameArgs = true;
                for (int i = 0; i < args.length - 1 && sameArgs; i++) {
                    sameArgs = (otherArgs[i] == null
                            || (args[i] != null && otherArgs[i] != null && args[i].isCompatible(otherArgs[i])));
                }
                if (otherLenght == argsLenght - 1) {
                    return sameArgs;
                }
                SymbolType last = args[args.length - 1];
                if (last != null) {
                    SymbolType aux = last.clone();
                    aux.setArrayCount(0);
                    for (int i = args.length - 1; i < otherLenght && sameArgs; i++) {
                        sameArgs = (otherArgs[i] == null
                                || (aux.isCompatible(otherArgs[i]) || last.isCompatible(otherArgs[i])));
                    }
                }
                return sameArgs;

            }
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

    public boolean isVarArgs() {
        return hasDynamicArgs;
    }

    public Method getReferencedMethod() {
        return referencedMethod;
    }

    public Constructor<?> getReferencedConstructor() {
        return referencedConstructor;
    }

    public boolean isConstructor() {
        return referencedConstructor != null;
    }

}
