# Performance Optimization Recommendations for Globeco Pricing Service

## 1. Introduction

This document provides detailed recommendations to reduce the CPU and memory footprint of the Globeco Pricing Service. The analysis has identified several areas for improvement, primarily related to data loading, caching, and data access patterns.

## 2. Analysis of Performance Issues

The high CPU and memory consumption of the microservice can be attributed to the following key factors:

*   **Inefficient Data Loading:** The Flyway database migration (`V2__LoadPricingData.java`) reads a large compressed CSV file (`prices.csv.gz`) into memory to filter data for a specific date. This process is highly inefficient and consumes significant CPU and I/O resources.
*   **Unbounded Caching and Memory Consumption:** The `PriceServiceImpl` class contains a method `getAllPrices()` that retrieves all price records from the database and stores them in an in-memory cache. This is a major source of memory consumption, as the entire `price` table is loaded into memory.
*   **Lack of Cache Size Limits:** The Caffeine cache is configured with a time-based expiration policy but lacks a size-based limit. This allows the cache to grow indefinitely, potentially leading to out-of-memory errors.
*   **Inefficient Data Transfer:** The application fetches complete `Price` entities from the database, even when only a subset of the data is required. This results in unnecessary data transfer between the application and the database.

## 3. Recommendations for Optimization

To address the identified performance issues, the following optimizations are recommended:

### 3.1. Optimize the Database Migration

The current data loading process in `V2__LoadPricingData.java` is a major performance bottleneck. To optimize it, consider the following changes:

*   **Use a Temporary Table for Staging:** Instead of reading the entire CSV file in the Java migration, load the data into a temporary staging table in the database. This can be done using the `COPY` command in PostgreSQL, which is highly optimized for this purpose.
*   **Perform Filtering in the Database:** Once the data is in the staging table, use SQL to filter and insert the required data into the `price` table. This will be significantly faster and more memory-efficient than performing the filtering in Java.
*   **Adopt a Deterministic Approach:** The current practice of selecting a random date for data loading makes the database state non-deterministic. For a production environment, it is crucial to have a repeatable and predictable migration process. Consider loading data for all dates or a specific, fixed date.

### 3.2. Refactor the Caching and Data Access Layer

The `getAllPrices()` method and its associated caching are the primary cause of high memory usage. The following changes are recommended:

*   **Remove the `getAllPrices()` Method:** The `getAllPrices()` method, which loads the entire `price` table into memory, should be removed. If there is a business requirement to access all prices, it should be implemented using a paginated or streaming approach.
*   **Implement Pagination:** For use cases that require browsing through a large number of prices, implement pagination. Spring Data JPA provides excellent support for pagination through the `PagingAndSortingRepository`.
*   **Use Database Projections:** To reduce data transfer and memory usage, use Spring Data JPA projections to fetch only the required fields from the database. This avoids fetching the entire `Price` entity when only a subset of its attributes is needed.

### 3.3. Tune the Cache Configuration

The current cache configuration can be improved to prevent excessive memory consumption:

*   **Set a Maximum Cache Size:** Configure a maximum size for the Caffeine cache to prevent it from growing uncontrollably. This can be done by adding `.maximumSize(long)` to the `Caffeine.newBuilder()` configuration. The optimal size will depend on the application's usage patterns and available memory.
*   **Adjust Eviction Policies:** Evaluate the cache eviction policy. While the current time-based expiration is a reasonable default, a size-based eviction policy or a combination of both might be more effective in managing memory usage.

### 3.4. Remove Unused Dependencies

The `build.gradle` file includes the `commons-math3` library, but it is not used anywhere in the codebase. This is a "zombie dependency" that unnecessarily increases the size of the application artifact and can introduce security vulnerabilities.

**Recommendation:** Remove the `org.apache.commons:commons-math3` dependency from the `build.gradle` file.


## 4. Conclusion

By implementing the recommendations outlined in this document, the CPU and memory footprint of the Globeco Pricing Service can be significantly reduced. The key is to adopt more efficient data loading and access strategies, and to configure the cache to prevent excessive memory consumption. These changes will not only improve the performance of the application but also make it more robust and scalable. It is recommended to implement these changes incrementally and to monitor the application's performance after each change to verify the impact.
