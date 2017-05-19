package org.walkmod.javalang.compiler.types;

public class TypeNotFoundException extends RuntimeException {

    public TypeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
