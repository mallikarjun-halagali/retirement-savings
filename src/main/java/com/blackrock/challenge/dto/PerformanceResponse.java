package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PerformanceResponse {

    @JsonProperty("executionTimeMs")
    private long executionTimeMs;

    @JsonProperty("memoryUsageMb")
    private double memoryUsageMb;

    @JsonProperty("activeThreads")
    private int activeThreads;

    public PerformanceResponse() {
    }

    public PerformanceResponse(long executionTimeMs, double memoryUsageMb, int activeThreads) {
        this.executionTimeMs = executionTimeMs;
        this.memoryUsageMb = memoryUsageMb;
        this.activeThreads = activeThreads;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public double getMemoryUsageMb() {
        return memoryUsageMb;
    }

    public void setMemoryUsageMb(double memoryUsageMb) {
        this.memoryUsageMb = memoryUsageMb;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public void setActiveThreads(int activeThreads) {
        this.activeThreads = activeThreads;
    }
}
