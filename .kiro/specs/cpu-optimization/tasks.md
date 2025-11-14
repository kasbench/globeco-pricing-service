# Implementation Plan

- [x] 1. Optimize random sampling in PriceController
  - Replace Apache Commons Math3 NormalDistribution with Java's Random.nextGaussian()
  - Add ThreadLocal<Random> field for thread-safe random number generation
  - Update toSampledDto() method to use simple gaussian transformation
  - Add error handling with fallback to mean price
  - Remove NormalDistribution import
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 2. Simplify HttpMetricsFilter path normalization
- [ ] 2.1 Replace regex-based path normalization with string operations
  - Remove Pattern constants (UUID_PATTERN, NUMERIC_ID_PATTERN, SINGLE_DIGIT_ID_PATTERN)
  - Implement new normalizePathParameters() using string splitting
  - Maintain same normalization behavior for UUID and numeric ID detection
  - _Requirements: 2.1, 2.2, 2.4_

- [ ] 2.2 Reduce logging overhead in HttpMetricsFilter
  - Remove DEBUG-level logging in request processing path
  - Keep only ERROR-level logging for actual failures
  - Remove redundant context logging
  - _Requirements: 2.3, 2.4_

- [ ] 3. Optimize logging configuration
  - Update application.properties to set Micrometer OTLP logging to INFO with environment variable override
  - Update application.properties to set OpenTelemetry OTLP logging to INFO with environment variable override
  - Document environment variables for temporary DEBUG enabling
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 4. Configure JPA and Hibernate optimization
  - Add hibernate.jdbc.batch_size configuration
  - Add hibernate.order_inserts and hibernate.order_updates configuration
  - Add hibernate.jdbc.fetch_size configuration
  - Disable SQL logging (show-sql, format_sql, use_sql_comments)
  - _Requirements: 4.1, 4.2, 4.3, 4.4_


- [ ] 5. Configure HikariCP connection pool
  - Add maximum-pool-size configuration (5 connections per replica)
  - Add minimum-idle configuration (2 connections)
  - Add connection-timeout configuration (20 seconds)
  - Add idle-timeout configuration (5 minutes)
  - Add max-lifetime configuration (30 minutes)
  - Add connection-test-query configuration
  - Document scaling considerations for 20 replicas
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 6. Add pagination support to prices endpoint
- [ ] 6.1 Update PriceController to support pagination parameters
  - Add page and size request parameters with defaults (page=0, size=100)
  - Change return type from List<PriceDto> to Page<PriceDto>
  - Add pagination parameter validation
  - _Requirements: 6.1, 6.2_

- [ ] 6.2 Update PriceService to support pagination
  - Add getAllPrices(Pageable) method signature
  - Update implementation to use repository pagination
  - _Requirements: 6.1, 6.2_

- [ ] 6.3 Update PriceRepository for pagination
  - Verify JpaRepository provides findAll(Pageable) method
  - No code changes needed (already provided by JpaRepository)
  - _Requirements: 6.1_

- [ ] 6.4 Consider parallel processing for pagination
  - Evaluate if parallel stream processing improves performance
  - Implement if beneficial without increasing resource consumption
  - _Requirements: 6.3, 6.4_

- [ ] 7. Remove or profile-gate MetricsDebugConfig
  - Add @Profile("dev") annotation to MetricsDebugConfig class
  - Document that debug metrics only run in dev profile
  - _Requirements: 7.1, 7.2, 7.3_

- [ ] 8. Update Kubernetes deployment configuration
  - Update CPU request to 150m (from 1000m)
  - Update CPU limit to 300m (from 2000m)
  - Update memory request to 512Mi (from 2000Mi)
  - Update memory limit to 1Gi (from 4000Mi)
  - Document rollback plan in deployment
  - _Requirements: 8.1, 8.2_

- [ ]* 9. Validate optimizations with tests
- [ ]* 9.1 Create unit tests for random sampling
  - Test that sampled values follow expected distribution
  - Test ThreadLocal Random thread safety
  - Test error handling fallback
  - _Requirements: 8.4_

- [ ]* 9.2 Create unit tests for path normalization
  - Test that string-based normalization produces same results
  - Test UUID detection (36 chars with hyphens)
  - Test numeric ID detection
  - _Requirements: 8.4_

- [ ]* 9.3 Create unit tests for pagination
  - Test default pagination values
  - Test pagination boundaries (first page, last page)
  - Test pagination parameter validation
  - _Requirements: 8.4_

- [ ]* 9.4 Run integration tests
  - Test end-to-end performance of optimized endpoints
  - Verify metrics are still collected correctly
  - Test database connection pool under load
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ]* 9.5 Perform load testing
  - Test single instance with 100 concurrent requests
  - Test 20 replicas with 2000 concurrent requests
  - Test sustained load (50 req/sec for 10 minutes)
  - Validate CPU usage ≤ 150m and response time <100ms p95
  - _Requirements: 8.1, 8.2, 8.3_
