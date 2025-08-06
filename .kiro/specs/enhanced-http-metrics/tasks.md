# Implementation Plan

- [x] 1. Update build dependencies and application configuration
  - Add Micrometer Prometheus registry dependency to build.gradle
  - Update application.properties to enable Prometheus endpoint and verify OTLP configuration
  - Verify existing OpenTelemetry dependencies are compatible with implementation guide versions
  - _Requirements: 2.1, 2.3_

- [x] 2. Create HTTP metrics configuration infrastructure
  - Create HttpMetricsConfiguration class with AtomicInteger bean for in-flight counter
  - Register http_requests_in_flight Gauge with MeterRegistry using AtomicInteger reference
  - Configure FilterRegistrationBean with high priority (order=1) and URL pattern "/*"
  - _Requirements: 1.3, 1.4, 4.4_

- [x] 3. Implement core HttpMetricsFilter with Timer caching
  - Create HttpMetricsFilter class implementing jakarta.servlet.Filter interface
  - Implement ConcurrentHashMap-based Timer cache with composite key strategy from implementation guide
  - Add histogram bucket configuration using Duration.ofMillis() values: [5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000]
  - Implement basic doFilter method with timing and in-flight counter management
  - _Requirements: 1.1, 1.2, 4.1, 4.5_

- [ ] 4. Implement sophisticated path normalization
  - Create path normalization method with multiple regex patterns for UUID and numeric ID replacement
  - Implement Spring MVC HandlerMapping integration to prefer BEST_MATCHING_PATTERN_ATTRIBUTE
  - Add context-aware single digit ID replacement for API endpoints only
  - Include fallback to "/unknown" for normalization failures as documented in implementation guide
  - _Requirements: 3.3, 4.2_

- [ ] 5. Implement robust exception handling and state tracking
  - Add boolean flag tracking for in-flight increment state to prevent double-decrement scenarios
  - Implement try-catch-finally pattern that records metrics even during exceptions
  - Add comprehensive error logging with request context information
  - Ensure metrics recording failures don't interfere with request processing
  - _Requirements: 3.2, 4.4_

- [ ] 6. Add metric recording methods with label normalization
  - Implement recordMetrics method that extracts and normalizes method, path, and status labels
  - Add counter increment logic for http_requests_total with proper labels
  - Implement Timer recording in milliseconds using cached Timer instances
  - Add label value normalization methods (uppercase methods, string status codes)
  - _Requirements: 1.1, 1.2, 1.5_

- [ ] 7. Create comprehensive unit tests for metrics functionality
  - Write tests for successful request processing with metric verification
  - Create tests for in-flight counter tracking during request processing
  - Add exception handling tests that verify metrics are recorded even when exceptions occur
  - Implement path normalization tests for various URL patterns (IDs, UUIDs, complex routes)
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 8. Add concurrency and performance tests
  - Create thread safety tests for concurrent request processing
  - Write Timer cache performance tests to verify caching effectiveness
  - Add tests for high-cardinality scenarios to ensure path normalization prevents explosion
  - Implement load testing scenarios to validate Timer caching reduces CPU overhead
  - _Requirements: 5.5, 4.1_

- [ ] 9. Create integration tests for metrics endpoints
  - Write tests that verify /actuator/prometheus endpoint returns expected HTTP metrics
  - Create tests that make actual HTTP requests to pricing service endpoints and verify metrics
  - Add tests for various HTTP methods (GET, POST) and status codes (200, 404, 500)
  - Verify metric labels contain correct normalized values for real requests
  - _Requirements: 2.1, 2.2, 3.1_

- [ ] 10. Add validation and troubleshooting support
  - Create metric validation utility methods for testing and debugging
  - Add logging configuration for metric registration and export debugging
  - Implement health check integration to ensure metrics don't interfere with existing endpoints
  - Create documentation comments with troubleshooting commands from implementation guide
  - _Requirements: 2.4, 3.1_