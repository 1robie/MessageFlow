package fr.robie.messageflow.impl;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.TextResolverRegistry;
import fr.robie.messageflow.api.IMessageManager;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.configuration.lang.LanguageConfiguration;
import fr.robie.messageflow.configuration.lang.LanguageEntry;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.formatter.LegacyMessageFormatter;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.AdventureLogger;
import fr.robie.messageflow.logger.LegacyLogger;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.*;
import fr.robie.messageflow.util.PlatformType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * Default implementation of {@link IMessageManager} that handles message loading, reloading,
 * and resolution from YAML-based language files.
 * <p>
 * This manager supports automatic file creation, key synchronization (adding missing keys,
 * removing obsolete keys), and backup management. It automatically selects the appropriate
 * {@link MessageFormatter} (Adventure or Legacy) based on platform capabilities.
 *
 * @param <T> the type of the plugin using this manager
 * @param <E> the type used to represent languages (e.g., String or an Enum)
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public final class MessageManager<T extends Plugin, E> implements IMessageManager<T, E> {
    private final T plugin;
    private final ConfigurationOptions<E> options;
    private final Supplier<? extends Iterable<? extends Message>> messages;
    private final MessageFormatter<T, ?> messageFormatter;

    private final DateTimeFormatter BACKUP_DATE_FORMAT;

    private final LanguageConfiguration<E> languageConfiguration;

    /**
     * Creates a new MessageManager with the specified plugin, configuration options, and message provider.
     *
     * @param plugin   the plugin instance
     * @param options  the configuration options for this manager
     * @param messages an iterable collection of message definitions
     */
    public MessageManager(@NotNull T plugin, @NotNull ConfigurationOptions<E> options, @NotNull Iterable<? extends Message> messages) {
        this(plugin, options, () -> {
            Preconditions.checkNotNull(messages, "Messages iterable cannot be null");
            return messages;
        });
    }

    /**
     * Creates a new MessageManager with an enum-based message provider.
     *
     * @param plugin           the plugin instance
     * @param options          the configuration options for this manager
     * @param messageEnumClass the enum class containing message definitions (must implement {@link Message})
     * @param <En>             the enum type representing messages
     * @throws IllegalArgumentException if the provided class is not a valid enum or does not implement Message
     */
    public <En extends Enum<En> & Message> MessageManager(@NotNull T plugin, @NotNull ConfigurationOptions<E> options, @NotNull Class<En> messageEnumClass) throws IllegalArgumentException {
        this(plugin, options, () -> iterableEnum(Preconditions.checkNotNull(messageEnumClass, "Message enum class cannot be null")));
    }

    private MessageManager(@NotNull T plugin, @NotNull ConfigurationOptions<E> options, @NotNull Supplier<? extends Iterable<? extends Message>> messages) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(options, "Configuration options cannot be null");
        this.plugin = plugin;
        this.options = options;
        this.messages = messages;

        TextResolverRegistry registry = new TextResolverRegistry();
        registry.initialize();

        this.messageFormatter = PlatformType.hasComponent()
                ? new AdventureMessageFormatter<>(plugin, options)
                : new LegacyMessageFormatter<>(plugin, options);

        this.messageFormatter.setTextResolverRegistry(registry);

        String loggerPrefix = options.loggerPrefix();
        if (loggerPrefix == null) {
            try {
                loggerPrefix = "<dark_gray>[</dark_gray>" + this.plugin.getPluginMeta().getName() + " " + this.plugin.getPluginMeta().getVersion() + "<dark_gray>]</dark_gray>";
            } catch (Throwable ignored) {
                loggerPrefix = "§8[" + this.plugin.getDescription().getFullName() + "§8]";
            }
        }

        if (this.messageFormatter instanceof AdventureMessageFormatter<?> adventureFormatter) {
            new AdventureLogger(loggerPrefix, adventureFormatter);
        } else {
            LegacyMessageFormatter<?> legacyFormatter = (LegacyMessageFormatter<?>) this.messageFormatter;
            new LegacyLogger(loggerPrefix, legacyFormatter);
        }

        this.BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern(options.backupDateFormat());
        this.languageConfiguration = options.languageConfiguration();
    }

    @Override
    public @NotNull MessageFormatter<T, ?> formatter() {
        return this.messageFormatter;
    }

    @Override
    public @NotNull LanguageConfiguration<E> languageConfiguration() {
        return this.languageConfiguration;
    }

    @Override
    public void reload() {
        for (LanguageEntry languageEntry : this.languageConfiguration.getLanguagesEntries()) {
            String lang = languageEntry.language();
            String relFile = languageEntry.path();
            File file = new File(this.plugin.getDataFolder(), relFile);

            if (!file.exists() && this.options.autoCreateFiles()) {
                if (!this.tryCopyBundledResource(relFile)) {
                    ensureParentExists(file);
                    YamlConfiguration config = new YamlConfiguration();
                    this.writeAllDefaults(config);
                    saveQuietly(config, file);
                }
            }

            if (file.exists()) {
                this.updateKeysIfNeeded(lang, relFile, file);
            }
        }


        this.loadLanguage(this.languageConfiguration.getActiveLanguage());
    }

    @Override
    public void loadLanguage(@NotNull E language) {
        Preconditions.checkNotNull(language, "language cannot be null");
        String lang = this.languageConfiguration.getNormalizedLanguage(language);
        String rel = this.languageConfiguration.getRelativePath(language);
        if (rel == null) {
            lang = this.languageConfiguration.getNormalizedLanguage(language = this.languageConfiguration.getDefaultLanguage());
            rel = this.languageConfiguration.getRelativePath(language);
            if (rel == null) {
                return;
            }
        }

        File file = new File(this.plugin.getDataFolder(), rel);
        if (!file.exists() && this.options.autoCreateFiles()) {
            if (!this.tryCopyBundledResource(rel)) {
                ensureParentExists(file);
                YamlConfiguration config = new YamlConfiguration();
                this.writeAllDefaults(config);
                saveQuietly(config, file);
            }
        }

        if (file.exists()) {
            this.updateKeysIfNeeded(lang, rel, file);
            this.loadLanguageFileIntoMessages(file);
        }
        this.languageConfiguration.setActiveLanguage(language);
    }

    /**
     * Updates the keys in a language file by adding missing keys and/or removing obsolete keys
     * based on the current configuration options. Creates a backup before removing obsolete keys
     * if configured to do so.
     *
     * @param lang    the language code
     * @param relPath the relative path to the language file
     * @param file    the language file to update
     */
    private void updateKeysIfNeeded(@NotNull String lang, @NotNull String relPath, @NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration bundled = this.loadBundledYaml(relPath);

        boolean changed = false;

        if (this.options.autoRemoveObsoleteKeys()) {
            Set<String> valid = new LinkedHashSet<>();
            for (Message m : this.messages.get()) valid.add(m.key());

            List<String> obsolete = new ArrayList<>();
            for (String key : config.getKeys(true)) {
                if (config.isConfigurationSection(key)) {
                    continue;
                }
                if (!isUnderValidRoot(key, valid)) {
                    obsolete.add(key);
                }
            }
            if (!obsolete.isEmpty()) {
                if (this.options.backupBeforeRemovingObsoleteKeys()) {
                    this.backupFile(file, lang);
                }
                for (String k : obsolete) config.set(k, null);
                changed = true;
            }
        }

        if (this.options.autoAddMissingKeys()) {
            for (Message m : this.messages.get()) {
                if (config.contains(m.key())) {
                    continue;
                }
                Object fromBundled = bundled != null ? bundled.get(m.key()) : null;
                if (fromBundled != null) {
                    config.set(m.key(), fromBundled);
                } else {
                    config.set(m.key(), toYamlValue(m.defaults()));
                }
                changed = true;
            }
        }

        if (changed) {
            saveQuietly(config, file);
        }
    }

    /**
     * Loads a language file into all registered messages, parsing the YAML configuration
     * and populating the loaded state of each message.
     *
     * @param file the language file to load
     */
    private void loadLanguageFileIntoMessages(@NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (Message m : this.messages.get()) {
            if (!config.contains(m.key())) {
                m.setLoaded(m.defaults());
                continue;
            }
            List<MessageTypeAdapter> parsed = this.parseMessageList(config, m.key());
            List<MessageTypeAdapter> filtered = parsed.stream()
                    .filter(adapter -> m.settings().isTypeAllowed(adapter.messageType()))
                    .toList();

            if (filtered.isEmpty() && !parsed.isEmpty()) {
                Logger.warn("All parsed message types for key '%key%' are blocked by its settings. Falling back to defaults.", Placeholder.of("key", m.key()));
                m.setLoaded(m.defaults());
            } else {
                m.setLoaded(filtered);
            }
        }
    }

    /**
     * Writes all default message values to the given YAML configuration.
     *
     * @param config the configuration to populate with default values
     */
    private void writeAllDefaults(YamlConfiguration config) {
        for (Message m : this.messages.get()) {
            config.set(m.key(), toYamlValue(m.defaults()));
        }
    }

    /**
     * Checks whether a given key is under a valid root key from the registered messages.
     *
     * @param key        the key to check
     * @param validRoots the set of valid root keys
     * @return true if the key is under a valid root, false otherwise
     */
    private static boolean isUnderValidRoot(String key, Set<String> validRoots) {
        if (validRoots.contains(key)) {
            return true;
        }
        int lastDot = key.lastIndexOf('.');
        while (lastDot > 0) {
            String parent = key.substring(0, lastDot);
            if (validRoots.contains(parent)) {
                return true;
            }
            lastDot = parent.lastIndexOf('.');
        }
        return false;
    }

    /**
     * Ensures that the parent directory of the given file exists, creating it if necessary.
     *
     * @param file the file whose parent directory should exist
     */
    private static void ensureParentExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Saves a YAML configuration to a file, silently ignoring any IO exceptions.
     *
     * @param config the configuration to save
     * @param file   the file to save to
     */
    private static void saveQuietly(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    /**
     * Attempts to copy a bundled resource from the plugin's JAR to the plugin data folder.
     *
     * @param relativePath the relative path of the resource to copy
     * @return true if the resource was found and copied, false otherwise
     */
    private boolean tryCopyBundledResource(@NotNull String relativePath) {
        try {
            if (this.plugin.getResource(relativePath) == null) {
                return false;
            }
            this.plugin.saveResource(relativePath, false);
            return true;
        } catch (IllegalArgumentException exception) {
            Logger.warn("Failed to copy bundled resource: %path%. Resource not found in JAR.", exception, Placeholder.of("path", relativePath));
            return false;
        }
    }

    /**
     * Loads a YAML configuration from a bundled resource in the plugin's JAR.
     *
     * @param relativePath the relative path of the resource to load
     * @return the loaded configuration, or null if the resource was not found or could not be loaded
     */
    private @Nullable YamlConfiguration loadBundledYaml(@NotNull String relativePath) {
        try (InputStream is = this.plugin.getResource(relativePath)) {
            if (is == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException exception) {
            Logger.warn("Failed to load bundled resource: %path%", exception, Placeholder.of("path", relativePath));
            return null;
        }
    }

    /**
     * Creates a backup of the given file in the configured backup directory.
     *
     * @param file the file to backup
     * @param lang the language code used in the backup filename
     */
    private void backupFile(@NotNull File file, @NotNull String lang) {
        File backupDir = new File(this.plugin.getDataFolder(), this.options.backupFolder());
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            Logger.warn("Failed to create backup directory: %dir%. Skipping backup.", Placeholder.of("dir", backupDir.getAbsolutePath()));
            return;
        }

        String baseName = file.getName().replaceAll("\\.yml$", "");
        String stamp = LocalDateTime.now().format(this.BACKUP_DATE_FORMAT);
        File dest = new File(backupDir, lang + "_" + baseName + "_" + stamp + ".yml");

        try {
            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Logger.warn("Failed to backup file %file% to %dest%", exception, Placeholder.of("file", file.getAbsolutePath(), "dest", dest.getAbsolutePath()));
        }
    }

    /**
     * Converts a list of message type adapters to a YAML-compatible value.
     * Single simple messages with one line are serialized as a plain string,
     * while complex messages are serialized as a list of maps.
     *
     * @param defaults the list of message type adapters to convert
     * @return a YAML-compatible value (String, List, or Map)
     */
    private static Object toYamlValue(List<? extends MessageTypeAdapter> defaults) {
        if (defaults == null || defaults.isEmpty()) {
            return List.of();
        }

        if (defaults.size() == 1 && defaults.getFirst() instanceof SimpleMessage sm) {
            if (sm.messages().size() == 1) {
                return sm.messages().getFirst();
            }
            return sm.messages();
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (MessageTypeAdapter m : defaults) {
            Map<String, Object> entry = new LinkedHashMap<>(m.serialize());
            entry.put("type", m.messageType().name());
            list.add(entry);
        }
        return list.size() == 1 ? list.getFirst() : list;
    }

    /**
     * Parses a raw YAML value into a list of message type adapters.
     * Handles various input formats: plain strings, string lists, and structured message maps.
     *
     * @param config the YAML configuration containing the value
     * @param key    the key to read from the configuration
     * @return a list of parsed message type adapters
     */
    private List<MessageTypeAdapter> parseMessageList(YamlConfiguration config, String key) {
        Object raw = config.get(key);
        switch (raw) {
            case null -> {
                return List.of();
            }
            case String str -> {
                return List.of(new SimpleMessage(MessageType.TCHAT, List.of(str)));
            }
            case List<?> list -> {
                if (list.isEmpty()) {
                    return List.of();
                }

                if (list.getFirst() instanceof String) {
                    List<String> lines = list.stream()
                            .filter(e -> e instanceof String)
                            .map(e -> (String) e)
                            .toList();
                    return List.of(new SimpleMessage(MessageType.TCHAT, lines));
                }

                if (list.getFirst() instanceof Map<?, ?>) {
                    List<MessageTypeAdapter> result = new ArrayList<>();
                    for (Object entry : list) {
                        if (!(entry instanceof Map<?, ?> map)) {
                            continue;
                        }
                        MessageTypeAdapter parsed = this.parseAdapterFromMap(map);
                        if (parsed != null) {
                            result.add(parsed);
                        }
                    }
                    return result;
                }
            }
            case ConfigurationSection section -> {
                MessageTypeAdapter parsed = this.parseAdapterFromMap(section.getValues(true));
                return parsed != null ? List.of(parsed) : List.of();
            }
            case Map<?, ?> map -> {
                MessageTypeAdapter parsed = this.parseAdapterFromMap(map);
                return parsed != null ? List.of(parsed) : List.of();
            }
            default -> {
            }
        }
        return List.of();
    }

    /**
     * Parses a single message type adapter from a map of values.
     *
     * @param map the map containing message data (type, message content, etc.)
     * @return the parsed message type adapter, or null if parsing failed
     */
    private @Nullable MessageTypeAdapter parseAdapterFromMap(Map<?, ?> map) {
        Object rawType = map.get("type");
        MessageType type = MessageType.TCHAT;
        if (rawType instanceof String s) {
            try {
                type = MessageType.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String k = String.valueOf(e.getKey());
            if ("type".equalsIgnoreCase(k)) {
                continue;
            }
            values.put(k, e.getValue());
        }

        try {
            return switch (type) {
                case TITLE -> TitleMessage.deserialize(values);
                case BOSS_BAR ->
                        PlatformType.hasComponent() ? AdventureBossBarMessage.deserialize(values) : LegacyBossBarMessage.deserialize(values);
                case ACTION_BAR, TCHAT, NONE, WITHOUT_PREFIX, BROADCAST -> SimpleMessage.deserialize(type, values);
            };
        } catch (Exception e) {
            Logger.warn("Failed to parse message of type %type%: %error%", Placeholder.of("type", type.name(), "error", e.getMessage()));
            return null;
        }
    }

    /**
     * Creates an iterable view of an enum class.
     *
     * @param enumClass the enum class to create an iterable for
     * @return an iterable over the enum constants
     * @throws IllegalArgumentException if the class is not an enum
     */
    private static <E extends Enum<E> & Message> @NotNull Iterable<E> iterableEnum(@NotNull Class<E> enumClass) {
        E[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("Not an enum: " + enumClass.getName());
        }

        return () -> new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return this.i < constants.length;
            }

            @Override
            public E next() {
                return constants[this.i++];
            }
        };
    }
}
