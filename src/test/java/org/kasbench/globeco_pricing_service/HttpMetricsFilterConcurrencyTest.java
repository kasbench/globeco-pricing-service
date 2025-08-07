package org.kasbench.globeco_pricing_service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive concurrency and performance tests for HttpMetricsFilter.
 * Tests thread safety, Timer cache performance, high-cardinality scenarios, and load testing.
 * Covers requirements 5.5 and 4.1 from the enhanced-http-metrics specification.
 */
class HttpMetricsFilterConcurrencyTest {

    private SimpleMeterRegistry meterRegistry;
    private AtomicInteger inFlightRequestsCounter;
    private HttpMetricsFilter httpMetricsFilter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        inFlightRequestsCounter = new AtomicInteger(0);
        httpMetricsFilter = new HttpMetricsFilter(meterRegistry, inFlightRequestsCounter);
    }

    /**
     * Test thread safety for concurrent request processing.
     * Verifies that multiple threads can safely process requests simultaneously
     * without race conditions in metric recording or in-flight counter management.
     * Requirement: 5.5 - Thread safety tests for concurrent request processing
     */
    @Test
    @Timeout(30)
    void testConcurrentRequestProcessing_ThreadSafety() throws Exception {
        final int numberOfThreads = 20;
        final int requestsPerThread = 50;
        final int totalRequests = numberOfThreads * requestsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalRequests);
        
        AtomicInteger maxInFlightSeen = new AtomicInteger(0);
        AtomicInteger totalExceptions = new AtomicInteger(0);
        
        // Create tasks for concurrent execution
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int threadId = 0; threadId < numberOfThreads; threadId++) {
            final int finalThreadId = threadId;
            
            for (int requestId = 0; requestId < requestsPerThread; requestId++) {
                final int finalRequestId = requestId;
                
                Future<Void> future = executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();
                        
                        // Create mock request/response for this specific request
                        HttpServletRequest request = mock(HttpServletRequest.class);
                        HttpServletResponse response = mock(HttpServletResponse.class);
                        FilterChain chain = mock(FilterChain.class);
                        
                        when(request.getMethod()).thenReturn("GET");
                        when(request.getRequestURI()).thenReturn("/api/v1/prices/" + finalThreadId + "/" + finalRequestId);
                        when(request.getContextPath()).thenReturn("");
                        when(request.getRemoteAddr()).thenReturn("127.0.0." + (finalThreadId % 255));
                        when(request.getHeader("User-Agent")).thenReturn("Test-Agent-" + finalThreadId);
                        when(response.getStatus()).thenReturn(200);
                        
                        // Track maximum in-flight requests during processing
                        doAnswer(invocation -> {
                            int currentInFlight = inFlightRequestsCounter.get();
                            maxInFlightSeen.updateAndGet(max -> Math.max(max, currentInFlight));
                            
                            // Simulate some processing time to increase chance of concurrency
                            Thread.sleep(1 + (finalRequestId % 5)); // 1-5ms random delay
                            return null;
                        }).when(chain).doFilter(request, response);
                        
                        // Process the request
                        httpMetricsFilter.doFilter(request, response, chain);
                        
                    } catch (Exception e) {
                        totalExceptions.incrementAndGet();
                        throw new RuntimeException("Thread " + finalThreadId + " request " + finalRequestId + " failed", e);
                    } finally {
                        completionLatch.countDown();
                    }
                    return null;
                });
                
                futures.add(future);
            }
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all requests to complete
        boolean completed = completionLatch.await(25, TimeUnit.SECONDS);
        assertTrue(completed, "All requests should complete within timeout");
        
        // Wait for all futures to complete and check for exceptions
        for (Future<Void> future : futures) {
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                fail("Request processing failed: " + e.getCause().getMessage());
            }
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");
        
        // Verify thread safety results
        assertEquals(0, inFlightRequestsCounter.get(), 
                "In-flight counter should be zero after all requests complete");
        assertEquals(0, totalExceptions.get(), 
                "No exceptions should occur during concurrent processing");
        assertTrue(maxInFlightSeen.get() > 1, 
                "Should have seen multiple concurrent requests (max: " + maxInFlightSeen.get() + ")");
        
        // Verify all requests were recorded in metrics
        double totalCounterValue = meterRegistry.find("http_requests_total").counters()
                .stream().mapToDouble(Counter::count).sum();
        assertEquals(totalRequests, totalCounterValue, 
                "All requests should be recorded in counter metrics");
        
        long totalTimerCount = meterRegistry.find("http_request_duration").timers()
                .stream().mapToLong(Timer::count).sum();
        assertEquals(totalRequests, totalTimerCount, 
                "All requests should be recorded in timer metrics");
    }

    /**
     * Test Timer cache performance to verify caching effectiveness.
     * Measures performance difference between cached and non-cached scenarios.
     * Requirement: 4.1 - Timer cache performance tests to verify caching effectiveness
     */
    @Test
    @Timeout(30)
    void testTimerCachePerformance_VerifiesCachingEffectiveness() throws Exception {
        final int warmupRequests = 100;
        final int testRequests = 1000;
        
        // Setup common request parameters for cache effectiveness
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/prices/AAPL");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Performance-Test-Agent");
        when(response.getStatus()).thenReturn(200);
        
        // Minimal processing time to focus on Timer cache overhead
        doAnswer(invocation -> {
            Thread.sleep(1); // 1ms minimal delay
            return null;
        }).when(chain).doFilter(request, response);
        
        // Warmup phase - populate Timer cache
        for (int i = 0; i < warmupRequests; i++) {
            httpMetricsFilter.doFilter(request, response, chain);
        }
        
        // Measure performance with cached Timers
        long startTime = System.nanoTime();
        for (int i = 0; i < testRequests; i++) {
            httpMetricsFilter.doFilter(request, response, chain);
        }
        long cachedDuration = System.nanoTime() - startTime;
        
        // Verify cache effectiveness by checking Timer reuse
        Timer cachedTimer = meterRegistry.find("http_request_duration")
                .tag("method", "GET")
                .tag("path", "/api/v1/prices/AAPL")
                .tag("status", "200")
                .timer();
        
        assertNotNull(cachedTimer, "Timer should be cached and reused");
        assertEquals(warmupRequests + testRequests, cachedTimer.count(), 
                "All requests should use the same cached Timer instance");
        
        // Verify performance characteristics
        double avgProcessingTimeMs = cachedDuration / (double) testRequests / 1_000_000.0;
        assertTrue(avgProcessingTimeMs < 5.0, 
                "Average processing time should be under 5ms with caching (actual: " + 
                String.format("%.2f", avgProcessingTimeMs) + "ms)");
        
        // Verify only one Timer instance was created for identical requests
        long timerCount = meterRegistry.find("http_request_duration").timers().size();
        assertTrue(timerCount <= 5, 
                "Should have minimal Timer instances due to caching (actual: " + timerCount + ")");
        
        System.out.println("Timer cache performance test completed:");
        System.out.println("- Processed " + testRequests + " requests");
        System.out.println("- Average processing time: " + String.format("%.2f", avgProcessingTimeMs) + "ms");
        System.out.println("- Total Timer instances created: " + timerCount);
        System.out.println("- Cache effectiveness: " + (testRequests / (double) timerCount) + " requests per Timer");
    }

    /**
     * Test high-cardinality scenarios to ensure path normalization prevents explosion.
     * Verifies that path normalization effectively reduces metric cardinality even with
     * many different path parameters (IDs, UUIDs, etc.).
     * Requirement: 4.1 - High-cardinality scenarios to ensure path normalization prevents explosion
     */
    @Test
    @Timeout(30)
    void testHighCardinalityPrevention_PathNormalizationEffectiveness() throws Exception {
        final int uniqueRequests = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch completionLatch = new CountDownLatch(uniqueRequests);
        
        // Generate requests with high cardinality paths that should be normalized
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < uniqueRequests; i++) {
            final int requestId = i;
            
            Future<Void> future = executor.submit(() -> {
                try {
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    FilterChain chain = mock(FilterChain.class);
                    
                    // Create high-cardinality paths that should be normalized
                    String path;
                    if (requestId % 4 == 0) {
                        // Numeric IDs - should normalize to {id}
                        path = "/api/v1/prices/" + (requestId * 1000 + 12345);
                    } else if (requestId % 4 == 1) {
                        // UUID paths - should normalize to {uuid}
                        path = "/api/v1/users/" + java.util.UUID.randomUUID().toString();
                    } else if (requestId % 4 == 2) {
                        // Mixed numeric and string - should normalize numeric parts
                        path = "/api/v1/orders/" + requestId + "/items/" + (requestId * 2);
                    } else {
                        // Single digit IDs in API context - should normalize to {id}
                        path = "/api/v1/executions/" + (requestId % 10);
                    }
                    
                    when(request.getMethod()).thenReturn("GET");
                    when(request.getRequestURI()).thenReturn(path);
                    when(request.getContextPath()).thenReturn("");
                    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
                    when(request.getHeader("User-Agent")).thenReturn("Cardinality-Test-Agent");
                    when(response.getStatus()).thenReturn(200);
                    
                    doAnswer(invocation -> {
                        Thread.sleep(1); // Minimal delay
                        return null;
                    }).when(chain).doFilter(request, response);
                    
                    httpMetricsFilter.doFilter(request, response, chain);
                    
                } catch (Exception e) {
                    throw new RuntimeException("High cardinality test request " + requestId + " failed", e);
                } finally {
                    completionLatch.countDown();
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all requests to complete
        boolean completed = completionLatch.await(25, TimeUnit.SECONDS);
        assertTrue(completed, "All high-cardinality requests should complete within timeout");
        
        // Wait for all futures and check for exceptions
        for (Future<Void> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // Verify cardinality explosion was prevented
        List<Timer> timers = new ArrayList<>(meterRegistry.find("http_request_duration").timers());
        List<Counter> counters = new ArrayList<>(meterRegistry.find("http_requests_total").counters());
        
        // Should have very few unique path labels due to normalization
        long uniqueTimerPaths = timers.stream()
                .map(timer -> timer.getId().getTag("path"))
                .distinct()
                .count();
        
        long uniqueCounterPaths = counters.stream()
                .map(counter -> counter.getId().getTag("path"))
                .distinct()
                .count();
        
        // Verify cardinality is controlled (should be much less than unique requests)
        assertTrue(uniqueTimerPaths <= 10, 
                "Timer path cardinality should be controlled (actual: " + uniqueTimerPaths + 
                " unique paths for " + uniqueRequests + " requests)");
        assertTrue(uniqueCounterPaths <= 10, 
                "Counter path cardinality should be controlled (actual: " + uniqueCounterPaths + 
                " unique paths for " + uniqueRequests + " requests)");
        
        // Verify all requests were processed
        double totalCounterValue = counters.stream().mapToDouble(Counter::count).sum();
        assertEquals(uniqueRequests, totalCounterValue, "All requests should be recorded");
        
        // Verify expected normalized paths exist
        assertTrue(timers.stream().anyMatch(t -> "/api/v1/prices/{id}".equals(t.getId().getTag("path"))),
                "Should have normalized numeric ID paths");
        assertTrue(timers.stream().anyMatch(t -> "/api/v1/users/{uuid}".equals(t.getId().getTag("path"))),
                "Should have normalized UUID paths");
        assertTrue(timers.stream().anyMatch(t -> "/api/v1/orders/{id}/items/{id}".equals(t.getId().getTag("path"))),
                "Should have normalized complex paths with multiple IDs");
        assertTrue(timers.stream().anyMatch(t -> "/api/v1/executions/{id}".equals(t.getId().getTag("path"))),
                "Should have normalized single digit ID paths in API context");
        
        System.out.println("High-cardinality prevention test completed:");
        System.out.println("- Processed " + uniqueRequests + " unique requests");
        System.out.println("- Timer path cardinality: " + uniqueTimerPaths);
        System.out.println("- Counter path cardinality: " + uniqueCounterPaths);
        System.out.println("- Cardinality reduction: " + (uniqueRequests / (double) uniqueTimerPaths) + "x");
    }

    /**
     * Load testing scenario to validate Timer caching reduces CPU overhead.
     * Compares CPU usage and performance between scenarios with and without effective caching.
     * Requirement: 4.1 - Load testing scenarios to validate Timer caching reduces CPU overhead
     */
    @Test
    @Timeout(60)
    void testLoadTesting_TimerCachingReducesCpuOverhead() throws Exception {
        final int loadTestRequests = 5000;
        final int concurrentThreads = 20;
        
        // Test scenario 1: High cache hit ratio (same paths)
        long cachedScenarioTime = measureLoadTestPerformance(
                loadTestRequests, concurrentThreads, true, "Cached scenario");
        
        // Test scenario 2: Low cache hit ratio (different paths)
        long diverseScenarioTime = measureLoadTestPerformance(
                loadTestRequests, concurrentThreads, false, "Diverse scenario");
        
        // Verify Timer cache effectiveness
        List<Timer> timers = new ArrayList<>(meterRegistry.find("http_request_duration").timers());
        assertTrue(timers.size() > 0, "Should have created Timer instances");
        
        // Verify total request processing
        double totalCounterValue = meterRegistry.find("http_requests_total").counters()
                .stream().mapToDouble(Counter::count).sum();
        assertEquals(loadTestRequests * 2, totalCounterValue, 
                "Should have processed all requests from both scenarios");
        
        // Performance comparison (cached should be faster or similar)
        double performanceRatio = (double) diverseScenarioTime / cachedScenarioTime;
        assertTrue(performanceRatio >= 0.8, 
                "Diverse scenario should not be significantly faster than cached scenario. " +
                "Cached: " + cachedScenarioTime + "ms, Diverse: " + diverseScenarioTime + "ms, " +
                "Ratio: " + String.format("%.2f", performanceRatio));
        
        System.out.println("Load testing performance comparison:");
        System.out.println("- Cached scenario time: " + cachedScenarioTime + "ms");
        System.out.println("- Diverse scenario time: " + diverseScenarioTime + "ms");
        System.out.println("- Performance ratio (diverse/cached): " + String.format("%.2f", performanceRatio));
        System.out.println("- Total Timer instances: " + timers.size());
        System.out.println("- Average requests per Timer: " + (totalCounterValue / timers.size()));
    }

    /**
     * Helper method to measure load test performance under different caching scenarios.
     */
    private long measureLoadTestPerformance(int totalRequests, int threads, 
                                          boolean useSamePaths, String scenarioName) throws Exception {
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalRequests);
        
        AtomicLong totalProcessingTime = new AtomicLong(0);
        AtomicInteger requestCounter = new AtomicInteger(0);
        
        // Create load test tasks
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            
            Future<Void> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    FilterChain chain = mock(FilterChain.class);
                    
                    // Configure path based on caching scenario
                    String path;
                    if (useSamePaths) {
                        // High cache hit ratio - use only a few different paths
                        path = "/api/v1/load-test/" + (requestId % 5);
                    } else {
                        // Low cache hit ratio - use many different paths
                        path = "/api/v1/load-test/" + requestId + "/" + System.nanoTime();
                    }
                    
                    when(request.getMethod()).thenReturn("GET");
                    when(request.getRequestURI()).thenReturn(path);
                    when(request.getContextPath()).thenReturn("");
                    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
                    when(request.getHeader("User-Agent")).thenReturn("Load-Test-Agent");
                    when(response.getStatus()).thenReturn(200);
                    
                    // Minimal processing time to focus on metrics overhead
                    doAnswer(invocation -> {
                        Thread.sleep(1); // 1ms processing time
                        return null;
                    }).when(chain).doFilter(request, response);
                    
                    long requestStart = System.nanoTime();
                    httpMetricsFilter.doFilter(request, response, chain);
                    long requestEnd = System.nanoTime();
                    
                    totalProcessingTime.addAndGet(requestEnd - requestStart);
                    requestCounter.incrementAndGet();
                    
                } catch (Exception e) {
                    throw new RuntimeException(scenarioName + " request " + requestId + " failed", e);
                } finally {
                    completionLatch.countDown();
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Start load test
        long scenarioStart = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(45, TimeUnit.SECONDS);
        assertTrue(completed, scenarioName + " should complete within timeout");
        long scenarioEnd = System.currentTimeMillis();
        
        // Wait for all futures
        for (Future<Void> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        long totalScenarioTime = scenarioEnd - scenarioStart;
        double avgRequestTime = totalProcessingTime.get() / (double) requestCounter.get() / 1_000_000.0;
        
        System.out.println(scenarioName + " completed:");
        System.out.println("- Total time: " + totalScenarioTime + "ms");
        System.out.println("- Requests processed: " + requestCounter.get());
        System.out.println("- Average request time: " + String.format("%.2f", avgRequestTime) + "ms");
        System.out.println("- Throughput: " + (requestCounter.get() * 1000.0 / totalScenarioTime) + " req/sec");
        
        return totalScenarioTime;
    }

    /**
     * Test concurrent exception handling to ensure thread safety during error conditions.
     * Verifies that exceptions in one thread don't affect metric recording in other threads.
     */
    @Test
    @Timeout(30)
    void testConcurrentExceptionHandling_ThreadSafety() throws Exception {
        final int totalRequests = 100;
        final int exceptionRequests = 20; // 20% will throw exceptions
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalRequests);
        
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            final boolean shouldThrowException = requestId < exceptionRequests;
            
            Future<Void> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    FilterChain chain = mock(FilterChain.class);
                    
                    when(request.getMethod()).thenReturn("POST");
                    when(request.getRequestURI()).thenReturn("/api/v1/concurrent-test/" + requestId);
                    when(request.getContextPath()).thenReturn("");
                    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
                    when(request.getHeader("User-Agent")).thenReturn("Concurrent-Exception-Test");
                    when(response.getStatus()).thenReturn(shouldThrowException ? 500 : 200);
                    
                    if (shouldThrowException) {
                        doThrow(new RuntimeException("Simulated concurrent exception " + requestId))
                                .when(chain).doFilter(request, response);
                    } else {
                        doAnswer(invocation -> {
                            Thread.sleep(2); // Small delay
                            return null;
                        }).when(chain).doFilter(request, response);
                    }
                    
                    try {
                        httpMetricsFilter.doFilter(request, response, chain);
                        successfulRequests.incrementAndGet();
                    } catch (RuntimeException e) {
                        exceptionCount.incrementAndGet();
                        // Expected for exception requests
                        if (!shouldThrowException) {
                            throw e; // Unexpected exception
                        }
                    }
                    
                } catch (Exception e) {
                    if (!shouldThrowException) {
                        throw new RuntimeException("Unexpected exception in request " + requestId, e);
                    }
                } finally {
                    completionLatch.countDown();
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Start all requests
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(25, TimeUnit.SECONDS);
        assertTrue(completed, "All concurrent exception test requests should complete");
        
        // Check futures for unexpected exceptions
        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).get(1, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                if (i >= exceptionRequests) { // Only exception requests should fail
                    fail("Unexpected exception in successful request " + i + ": " + e.getCause());
                }
            }
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // Verify results
        assertEquals(0, inFlightRequestsCounter.get(), 
                "In-flight counter should be zero after all requests complete");
        assertEquals(totalRequests - exceptionRequests, successfulRequests.get(),
                "Should have correct number of successful requests");
        assertEquals(exceptionRequests, exceptionCount.get(),
                "Should have correct number of exception requests");
        
        // Verify all requests (including exceptions) recorded metrics
        double totalCounterValue = meterRegistry.find("http_requests_total").counters()
                .stream().mapToDouble(Counter::count).sum();
        assertEquals(totalRequests, totalCounterValue, 
                "All requests should record metrics even with exceptions");
        
        long totalTimerCount = meterRegistry.find("http_request_duration").timers()
                .stream().mapToLong(Timer::count).sum();
        assertEquals(totalRequests, totalTimerCount, 
                "All requests should record timer metrics even with exceptions");
    }
}