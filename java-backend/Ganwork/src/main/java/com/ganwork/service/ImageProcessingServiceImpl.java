package com.ganwork.service;

import com.ganwork.exception.ModelType;
import com.ganwork.exception.ResourceLoadException;
import com.ganwork.exception.ResourceNotFoundException;
import com.ganwork.util.FileStorageUtil;
import com.ganwork.util.ImageValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private final FileStorageUtil fileStorageUtil;
    private final ApiProcessingService apiProcessingService;
    private final ProcessingMonitor processingMonitor;

    @Autowired
    public ImageProcessingServiceImpl(
            FileStorageUtil fileStorageUtil,
            ApiProcessingService apiProcessingService,
            ProcessingMonitor processingMonitor) {
        this.fileStorageUtil = fileStorageUtil;
        this.apiProcessingService = apiProcessingService;
        this.processingMonitor = processingMonitor;
    }


    @Override
    public String processImage(MultipartFile file, String mode, String imageType, int scale) {
        // 验证文件
        ImageValidator.validate(file);

        // 使用正确的转换方法
        ModelType modelType = ModelType.fromRequestParam(mode); // 使用 fromRequestParam

        // 存储原始文件
        String originalFilename = fileStorageUtil.store(file);
        Path inputPath = fileStorageUtil.getPath(originalFilename);

        // 生成任务ID
        String taskId = "task_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        processingMonitor.startProcess(taskId, modelType.getDisplayName());

        try {
            // 传递所有参数
            String resultUrl = processImageInternal(
                    inputPath.toString(),
                    modelType,
                    imageType,
                    scale,
                    taskId
            );

            processingMonitor.completeProcess(taskId, resultUrl);
            return resultUrl;
        } catch (Exception e) {
            processingMonitor.failProcess(taskId, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<String> processImages(MultipartFile[] files, String mode, String imageType, int scale) {
        List<String> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(processImage(file, mode, imageType, scale));
        }
        return results;
    }


    private String processImageInternal(
            String inputPath,
            ModelType modelType,
            String imageType,
            int scale,
            String taskId
    ) {
        List<ModelType.InternalModel> pipeline = modelType.getProcessingPipeline();
        String currentInput = inputPath;
        String result = null;

        int totalSteps = pipeline.size();
        for (int i = 0; i < totalSteps; i++) {
            ModelType.InternalModel model = pipeline.get(i);
            String stepName = ModelType.getInternalModelDisplayName(model);
            processingMonitor
                    .updateProcessStatus(taskId, "正在处理: " + stepName);

            // 检查当前输入是否为URL，如果是则下载到本地
            if (currentInput.startsWith("http") || currentInput.startsWith("/processed/")) {
                try {
                    // 下载文件到临时位置
                    String downloadedPath = downloadFileFromUrl(currentInput);
                    currentInput= downloadedPath;
                } catch (Exception e) {
                    //throw new RuntimeException("下载文件失败: " + currentInput, e);
                }
            }

            // 调用处理方法
            boolean isLastStep = (i == totalSteps - 1);
            result= apiProcessingService.process(
                    currentInput ,
                    model,
                    imageType,
                    scale,
                    isLastStep
// 修改这里：最后一步返回本地路径
            );
            currentInput= result;
        }

        // 确保返回的是前端可访问的URL
        if (result != null && !result.startsWith("/processed/")) {
            // 如果结果是本地路径，转换为前端可访问的URL
            File resultFile = new File(result);
            if (resultFile.exists()) {
                return "/processed/" + resultFile.getName();
            }
        }

        return result;
    }

    // 添加下载文件的方法
    private String downloadFileFromUrl(String fileUrl) throws IOException {

        // 如果已经是本地路径，直接返回
        if (!fileUrl.startsWith("http") && !fileUrl.startsWith("/processed/")) {
            return fileUrl;
        }

        // 提取文件名
        String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);


        if (fileUrl.startsWith("/processed/")) {
            filename= fileUrl.substring("/processed/".length());
        } else {
            filename= fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        }

        // 移除可能的时间戳前缀（13位数字后跟下划线）
        if (filename.matches("^\\d{13}_.*")) {
            filename= filename.substring(14); // 移除13位数字和1个下划线
            //logger.info("移除时间戳后的文件名: {}", filename);
        }
        // 创建临时文件路径
        String tempDir = "download/";
        Files.createDirectories(Paths.get(tempDir));
        String localPath = tempDir + filename;

        // 构建完整的URL（如果是相对路径）
        String fullUrl = fileUrl.startsWith("http") ?
                fileUrl : "http://localhost:8000" + fileUrl;

        // 下载文件
        RestTemplate restTemplate = new RestTemplate();
        byte[] fileContent = restTemplate.getForObject(fullUrl, byte[].class);

        // 保存到本地
        Files.write(Paths.get(localPath), fileContent);

        return localPath;
    }


    @Override
    public Resource loadAsResource(String filename) {
        try {
            // 使用 FileStorageUtil 加载资源
            Resource resource = fileStorageUtil.loadAsResource(filename);

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("无法读取文件: " + filename);
            }
        } catch (Exception e) {
            throw new ResourceLoadException("加载资源失败: " + filename, e);
        }
    }


}