package com.ganwork.service;

import com.ganwork.config.ModelConfig;
import com.ganwork.exception.ApiCallException;
import com.ganwork.exception.ModelType;
import com.ganwork.util.FileStorageUtil;
import com.ganwork.util.JsonPathExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

@Service
public class ApiProcessingService implements ImageProcessingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ApiProcessingService.class);

    private final RestTemplate restTemplate;
    private final ModelConfig modelConfig;
    private final FileStorageUtil fileStorageUtil; // 添加FileStorageUtil依赖

    @Autowired
    public ApiProcessingService(RestTemplate restTemplate, ModelConfig modelConfig, FileStorageUtil fileStorageUtil) {
        this.restTemplate = restTemplate;
        this.modelConfig = modelConfig;
        this.fileStorageUtil = fileStorageUtil; // 初始化FileStorageUtil
    }

    // 添加重载方法，支持返回本地路径
    public String process(
            String inputFilename,
            ModelType.InternalModel internalModel,
            String imageType,
            int scale,
            boolean returnLocalPath // 新增参数，控制返回类型
    ) {
        String modelId = internalModel.getModelId();
        logger
                .info("Processing with internal model: {}", modelId);

        // 获取模型配置 - 使用内部模型的 modelId
        ModelConfig.ModelProperties properties =
                modelConfig
                        .getModelProperties(modelId);

        if (properties == null) {
            logger.warn("Using default config for model: {}", modelId);
            properties= new ModelConfig.ModelProperties();
            properties.setApiEndpoint("http://localhost:8000/process"); // 默认地址
        }

        // 准备API请求
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createRequestEntity(
                inputFilename,
                internalModel,
                properties,
                imageType,
                scale
        );

        // 发送请求到Python服务
        ResponseEntity<Map> response = restTemplate.exchange(
                properties.getApiEndpoint(),
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        // 处理响应，根据参数决定返回类型
        return processApiResponse(response.getBody(), properties, returnLocalPath);
    }

    // 保持原有方法兼容性
    public String process(
            String inputFilename,
            ModelType.InternalModel internalModel,
            String imageType,
            int scale
    ) {
        return process(inputFilename, internalModel, imageType, scale, false);
    }
    private HttpEntity<MultiValueMap<String, Object>> createRequestEntity(
            String inputFilename,
            ModelType.InternalModel internalModel,
            ModelConfig.ModelProperties properties,
            String imageType,
            int scale
    ) {
        // 1. 准备文件资源
        File inputFile = new File(inputFilename);
        if (!inputFile.exists()) {
            throw new ApiCallException("输入文件不存在: " + inputFilename);
        }
        FileSystemResource fileResource = new FileSystemResource(inputFile);

        // 2. 构建请求体 - 只发送Python端需要的参数
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        // 只发送Python端需要的参数
        body.add("model_name", internalModel.getModelId()); // 使用model_name而不是model_type

        // 添加详细的日志
        logger.info("Sending parameters: model_name={}", internalModel.getModelId());

        // 3. 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(body, headers);
    }

    //  processApiResponse 方法，添加 returnLocalPath 参数

    private String processApiResponse(Map<String, Object> responseBody,
                                      ModelConfig.ModelProperties properties,
                                      boolean returnLocalPath) {
        if (responseBody == null) {
            throw new ApiCallException("API返回空响应体");
        }

        // 添加响应日志
        logger.info("Python API response: {}", responseBody);

        // 检查状态
        String status = extractResponseValue(responseBody, properties.getStatusPath(), "status");
        if (!"success".equalsIgnoreCase(status)) {
            String errorMsg = extractResponseValue(responseBody, "error.message", "错误消息");
            throw new ApiCallException("API返回失败状态: " + status +
                    (errorMsg != null ? ", 原因: " + errorMsg : ""));
        }

        // 尝试提取结果路径
        String resultPath = extractResponseValue(responseBody, properties.getResultPath(), "result_url");
        if (resultPath == null || resultPath.isBlank()) {
            // 如果result_url不存在，尝试processed_path
            resultPath = extractResponseValue(responseBody, "processed_path", "processed_path");
        }

        if (resultPath == null || resultPath.isBlank()) {
            throw new ApiCallException("API响应中未找到结果路径");
        }



        // 从Python服务下载文件到本地
        String localFilePath = downloadFromPythonService(resultPath);

        // 如果指定返回本地路径，直接返回处理后的文件路径
        if (returnLocalPath) {
            return resultPath; // 直接返回本地路径
        }
        // 返回前端可访问的URL
        return "/processed/" + new File(localFilePath).getName();
    }

    // 添加从Python服务下载文件的方法
    private String downloadFromPythonService(String pythonFileUrl) {
        try {
            // 提取文件名
            String filename = pythonFileUrl.substring(pythonFileUrl.lastIndexOf('/') + 1);

            // 移除可能的时间戳前缀（13位数字后跟下划线）
            if (filename.matches("^\\d{13}_.*")) {
                filename
                        = filename.substring(14); // 移除13位数字和1个下划线
                logger.info("移除时间戳后的文件名: {}", filename);
            }

            // 构建完整的Python服务URL
            String fullUrl = pythonFileUrl.startsWith("http") ?
                    pythonFileUrl
                    : "http://localhost:8000" + pythonFileUrl;

            // 下载文件
            byte[] fileContent = restTemplate.getForObject(fullUrl, byte[].class);

            if (fileContent == null || fileContent.length == 0) {
                throw new ApiCallException("从Python服务下载文件失败: 文件内容为空");
            }

            // 保存到本地
            String localPath = fileStorageUtil.storeProcessedFile(filename, fileContent);

            return localPath;
        } catch (Exception e) {
            throw new ApiCallException("从Python服务下载文件失败: " + e.getMessage(), e);
        }
    }

    // 添加辅助方法，用于从URL提取本地路径
    public String extractLocalPathFromUrl(String url) {
        if (url == null || !url.startsWith("http")) {
            return url; // 已经是本地路径或null
        }

        // 从URL中提取文件名部分
        String filename = url.substring(url.lastIndexOf('/') + 1);

        // 根据您的文件存储结构构建本地路径
        // 这里假设文件存储在"download/"目录下
        return "processed/" + filename;
    }


    private String extractResponseValue(Map<String, Object> responseBody, String path, String fieldName) {
        try {
            Object value = JsonPathExtractor.extract(responseBody, path);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.warn("解析响应字段失败: {} - {}", fieldName, e.getMessage());
            return null;
        }
    }

    private void handleApiException(Exception ex, String apiUrl) {
        if (ex instanceof HttpClientErrorException e) {
            // 4xx 客户端错误
            String responseBody = e.getResponseBodyAsString();
            logger.error("API客户端错误 [{}]: {}", apiUrl, responseBody);
            throw new ApiCallException("API客户端错误: " + e.getStatusCode() +
                    ", 响应: " + responseBody);

        } else if (ex instanceof HttpServerErrorException e) {
            // 5xx 服务端错误
            String responseBody = e.getResponseBodyAsString();
            logger.error("API服务端错误 [{}]: {}", apiUrl, responseBody);
            throw new ApiCallException("API服务端错误: " + e.getStatusCode() +
                    ", 响应: " + responseBody);

        } else if (ex instanceof ResourceAccessException) {
            // 连接超时或网络问题
            logger.error("API连接失败: {}", apiUrl, ex);
            throw new ApiCallException("API服务不可达: " + apiUrl);

        } else {
            // 其他异常
            logger.error("API调用未知错误: {}", apiUrl, ex);
            throw new ApiCallException("API调用失败: " + ex.getMessage());
        }
    }
}