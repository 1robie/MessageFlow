package fr.robie.messageflow.formatter;

import fr.robie.messageflow.api.PlaceholderValue;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a collection of named placeholders for message formatting.
 * Immutable placeholder map that can be used to replace tokens in messages.
 *
 * <p>Placeholders can be:
 * <ul>
 *   <li><strong>Static:</strong> Fixed string values (e.g., "John")</li>
 *   <li><strong>Dynamic:</strong> Supplier-based values evaluated at resolution time (e.g., {@code () -> "value"})</li>
 *   <li><strong>Player-specific:</strong> Function-based values evaluated per player (e.g., {@code player -> String.valueOf(player.getHealth())})</li>
 * </ul>
 *
 * <p>Null values are converted to empty strings for consistency.
 *
 * <p>Usage examples:
 * <pre>
 * // Static placeholder
 * Placeholder p1 = Placeholder.of("name", "John");
 *
 * // Dynamic placeholder with supplier
 * Placeholder p2 = Placeholder.of("time", () -> System.currentTimeMillis());
 *
 * // Player-specific placeholder with function
 * Placeholder p3 = Placeholder.of("health", player -> String.valueOf(player.getHealth()));
 *
 * // Multiple placeholders using builder
 * Placeholder p4 = Placeholder.builder()
 *     .register("name", "John")
 *     .register("level", "10")
 *     .register("ping", player -> String.valueOf(player.getPing()))
 *     .build();
 *
 * // Empty placeholders (no replacements)
 * Placeholder p5 = Placeholder.empty();
 * </pre>
 */
