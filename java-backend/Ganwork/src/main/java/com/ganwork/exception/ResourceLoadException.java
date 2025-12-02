package com.ganwork.exception;

public class ResourceLoadException extends RuntimeException {
    public ResourceLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
