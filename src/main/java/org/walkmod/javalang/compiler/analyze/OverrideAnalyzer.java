package org.walkmod.javalang.compiler.analyze;

import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.ModifierSet;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.compiler.reflection.ClassInspector;
import org.walkmod.javalang.compiler.reflection.MethodInspector;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class OverrideAnalyzer {
    /** @return true if method declaration overrides or implements another method */
    public static boolean isMethodOverride(MethodDeclaration md) {
        return collectOverriddenMethods(md, null);
    }

    /** @return the methods overridden by given method declaration */
    public static List<Method> findOverriddenMethods(MethodDeclaration md) {
        final List<Method> methods = new ArrayList<Method>();
        collectOverriddenMethods(md, methods);
        return methods.isEmpty() ? Collections.<Method>emptyList() : Collections.unmodifiableList(methods);
    }

    /**
    * @param overriddenMethods if != null add all methods to collection otherwise return on first match
    * @return true if method found
    */
    private static boolean collectOverriddenMethods(MethodDeclaration md,
            /* Nullable */ List<Method> overriddenMethods) {
        final MethodSymbolData sdata = md.getSymbolData();

        if (sdata == null || isStatic(sdata.getMethod()) || isPrivate(sdata.getMethod())) {
            return false;
        } else {
            boolean result = false;

            final Class<?> declaringClass = sdata.getMethod().getDeclaringClass();
            final Class<?> parentClass = declaringClass.getSuperclass();
            final Class<?>[] interfaces = declaringClass.getInterfaces();

            if (parentClass != null || interfaces.length > 0) {
                final SymbolData[] args = getParameterSymbolData(md);

                List<Class<?>> scopesToCheck = new LinkedList<Class<?>>();
                if (parentClass != null) {
                    scopesToCheck.add(parentClass);
                }
                for (int i = 0; i < interfaces.length; i++) {
                    scopesToCheck.add(interfaces[i]);
                }
                final Iterator<Class<?>> it = scopesToCheck.iterator();
                while (it.hasNext() && !result) {
                    final Class<?> clazzToAnalyze = it.next();

                    final Method foundMethod = MethodInspector.findMethod(clazzToAnalyze, args, md.getName());
                    if (foundMethod != null) {
                        if (foundMethod.getDeclaringClass().isAssignableFrom(clazzToAnalyze)) {
                            List<Type> types = ClassInspector.getInterfaceOrSuperclassImplementations(declaringClass,
                                    clazzToAnalyze);
                            Class<?> returnType = null;
                            if (types != null && !types.isEmpty()) {
                                if (types.get(0) instanceof Class) {
                                    returnType = (Class<?>) types.get(0);
                                }
                            }

                            if (matchesReturnAndParameters(foundMethod, returnType, args)) {
                                if (overriddenMethods != null) {
                                    overriddenMethods.add(foundMethod);
                                } else {
                                    result = true;
                                }
                            }
                        }
                    }
                }
            }
            return overriddenMethods != null ? !overriddenMethods.isEmpty() : result;
        }
    }

    private static SymbolData[] getParameterSymbolData(MethodDeclaration md) {
        // it should be initialized after resolving the method
        List<Parameter> params = md.getParameters();
        SymbolData[] args;
        if (params != null) {
            args = new SymbolData[params.size()];
            int i = 0;
            for (Parameter param : params) {
                args[i] = param.getType().getSymbolData();
                i++;
            }
        } else {
            args = new SymbolData[0];
        }
        return args;
    }

    private static boolean matchesReturnAndParameters(Method m, /* @Nullable */ Class<?> returnType,
            SymbolData[] args) {
        Type[] parameterTypes = m.getGenericParameterTypes();
        int modifiers = m.getModifiers();
        boolean valid = ModifierSet.isPublic(modifiers) || ModifierSet.isProtected(modifiers);
        for (int i = 0; i < parameterTypes.length && valid; i++) {
            final Type parameterType = parameterTypes[i];
            final SymbolData arg = args[i];
            if (parameterType instanceof Class) {
                valid = (arg.getClazz().getName().equals(((Class<?>) parameterType).getName()));
            } else if (parameterType instanceof TypeVariable) {
                TypeVariable<?> tv = (TypeVariable<?>) parameterType;
                if (returnType != null) {
                    TypeVariable<?>[] tvs = returnType.getTypeParameters();
                    int pos = -1;
                    for (int k = 0; k < tvs.length && pos == -1; k++) {
                        if (tvs[k].getName().equals(tv.getName())) {
                            pos = k;
                        }
                    }
                    if (pos > -1) {
                        Type[] bounds = tvs[pos].getBounds();
                        for (int k = 0; k < bounds.length && valid; k++) {
                            if (bounds[k] instanceof Class<?>) {
                                valid = arg.getClazz().isAssignableFrom((Class) bounds[k]);
                            }
                        }
                    }
                }
            } else if (parameterType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) parameterType;
                valid = arg.getClazz().isAssignableFrom((Class<?>) pt.getRawType());
            }
        }
        return valid;
    }

    private static boolean isStatic(Method m) {
        return Modifier.isStatic(m.getModifiers());
    }

    private static boolean isPrivate(Method m) {
        return Modifier.isPrivate(m.getModifiers());
    }
}
