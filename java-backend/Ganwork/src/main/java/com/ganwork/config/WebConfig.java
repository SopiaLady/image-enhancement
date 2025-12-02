package com.ganwork.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.io.File;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private Environment env;

    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/static/");
        resolver.setSuffix(".html");
        return resolver;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(getAllowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 显式忽略 favicon 请求
        registry
                .addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);

        // 映射上传目录为静态资源
        String uploadDir = "file:" + env.getProperty("app.upload-dir") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir)
                .setCachePeriod(3600);

        // 添加processed目录映射
        String processedDir = "file:" + env.getProperty("app.python.processed-dir") + "/";
        System.out.println("Processed目录路径: " + processedDir);

        // 检查目录是否存在
        File dir = new File(env.getProperty("app.python.processed-dir"));
        System.out.println("Processed目录是否存在: " + dir.exists());
        if (dir.exists()) {
            System.out.println("Processed目录绝对路径: " + dir.getAbsolutePath());
        }

        registry.addResourceHandler("/processed/**")
                .addResourceLocations(processedDir)
                .setCachePeriod(3600);

        // 修正静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // 特殊处理根路径的 index.html
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/index.html");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/index.html");
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    private String[] getAllowedOrigins() {
        String origins = env.getProperty("app.cors.allowed-origins",
                "http://localhost:63342," +
                        "http://127.0.0.1:63342," +
                        "http://localhost:3000");
        return origins.split(",");
    }
}