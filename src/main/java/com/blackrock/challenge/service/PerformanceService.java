package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.PerformanceResponse;
import org.springframework.stereotype.Service;

@Service
public class PerformanceService {

    private final long startTime = System.currentTimeMillis();

    public PerformanceResponse getPerformance() {
        long uptimeMs = System.currentTimeMillis() - startTime;

        // Format as "1970-01-01 HH:mm:ss.SSS" (epoch-based time representation)
        long hours = uptimeMs / 3_600_000;
        long minutes = (uptimeMs % 3_600_000) / 60_000;
        long seconds = (uptimeMs % 60_000) / 1_000;
        long millis = uptimeMs % 1_000;
        String time = String.format("1970-01-01 00:%02d:%02d.%03d", minutes, seconds, millis);
        if (hours > 0) {
            time = String.format("1970-01-01 %02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        }

        // Memory usage in MB
        Runtime runtime = Runtime.getRuntime();
        double memoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        String memory = String.format("%.2f", memoryMb);

        // Active threads
        int threads = Thread.activeCount();

        return new PerformanceResponse(time, memory, threads);
    }
}
