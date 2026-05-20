package fr.robie.messageflow.configuration;

import com.google.common.cache.CacheBuilder;
import fr.robie.messageflow.api.GlobalPlaceholderRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Immutable configuration for placeholder caching behavior.
 *
 * <p>This class encapsulates all cache-related settings for the {@link GlobalPlaceholderRegistry},
 * including separate configurations for global and player-specific caches. Supports fluent
 * builder pattern for easy customization.
 *
 * <p>Thread-safe. All fields are final and immutable.
 */
@SuppressWarnings("rawtypes")
public final class PlaceholderCacheConfig {

    private final int globalCacheMaximumSize;
    private final int globalCacheInitialCapacity;
    private final int globalCacheConcurrencyLevel;
    private final long globalCacheExpireAfterWriteMinutes;
    private final long globalCacheExpireAfterAccessMinutes;
    private final boolean recordGlobalCacheStats;
    private final boolean useGlobalCacheSoftValues;

    private final int playerCacheMaximumSize;
    private final int playerCacheInitialCapacity;
    private final int playerCacheConcurrencyLevel;
    private final long playerCacheExpireAfterWriteMinutes;
    private final long playerCacheExpireAfterAccessMinutes;
    private final boolean recordPlayerCacheStats;
    private final boolean usePlayerCacheSoftValues;

    /**
     * Creates a new configuration with the specified cache settings.
     *
     * <p>This constructor is package-private. Use {@link #builder()} for fluent configuration.
     */
    PlaceholderCacheConfig(
            int globalCacheMaximumSize,
            int globalCacheInitialCapacity,
            int globalCacheConcurrencyLevel,
            long globalCacheExpireAfterWriteMinutes,
            long globalCacheExpireAfterAccessMinutes,
            boolean recordGlobalCacheStats,
            boolean useGlobalCacheSoftValues,
            int playerCacheMaximumSize,
            int playerCacheInitialCapacity,
            int playerCacheConcurrencyLevel,
            long playerCacheExpireAfterWriteMinutes,
            long playerCacheExpireAfterAccessMinutes,
            boolean recordPlayerCacheStats,
            boolean usePlayerCacheSoftValues) {
        this.globalCacheMaximumSize = globalCacheMaximumSize;
        this.globalCacheInitialCapacity = globalCacheInitialCapacity;
        this.globalCacheConcurrencyLevel = globalCacheConcurrencyLevel;
        this.globalCacheExpireAfterWriteMinutes = globalCacheExpireAfterWriteMinutes;
        this.globalCacheExpireAfterAccessMinutes = globalCacheExpireAfterAccessMinutes;
        this.recordGlobalCacheStats = recordGlobalCacheStats;
        this.useGlobalCacheSoftValues = useGlobalCacheSoftValues;
        this.playerCacheMaximumSize = playerCacheMaximumSize;
        this.playerCacheInitialCapacity = playerCacheInitialCapacity;
        this.playerCacheConcurrencyLevel = playerCacheConcurrencyLevel;
        this.playerCacheExpireAfterWriteMinutes = playerCacheExpireAfterWriteMinutes;
        this.playerCacheExpireAfterAccessMinutes = playerCacheExpireAfterAccessMinutes;
        this.recordPlayerCacheStats = recordPlayerCacheStats;
        this.usePlayerCacheSoftValues = usePlayerCacheSoftValues;
    }

    /**
     * Creates a configuration with default values matching the original implementation.
     *
     * @return default configuration
     */
    public static @NotNull PlaceholderCacheConfig defaults() {
        return builder().build();
    }

    /**
     * Creates a new builder for fluent configuration.
     *
     * @return new builder with default values
     */
    public static @NotNull PlaceholderCacheConfigBuilder builder() {
        return new PlaceholderCacheConfigBuilder();
    }

    // ========== Global Cache Getters ==========

    public int getGlobalCacheMaximumSize() {
        return this.globalCacheMaximumSize;
    }

    public int getGlobalCacheInitialCapacity() {
        return this.globalCacheInitialCapacity;
    }

