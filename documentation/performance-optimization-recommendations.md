# Performance Optimization Recommendations for Globeco Pricing Service

## 1. Introduction

This document provides detailed recommendations to reduce the CPU and memory footprint of the Globeco Pricing Service. The analysis has identified several areas for improvement, primarily related to data loading, caching, and data access patterns.

## 2. Analysis of Performance Issues

The high CPU and memory consumption of the microservice can be attributed to the following key factors:

*   **Inefficient Data Loading:** The Flyway database migration (`V2__LoadPricingData.java`) reads a large compressed CSV file (`prices.csv.gz`) into memory to filter data for a specific date. This process is highly inefficient and consumes significant CPU and I/O resources. 

*   **Lack of Cache Size Limits:** The Caffeine cache is configured with a time-based expiration policy but lacks a size-based limit. This allows the cache to grow indefinitely, potentially leading to out-of-memory errors.
*   **Inefficient Data Transfer:** The application fetches complete `Price` entities from the database, even when only a subset of the data is required. This results in unnecessary data transfer between the application and the database.

## 3. Recommendations for Optimization

To address the identified performance issues, the following optimizations are recommended:

### 3.1. Optimize the Database Migration

The current data loading process in `V2__LoadPricingData.java` is a major performance bottleneck. To optimize it, consider the following changes:

* Load a static data set from [selected_prices.csv](../src/main/resources/static/selected_prices.csv).  Load the same set of prices every run.  

* Make the static load as efficient as possible.  This might include creating a static file with bulk insert statements to load the prices or using Postgres's `copy` utility.  The focus should be on speed and lowering the memory footprint.



### 3.2. Tune the Cache Configuration

The current cache configuration can be improved to prevent excessive memory consumption:

*   **Set a Maximum Cache Size:** Configure a maximum size for the Caffeine cache to prevent it from growing uncontrollably. This can be done by adding `.maximumSize(long)` to the `Caffeine.newBuilder()` configuration. The optimal size will depend on the application's usage patterns and available memory.
*   **Adjust Eviction Policies:** Evaluate the cache eviction policy. While the current time-based expiration is a reasonable default, a size-based eviction policy or a combination of both might be more effective in managing memory usage.

### 3.4. Remove Unused Dependencies

The `build.gradle` file includes the `commons-math3` library, but it is not used anywhere in the codebase. This is a "zombie dependency" that unnecessarily increases the size of the application artifact and can introduce security vulnerabilities.

**Recommendation:** Remove the `org.apache.commons:commons-math3` dependency from the `build.gradle` file.


## 4. Conclusion

By implementing the recommendations outlined in this document, the CPU and memory footprint of the Globeco Pricing Service can be significantly reduced. The key is to adopt more efficient data loading and access strategies, and to configure the cache to prevent excessive memory consumption. These changes will not only improve the performance of the application but also make it more robust and scalable. It is recommended to implement these changes incrementally and to monitor the application's performance after each change to verify the impact.
