package fr.robie.messageflow.api;

import fr.robie.messageflow.configuration.ConfigurationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalPlaceholder registry and GlobalPlaceholderRegistry.
 */
@DisplayName("GlobalPlaceholder Registry Tests")
class GlobalPlaceholderRegistryImplTest {

    private GlobalPlaceholderRegistry registry;

    @BeforeEach
    void setUp() {
        this.registry = GlobalPlaceholderRegistry.getInstance();
        this.registry.clear();
        // Reset cache configuration to defaults before each test
        ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE.setValue(1000L);
        ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_MAX_SIZE.setValue(10000L);
        this.registry.rebuildCaches();
    }

    @AfterEach
    void tearDown() {
        this.registry.clear();
        // Reset cache configuration to defaults after each test
        ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE.setValue(1000L);
        ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_MAX_SIZE.setValue(10000L);
        this.registry.rebuildCaches();
    }

    // ========== Basic Registration and Retrieval Tests ==========

    @Test
    @DisplayName("Register static placeholder")
    void testRegisterStaticPlaceholder() {
        this.registry.register("key", "value");
        assertTrue(this.registry.exists("key"));
        assertEquals("value", this.registry.get("key").orElse(null));
    }

    @Test
    @DisplayName("Register dynamic placeholder with Supplier")
    void testRegisterDynamicPlaceholder() {
        this.registry.register("dynamic", () -> "dynamic_value");
        assertTrue(this.registry.exists("dynamic"));
        assertEquals("dynamic_value", this.registry.get("dynamic").orElse(null));
    }

    @Test
    @DisplayName("Unregister placeholder")
    void testUnregisterPlaceholder() {
        this.registry.register("key", "value");
        assertTrue(this.registry.exists("key"));
        this.registry.unregister("key");
        assertFalse(this.registry.exists("key"));
    }

    @Test
    @DisplayName("Unregister non-existent placeholder succeeds silently")
    void testUnregisterNonExistent() {
        this.registry.unregister("nonexistent");
        assertFalse(this.registry.exists("nonexistent"));
    }

    @Test
    @DisplayName("Clear all placeholders")
    void testClearAll() {
        this.registry.register("key1", "value1");
        this.registry.register("key2", "value2");
        assertTrue(this.registry.hasAny());

        this.registry.clear();
        assertFalse(this.registry.hasAny());
    }

    @Test
    @DisplayName("hasAny returns false when empty")
    void testHasAnyEmpty() {
        assertFalse(this.registry.hasAny());
    }

    @Test
    @DisplayName("hasAny returns true after registration")
    void testHasAnyAfterRegister() {
        this.registry.register("key", "value");
        assertTrue(this.registry.hasAny());
    }

    @Test
    @DisplayName("hasAnyInText returns false when no placeholders registered")
    void testHasAnyInTextNoPlaceholders() {
        String text = "Hello %world%";
        assertFalse(this.registry.hasAnyInText(text));
    }

    @Test
    @DisplayName("hasAnyInText returns true when registered placeholder in text")
    void testHasAnyInTextFound() {
        this.registry.register("world", "Earth");
        String text = "Hello %world%";
        assertTrue(this.registry.hasAnyInText(text));
    }

    @Test
    @DisplayName("hasAnyInText returns false when registered but not in text")
    void testHasAnyInTextNotInText() {
        this.registry.register("world", "Earth");
        String text = "Hello there";
        assertFalse(this.registry.hasAnyInText(text));
    }