public final class Placeholder {
    /**
     * Pattern used to identify placeholders in strings (e.g., %key%).
     */
    public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^% ]+%");
    private static final Placeholder EMPTY = new Placeholder(Collections.emptyMap());

    private final Map<String, PlaceholderValue> placeholders;

    /**
     * Creates a new Placeholder with the given key-value pairs.
     *
     * @param placeholders an immutable map of placeholders
     */
    private Placeholder(@NotNull Map<String, PlaceholderValue> placeholders) {
        this.placeholders = placeholders;
    }

    /**
     * Gets the immutable map of placeholders.
     *
     * @return an immutable map of placeholder key-value pairs
     */
    public @NotNull Map<String, PlaceholderValue> getMap() {
        return this.placeholders;
    }

    /**
     * Checks if there are no placeholders defined.
     *
     * @return true if there are no placeholders, false otherwise
     */
    public boolean isEmpty() {
        return this.placeholders.isEmpty();
    }


    @Contract("null -> null; !null -> _")
    public String parse(@Nullable String message) {
        return this.parse(message, null);
    }

    /**
     * Parses the message by replacing placeholders with their evaluated values.
     *
     * @param message the message to parse (may be null)
     * @param player  the player context for player-specific placeholders (may be null)
     * @return the parsed message with replacements, or null if input is null
     */
    @Contract("null, _ -> null; !null, _ -> _")
    public String parse(@Nullable String message, @Nullable Player player) {
        if (message == null) {
            return null;
        }
        if (this.isEmpty()) {
            return message;
        }

        Map<String, String> replacements = new HashMap<>();
        for (var entry : this.placeholders.entrySet()) {
            String key = entry.getKey();
            PlaceholderValue value = entry.getValue();
            try {
                String evaluated = value.evaluate(player);
                replacements.put("%" + key + "%", evaluated);
            } catch (Exception e) {
                replacements.put("%" + key + "%", "%" + key + "%");
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = replacements.getOrDefault(matcher.group(), matcher.group());
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    /**
     * Gets an empty Placeholder (no replacements).
     * This is a singleton-like instance that can be safely reused.
     *
     * @return an empty Placeholder
     */
    public static @NotNull Placeholder empty() {
        return EMPTY;
    }

    /**
     * Creates a Placeholder with a single static key-value pair.
     *
     * @param key   the placeholder key (must not be null)
     * @param value the placeholder value (null values become empty strings)
     * @return a Placeholder containing the single key-value pair
     */
    public static @NotNull Placeholder of(@NotNull String key, String value) {
        return new Placeholder(Map.of(key, PlaceholderValue.ofStatic(value == null ? "" : value)));
    }

    /**
     * Creates a Placeholder with a single dynamic key-value pair using a supplier.
     *
     * @param key      the placeholder key (must not be null)
     * @param supplier the supplier that provides the placeholder value
     * @return a Placeholder containing the single key-value pair
     */
    public static @NotNull Placeholder of(@NotNull String key, @NotNull Supplier<String> supplier) {
        return new Placeholder(Map.of(key, PlaceholderValue.ofDynamic(supplier)));
    }

    /**
     * Creates a Placeholder with a single player-specific key-value pair using a function.
     *
     * @param key      the placeholder key (must not be null)
     * @param function the function that provides the placeholder value based on a player
     * @return a Placeholder containing the single key-value pair
     */
    public static @NotNull Placeholder of(@NotNull String key, @NotNull Function<Player, String> function) {
        return new Placeholder(Map.of(key, PlaceholderValue.ofPlayer(function)));
    }

    /**
     * Creates a Placeholder with multiple static key-value pairs.
     * Arguments must be provided in key-value order.
     *
     * @param key1   the first placeholder key (must not be null)
     * @param value1 the first placeholder value (null values become empty strings)
     * @param key2   the second placeholder key (must not be null)
     * @param value2 the second placeholder value (null values become empty strings)
     * @return a Placeholder containing the provided key-value pairs
     */
    public static @NotNull Placeholder of(@NotNull String key1, String value1,
                                          @NotNull String key2, String value2) {
        Map<String, PlaceholderValue> map = new HashMap<>();
        map.put(key1, PlaceholderValue.ofStatic(value1 == null ? "" : value1));
        map.put(key2, PlaceholderValue.ofStatic(value2 == null ? "" : value2));
        return new Placeholder(Map.copyOf(map));
    }

    /**
     * Creates a builder for constructing Placeholder instances with multiple key-value pairs.
     *
     * @return a new Builder instance
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating Placeholder instances with multiple key-value pairs.
     * Supports static, dynamic, and player-specific values.
     * Useful for complex cases with many placeholders.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private final Map<String, PlaceholderValue> map = new HashMap<>();

        /**
         * Adds a static key-value pair to the builder.
         *
         * @param key   the placeholder key (must not be null)
         * @param value the placeholder value (null values become empty strings)
         * @return this Builder instance for method chaining
         */
        public @NotNull Builder register(@NotNull String key, String value) {
            this.map.put(key, PlaceholderValue.ofStatic(value == null ? "" : value));
            return this;
        }

        /**
         * Adds a dynamic key-value pair to the builder using a supplier.
         *
         * @param key      the placeholder key (must not be null)
         * @param supplier the supplier that provides the placeholder value
         * @return this Builder instance for method chaining
         */
        public @NotNull Builder register(@NotNull String key, @NotNull Supplier<String> supplier) {
            this.map.put(key, PlaceholderValue.ofDynamic(supplier));
            return this;
        }

        /**
         * Adds a player-specific key-value pair to the builder using a function.
         *
         * @param key      the placeholder key (must not be null)
         * @param function the function that provides the placeholder value based on a player
         * @return this Builder instance for method chaining
         */
        public @NotNull Builder register(@NotNull String key, @NotNull Function<Player, String> function) {
            this.map.put(key, PlaceholderValue.ofPlayer(function));
            return this;
        }

        /**
         * Builds an immutable Placeholder from the accumulated key-value pairs.
         *
         * @return a new Placeholder instance
         */
        public @NotNull Placeholder build() {
            return new Placeholder(Map.copyOf(this.map));
        }
    }
}
