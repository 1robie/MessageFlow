package fr.robie.messageflow.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.formatter.PlaceholderModifiers;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.util.PlaceholderScanner;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Singleton registry for global placeholders with optional caching support.
 *
 * <p>Thread-safe implementation using {@link ConcurrentHashMap} for the registry.
 * Supports three types of placeholders:
 * <ul>
 *   <li>Static: Fixed string values</li>
 *   <li>Dynamic: {@link Supplier} that generates values at resolution time</li>
 *   <li>Player-specific: {@link Function} that generates values based on player context</li>
 * </ul>
 *
 * <p>Caching is optional per-placeholder:
 * <ul>
 *   <li>Global cache: For static suppliers (no player context)</li>
 *   <li>Player caches: For player-specific functions (per-player/key isolation)</li>
 * </ul>
 *
 * <p>Cache configuration is managed via {@link ConfigurationManager.Setting}.
 * Caches can be rebuilt via {@link #rebuildCaches()}.
 */
public final class GlobalPlaceholderRegistry {
    private static final GlobalPlaceholderRegistry INSTANCE = new GlobalPlaceholderRegistry();

    private final Map<String, PlaceholderValue> registry = new ConcurrentHashMap<>();
    private final Map<String, CacheConfig> cacheConfig = new ConcurrentHashMap<>();

    private volatile Cache<String, CacheEntry> globalCache;

    private final ConcurrentHashMap<String, Cache<UUID, CacheEntry>> playerCaches = new ConcurrentHashMap<>();

    private record CacheEntry(String value, long expiresAt) {
        static @NotNull CacheEntry of(@NotNull String value, long ttlMillis) {
            return new CacheEntry(value, System.currentTimeMillis() + ttlMillis);
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= this.expiresAt;
        }
    }

    private record CacheConfig(long ttlMillis, boolean isPlayerSpecific) {
    }

    private GlobalPlaceholderRegistry() {
        this.globalCache = this.createGlobalCache();
    }

    /**
     * Creates a new global cache using the provided configuration.
     *
     * @return configured cache instance
     */
    private Cache<String, CacheEntry> createGlobalCache() {
        return this.applyCacheSettings(CacheBuilder.newBuilder(), true).build();
    }

    /**
     * Creates a new player cache using the provided configuration.
     *
     * @return configured cache instance
     */
    private @NotNull Cache<UUID, CacheEntry> createPlayerCache() {
        return this.applyCacheSettings(CacheBuilder.newBuilder(), false).build();
    }

    private @NotNull CacheBuilder<Object, Object> applyCacheSettings(@NotNull CacheBuilder<Object, Object> builder, boolean isGlobal) {
        if (isGlobal) {
            builder.maximumSize(ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE.getValue());
            if (ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_INITIAL_CAPACITY.<Integer>getValue() > 0) {
                builder.initialCapacity(ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_INITIAL_CAPACITY.getValue());
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_CONCURRENCY_LEVEL.<Integer>getValue() > 0) {
                builder.concurrencyLevel(ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_CONCURRENCY_LEVEL.getValue());
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_WRITE.<Long>getValue() > 0) {
                builder.expireAfterWrite(ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_WRITE.getValue(), TimeUnit.MINUTES);
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_ACCESS.<Long>getValue() > 0) {
                builder.expireAfterAccess(ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_ACCESS.getValue(), TimeUnit.MINUTES);
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_RECORD_STATS.getValue()) {
                builder.recordStats();
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_GLOBAL_CACHE_SOFT_VALUES.getValue()) {
                builder.softValues();
            }
        } else {
            builder.maximumSize(ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_MAX_SIZE.getValue());
            if (ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_INITIAL_CAPACITY.<Integer>getValue() > 0) {
                builder.initialCapacity(ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_INITIAL_CAPACITY.getValue());
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_CONCURRENCY_LEVEL.<Integer>getValue() > 0) {
                builder.concurrencyLevel(ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_CONCURRENCY_LEVEL.getValue());
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_WRITE.<Long>getValue() > 0) {
                builder.expireAfterWrite(ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_WRITE.getValue(), TimeUnit.MINUTES);
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_ACCESS.<Long>getValue() > 0) {
                builder.expireAfterAccess(ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_ACCESS.getValue(), TimeUnit.MINUTES);
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_RECORD_STATS.getValue()) {
                builder.recordStats();
            }
            if (ConfigurationManager.Setting.PLACEHOLDER_PLAYER_CACHE_SOFT_VALUES.getValue()) {
                builder.softValues();
            }
        }
        return builder;
    }

    public static @NotNull GlobalPlaceholderRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Rebuilds all caches with current settings from ConfigurationManager.
     * This method is thread-safe but will invalidate existing cache entries.
     *
     * <p>After calling this method:
     * <ul>
     *   <li>All global cache entries are invalidated</li>
     *   <li>All per-player cache entries are cleared and rebuilt with new settings</li>
     *   <li>New cache entries will be created on next access</li>
     * </ul>
     */
    public synchronized void rebuildCaches() {
        if (this.globalCache != null) {
            this.globalCache.invalidateAll();
        }
        this.globalCache = this.createGlobalCache();
        this.playerCaches.clear();
    }

    public boolean isEmpty() {
        return this.registry.isEmpty();
    }

    // --- Registration methods ---

    public void register(@NotNull String key, @NotNull String value) {
        if (this.registry.containsKey(key)) {
            this.logOverwriteWarning(key);
        }
        this.registry.put(key, PlaceholderValue.ofStatic(value));
        this.cacheConfig.remove(key);
    }

    public void register(@NotNull String key, @NotNull Supplier<String> supplier) {
        if (this.registry.containsKey(key)) {
            this.logOverwriteWarning(key);
        }
        this.registry.put(key, PlaceholderValue.ofDynamic(supplier));
        this.cacheConfig.remove(key);
    }

    public void registerPlayer(@NotNull String key, @NotNull Function<Player, String> function) {
        if (this.registry.containsKey(key)) {
            this.logOverwriteWarning(key);
        }
        this.registry.put(key, PlaceholderValue.ofPlayer(function));
        this.cacheConfig.remove(key);
        this.playerCaches.remove(key);
    }

    public void registerCached(@NotNull String key, @NotNull Supplier<String> supplier, long ttlMillis) {
        this.registerCached(key, supplier, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public void registerCached(@NotNull String key, @NotNull Supplier<String> supplier, long time, @NotNull TimeUnit unit) {
        if (this.registry.containsKey(key)) {
            this.logOverwriteWarning(key);
        }
        this.registry.put(key, PlaceholderValue.ofDynamic(supplier));
        this.cacheConfig.put(key, new CacheConfig(unit.toMillis(time), false));
    }

    public void registerPlayerCached(@NotNull String key, @NotNull Function<Player, String> function, long ttlMillis) {
        this.registerPlayerCached(key, function, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public void registerPlayerCached(@NotNull String key, @NotNull Function<Player, String> function, long time, @NotNull TimeUnit unit) {
        if (this.registry.containsKey(key)) {
            this.logOverwriteWarning(key);
        }
        this.registry.put(key, PlaceholderValue.ofPlayer(function));
        this.cacheConfig.put(key, new CacheConfig(unit.toMillis(time), true));
        this.playerCaches.remove(key);
    }


    public @NotNull Optional<String> get(@NotNull String key) {
        PlaceholderValue value = this.registry.get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(this.evaluateValue(key, value, null));
        } catch (Exception e) {
            Logger.warn("Exception evaluating global placeholder '%key%': %error%",
                    Placeholder.of("key", key, "error", e.getMessage()));
            return Optional.empty();
        }
    }

    public @NotNull Optional<String> getPlayer(@NotNull String key, @NotNull Player player) {
        PlaceholderValue value = this.registry.get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(this.evaluateValue(key, value, player));
        } catch (Exception e) {
            Logger.warn("Exception evaluating global placeholder '%key%' for player '%player%': %error%",
                    Placeholder.builder()
                            .register("key", key)
                            .register("player", player.getName())
                            .register("error", e.getMessage())
                            .build());
            return Optional.empty();
        }
    }

    private void logOverwriteWarning(@NotNull String key) {
        Logger.warn("Global placeholder '%key%' is being overwritten",
                Placeholder.of("key", key));
    }

    public @NotNull Map<String, PlaceholderValue> getAll() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(this.registry));
    }

    public @NotNull Optional<PlaceholderValue> getValue(@NotNull String key) {
        return Optional.ofNullable(this.registry.get(key));
    }

    public boolean exists(@NotNull String key) {
        return this.registry.containsKey(key);
    }

    public void unregister(@NotNull String key) {
        this.registry.remove(key);
        this.cacheConfig.remove(key);
        this.playerCaches.remove(key);
    }

    public void clear() {
        this.registry.clear();
        this.cacheConfig.clear();
        this.playerCaches.clear();
        if (this.globalCache != null) {
            this.globalCache.invalidateAll();
        }
    }

    public boolean hasAny() {
        return !this.registry.isEmpty();
    }

    public boolean hasAnyInText(@NotNull String text) {
        if (this.registry.isEmpty()) {
            return false;
        }
        return PlaceholderScanner.anyKey(text, key -> PlaceholderModifiers.baseKey(key, this.registry::containsKey) != null);
    }

    /**
     * Checks if a text uses a player-specific global placeholder.
     *
     * @param text the text to check
     * @return true if the text needs a player to be resolved
     */
    public boolean hasPlayerSpecificInText(@NotNull String text) {
        if (this.registry.isEmpty()) {
            return false;
        }
        return PlaceholderScanner.anyKey(text, key -> {
            String base = PlaceholderModifiers.baseKey(key, this.registry::containsKey);
            return base != null && this.registry.get(base) instanceof PlaceholderValue.PlayerValue;
        });
    }

    private @NotNull String evaluateValue(@NotNull String key, @NotNull PlaceholderValue value, @Nullable Player player) throws Exception {
        CacheConfig config = this.cacheConfig.get(key);

        if (config == null) {
            return value.evaluate(player);
        }

        if (config.isPlayerSpecific) {
            if (player == null) {
                return value.evaluate(null);
            }
            return this.evaluatePlayerCached(key, value, player, config.ttlMillis);
        } else {
            return this.evaluateGlobalCached(key, value, config.ttlMillis);
        }
    }

    private @NotNull String evaluateGlobalCached(@NotNull String key, @NotNull PlaceholderValue value, long ttlMillis) throws Exception {
        Cache<String, CacheEntry> cache = this.globalCache;
        CacheEntry cached = cache.getIfPresent(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        String evaluated = value.evaluate(null);
        cache.put(key, CacheEntry.of(evaluated, ttlMillis));
        return evaluated;
    }

    private @NotNull String evaluatePlayerCached(@NotNull String key, @NotNull PlaceholderValue value, @NotNull Player player, long ttlMillis) throws Exception {
        Cache<UUID, CacheEntry> playerCache = this.playerCaches.computeIfAbsent(key, k -> this.createPlayerCache());
        UUID id = player.getUniqueId();
        CacheEntry cached = playerCache.getIfPresent(id);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        String evaluated = value.evaluate(player);
        playerCache.put(id, CacheEntry.of(evaluated, ttlMillis));
        return evaluated;
    }
}
