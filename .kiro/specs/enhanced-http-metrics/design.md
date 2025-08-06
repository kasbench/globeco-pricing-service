# Design Document

## Overview

This design implements standardized HTTP metrics for the GlobeCo Pricing Service following the enhanced-metrics.md specification and incorporating critical lessons learned from the java-microservice-http-metrics-implementation-guide.md. The implementation will provide three core metrics (counter, histogram, gauge) with proper label normalization, exception handling, and performance optimizations proven in production environments.

The design addresses key challenges identified in previous implementations:
- Unit conversion differences between Prometheus and OpenTelemetry Collector
- Timer instance caching to prevent performance overhead
- Sophisticated path normalization to prevent cardinality explosion
- Thread-safe in-flight request tracking with proper exception handling

## Architecture

### Component Overview

```mermaid
graph TB
    A[HTTP Request] --> B[HttpMetricsFilter]
    B --> C[FilterChain]
    C --> D[PriceController]
    
    B --> E[MeterRegistry]
    E --> F[Counter: http_requests_total]
    E --> G[Timer: http_request_duration]
    E --> H[Gauge: http_requests_in_flight]
    
    E --> I[Prometheus Endpoint]
    E --> J[OTLP Exporter]
    
    I --> K[/actuator/prometheus]
    J --> L[OpenTelemetry Collector]
    
    B --> M[Timer Cache]
    B --> N[Path Normalizer]
    B --> O[In-Flight Counter]
```

### Integration Points

1. **Servlet Filter Integration**: Implements `jakarta.servlet.Filter` to intercept all HTTP requests
2. **Spring Boot Actuator**: Leverages existing actuator endpoints for metrics exposure
3. **Micrometer Registry**: Uses Spring Boot's auto-configured MeterRegistry for metric collection
4. **OpenTelemetry Export**: Utilizes existing OTLP configuration for collector integration

## Components and Interfaces

### 1. HttpMetricsConfiguration

**Purpose**: Spring configuration class that sets up metrics infrastructure and filter registration.

**Key Responsibilities**:
- Register the in-flight counter as an AtomicInteger bean
- Create and register the http_requests_in_flight Gauge
- Configure FilterRegistrationBean with high priority (order=1)
- Ensure filter applies to all URL patterns ("/*")

**Critical Design Decision**: Based on implementation guide lesson - use AtomicInteger for thread-safe in-flight tracking rather than complex synchronization mechanisms.

### 2. HttpMetricsFilter

**Purpose**: Core servlet filter that intercepts HTTP requests and records metrics.

**Key Responsibilities**:
- Record start time with nanosecond precision
- Manage in-flight request counter (increment/decrement)
- Extract and normalize request labels (method, path, status)
- Record counter and timer metrics
- Handle exceptions gracefully while ensuring metrics are recorded

**Critical Design Decisions**:
- **Timer Caching**: Use ConcurrentHashMap to cache Timer instances (key lesson from guide)
- **Exception State Tracking**: Track increment state to prevent double-decrement scenarios
- **Millisecond Recording**: Record durations in milliseconds for OTLP compatibility
- **Path Normalization**: Use multiple regex patterns for robust ID/UUID replacement

### 3. Path Normalization Component

**Purpose**: Normalize URL paths to prevent metric cardinality explosion while maintaining meaningful labels.

**Normalization Strategy**:
1. **Spring MVC Integration**: Prefer `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` when available
2. **UUID Pattern**: Replace full UUIDs with `{uuid}` placeholder
3. **Numeric ID Pattern**: Replace multi-digit numbers with `{id}` placeholder  
4. **Context-Aware Single Digits**: Only replace single digits in API contexts

**Regex Patterns** (from implementation guide):
```java
private static final Pattern UUID_PATTERN = Pattern.compile(
    "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("/\\d{2,}(?=/|$)");
private static final Pattern SINGLE_DIGIT_ID_PATTERN = Pattern.compile("/\\d(?=/|$)");
```

### 4. Timer Cache Management

**Purpose**: Optimize performance by caching Timer instances to prevent re-registration overhead.

