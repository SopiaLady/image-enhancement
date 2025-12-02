package com.ganwork.service;

import com.ganwork.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<?>> handleFileUploadError(MultipartException ex) {
        logger.error("文件上传错误", ex);
        return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "文件上传失败: " + ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidArgument(IllegalArgumentException ex) {
        logger.error("无效参数", ex);
        return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralError(Exception ex) {
        logger.error("处理请求时出错", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "服务器内部错误", null));
    }
}
