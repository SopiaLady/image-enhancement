package com.ganwork.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProcessingMonitor {

    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    // 添加状态跟踪映射
    private final Map<String, ProcessStatus> processStatusMap = new ConcurrentHashMap<>();

    public static class ProcessStatus {
        public String taskId;
        public String displayName;
        public String currentStatus;
        public String resultUrl;
        public String errorMessage;
    }

    public void trackProcess(String taskId, Process process) {
        runningProcesses.put(taskId, process);
    }

    public void stopProcess(String taskId) {
        Process process = runningProcesses.get(taskId);
        if (process != null && process.isAlive()) {
            process.destroy();
            runningProcesses.remove(taskId);
        }
    }

    public String getProcessStatus(String taskId) {
        Process process = runningProcesses.get(taskId);
        if (process == null) {
            return "NOT_FOUND";
        }
        if (process.isAlive()) {
            return "RUNNING";
        }
        return "COMPLETED";
    }

    // 添加缺失的方法
    public void startProcess(String taskId, String displayName) {
        ProcessStatus status = new ProcessStatus();
        status.taskId = taskId;
        status.displayName = displayName;
        status.currentStatus = "STARTED";
        processStatusMap.put(taskId, status);
    }

    public void updateProcessStatus(String taskId, String statusMessage) {
        ProcessStatus status = processStatusMap.get(taskId);
        if (status != null) {
            status.currentStatus = statusMessage;
        }
    }

    public void completeProcess(String taskId, String resultUrl) {
        ProcessStatus status = processStatusMap.get(taskId);
        if (status != null) {
            status.resultUrl = resultUrl;
            status.currentStatus = "COMPLETED";
        }
    }

    public void failProcess(String taskId, String errorMessage) {
        ProcessStatus status = processStatusMap.get(taskId);
        if (status != null) {
            status.errorMessage = errorMessage;
            status.currentStatus = "FAILED";
        }
    }

    public ProcessStatus getProcessStatusObject(String taskId) {
        return processStatusMap.get(taskId);
    }
}