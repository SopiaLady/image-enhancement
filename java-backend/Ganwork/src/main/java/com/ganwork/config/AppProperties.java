package com.ganwork.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cloud")
public class AppProperties {

    // 文件上传目录
    private String uploadDir = "./uploads";  // 默认值

    // Python 脚本路径
    private String pythonScriptPath;

    // 大小阈值（默认 5MB）
    private long sizeThreshold = 5 * 1024 * 1024;

    // 自动生成的 Getter 和 Setter
    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getPythonScriptPath() {
        return pythonScriptPath;
    }

    public void setPythonScriptPath(String pythonScriptPath) {
        this.pythonScriptPath = pythonScriptPath;
    }

    public long getSizeThreshold() {
        return sizeThreshold;
    }

    public void setSizeThreshold(long sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }
}