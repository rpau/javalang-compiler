package org.walkmod.javalang.exceptions;

public class NoSuchExpressionTypeException extends RuntimeException{

	public NoSuchExpressionTypeException(Exception e){
		super(e);
	}
	
	public NoSuchExpressionTypeException(String msg, Exception e){
		super(msg, e);
	}
}
