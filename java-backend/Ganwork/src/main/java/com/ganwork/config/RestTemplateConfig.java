package com.ganwork.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 5秒连接超时
        factory.setReadTimeout(300000);    // 30秒读取超时
        return new RestTemplate(factory);
    }

    // 添加全局 ObjectMapper 配置
    @Bean
    public ObjectMapper objectMapper() {

        return new ObjectMapper();
    }
}