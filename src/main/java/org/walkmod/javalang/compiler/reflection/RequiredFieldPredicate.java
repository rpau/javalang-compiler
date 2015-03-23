package org.walkmod.javalang.compiler.reflection;

import java.lang.reflect.Field;

import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.compiler.Predicate;

public class RequiredFieldPredicate implements Predicate<Class<?>> {

	private FieldAccessExpr requiredField;

	public RequiredFieldPredicate(
			FieldAccessExpr field) {
		this.requiredField = field;
	}

	@Override
	public boolean filter(Class<?> candidate) {
		boolean isCompatible = true;
		boolean fieldCompatible = false;

			if (candidate.isArray()
					&& requiredField.getField().equals("length")) {
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