**Cache Strategy**:
- **Key Format**: `"http_request_duration:method:path:status"`
- **Thread Safety**: Use ConcurrentHashMap for concurrent access
- **Lazy Creation**: Create Timer instances on first use with `computeIfAbsent`
- **Configuration**: Pre-configure with histogram buckets and labels

**Performance Impact**: Implementation guide showed 60% CPU overhead reduction under load.

## Data Models

### Metric Definitions

#### 1. HTTP Requests Total Counter
```java
Counter.builder("http_requests_total")
    .description("Total number of HTTP requests")
    .tag("method", method)
    .tag("path", normalizedPath)
    .tag("status", statusCode)
    .register(meterRegistry)
```

#### 2. HTTP Request Duration Histogram
```java
Timer.builder("http_request_duration")
    .description("Duration of HTTP requests")
    .serviceLevelObjectives(HISTOGRAM_BUCKETS)
    .publishPercentileHistogram(false)
    .tag("method", method)
    .tag("path", normalizedPath)
    .tag("status", statusCode)
    .register(meterRegistry)
```

**Histogram Buckets** (milliseconds): `[5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000]`

#### 3. HTTP Requests In Flight Gauge
```java
Gauge.builder("http_requests_in_flight")
    .description("Number of HTTP requests currently being processed")
    .register(meterRegistry, inFlightCounter, AtomicInteger::get)
```

### Label Value Standards

- **method**: Uppercase HTTP method (GET, POST, PUT, DELETE, etc.)
- **path**: Normalized route pattern (e.g., `/api/v1/price/{ticker}`)
- **status**: HTTP status code as string (e.g., "200", "404", "500")

## Error Handling

### Exception Management Strategy

**Core Principle**: Metrics recording must never interfere with normal request processing.

**Implementation Pattern**:
```java
boolean inFlightIncremented = false;
try {
    inFlightCounter.incrementAndGet();
    inFlightIncremented = true;
    chain.doFilter(request, response);
} catch (Exception e) {
    recordMetrics(request, response, startTime); // Record before re-throwing
    throw e;
} finally {
    recordMetrics(request, response, startTime);
    if (inFlightIncremented) {
        inFlightCounter.decrementAndGet();
    }
}
```

**Error Scenarios Handled**:
1. **Servlet Exceptions**: Record metrics before propagating exception
2. **Metric Recording Failures**: Log error but continue request processing
3. **Path Normalization Failures**: Fall back to "/unknown" path
4. **Timer Cache Issues**: Create new Timer if cache fails

### Logging Strategy

- **Metric Failures**: WARN level with request context
- **Path Normalization Issues**: DEBUG level to avoid log spam
- **Filter Registration**: INFO level for startup verification
- **OTLP Export Issues**: DEBUG level (configured in application.properties)

## Testing Strategy

### Unit Testing Approach

**Test Categories**:
1. **Metric Recording Tests**: Verify counter, timer, and gauge behavior
2. **Path Normalization Tests**: Test various URL patterns and edge cases
3. **Exception Handling Tests**: Verify metrics recorded during exceptions
4. **Concurrency Tests**: Test thread safety and in-flight tracking
5. **Timer Cache Tests**: Verify caching behavior and performance

**Key Test Scenarios** (from implementation guide):
```java
@Test
void testInFlightTracking() {
    // Verify in-flight counter increments during processing
    doAnswer(invocation -> {
        assertEquals(1, inFlightRequestsCounter.get());
        return null;
    }).when(filterChain).doFilter(request, response);
}

@Test
void testDoFilter_WithException() {
    // Verify metrics recorded even when exceptions occur
    doThrow(new ServletException("Test exception"))
        .when(filterChain).doFilter(request, response);
    
    assertThrows(ServletException.class, () -> {
        httpMetricsFilter.doFilter(request, response, filterChain);
    });
    
    assertEquals(0, inFlightRequestsCounter.get());
    assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
}
```

### Integration Testing

