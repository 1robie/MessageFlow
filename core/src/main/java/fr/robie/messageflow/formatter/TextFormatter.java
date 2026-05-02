package fr.robie.messageflow.formatter;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface for basic text formatting, specifically handling placeholder replacement.
 */
public interface TextFormatter {

    /**
     * Pattern used to identify placeholders in format %key%.
     */
    Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%]+%");

    /**
     * Parses the given message by replacing placeholders with their corresponding values.
     *
     * @param message the message containing placeholders
     * @param args an even number of arguments (key, value pairs)
     * @return the formatted message
     * @throws IllegalArgumentException if the number of arguments is not even or if a key is null
     */
    @SuppressWarnings("ConstantValue")
    default @NotNull String parseText(@NotNull String message, @NotNull Object... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Invalid placeholders: expected an even number of arguments (key, value pairs), got " + args.length + "."
            );
        }

        if (args.length == 0) {
            return message;
        }

        Map<String, String> replacements = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (args[i] == null) {
                throw new IllegalArgumentException("Placeholder key at index " + i + " must not be null.");
            }
            String value = args[i + 1] != null ? String.valueOf(args[i + 1]) : "";
            replacements.put("%" + args[i] + "%", value);
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
}