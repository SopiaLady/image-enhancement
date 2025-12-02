package com.ganwork.service;

import com.ganwork.exception.ResourceLoadException;
import com.ganwork.exception.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageProcessingService {
    String processImage(MultipartFile file, String mode, String imageType, int scale);
    List<String> processImages(MultipartFile[] files, String mode, String imageType, int scale);
    Resource loadAsResource(String filename);
}