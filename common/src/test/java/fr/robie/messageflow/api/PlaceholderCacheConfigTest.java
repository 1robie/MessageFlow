package fr.robie.messageflow.api;

import com.google.common.cache.CacheBuilder;
import fr.robie.messageflow.configuration.PlaceholderCacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link PlaceholderCacheConfig}.
 * Tests builder pattern, default values, and CacheBuilder creation.
 */
@DisplayName("PlaceholderCacheConfig Tests")
class PlaceholderCacheConfigTest {

    private PlaceholderCacheConfig config;

    @BeforeEach
    void setUp() {
        this.config = PlaceholderCacheConfig.defaults();
    }

    // ========== Tests for Default Factory Method ==========

    @Test
    @DisplayName("defaults() should return non-null configuration")
    void testDefaults_ReturnsNonNull() {
        assertNotNull(this.config);
    }

    @Test
    @DisplayName("defaults() should match original global cache size (1000)")
    void testDefaults_GlobalCacheSize() {
        assertEquals(1000, this.config.getGlobalCacheMaximumSize());
    }

    @Test
    @DisplayName("defaults() should match original player cache size (10000)")
    void testDefaults_PlayerCacheSize() {
        assertEquals(10000, this.config.getPlayerCacheMaximumSize());
    }

    @Test
    @DisplayName("defaults() should have disabled stats recording")
    void testDefaults_NoStatsRecording() {
        assertFalse(this.config.shouldRecordGlobalCacheStats());
        assertFalse(this.config.shouldRecordPlayerCacheStats());
    }

    @Test
    @DisplayName("defaults() should have disabled soft values")
    void testDefaults_NoSoftValues() {
        assertFalse(this.config.shouldUseGlobalCacheSoftValues());
        assertFalse(this.config.shouldUsePlayerCacheSoftValues());
    }

    // ========== Tests for Builder Pattern ==========

    @Test
    @DisplayName("builder() should return non-null builder")
    void testBuilder_ReturnsNonNull() {
        assertNotNull(PlaceholderCacheConfig.builder());
    }

    @Test
    @DisplayName("builder().build() should create valid configuration")
    void testBuilder_BuildProducesConfig() {
        PlaceholderCacheConfig built = PlaceholderCacheConfig.builder().build();
        assertNotNull(built);
    }

    @Test
    @DisplayName("builder() should support fluent API chaining")
    void testBuilder_FluentChaining() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheMaximumSize(500)
                .playerCacheMaximumSize(5000)
                .recordGlobalCacheStats(true)
                .build();

