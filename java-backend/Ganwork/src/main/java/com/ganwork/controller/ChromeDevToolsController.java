package com.ganwork.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChromeDevToolsController {

    @GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
    public ResponseEntity<String> handleChromeDevToolsRequest() {
        // 返回空响应，避免日志污染
        return ResponseEntity.ok().build();
    }
}