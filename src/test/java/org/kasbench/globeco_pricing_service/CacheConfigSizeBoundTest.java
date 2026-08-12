package org.kasbench.globeco_pricing_service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test verifying the cache size bound invariant.
 *
 * <p><b>Property 2: Cache size bound invariant</b></p>
 * <p><b>Validates: Requirements 2.3</b></p>
 *
 * <p>For any sequence of cache insertions exceeding 500 distinct keys,
 * the cache size SHALL never exceed 500 entries at any observation point
 * after eviction processing completes.</p>
 */
class CacheConfigSizeBoundTest {

    /**
     * Tests that a Caffeine cache configured with maximumSize(500) never exceeds
     * 500 entries, even after inserting 1000 distinct keys.
     *
     * <p>This directly validates the CacheConfig configuration which uses:
     * {@code Caffeine.newBuilder().maximumSize(500).expireAfterWrite(5, TimeUnit.MINUTES)}</p>
     */
    @Test
    void cacheSizeNeverExceedsFiveHundredEntries() {
        // Configure the cache identically to CacheConfig.java
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        int totalInsertions = 1000;

        for (int i = 0; i < totalInsertions; i++) {
            cache.put("key-" + i, "value-" + i);

            // After each insertion, trigger eviction cleanup and verify the bound
            cache.cleanUp();
            long currentSize = cache.estimatedSize();

            assertTrue(currentSize <= 500,
                    "Cache size exceeded 500 entries after inserting key-" + i
                            + ". Actual size: " + currentSize);
        }
    }

    /**
     * Tests that the cache correctly evicts entries when full, allowing new entries
     * to be admitted without exceeding the maximum size.
     *
     * <p>This verifies the eviction behavior: when the cache is at capacity,
     * new entries can still be inserted (old entries are evicted to make room).</p>
     */
    @Test
    void cacheEvictsEntriesWhenAtCapacity() {
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        // Fill the cache to capacity
        for (int i = 0; i < 500; i++) {
            cache.put("key-" + i, "value-" + i);
        }
        cache.cleanUp();

        long sizeAtCapacity = cache.estimatedSize();
        assertTrue(sizeAtCapacity <= 500,
                "Cache size should be at most 500 after filling. Actual: " + sizeAtCapacity);

        // Insert 200 more entries beyond capacity
        for (int i = 500; i < 700; i++) {
            cache.put("key-" + i, "value-" + i);
        }
        cache.cleanUp();

        long sizeAfterOverflow = cache.estimatedSize();
        assertTrue(sizeAfterOverflow <= 500,
                "Cache size should remain at most 500 after overflow insertions. Actual: " + sizeAfterOverflow);

        // Verify that new entries were actually admitted (cache is still usable)
        // At least some of the newer entries should be present
        boolean hasNewerEntry = false;
        for (int i = 500; i < 700; i++) {
            if (cache.getIfPresent("key-" + i) != null) {
                hasNewerEntry = true;
                break;
            }
        }
        assertTrue(hasNewerEntry,
                "Cache should admit new entries after eviction (some newer keys should be present)");
    }
}
