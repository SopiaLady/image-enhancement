package com.ganwork.config;

import com.ganwork.exception.ModelType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "cloud.models")
public class ModelConfig {

    private final Map<ModelType, ModelProperties> modelMap = new EnumMap<>(ModelType.class);

    private static final Logger logger = LoggerFactory.getLogger(ModelConfig.class);


//    @PostConstruct
//    public void init() {
//        ModelConfig.ModelProperties properties = modelConfig.getModelProperties(internalModel.getModelId());
//        logger.info("Loaded model configurations: {}", properties.keySet());
//    }

    public Map<ModelType, ModelProperties> getModelMap() {
        return modelMap;
    }

    public void setModelMap(Map<ModelType, ModelProperties> modelMap) {
        this.modelMap.clear();
        this.modelMap.putAll(modelMap);
    }


    // 新方法：通过字符串模型ID获取配置
    public ModelProperties getModelProperties(String modelId) {

        // 先尝试按枚举名称查找
        for (ModelType type : modelMap.keySet()) {
            if (type.name().equalsIgnoreCase(modelId)) {
                return modelMap.get(type);
            }
        }
        // 再尝试按内部模型ID查找
        for (ModelType type : modelMap.keySet()) {
            if (type.getRequestParam().equalsIgnoreCase(modelId)) {
                return modelMap.get(type);
            }
        }
        return null;
    }

    public static class ModelProperties {
        // API端点配置
        private String apiEndpoint;
        private String apiKey;
        private String authMethod = "API_KEY"; // 认证方式：API_KEY, OAUTH2, JWT
        private int timeoutSeconds = 300; // API调用超时时间

        // 请求参数配置
        private Map<String, String> headers = new HashMap<>(); // 自定义请求头
        private Map<String, Object> parameters = new HashMap<>(); // API参数

        // 响应处理配置
        private String resultPath = "result_url"; // JSON响应中结果URL的路径
        private String statusPath = "status"; // JSON响应中状态字段的路径

        // Getters and Setters
        public String getApiEndpoint() {
            return apiEndpoint;
        }

        public void setApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getAuthMethod() {
            return authMethod;
        }

        public void setAuthMethod(String authMethod) {
            this.authMethod = authMethod;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public String getResultPath() {
            return resultPath;
        }

        public void setResultPath(String resultPath) {
            this.resultPath = resultPath;
        }

        public String getStatusPath() {
            return statusPath;
        }

        public void setStatusPath(String statusPath) {
            this.statusPath = statusPath;
        }
    }
}