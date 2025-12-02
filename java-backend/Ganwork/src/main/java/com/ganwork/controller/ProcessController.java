package com.ganwork.controller;

import com.ganwork.service.ProcessingMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    private final ProcessingMonitor processingMonitor;

    @Autowired
    public ProcessController(ProcessingMonitor processingMonitor) {
        this.processingMonitor = processingMonitor;
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, String>> getProcessStatus(
            @PathVariable String taskId) {

        String status = processingMonitor.getProcessStatus(taskId);
        Map<String, String> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("status", status);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{taskId}")
    public ResponseEntity<Void> cancelProcess(
            @PathVariable String taskId) {

        processingMonitor.stopProcess(taskId);
        return ResponseEntity.noContent().build();
    }
}
