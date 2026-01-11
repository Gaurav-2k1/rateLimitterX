package com.ratelimitx.controller;

import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.repository.UsageMetricRepository;
import com.ratelimitx.service.TierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.ratelimitx.common.entity.UsageMetric;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final UsageMetricRepository metricsRepository;
    private final TierService tierService;
    
    @GetMapping("/realtime")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealtime(Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        Long totalChecks = metricsRepository.countByTenantIdAndTimestampAfter(tenantId, oneHourAgo);
        Long rateLimitHits = metricsRepository.countDeniedByTenantIdAndTimestampAfter(tenantId, oneHourAgo);
        
        // Calculate latency percentiles
        List<UsageMetric> recentMetrics = metricsRepository.findByTenantIdAndTimestampAfter(tenantId, oneHourAgo);
        List<Integer> latencies = recentMetrics.stream()
            .filter(m -> m.getLatencyMs() != null)
            .map(UsageMetric::getLatencyMs)
            .sorted()
            .collect(Collectors.toList());
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalChecks", totalChecks);
        metrics.put("rateLimitHits", rateLimitHits);
        metrics.put("hitRate", totalChecks > 0 ? (double) rateLimitHits / totalChecks * 100 : 0);
        metrics.put("timestamp", LocalDateTime.now());
        
        // Latency percentiles
        if (!latencies.isEmpty()) {
            metrics.put("latencyP50", calculatePercentile(latencies, 50));
            metrics.put("latencyP95", calculatePercentile(latencies, 95));
            metrics.put("latencyP99", calculatePercentile(latencies, 99));
        } else {
            metrics.put("latencyP50", 0);
            metrics.put("latencyP95", 0);
            metrics.put("latencyP99", 0);
        }
        
        // Remaining checks this month
        int remaining = tierService.getRemainingChecksThisMonth(tenantId);
        metrics.put("remainingChecksThisMonth", remaining);
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
    
    @GetMapping("/top-identifiers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopIdentifiers(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        
        List<UsageMetric> metrics = metricsRepository.findByTenantIdAndTimestampAfter(tenantId, oneDayAgo);
        
        // Group by identifier and count denials
        Map<String, Long> identifierCounts = metrics.stream()
            .filter(m -> m.getChecksDenied() > 0)
            .collect(Collectors.groupingBy(
                UsageMetric::getIdentifier,
                Collectors.summingLong(UsageMetric::getChecksDenied)
            ));
        
        List<Map<String, Object>> topIdentifiers = identifierCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("identifier", entry.getKey());
                item.put("deniedCount", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(topIdentifiers));
    }
    
    private int calculatePercentile(List<Integer> sortedList, int percentile) {
        if (sortedList.isEmpty()) return 0;
        int index = (int) Math.ceil((percentile / 100.0) * sortedList.size()) - 1;
        return sortedList.get(Math.max(0, index));
    }
    
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrends(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        LocalDateTime startTime = start != null ? 
            LocalDateTime.parse(start) : LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = end != null ? 
            LocalDateTime.parse(end) : LocalDateTime.now();
        
        // Simplified - in production, use proper time-series aggregation
        Long totalChecks = metricsRepository.countByTenantIdAndTimestampAfter(tenantId, startTime);
        Long rateLimitHits = metricsRepository.countDeniedByTenantIdAndTimestampAfter(tenantId, startTime);
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("totalChecks", totalChecks);
        trends.put("rateLimitHits", rateLimitHits);
        trends.put("startTime", startTime);
        trends.put("endTime", endTime);
        
        return ResponseEntity.ok(ApiResponse.success(trends));
    }
    
    @GetMapping("/hourly")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHourlyData(
            @RequestParam(defaultValue = "24") int hours,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        
        List<UsageMetric> metrics = metricsRepository.findByTenantIdAndTimestampAfter(tenantId, startTime);
        
        // Group by hour
        Map<String, Map<String, Long>> hourlyMap = new LinkedHashMap<>();
        
        // Initialize all hours with zeros
        LocalDateTime now = LocalDateTime.now();
        for (int i = hours - 1; i >= 0; i--) {
            LocalDateTime hourStart = now.minusHours(i).withMinute(0).withSecond(0).withNano(0);
            String hourKey = String.format("%02d:00", hourStart.getHour());
            hourlyMap.put(hourKey, new HashMap<>());
            hourlyMap.get(hourKey).put("checks", 0L);
            hourlyMap.get(hourKey).put("hits", 0L);
            hourlyMap.get(hourKey).put("allowed", 0L);
        }
        
        // Aggregate metrics by hour
        for (UsageMetric metric : metrics) {
            LocalDateTime metricTime = metric.getTimestamp();
            String hourKey = String.format("%02d:00", metricTime.getHour());
            
            if (hourlyMap.containsKey(hourKey)) {
                hourlyMap.get(hourKey).put("checks", 
                    hourlyMap.get(hourKey).get("checks") + metric.getChecksPerformed());
                hourlyMap.get(hourKey).put("hits", 
                    hourlyMap.get(hourKey).get("hits") + metric.getChecksDenied());
                hourlyMap.get(hourKey).put("allowed", 
                    hourlyMap.get(hourKey).get("allowed") + (metric.getChecksPerformed() - metric.getChecksDenied()));
            }
        }
        
        // Convert to list format
        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry : hourlyMap.entrySet()) {
            Map<String, Object> hourData = new HashMap<>();
            hourData.put("hour", entry.getKey());
            hourData.put("checks", entry.getValue().get("checks"));
            hourData.put("hits", entry.getValue().get("hits"));
            hourData.put("allowed", entry.getValue().get("allowed"));
            hourlyData.add(hourData);
        }
        
        return ResponseEntity.ok(ApiResponse.success(hourlyData));
    }
    
    @GetMapping("/latency-trends")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLatencyTrends(
            @RequestParam(defaultValue = "12") int intervals,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        
        List<UsageMetric> metrics = metricsRepository.findByTenantIdAndTimestampAfter(tenantId, startTime);
        
        // Group by 5-minute intervals
        Map<Integer, List<Integer>> intervalLatencies = new LinkedHashMap<>();
        
        // Initialize intervals (0 = most recent, intervals-1 = oldest)
        for (int i = 0; i < intervals; i++) {
            intervalLatencies.put(i, new ArrayList<>());
        }
        
        // Group latencies by interval
        for (UsageMetric metric : metrics) {
            if (metric.getLatencyMs() != null) {
                LocalDateTime metricTime = metric.getTimestamp();
                long minutesAgo = java.time.Duration.between(metricTime, LocalDateTime.now()).toMinutes();
                int interval = (int) (minutesAgo / 5);
                if (interval >= 0 && interval < intervals) {
                    intervalLatencies.get(interval).add(metric.getLatencyMs());
                }
            }
        }
        
        // Calculate percentiles for each interval (most recent first)
        List<Map<String, Object>> latencyData = new ArrayList<>();
        for (int i = 0; i < intervals; i++) {
            List<Integer> latencies = intervalLatencies.get(i);
            Map<String, Object> intervalData = new HashMap<>();
            intervalData.put("time", String.format("%dm", i * 5));
            
            if (!latencies.isEmpty()) {
                List<Integer> sorted = new ArrayList<>(latencies);
                Collections.sort(sorted);
                intervalData.put("p50", calculatePercentile(sorted, 50));
                intervalData.put("p95", calculatePercentile(sorted, 95));
                intervalData.put("p99", calculatePercentile(sorted, 99));
            } else {
                intervalData.put("p50", 0);
                intervalData.put("p95", 0);
                intervalData.put("p99", 0);
            }
            latencyData.add(intervalData);
        }
        
        return ResponseEntity.ok(ApiResponse.success(latencyData));
    }
    
    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        
        List<UsageMetric> metrics = metricsRepository.findByTenantIdAndTimestampAfter(tenantId, startTime);
        
        // Filter for significant events (rate limit hits or high-volume checks)
        List<Map<String, Object>> activities = new ArrayList<>();
        
        for (UsageMetric metric : metrics) {
            if (metric.getChecksDenied() > 0 || metric.getChecksPerformed() > 100) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", metric.getId());
                
                if (metric.getChecksDenied() > 0) {
                    activity.put("type", "alert");
                    activity.put("action", "Rate limit threshold reached");
                } else {
                    activity.put("type", "rule");
                    activity.put("action", "High request volume detected");
                }
                
                activity.put("resource", metric.getResource() != null ? metric.getResource() : "Unknown");
                activity.put("timestamp", metric.getTimestamp());
                
                // Calculate time ago
                long minutesAgo = java.time.Duration.between(metric.getTimestamp(), LocalDateTime.now()).toMinutes();
                if (minutesAgo < 60) {
                    activity.put("time", minutesAgo + " minutes ago");
                } else if (minutesAgo < 1440) {
                    activity.put("time", (minutesAgo / 60) + " hours ago");
                } else {
                    activity.put("time", (minutesAgo / 1440) + " days ago");
                }
                
                activities.add(activity);
            }
        }
        
        // Sort by timestamp descending and limit
        activities.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("timestamp");
            LocalDateTime timeB = (LocalDateTime) b.get("timestamp");
            return timeB.compareTo(timeA);
        });
        
        List<Map<String, Object>> limitedActivities = activities.stream()
            .limit(limit)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(limitedActivities));
    }
    
    private UUID getTenantId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}

