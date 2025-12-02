package com.ganwork.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProcessingStrategyFactory {

    @Autowired
    private ApiProcessingService apiService;

    public ImageProcessingStrategy getStrategy() {
        // 统一使用API服务
        return apiService;
    }
}