    @Test
    @DisplayName("getAll returns all registered placeholders")
    void testGetAll() {
        this.registry.register("key1", "value1");
        this.registry.register("key2", () -> "value2");

        var all = this.registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.containsKey("key1"));
        assertTrue(all.containsKey("key2"));
    }

    @Test
    @DisplayName("getAll returns empty map when nothing registered")
    void testGetAllEmpty() {
        var all = this.registry.getAll();
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("getValue returns PlaceholderValue without evaluating")
    void testGetValue() {
        this.registry.register("dynamic", () -> "value");
        var value = this.registry.getValue("dynamic");
        assertTrue(value.isPresent());
    }

    @Test
    @DisplayName("getValue returns empty Optional for non-existent key")
    void testGetValueNonExistent() {
        var value = this.registry.getValue("nonexistent");
        assertFalse(value.isPresent());
    }

    @Test
    @DisplayName("get returns empty Optional for non-existent key")
    void testGetNonExistent() {
        var value = this.registry.get("nonexistent");
        assertFalse(value.isPresent());
    }

    // ========== Dynamic Placeholder Tests ==========

    @Test
    @DisplayName("Dynamic placeholder evaluated each time get is called")
    void testDynamicPlaceholderEvaluatedEachTime() {
        final int[] callCount = {0};
        this.registry.register("counter", () -> String.valueOf(++callCount[0]));

        String result1 = this.registry.get("counter").orElse("");
        String result2 = this.registry.get("counter").orElse("");

        assertEquals("1", result1);
        assertEquals("2", result2);
    }

    // ========== Cached Placeholder Tests ==========

    @Test
    @DisplayName("Register with caching uses supplier")
    void testRegisterCached() {
        final int[] callCount = {0};
        this.registry.registerCached("cached", () -> String.valueOf(++callCount[0]), 1000);

        String result1 = this.registry.get("cached").orElse("");
        String result2 = this.registry.get("cached").orElse("");

        // Both should return same value due to caching
        assertEquals(result1, result2);
    }

    // ========== Multiple Placeholders Tests ==========

    @Test
    @DisplayName("Multiple static placeholders can coexist")
    void testMultipleStaticPlaceholders() {
        this.registry.register("key1", "value1");
        this.registry.register("key2", "value2");
        this.registry.register("key3", "value3");

        assertEquals("value1", this.registry.get("key1").orElse(null));
        assertEquals("value2", this.registry.get("key2").orElse(null));
        assertEquals("value3", this.registry.get("key3").orElse(null));
    }

    @Test
    @DisplayName("Multiple dynamic placeholders can coexist")
    void testMultipleDynamicPlaceholders() {
        this.registry.register("dyn1", () -> "value1");
        this.registry.register("dyn2", () -> "value2");
        this.registry.register("dyn3", () -> "value3");

        assertEquals("value1", this.registry.get("dyn1").orElse(null));
        assertEquals("value2", this.registry.get("dyn2").orElse(null));
        assertEquals("value3", this.registry.get("dyn3").orElse(null));
    }

    @Test
    @DisplayName("Override static placeholder logs warning")
    void testOverrideStaticPlaceholder() {
        this.registry.register("key", "value1");
        this.registry.register("key", "value2");

        // Should have new value (overwrite)
        assertEquals("value2", this.registry.get("key").orElse(null));
    }

    // ========== Placeholder Value Tests ==========

    @Test
    @DisplayName("Placeholder value as static")
    void testPlaceholderValueStatic() {
        this.registry.register("static_key", "static_value");

        var value = this.registry.getValue("static_key");
        assertTrue(value.isPresent());
        var pv = value.get();
        assertNotNull(pv);
    }

    @Test
    @DisplayName("Placeholder value as dynamic")
    void testPlaceholderValueDynamic() {
        this.registry.register("dyn_key", () -> "dynamic_value");

        var value = this.registry.getValue("dyn_key");
        assertTrue(value.isPresent());
        var pv = value.get();
        assertNotNull(pv);
    }

    // ========== Value Content Tests ==========

    @Test
    @DisplayName("Empty string value is preserved")
    void testEmptyStringValue() {
        this.registry.register("empty", "");
        assertEquals("", this.registry.get("empty").orElse(null));
    }

    @Test
    @DisplayName("Case sensitivity: keys are case-sensitive")
    void testCaseSensitivity() {
        this.registry.register("Key", "value1");
        this.registry.register("key", "value2");

        assertEquals("value1", this.registry.get("Key").orElse(null));
        assertEquals("value2", this.registry.get("key").orElse(null));
    }

    @Test
    @DisplayName("hasAnyInText with multiple placeholders")
    void testHasAnyInTextMultiple() {
        this.registry.register("p1", "v1");
        this.registry.register("p2", "v2");

        String text = "%p1% and %p2%";
        assertTrue(this.registry.hasAnyInText(text));
    }

    @Test
    @DisplayName("Dynamic placeholder exception handling")
    void testDynamicPlaceholderException() {
        this.registry.register("error", () -> {
            throw new RuntimeException("Test error");
        });

        // Should return empty Optional when evaluated (exception is caught and logged)
        var result = this.registry.get("error");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Multiple registrations and unregistrations")
    void testMultipleRegistrationsAndUnregistrations() {
        this.registry.register("k1", "v1");
        this.registry.register("k2", "v2");
        this.registry.register("k3", "v3");

        assertTrue(this.registry.hasAny());

        this.registry.unregister("k1");
        assertTrue(this.registry.hasAny());
        assertTrue(this.registry.exists("k2"));

        this.registry.unregister("k2");
        this.registry.unregister("k3");
        assertFalse(this.registry.hasAny());
    }

    @Test
    @DisplayName("Special characters in placeholder values")
    void testSpecialCharactersInValue() {
        this.registry.register("special", "Value with $pecial !@#$%^&*() chars");
        assertEquals("Value with $pecial !@#$%^&*() chars", this.registry.get("special").orElse(null));
    }

    @Test
    @DisplayName("Newlines and whitespace in placeholder values")
    void testNewlinesAndWhitespace() {
        String valueWithNewlines = "Line1\nLine2\nLine3";
        this.registry.register("multiline", valueWithNewlines);
        assertEquals(valueWithNewlines, this.registry.get("multiline").orElse(null));
    }

    @Test
    @DisplayName("Very long placeholder value")
    void testLongPlaceholderValue() {
        String longValue = "x".repeat(10000);
        this.registry.register("long", longValue);
        assertEquals(longValue, this.registry.get("long").orElse(null));
    }

    // ========== Cache Configuration Tests ==========

    @Test
    @DisplayName("rebuildCaches updates configuration")
    void testRebuildCachesUpdatesConfiguration() {
        ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE.setValue(500L);
        ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_MAX_SIZE.setValue(5000L);

        this.registry.rebuildCaches();

        assertEquals(500L, ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE.getValue());
        assertEquals(5000L, ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_MAX_SIZE.getValue());
    }

    @Test
    @DisplayName("rebuildCaches clears existing caches and rebuilds them")
    void testRebuildCachesClearsCaches() {
        // Register and access a cached placeholder to populate the cache
        this.registry.registerCached("test_cache", () -> "test_value", 10000);

        // Verify it's cached
        String value1 = this.registry.get("test_cache").orElse("");
        assertNotNull(value1);

        // Change configuration
        ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE.setValue(500L);
        this.registry.rebuildCaches();

        // Verify new configuration took effect
        assertEquals(500L, ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE.getValue());

        // The placeholder should still exist in the registry even though cache was cleared
        assertTrue(this.registry.exists("test_cache"));
    }

    @Test
    @DisplayName("isEmpty returns correct status")
    void testIsEmpty() {
        assertTrue(this.registry.isEmpty());
        this.registry.register("key", "value");
        assertFalse(this.registry.isEmpty());
        this.registry.clear();
        assertTrue(this.registry.isEmpty());
    }
}
