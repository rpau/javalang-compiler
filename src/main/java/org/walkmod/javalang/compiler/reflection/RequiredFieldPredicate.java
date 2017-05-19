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
package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Field;

import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.compiler.Predicate;

public class RequiredFieldPredicate implements Predicate<Class<?>> {

    private FieldAccessExpr requiredField;

    public RequiredFieldPredicate(FieldAccessExpr field) {
        this.requiredField = field;
    }

    @Override
    public boolean filter(Class<?> candidate) {
        boolean isCompatible = true;
        boolean fieldCompatible = false;

        if (candidate.isArray() && requiredField.getField().equals("length")) {
            return true;
        }
        try {
            // the return type has the required field as
            // public?
            candidate.getField(requiredField.getField());
            fieldCompatible = true;
        } catch (NoSuchFieldException e) {
            // searching in all fields
            Field[] fields = candidate.getDeclaredFields();
            String fieldName = requiredField.getField();
            fieldCompatible = false;

            for (int i = 0; i < fields.length && !fieldCompatible; i++) {
                fieldCompatible = (fields[i].getName().equals(fieldName));
            }
        }

        isCompatible = fieldCompatible;
        // the field has been found. Then, the method is
        // compatible
        return isCompatible;
    }

}
