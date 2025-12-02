package com.ganwork.exception;

public class ApiCallException extends RuntimeException {
    // 添加单参数构造函数
    public ApiCallException(String message) {
        super(message);
    }

    public ApiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}