    public int getGlobalCacheConcurrencyLevel() {
        return this.globalCacheConcurrencyLevel;
    }

    public long getGlobalCacheExpireAfterWriteMinutes() {
        return this.globalCacheExpireAfterWriteMinutes;
    }

    public long getGlobalCacheExpireAfterAccessMinutes() {
        return this.globalCacheExpireAfterAccessMinutes;
    }

    public boolean shouldRecordGlobalCacheStats() {
        return this.recordGlobalCacheStats;
    }

    public boolean shouldUseGlobalCacheSoftValues() {
        return this.useGlobalCacheSoftValues;
    }

    // ========== Player Cache Getters ==========

    public int getPlayerCacheMaximumSize() {
        return this.playerCacheMaximumSize;
    }

    public int getPlayerCacheInitialCapacity() {
        return this.playerCacheInitialCapacity;
    }

    public int getPlayerCacheConcurrencyLevel() {
        return this.playerCacheConcurrencyLevel;
    }

    public long getPlayerCacheExpireAfterWriteMinutes() {
        return this.playerCacheExpireAfterWriteMinutes;
    }

    public long getPlayerCacheExpireAfterAccessMinutes() {
        return this.playerCacheExpireAfterAccessMinutes;
    }

    public boolean shouldRecordPlayerCacheStats() {
        return this.recordPlayerCacheStats;
    }

    public boolean shouldUsePlayerCacheSoftValues() {
        return this.usePlayerCacheSoftValues;
    }

    // ========== CacheBuilder Factory Methods ==========

    /**
     * Creates a configured CacheBuilder for the global placeholder cache.
     *
     * @return configured CacheBuilder
     */
    public @NotNull CacheBuilder createGlobalCacheBuilder() {
        return this.applyCacheSettings(CacheBuilder.newBuilder(), true);
    }

    /**
     * Creates a configured CacheBuilder for per-player placeholder caches.
     *
     * @return configured CacheBuilder
     */
    public @NotNull CacheBuilder createPlayerCacheBuilder() {
        return this.applyCacheSettings(CacheBuilder.newBuilder(), false);
    }

    /**
     * Applies cache settings to a CacheBuilder instance.
     *
     * @param builder  the cache builder to configure
     * @param isGlobal true for global cache settings, false for player cache settings
     * @return the configured builder
     */
    private @NotNull CacheBuilder applyCacheSettings(@NotNull CacheBuilder builder, boolean isGlobal) {
        if (isGlobal) {
            builder.maximumSize(this.globalCacheMaximumSize);
            if (this.globalCacheInitialCapacity > 0) {
                builder.initialCapacity(this.globalCacheInitialCapacity);
            }
            if (this.globalCacheConcurrencyLevel > 0) {
                builder.concurrencyLevel(this.globalCacheConcurrencyLevel);
            }
            if (this.globalCacheExpireAfterWriteMinutes > 0) {
                builder.expireAfterWrite(this.globalCacheExpireAfterWriteMinutes, TimeUnit.MINUTES);
            }
            if (this.globalCacheExpireAfterAccessMinutes > 0) {
                builder.expireAfterAccess(this.globalCacheExpireAfterAccessMinutes, TimeUnit.MINUTES);
            }
            if (this.recordGlobalCacheStats) {
                builder.recordStats();
            }
            if (this.useGlobalCacheSoftValues) {
                builder.softValues();
            }
        } else {
            builder.maximumSize(this.playerCacheMaximumSize);
            if (this.playerCacheInitialCapacity > 0) {
                builder.initialCapacity(this.playerCacheInitialCapacity);
            }
            if (this.playerCacheConcurrencyLevel > 0) {
                builder.concurrencyLevel(this.playerCacheConcurrencyLevel);
            }
            if (this.playerCacheExpireAfterWriteMinutes > 0) {
                builder.expireAfterWrite(this.playerCacheExpireAfterWriteMinutes, TimeUnit.MINUTES);
            }
            if (this.playerCacheExpireAfterAccessMinutes > 0) {
                builder.expireAfterAccess(this.playerCacheExpireAfterAccessMinutes, TimeUnit.MINUTES);
            }
            if (this.recordPlayerCacheStats) {
                builder.recordStats();
            }
            if (this.usePlayerCacheSoftValues) {
                builder.softValues();
            }
        }
        return builder;
    }

