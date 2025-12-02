package com.ganwork.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private final Path rootLocation;

    @Autowired
    public FileStorageUtil(@Value("${app.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
        init();
    }

    private void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法初始化存储目录", e);
        }
    }

    public String store(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            Path targetLocation = rootLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation,
                    StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("存储文件失败: " + filename, e);
        }
    }

    public Path getPath(String filename) {
        return rootLocation.resolve(filename);
    }

    public Resource loadAsResource(String filename) {
        try {
            Path filePath = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("无法读取文件: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("无法读取文件: " + filename, e);
        }
    }


    // 添加存储处理后的文件的方法
    public String storeProcessedFile(String filename, byte[] fileContent) {
        try {
            // 创建处理后的文件目录
            Path processedDir = Paths.get("processed");
            Files.createDirectories(processedDir);

            // 生成唯一的文件名（避免冲突）
            //String uniqueFilename = System.currentTimeMillis() + "_" + filename;
            Path filePath = processedDir.resolve(filename);

            // 保存文件
            Files.write(filePath, fileContent);

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("保存处理后的文件失败: " + e.getMessage(), e);
        }
    }

    // 添加获取处理后的文件资源的方法
    public Resource loadProcessedFileAsResource(String filename) {
        try {
            Path processedDir = Paths.get("processed");
            Path filePath = processedDir.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("无法读取处理后的文件: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("无法读取处理后的文件: " + filename, e);
        }
    }


}