package org.kasbench.globeco_pricing_service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test class for HttpMetricsFilter functionality.
 * Tests successful request processing with metric verification and in-flight counter tracking.
 * Covers requirements 5.1, 5.2 from the enhanced-http-metrics specification.
 */
class HttpMetricsFilterTest {

    private SimpleMeterRegistry meterRegistry;
    private AtomicInteger inFlightRequestsCounter;
    private HttpMetricsFilter httpMetricsFilter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create fresh instances for each test to avoid interference
        meterRegistry = new SimpleMeterRegistry();
        inFlightRequestsCounter = new AtomicInteger(0);
        httpMetricsFilter = new HttpMetricsFilter(meterRegistry, inFlightRequestsCounter);
    }

    @Test
    void testSuccessfulRequestProcessing_VerifiesAllMetrics() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/prices");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(200);

        // Add a small delay to ensure measurable duration
        doAnswer(invocation -> {
            Thread.sleep(1); // 1ms delay to ensure measurable duration
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert - Verify counter metric was recorded
        Counter counter = meterRegistry.find("http_requests_total")
                .tag("method", "GET")
                .tag("path", "/api/v1/prices")
                .tag("status", "200")
                .counter();
        assertNotNull(counter, "Counter metric should be registered");
        assertEquals(1.0, counter.count(), "Counter should be incremented once");

        // Assert - Verify timer metric was recorded
        Timer timer = meterRegistry.find("http_request_duration")
                .tag("method", "GET")
                .tag("path", "/api/v1/prices")
                .tag("status", "200")
                .timer();
        assertNotNull(timer, "Timer metric should be registered");
        assertEquals(1L, timer.count(), "Timer should record one measurement");
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0, 
                "Timer should record positive duration");

        // Assert - Verify in-flight counter is back to zero
        assertEquals(0, inFlightRequestsCounter.get(), 
                "In-flight counter should be decremented after successful processing");

        // Verify filter chain was called
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testInFlightCounterTracking_DuringRequestProcessing() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(201);

        // Verify in-flight counter increments during processing
        doAnswer(invocation -> {
            assertEquals(1, inFlightRequestsCounter.get(), 
                    "In-flight counter should be incremented during request processing");
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert - Verify in-flight counter is properly decremented after processing
        assertEquals(0, inFlightRequestsCounter.get(), 
                "In-flight counter should be decremented after request processing");

        // Verify metrics were recorded
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0, 
                "Counter metric should be recorded");
        assertTrue(meterRegistry.find("http_request_duration").timer().count() > 0, 
                "Timer metric should be recorded");
    }

    @Test
    void testMultipleConcurrentRequests_InFlightCounterAccuracy() throws Exception {
        // Arrange - simulate multiple concurrent requests
        AtomicInteger maxInFlightSeen = new AtomicInteger(0);
        
        // Setup first request
        HttpServletRequest request1 = mock(HttpServletRequest.class);
        HttpServletResponse response1 = mock(HttpServletResponse.class);
        FilterChain filterChain1 = mock(FilterChain.class);
        
        when(request1.getMethod()).thenReturn("GET");
        when(request1.getRequestURI()).thenReturn("/api/v1/prices/123");
        when(request1.getContextPath()).thenReturn("");
        when(request1.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request1.getHeader("User-Agent")).thenReturn("Test-Agent-1");
        when(response1.getStatus()).thenReturn(200);

        // Setup second request
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        HttpServletResponse response2 = mock(HttpServletResponse.class);
        FilterChain filterChain2 = mock(FilterChain.class);
        
        when(request2.getMethod()).thenReturn("POST");
        when(request2.getRequestURI()).thenReturn("/api/v1/orders");
        when(request2.getContextPath()).thenReturn("");
        when(request2.getRemoteAddr()).thenReturn("127.0.0.2");
        when(request2.getHeader("User-Agent")).thenReturn("Test-Agent-2");
        when(response2.getStatus()).thenReturn(201);

        // Track maximum in-flight count during processing
        doAnswer(invocation -> {
            int currentInFlight = inFlightRequestsCounter.get();
            maxInFlightSeen.updateAndGet(max -> Math.max(max, currentInFlight));
            // Simulate some processing time
            Thread.sleep(50);
            return null;
        }).when(filterChain1).doFilter(request1, response1);

        doAnswer(invocation -> {
            int currentInFlight = inFlightRequestsCounter.get();
            maxInFlightSeen.updateAndGet(max -> Math.max(max, currentInFlight));
            // Simulate some processing time
            Thread.sleep(50);
            return null;
        }).when(filterChain2).doFilter(request2, response2);

        // Act - process requests concurrently
        Thread thread1 = new Thread(() -> {
            try {
                httpMetricsFilter.doFilter(request1, response1, filterChain1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                httpMetricsFilter.doFilter(request2, response2, filterChain2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread1.start();
        Thread.sleep(10); // Small delay to ensure first thread starts
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert - Verify concurrent requests were tracked correctly
        assertEquals(0, inFlightRequestsCounter.get(), 
                "In-flight counter should be zero after all requests complete");
        assertTrue(maxInFlightSeen.get() >= 1, 
                "Should have seen at least 1 request in flight during processing");

        // Verify both requests recorded metrics - check individual counters
        Counter getCounter = meterRegistry.find("http_requests_total")
                .tag("method", "GET")
                .tag("path", "/api/v1/prices/{id}")
                .tag("status", "200")
                .counter();
        Counter postCounter = meterRegistry.find("http_requests_total")
                .tag("method", "POST")
                .tag("path", "/api/v1/orders")
                .tag("status", "201")
                .counter();
        
        assertNotNull(getCounter, "GET request counter should be registered");
        assertNotNull(postCounter, "POST request counter should be registered");
        assertEquals(1.0, getCounter.count(), "GET request should be recorded once");
        assertEquals(1.0, postCounter.count(), "POST request should be recorded once");
    }

    @Test
    void testDifferentHttpMethods_RecordsCorrectLabels() throws Exception {
        // Test GET request
        testRequestWithMethodAndStatus("GET", "/api/v1/prices", 200);
        
        // Test POST request
        testRequestWithMethodAndStatus("POST", "/api/v1/orders", 201);
        
        // Test PUT request
        testRequestWithMethodAndStatus("PUT", "/api/v1/users/123", 200);
        
        // Test DELETE request
        testRequestWithMethodAndStatus("DELETE", "/api/v1/orders/456", 204);

        // Verify all methods were recorded with correct labels
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("method", "GET").counter());
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("method", "POST").counter());
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("method", "PUT").counter());
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("method", "DELETE").counter());
    }

    @Test
    void testDifferentStatusCodes_RecordsCorrectLabels() throws Exception {
        // Test successful responses
        testRequestWithMethodAndStatus("GET", "/api/v1/prices", 200);
        testRequestWithMethodAndStatus("POST", "/api/v1/orders", 201);
        
        // Test client error responses
        testRequestWithMethodAndStatus("GET", "/api/v1/prices/INVALID", 400);
        testRequestWithMethodAndStatus("GET", "/api/v1/nonexistent", 404);
        
        // Test server error responses
        testRequestWithMethodAndStatus("GET", "/api/v1/prices", 500);

        // Verify all status codes were recorded with correct labels
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("status", "200").counter());
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("status", "201").counter());
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("status", "400").counter());
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("status", "404").counter());
        assertNotNull(meterRegistry.find("http_requests_total")
                .tag("status", "500").counter());
    }

    @Test
    void testTimerCaching_PerformanceOptimization() throws Exception {
        // Arrange - make multiple requests with same method/path/status
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/prices");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(200);

        // Act - process multiple identical requests
        httpMetricsFilter.doFilter(request, response, filterChain);
        httpMetricsFilter.doFilter(request, response, filterChain);
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert - Verify same Timer instance is reused (cached)
        Timer timer = meterRegistry.find("http_request_duration")
                .tag("method", "GET")
                .tag("path", "/api/v1/prices")
                .tag("status", "200")
                .timer();
        
        assertNotNull(timer, "Timer should be registered");
        assertEquals(3L, timer.count(), "Timer should record all three measurements");
        
        // Verify counter was also incremented correctly
        Counter counter = meterRegistry.find("http_requests_total")
                .tag("method", "GET")
                .tag("path", "/api/v1/prices")
                .tag("status", "200")
                .counter();
        assertEquals(3.0, counter.count(), "Counter should be incremented three times");
    }

    @Test
    void testNonHttpServletRequest_SkipsMetricsProcessing() throws Exception {
        // Arrange - use non-HTTP servlet request/response
        jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

        // Act
        httpMetricsFilter.doFilter(nonHttpRequest, nonHttpResponse, filterChain);

        // Assert
        verify(filterChain).doFilter(nonHttpRequest, nonHttpResponse);
        assertEquals(0, inFlightRequestsCounter.get(), "In-flight counter should remain 0 for non-HTTP requests");
        
        // Verify no HTTP metrics were recorded
        assertNull(meterRegistry.find("http_requests_total").counter(), 
                "No HTTP counter metrics should be recorded for non-HTTP requests");
        assertNull(meterRegistry.find("http_request_duration").timer(), 
                "No HTTP timer metrics should be recorded for non-HTTP requests");
    }

    @Test
    void testMetricLabelsNormalization() throws Exception {
        // Arrange - test various label normalization scenarios
        when(request.getMethod()).thenReturn("get"); // lowercase method
        when(request.getRequestURI()).thenReturn("/API/V1/PRICES");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(200);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert - Verify method is normalized to uppercase
        Counter counter = meterRegistry.find("http_requests_total")
                .tag("method", "GET") // should be normalized to uppercase
                .counter();
        assertNotNull(counter, "Counter should be found with normalized method label");
        assertEquals(1.0, counter.count(), "Counter should be incremented");

        // Verify timer also uses normalized labels
        Timer timer = meterRegistry.find("http_request_duration")
                .tag("method", "GET") // should be normalized to uppercase
                .timer();
        assertNotNull(timer, "Timer should be found with normalized method label");
        assertEquals(1L, timer.count(), "Timer should record one measurement");
    }

    @Test
    void testMillisecondDurationRecording_OTLPCompatibility() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/prices");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(200);

        // Simulate some processing time in the filter chain
        doAnswer(invocation -> {
            Thread.sleep(5); // 5ms delay
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert - Verify duration is recorded in milliseconds
        Timer timer = meterRegistry.find("http_request_duration")
                .tag("method", "GET")
                .tag("path", "/api/v1/prices")
                .tag("status", "200")
                .timer();
        
        assertNotNull(timer, "Timer should be registered");
        assertEquals(1L, timer.count(), "Timer should record one measurement");
        
        // Verify duration is reasonable (should be at least 5ms due to sleep)
        double totalTimeMs = timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS);
        assertTrue(totalTimeMs >= 5.0, 
                "Duration should be at least 5ms, but was: " + totalTimeMs + "ms");
        assertTrue(totalTimeMs < 1000.0, 
                "Duration should be less than 1 second, but was: " + totalTimeMs + "ms");
    }

    /**
     * Helper method to test requests with specific method and status combinations
     */
    private void testRequestWithMethodAndStatus(String method, String uri, int status) throws Exception {
        HttpServletRequest testRequest = mock(HttpServletRequest.class);
        HttpServletResponse testResponse = mock(HttpServletResponse.class);
        FilterChain testFilterChain = mock(FilterChain.class);

        when(testRequest.getMethod()).thenReturn(method);
        when(testRequest.getRequestURI()).thenReturn(uri);
        when(testRequest.getContextPath()).thenReturn("");
        when(testRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(testRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(testResponse.getStatus()).thenReturn(status);

        httpMetricsFilter.doFilter(testRequest, testResponse, testFilterChain);

        verify(testFilterChain).doFilter(testRequest, testResponse);
    }
}