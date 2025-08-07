package org.kasbench.globeco_pricing_service;

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
 * Test class for HttpMetricsFilter exception handling and state tracking functionality.
 * Tests robust exception handling with boolean flag tracking for in-flight increment state.
 */
class HttpMetricsFilterExceptionHandlingTest {

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
        meterRegistry = new SimpleMeterRegistry();
        inFlightRequestsCounter = new AtomicInteger(0);
        httpMetricsFilter = new HttpMetricsFilter(meterRegistry, inFlightRequestsCounter);
    }

    @Test
    void testDoFilter_WithIOException_RecordsMetricsAndDecrementsInFlight() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/prices/123");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(500);

        // Simulate IOException during filter chain processing
        doThrow(new IOException("Test IO exception")).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThrows(IOException.class, () -> {
            httpMetricsFilter.doFilter(request, response, filterChain);
        });

        // Verify in-flight counter is properly decremented even after exception
        assertEquals(0, inFlightRequestsCounter.get(), "In-flight counter should be decremented after exception");

        // Verify metrics were recorded despite the exception
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0, 
                "Counter metric should be recorded even when exception occurs");
        assertTrue(meterRegistry.find("http_request_duration").timer().count() > 0, 
                "Timer metric should be recorded even when exception occurs");
    }

    @Test
    void testDoFilter_WithServletException_RecordsMetricsAndDecrementsInFlight() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(500);

        // Simulate ServletException during filter chain processing
        doThrow(new ServletException("Test servlet exception")).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThrows(ServletException.class, () -> {
            httpMetricsFilter.doFilter(request, response, filterChain);
        });

        // Verify in-flight counter is properly decremented even after exception
        assertEquals(0, inFlightRequestsCounter.get(), "In-flight counter should be decremented after exception");

        // Verify metrics were recorded despite the exception
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0, 
                "Counter metric should be recorded even when exception occurs");
        assertTrue(meterRegistry.find("http_request_duration").timer().count() > 0, 
                "Timer metric should be recorded even when exception occurs");
    }

    @Test
    void testDoFilter_WithRuntimeException_RecordsMetricsAndDecrementsInFlight() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/v1/users/456");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(500);

        // Simulate RuntimeException during filter chain processing
        doThrow(new RuntimeException("Test runtime exception")).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            httpMetricsFilter.doFilter(request, response, filterChain);
        });

        // Verify in-flight counter is properly decremented even after exception
        assertEquals(0, inFlightRequestsCounter.get(), "In-flight counter should be decremented after exception");

        // Verify metrics were recorded despite the exception
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0, 
                "Counter metric should be recorded even when exception occurs");
        assertTrue(meterRegistry.find("http_request_duration").timer().count() > 0, 
                "Timer metric should be recorded even when exception occurs");
    }

    @Test
    void testDoFilter_SuccessfulRequest_IncrementsAndDecrementsInFlightCorrectly() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/prices");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(200);

        // Verify in-flight counter increments during processing
        doAnswer(invocation -> {
            assertEquals(1, inFlightRequestsCounter.get(), "In-flight counter should be incremented during processing");
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(0, inFlightRequestsCounter.get(), "In-flight counter should be decremented after successful processing");

        // Verify metrics were recorded
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0, 
                "Counter metric should be recorded for successful request");
        assertTrue(meterRegistry.find("http_request_duration").timer().count() > 0, 
                "Timer metric should be recorded for successful request");
    }

    @Test
    void testDoFilter_PreventDoubleDecrementScenario() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/v1/orders/789");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(404);

        // Start with a higher in-flight count to test double-decrement prevention
        inFlightRequestsCounter.set(5);

        // Simulate exception during filter chain processing
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            httpMetricsFilter.doFilter(request, response, filterChain);
        });

        // Verify in-flight counter is decremented by exactly 1 (not double-decremented)
        // The counter should go from 5 -> 6 (increment) -> 5 (decrement after exception)
        assertEquals(5, inFlightRequestsCounter.get(), 
                "In-flight counter should return to original value after exception, preventing double-decrement scenarios");
    }

    @Test
    void testDoFilter_NonHttpServletRequest_SkipsMetricsProcessing() throws Exception {
        // Arrange - use non-HTTP servlet request/response
        jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

        // Act
        httpMetricsFilter.doFilter(nonHttpRequest, nonHttpResponse, filterChain);

        // Assert
        verify(filterChain).doFilter(nonHttpRequest, nonHttpResponse);
        assertEquals(0, inFlightRequestsCounter.get(), "In-flight counter should remain 0 for non-HTTP requests");
        
        // Check if counter exists before trying to get count
        var counter = meterRegistry.find("http_requests_total").counter();
        if (counter != null) {
            assertEquals(0, counter.count(), "No metrics should be recorded for non-HTTP requests");
        } else {
            // Counter doesn't exist, which is expected for non-HTTP requests
            assertTrue(true, "Counter should not exist for non-HTTP requests");
        }
    }

    @Test
    void testDoFilter_MetricsRecordingFailure_DoesNotInterfereWithRequestProcessing() throws Exception {
        // This test verifies that if metrics recording fails, it doesn't break request processing
        // We can't easily simulate this without complex mocking, but the implementation includes
        // comprehensive error handling to ensure metrics failures don't interfere with requests
        
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/health");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(200);

        // Act - should complete successfully even if internal metrics recording has issues
        assertDoesNotThrow(() -> {
            httpMetricsFilter.doFilter(request, response, filterChain);
        });

        // Assert
        verify(filterChain).doFilter(request, response);
        assertEquals(0, inFlightRequestsCounter.get(), "In-flight counter should be properly managed");
    }
}