    /**
     * Fluent builder for {@link PlaceholderCacheConfig}.
     */
    public static class PlaceholderCacheConfigBuilder {

        private int globalCacheMaximumSize = 1000;
        private int globalCacheInitialCapacity = -1;
        private int globalCacheConcurrencyLevel = -1;
        private long globalCacheExpireAfterWriteMinutes = -1;
        private long globalCacheExpireAfterAccessMinutes = -1;
        private boolean recordGlobalCacheStats = false;
        private boolean useGlobalCacheSoftValues = false;

        private int playerCacheMaximumSize = 10000;
        private int playerCacheInitialCapacity = -1;
        private int playerCacheConcurrencyLevel = -1;
        private long playerCacheExpireAfterWriteMinutes = -1;
        private long playerCacheExpireAfterAccessMinutes = -1;
        private boolean recordPlayerCacheStats = false;
        private boolean usePlayerCacheSoftValues = false;

        private PlaceholderCacheConfigBuilder() {
        }

        // ========== Global Cache Builder Methods ==========

        public @NotNull PlaceholderCacheConfigBuilder globalCacheMaximumSize(int size) {
            this.globalCacheMaximumSize = size;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder globalCacheInitialCapacity(int capacity) {
            this.globalCacheInitialCapacity = capacity;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder globalCacheConcurrencyLevel(int level) {
            this.globalCacheConcurrencyLevel = level;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder globalCacheExpireAfterWrite(long minutes) {
            this.globalCacheExpireAfterWriteMinutes = minutes;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder globalCacheExpireAfterAccess(long minutes) {
            this.globalCacheExpireAfterAccessMinutes = minutes;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder recordGlobalCacheStats(boolean record) {
            this.recordGlobalCacheStats = record;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder useGlobalCacheSoftValues(boolean useSoft) {
            this.useGlobalCacheSoftValues = useSoft;
            return this;
        }

        // ========== Player Cache Builder Methods ==========

        public @NotNull PlaceholderCacheConfigBuilder playerCacheMaximumSize(int size) {
            this.playerCacheMaximumSize = size;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder playerCacheInitialCapacity(int capacity) {
            this.playerCacheInitialCapacity = capacity;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder playerCacheConcurrencyLevel(int level) {
            this.playerCacheConcurrencyLevel = level;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder playerCacheExpireAfterWrite(long minutes) {
            this.playerCacheExpireAfterWriteMinutes = minutes;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder playerCacheExpireAfterAccess(long minutes) {
            this.playerCacheExpireAfterAccessMinutes = minutes;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder recordPlayerCacheStats(boolean record) {
            this.recordPlayerCacheStats = record;
            return this;
        }

        public @NotNull PlaceholderCacheConfigBuilder usePlayerCacheSoftValues(boolean useSoft) {
            this.usePlayerCacheSoftValues = useSoft;
            return this;
        }

        /**
         * Builds the final immutable configuration.
         *
         * @return new PlaceholderCacheConfig instance
         */
        public @NotNull PlaceholderCacheConfig build() {
            return new PlaceholderCacheConfig(
                    this.globalCacheMaximumSize,
                    this.globalCacheInitialCapacity,
                    this.globalCacheConcurrencyLevel,
                    this.globalCacheExpireAfterWriteMinutes,
                    this.globalCacheExpireAfterAccessMinutes,
                    this.recordGlobalCacheStats,
                    this.useGlobalCacheSoftValues,
                    this.playerCacheMaximumSize,
                    this.playerCacheInitialCapacity,
                    this.playerCacheConcurrencyLevel,
                    this.playerCacheExpireAfterWriteMinutes,
                    this.playerCacheExpireAfterAccessMinutes,
                    this.recordPlayerCacheStats,
                    this.usePlayerCacheSoftValues
            );
        }
    }
}
