package com.ganwork.controller;

import com.ganwork.exception.ModelType;
import com.ganwork.service.ApiProcessingService;
import com.ganwork.service.ImageProcessingService;
import com.ganwork.util.ImageProcessingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ApiProcessingService.class);
    private final ImageProcessingService imageProcessingService;

    @Autowired
    public ImageController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processImages( // 修改返回类型
     @RequestParam("file") MultipartFile file,
     @RequestParam("mode") String mode,
     @RequestParam(value = "imageType", required = false) String imageType,
     @RequestParam(value = "scale", defaultValue = "1") int scale) {
     logger.info("Received process request with parameters:");
     logger.info("Mode: {}, ImageType: {}, Scale: {}", mode, imageType, scale);
     logger.info("File: {} ({} bytes, {})",
     file.getOriginalFilename(),
     file.getSize(),
     file.getContentType());
        try {
            // 打印所有可用的ModelType
            logger.info("Available model types:");
            for (ModelType type : ModelType.values()) {
                logger.info(" - {} (param: {})", type.getDisplayName(), type.getRequestParam());
            }

            // 尝试转换模式
            ModelType modelType = ModelType.fromRequestParam(mode);
            logger.info("Successfully converted mode to: {}", modelType.getDisplayName());

            // 处理图片
            String resultUrl = imageProcessingService.processImage(file, mode, imageType, scale);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "处理成功");

            List<Map<String, String>> results = new ArrayList<>();
            Map<String, String> result = new HashMap<>();
            result.put("processedUrl", resultUrl);
            results.add(result);

            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Processing failed", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("results", new ArrayList<>());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}