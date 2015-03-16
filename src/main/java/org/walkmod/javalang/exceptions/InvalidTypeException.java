package org.walkmod.javalang.exceptions;


import java.lang.reflect.Type;

public class InvalidTypeException extends Exception {

	public InvalidTypeException(Type t) {
		super("Invalid Type " + t);
	}

}
