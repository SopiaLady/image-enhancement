package com.ganwork.util;

import java.util.List;

public class ImageProcessingResponse {
    private boolean success;
    private String message;
    private List<ProcessedImageResult> results;

    public ImageProcessingResponse(boolean success, String message, List<ProcessedImageResult> results) {
        this.success = success;
        this.message = message;
        this.results = results;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ProcessedImageResult> getResults() {
        return results;
    }

    public void setResults(List<ProcessedImageResult> results) {
        this.results = results;
    }

    // 内部类表示单个处理结果
    public static class ProcessedImageResult {
        private String processedUrl;

        public ProcessedImageResult(String processedUrl) {
            this.processedUrl = processedUrl;
        }

        public String getProcessedUrl() {
            return processedUrl;
        }

        public void setProcessedUrl(String processedUrl) {
            this.processedUrl = processedUrl;
        }
    }
}