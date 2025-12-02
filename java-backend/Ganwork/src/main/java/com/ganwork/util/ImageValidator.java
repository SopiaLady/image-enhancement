package com.ganwork.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class ImageValidator {

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    public static void validate(MultipartFile file) {
        // 检查文件类型
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Invalid file type: " + file.getContentType());
        }

        // 检查文件大小
        if (file.getSize() > 100 * 1024 * 1024) { // 100MB
            throw new RuntimeException("File too large: " + file.getSize());
        }
    }
}