**Validation Points**:
1. **Actuator Endpoint**: Verify `/actuator/prometheus` returns expected metrics
2. **OTLP Export**: Confirm metrics exported to OpenTelemetry Collector
3. **Real Request Processing**: Test with actual HTTP requests to pricing endpoints
4. **Performance Under Load**: Verify Timer caching effectiveness

## Performance Considerations

### Optimization Strategies

**1. Timer Instance Caching**
- **Problem**: Creating new Timer instances for each request causes significant overhead
- **Solution**: Cache Timer instances using ConcurrentHashMap with composite keys
- **Impact**: 60% CPU overhead reduction (proven in implementation guide)

**2. Efficient Path Normalization**
- **Problem**: Complex regex operations on every request
- **Solution**: Ordered pattern matching (UUID first, then numeric IDs)
- **Optimization**: Use Spring MVC handler mapping when available

**3. Minimal Memory Allocation**
- **Problem**: String concatenation and object creation in hot path
- **Solution**: Reuse StringBuilder, cache normalized values where possible
- **Monitoring**: Track Timer cache size for potential memory leaks

**4. Exception Path Optimization**
- **Problem**: Exception handling adds overhead to normal request flow
- **Solution**: Minimize try-catch scope, use boolean flags for state tracking

### Memory Management

**Timer Cache Considerations**:
- **Growth Pattern**: Cache grows with unique method/path/status combinations
- **Cardinality Control**: Path normalization prevents unbounded growth
- **Monitoring**: Log cache size periodically in production
- **Future Enhancement**: Implement LRU eviction if needed

## Configuration Requirements

### Build Dependencies

**Required Additions to build.gradle**:
```gradle
// Already present - verify versions
implementation 'io.micrometer:micrometer-registry-otlp'
implementation 'io.opentelemetry:opentelemetry-exporter-otlp:1.38.0'
implementation 'io.micrometer:micrometer-tracing-bridge-otel'

// Add Prometheus registry for direct scraping
implementation 'io.micrometer:micrometer-registry-prometheus'
```

### Application Properties Updates

**Required Additions**:
```properties
# Enable Prometheus endpoint (add to existing actuator exposure)
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true

# OTLP configuration already present - verify settings
management.otlp.metrics.export.url=http://otel-collector-collector.monitoring.svc.cluster.local:4318/v1/metrics
management.otlp.metrics.export.step=1m
```

### Spring Boot Integration

**Filter Registration Priority**: Order=1 to ensure metrics capture all requests before other filters
**Actuator Integration**: Leverage existing actuator configuration
**MeterRegistry**: Use Spring Boot's auto-configured registry

## Deployment Considerations

### Kubernetes Environment

**Service Discovery**: Use fully qualified service names for OTLP endpoints (already configured)
**Resource Attributes**: Include namespace information for multi-tenant environments (already configured)
**Health Checks**: Ensure metrics don't interfere with existing health check endpoints

### Monitoring Integration

**Prometheus Scraping**: `/actuator/prometheus` endpoint ready for scraping
**OpenTelemetry Pipeline**: Metrics exported via existing OTLP configuration
**Grafana Dashboards**: Metrics compatible with standard HTTP monitoring dashboards

### Troubleshooting Support

**Validation Commands**:
```bash
# Check metrics availability
curl http://localhost:8083/actuator/prometheus | grep http_requests

# Generate sample metrics
curl http://localhost:8083/api/v1/prices

# Verify metrics recorded
curl http://localhost:8083/actuator/prometheus | grep http_requests
```

**Debug Configuration**: OTLP debug logging already enabled in application.properties

## Security Considerations

**Metric Data Sensitivity**: HTTP metrics contain path information but no sensitive data
**Endpoint Exposure**: Prometheus endpoint exposes system metrics - ensure proper network security
**Label Cardinality**: Path normalization prevents potential DoS via cardinality explosion
**Error Information**: Avoid exposing sensitive error details in metric labels

## Migration Strategy

**Zero-Downtime Deployment**: Filter registration doesn't require service restart
**Backward Compatibility**: New metrics don't affect existing functionality
**Rollback Plan**: Filter can be disabled via configuration if issues arise
**Monitoring**: Verify metrics appear in dashboards after deployment