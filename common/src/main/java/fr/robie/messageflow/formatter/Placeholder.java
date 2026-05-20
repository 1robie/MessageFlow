package fr.robie.messageflow.formatter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a collection of named placeholders for message formatting.
 * Immutable placeholder map that can be used to replace tokens in messages.
 *
 * <p>Placeholders are stored as key-value pairs where both keys and values are strings.
 * Null values are converted to empty strings for consistency.
 *
 * <p>Usage examples:
 * <pre>
 * // Single placeholder
 * Placeholder p1 = Placeholder.of("name", "John");
 *
 * // Multiple placeholders using builder
 * Placeholder p2 = Placeholder.builder()
 *     .put("name", "John")
 *     .put("level", "10")
 *     .build();
 *
 * // Empty placeholders (no replacements)
 * Placeholder p3 = Placeholder.empty();
 * </pre>
 */
public final class Placeholder {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%]+%");
    private static final Placeholder EMPTY = new Placeholder(Collections.emptyMap());

    private final Map<String, String> placeholders;

    /**
     * Creates a new Placeholder with the given key-value pairs.
     *
     * @param placeholders an immutable map of placeholders
     */
    private Placeholder(@NotNull Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    /**
     * Gets the immutable map of placeholders.
     *
     * @return an immutable map of placeholder key-value pairs
     */
    public @NotNull Map<String, String> getMap() {
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
        if (message == null) {
            return null;
        }
        if (this.isEmpty()) {
            return message;
        }

        Map<String, String> replacements = new HashMap<>();
        for (var entry : this.placeholders.entrySet()) {
            replacements.put("%" + entry.getKey() + "%", entry.getValue());
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
     * Creates a Placeholder with a single key-value pair.
     *
     * @param key   the placeholder key (must not be null)
     * @param value the placeholder value (null values become empty strings)
     * @return a Placeholder containing the single key-value pair
     */
    public static @NotNull Placeholder of(@NotNull String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value == null ? "" : value);
        return new Placeholder(Collections.unmodifiableMap(map));
    }

    /**
     * Creates a Placeholder with multiple key-value pairs.
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
        return new Placeholder(Map.of(key1, value1 == null ? "" : value1, key2, value2 == null ? "" : value2));
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
     * Useful for complex cases with many placeholders.
     */
    public static final class Builder {
        private final Map<String, String> map = new HashMap<>();

        /**
         * Adds a key-value pair to the builder.
         *
         * @param key   the placeholder key (must not be null)
         * @param value the placeholder value (null values become empty strings)
         * @return this Builder instance for method chaining
         */
        public @NotNull Builder put(@NotNull String key, String value) {
            this.map.put(key, value == null ? "" : value);
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
