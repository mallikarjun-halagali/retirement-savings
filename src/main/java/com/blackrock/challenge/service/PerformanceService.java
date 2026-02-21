package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.PerformanceResponse;
import org.springframework.stereotype.Service;

@Service
public class PerformanceService {

    private final long startTime = System.currentTimeMillis();

    public PerformanceResponse getPerformance() {
        long executionTimeMs = System.currentTimeMillis() - startTime;

        Runtime runtime = Runtime.getRuntime();
        double memoryUsageMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        memoryUsageMb = Math.round(memoryUsageMb * 100.0) / 100.0;

        int activeThreads = Thread.activeCount();

        return new PerformanceResponse(executionTimeMs, memoryUsageMb, activeThreads);
    }
}