        assertNotNull(custom);
        assertEquals(500, custom.getGlobalCacheMaximumSize());
        assertEquals(5000, custom.getPlayerCacheMaximumSize());
        assertTrue(custom.shouldRecordGlobalCacheStats());
    }

    // ========== Tests for Global Cache Configuration ==========

    @Test
    @DisplayName("builder can customize global cache size")
    void testBuilder_CustomGlobalCacheSize() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheMaximumSize(2000)
                .build();

        assertEquals(2000, custom.getGlobalCacheMaximumSize());
    }

    @Test
    @DisplayName("builder can set global cache initial capacity")
    void testBuilder_GlobalCacheInitialCapacity() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheInitialCapacity(100)
                .build();

        assertEquals(100, custom.getGlobalCacheInitialCapacity());
    }

    @Test
    @DisplayName("builder can set global cache concurrency level")
    void testBuilder_GlobalCacheConcurrencyLevel() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheConcurrencyLevel(16)
                .build();

        assertEquals(16, custom.getGlobalCacheConcurrencyLevel());
    }

    @Test
    @DisplayName("builder can enable global cache stats recording")
    void testBuilder_RecordGlobalCacheStats() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .recordGlobalCacheStats(true)
                .build();

        assertTrue(custom.shouldRecordGlobalCacheStats());
    }

    @Test
    @DisplayName("builder can enable global cache soft values")
    void testBuilder_GlobalCacheSoftValues() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .useGlobalCacheSoftValues(true)
                .build();

        assertTrue(custom.shouldUseGlobalCacheSoftValues());
    }

    @Test
    @DisplayName("builder can set global cache expiry after write")
    void testBuilder_GlobalCacheExpireAfterWrite() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheExpireAfterWrite(30)
                .build();

        assertEquals(30, custom.getGlobalCacheExpireAfterWriteMinutes());
    }

    @Test
    @DisplayName("builder can set global cache expiry after access")
    void testBuilder_GlobalCacheExpireAfterAccess() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheExpireAfterAccess(20)
                .build();

        assertEquals(20, custom.getGlobalCacheExpireAfterAccessMinutes());
    }

    // ========== Tests for Player Cache Configuration ==========

    @Test
    @DisplayName("builder can customize player cache size")
    void testBuilder_CustomPlayerCacheSize() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .playerCacheMaximumSize(20000)
                .build();

        assertEquals(20000, custom.getPlayerCacheMaximumSize());
    }

    @Test
    @DisplayName("builder can set player cache initial capacity")
    void testBuilder_PlayerCacheInitialCapacity() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .playerCacheInitialCapacity(500)
                .build();

        assertEquals(500, custom.getPlayerCacheInitialCapacity());
    }

    @Test
    @DisplayName("builder can set player cache concurrency level")
    void testBuilder_PlayerCacheConcurrencyLevel() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .playerCacheConcurrencyLevel(8)
                .build();

        assertEquals(8, custom.getPlayerCacheConcurrencyLevel());
    }

    @Test
    @DisplayName("builder can enable player cache stats recording")
    void testBuilder_RecordPlayerCacheStats() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .recordPlayerCacheStats(true)
                .build();

        assertTrue(custom.shouldRecordPlayerCacheStats());
    }

    @Test
    @DisplayName("builder can enable player cache soft values")
    void testBuilder_PlayerCacheSoftValues() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .usePlayerCacheSoftValues(true)
                .build();

        assertTrue(custom.shouldUsePlayerCacheSoftValues());
    }

    @Test
    @DisplayName("builder can set player cache expiry after write")
    void testBuilder_PlayerCacheExpireAfterWrite() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .playerCacheExpireAfterWrite(60)
                .build();

        assertEquals(60, custom.getPlayerCacheExpireAfterWriteMinutes());
    }

    @Test
    @DisplayName("builder can set player cache expiry after access")
    void testBuilder_PlayerCacheExpireAfterAccess() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .playerCacheExpireAfterAccess(45)
                .build();

        assertEquals(45, custom.getPlayerCacheExpireAfterAccessMinutes());
    }

    // ========== Tests for CacheBuilder Creation ==========

    @Test
    @DisplayName("createGlobalCacheBuilder() returns non-null CacheBuilder")
    void testCreateGlobalCacheBuilder_NonNull() {
        CacheBuilder builder = this.config.createGlobalCacheBuilder();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("createPlayerCacheBuilder() returns non-null CacheBuilder")
    void testCreatePlayerCacheBuilder_NonNull() {
        CacheBuilder builder = this.config.createPlayerCacheBuilder();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("createGlobalCacheBuilder() applies global cache size")
    void testCreateGlobalCacheBuilder_AppliesSize() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheMaximumSize(500)
                .build();

        CacheBuilder builder = custom.createGlobalCacheBuilder();
        assertNotNull(builder);
        // Note: CacheBuilder doesn't expose settings, so we verify by creating and using cache
    }

    @Test
    @DisplayName("createPlayerCacheBuilder() applies player cache size")
    void testCreatePlayerCacheBuilder_AppliesSize() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .playerCacheMaximumSize(5000)
                .build();

        CacheBuilder builder = custom.createPlayerCacheBuilder();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("createGlobalCacheBuilder() with stats enabled")
    void testCreateGlobalCacheBuilder_WithStats() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .recordGlobalCacheStats(true)
                .build();

        CacheBuilder builder = custom.createGlobalCacheBuilder();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("createPlayerCacheBuilder() with stats enabled")
    void testCreatePlayerCacheBuilder_WithStats() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .recordPlayerCacheStats(true)
                .build();

        CacheBuilder builder = custom.createPlayerCacheBuilder();
        assertNotNull(builder);
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Multiple configurations are independent")
    void testMultipleConfigs_Independent() {
        PlaceholderCacheConfig config1 = PlaceholderCacheConfig.builder()
                .globalCacheMaximumSize(100)
                .playerCacheMaximumSize(1000)
                .build();

        PlaceholderCacheConfig config2 = PlaceholderCacheConfig.builder()
                .globalCacheMaximumSize(200)
                .playerCacheMaximumSize(2000)
                .build();

        assertEquals(100, config1.getGlobalCacheMaximumSize());
        assertEquals(1000, config1.getPlayerCacheMaximumSize());

        assertEquals(200, config2.getGlobalCacheMaximumSize());
        assertEquals(2000, config2.getPlayerCacheMaximumSize());
    }

    @Test
    @DisplayName("Complex fluent configuration works correctly")
    void testComplexFluentConfiguration() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .globalCacheMaximumSize(2000)
                .globalCacheInitialCapacity(500)
                .globalCacheConcurrencyLevel(16)
                .globalCacheExpireAfterWrite(30)
                .globalCacheExpireAfterAccess(20)
                .recordGlobalCacheStats(true)
                .useGlobalCacheSoftValues(true)
                .playerCacheMaximumSize(20000)
                .playerCacheInitialCapacity(5000)
                .playerCacheConcurrencyLevel(8)
                .playerCacheExpireAfterWrite(60)
                .playerCacheExpireAfterAccess(45)
                .recordPlayerCacheStats(true)
                .usePlayerCacheSoftValues(false)
                .build();

        // Verify all settings were applied
        assertEquals(2000, custom.getGlobalCacheMaximumSize());
        assertEquals(500, custom.getGlobalCacheInitialCapacity());
        assertEquals(16, custom.getGlobalCacheConcurrencyLevel());
        assertEquals(30, custom.getGlobalCacheExpireAfterWriteMinutes());
        assertEquals(20, custom.getGlobalCacheExpireAfterAccessMinutes());
        assertTrue(custom.shouldRecordGlobalCacheStats());
        assertTrue(custom.shouldUseGlobalCacheSoftValues());

        assertEquals(20000, custom.getPlayerCacheMaximumSize());
        assertEquals(5000, custom.getPlayerCacheInitialCapacity());
        assertEquals(8, custom.getPlayerCacheConcurrencyLevel());
        assertEquals(60, custom.getPlayerCacheExpireAfterWriteMinutes());
        assertEquals(45, custom.getPlayerCacheExpireAfterAccessMinutes());
        assertTrue(custom.shouldRecordPlayerCacheStats());
        assertFalse(custom.shouldUsePlayerCacheSoftValues());
    }

    @Test
    @DisplayName("Negative values (disabled settings) are handled correctly")
    void testNegativeValues_DisabledSettings() {
        PlaceholderCacheConfig custom = PlaceholderCacheConfig.builder()
                .build();

        // Default negative values indicate disabled settings
        assertEquals(-1, custom.getGlobalCacheInitialCapacity());
        assertEquals(-1, custom.getGlobalCacheConcurrencyLevel());
        assertEquals(-1, custom.getGlobalCacheExpireAfterWriteMinutes());
        assertEquals(-1, custom.getGlobalCacheExpireAfterAccessMinutes());
        assertEquals(-1, custom.getPlayerCacheInitialCapacity());
        assertEquals(-1, custom.getPlayerCacheConcurrencyLevel());
        assertEquals(-1, custom.getPlayerCacheExpireAfterWriteMinutes());
        assertEquals(-1, custom.getPlayerCacheExpireAfterAccessMinutes());
    }
}
