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
package org.walkmod.javalang.compiler.actions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.PackageDeclaration;
import org.walkmod.javalang.compiler.symbols.MethodSymbol;
import org.walkmod.javalang.compiler.symbols.ReferenceType;
import org.walkmod.javalang.compiler.symbols.Symbol;
import org.walkmod.javalang.compiler.symbols.SymbolAction;
import org.walkmod.javalang.compiler.symbols.SymbolTable;
import org.walkmod.javalang.compiler.symbols.SymbolType;
import org.walkmod.javalang.compiler.types.TypeNotFoundException;
import org.walkmod.javalang.compiler.types.TypesLoaderVisitor;

public class LoadStaticImportsAction extends SymbolAction {

    private String pkgName = null;

    @Override
    public void doPush(Symbol<?> symbol, SymbolTable table) throws Exception {
        Node n = symbol.getLocation();
        if (n instanceof ImportDeclaration) {
            ImportDeclaration id = (ImportDeclaration) n;
            if (id.isStatic()) {
                Class<?> clazz = null;
                try {
                    clazz = symbol.getType().getClazz();
                } catch (TypeNotFoundException e) {
                    String nameExpr = id.getName().toString();
                    int index = nameExpr.lastIndexOf(".");

                    while (index != -1 && clazz == null) {
                        nameExpr = nameExpr.substring(0, index) + "$" + nameExpr.substring(index + 1);
                        try {
                            clazz = TypesLoaderVisitor.getClassLoader().loadClass(nameExpr);
                        } catch (ClassNotFoundException e2) {

                        }
                        index = nameExpr.lastIndexOf(".");

                    }
                    if (clazz == null) {
                        throw e;
                    } else {
                        final SymbolType type = symbol.getType().withName(nameExpr);
                        symbol.setType(type);
                        type.getClazz();
                    }
                }

                Package pkg = clazz.getPackage();
                String importPkgName = "";
                if (pkg != null) {
                    importPkgName = pkg.getName();
                }

                if (pkgName == null) {
                    CompilationUnit cu = (CompilationUnit) id.getParentNode();
                    PackageDeclaration pkgDeclaration = cu.getPackage();
                    if (pkgDeclaration != null) {
                        pkgName = pkgDeclaration.getName().toString();
                    } else {
                        pkgName = "";
                    }
                }

                Class<?>[] declaredClasses = clazz.getDeclaredClasses();

                for (int i = 0; i < declaredClasses.length; i++) {
                    if (!id.isAsterisk() && id.getName().getName().equals(declaredClasses[i].getSimpleName())) {
                        int modifiers = declaredClasses[i].getModifiers();

                        if (Modifier.isStatic(modifiers)) {
                            boolean isVisible = Modifier.isPublic(modifiers)
                                    || (!Modifier.isPrivate(modifiers) && importPkgName.equals(pkgName));
                            if (isVisible) {

                                SymbolType st = new SymbolType(declaredClasses[i]);
                                table.pushSymbol(declaredClasses[i].getSimpleName(), ReferenceType.TYPE, st, n);
                            }
                        }
                    }
                }

                Class<?>[] interfaces = clazz.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    Class<?>[] nestedClasses = interfaces[i].getDeclaredClasses();
                    for (int j = 0; j < nestedClasses.length; j++) {
                        if (id.isAsterisk() || id.getName().getName().equals(nestedClasses[j].getSimpleName())) {
                            int modifiers = nestedClasses[j].getModifiers();
                            if (Modifier.isStatic(modifiers)) {
                                boolean isVisible = Modifier.isPublic(modifiers)
                                        || (!Modifier.isPrivate(modifiers) && importPkgName.equals(pkgName));
                                if (isVisible) {

                                    SymbolType st = new SymbolType(nestedClasses[j]);
                                    table.pushSymbol(nestedClasses[j].getSimpleName(), ReferenceType.TYPE, st, n);
                                }
                            }
                        }
                    }
                }

                Method[] methods = clazz.getDeclaredMethods();

                for (Method m : methods) {
                    if (id.isAsterisk() || id.getName().getName().equals(m.getName())) {
                        int modifiers = m.getModifiers();
                        if (Modifier.isStatic(modifiers)) {

                            boolean isVisible = Modifier.isPublic(modifiers)
                                    || (!Modifier.isPrivate(modifiers) && importPkgName.equals(pkgName));

                            if (isVisible) {
                                Class<?>[] params = m.getParameterTypes();
                                SymbolType[] args = null;
                                if (params.length > 0) {
                                    args = new SymbolType[params.length];
                                    int i = 0;
                                    for (Class<?> param : params) {
                                        args[i] = SymbolType.valueOf(param, null);
                                        i++;
                                    }
                                }

                                SymbolType st = SymbolType.valueOf(m, null);
                                MethodSymbol method = new MethodSymbol(m.getName(), st, n, symbol.getType(), args, true,
                                        m.isVarArgs(), m, (List<SymbolAction>) null);
                                table.pushSymbol(method);
                            }
                        }
                    }
                }
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (id.isAsterisk() || id.getName().getName().equals(field.getName())) {

                        int modifiers = field.getModifiers();
                        if (Modifier.isStatic(modifiers)) {

                            boolean isVisible = Modifier.isPublic(modifiers)
                                    || (!Modifier.isPrivate(modifiers) && importPkgName.equals(pkgName));
                            if (isVisible) {

                                SymbolType st = SymbolType.valueOf(field.getGenericType(),
                                        Collections.<String, SymbolType>emptyMap());
                                Symbol<?> s = new Symbol(field.getName(), st, n, ReferenceType.VARIABLE, true, null);
                                table.pushSymbol(s);
                            }
                        }
                    }
                }
            }
        }

    }
}
