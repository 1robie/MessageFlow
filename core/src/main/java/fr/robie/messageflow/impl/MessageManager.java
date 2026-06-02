package fr.robie.messageflow.impl;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.TextResolverRegistry;
import fr.robie.messageflow.api.GlobalPlaceholderRegistry;
import fr.robie.messageflow.api.IMessageBuilder;
import fr.robie.messageflow.api.IMessageManager;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.configuration.lang.LanguageConfiguration;
import fr.robie.messageflow.configuration.lang.LanguageEntry;
import fr.robie.messageflow.configuration.lang.NormalLanguageConfiguration;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.formatter.LegacyMessageFormatter;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.AdventureLogger;
import fr.robie.messageflow.logger.LegacyLogger;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.*;
import fr.robie.messageflow.util.PlatformType;
import io.papermc.paper.plugin.configuration.PluginMeta;
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
import java.util.function.BiFunction;
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
@SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
public final class MessageManager<T extends Plugin, E> implements IMessageManager<T, E> {

    private final ConfigurationManager<T> configurationManager;
    private final T plugin;
    private final Supplier<? extends Iterable<? extends Message>> messages;
    private final MessageFormatter<T, ?> messageFormatter;
    private final DateTimeFormatter BACKUP_DATE_FORMAT;
    private final LanguageConfiguration<E> languageConfiguration;

    /**
     * Creates a new MessageManager with the specified plugin, configuration options, and message iterable.
     *
     * @param plugin   the plugin instance
     * @param options  the configuration options for this manager
     * @param messages an iterable collection of message definitions
     */
    public MessageManager(@NotNull T plugin, @NotNull ConfigurationOptions<E> options, @NotNull Iterable<? extends Message> messages) {
        this(plugin, options.languageConfiguration(), toSupplier(messages));
    }

    /**
     * Creates a new MessageManager with an enum-based message provider and configuration options.
     *
     * @param plugin           the plugin instance
     * @param options          the configuration options for this manager
     * @param messageEnumClass the enum class containing message definitions (must implement {@link Message})
     * @param <En>             the enum type representing messages
     * @throws IllegalArgumentException if the provided class is not a valid enum or does not implement Message
     */
    public <En extends Enum<En> & Message> MessageManager(@NotNull T plugin, @NotNull ConfigurationOptions<E> options, @NotNull Class<En> messageEnumClass) throws IllegalArgumentException {
        this(plugin, options.languageConfiguration(), toEnumSupplier(messageEnumClass));
    }

    /**
     * Creates a new MessageManager with the specified plugin, language configuration, and message iterable.
     *
     * @param plugin                the plugin instance
     * @param languageConfiguration the language configuration to use for message resolution
     * @param messages              an iterable collection of message definitions
     */
    public MessageManager(@NotNull T plugin, @NotNull LanguageConfiguration<E> languageConfiguration, @NotNull Iterable<? extends Message> messages) {
        this(plugin, languageConfiguration, toSupplier(messages));
    }

    /**
     * Creates a new MessageManager with an enum-based message provider and a language configuration.
     *
     * @param plugin                the plugin instance
     * @param languageConfiguration the language configuration to use for message resolution
     * @param messageEnumClass      the enum class containing message definitions (must implement {@link Message})
     * @throws IllegalArgumentException if the provided class is not a valid enum or does not implement Message
     */
    public MessageManager(@NotNull T plugin, @NotNull LanguageConfiguration<E> languageConfiguration, @NotNull Class<? extends Enum<?>> messageEnumClass) throws IllegalArgumentException {
        this(plugin, languageConfiguration, toEnumSupplier(messageEnumClass));
    }

    /**
     * Internal constructor used by all public constructors. Initializes the message formatter,
     * placeholder registry, logger, and language configuration.
     *
     * @param plugin                the plugin instance
     * @param languageConfiguration the language configuration to use for message resolution
     * @param messages              a supplier providing an iterable collection of message definitions
     */
    private MessageManager(@NotNull T plugin, @NotNull LanguageConfiguration<E> languageConfiguration, @NotNull Supplier<? extends Iterable<? extends Message>> messages) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(languageConfiguration, "Language configuration cannot be null");
        Preconditions.checkNotNull(messages, "Messages supplier cannot be null");

        this.plugin = plugin;
        this.messages = messages;
        this.languageConfiguration = languageConfiguration;

        this.configurationManager = new ConfigurationManager<>(plugin);
        this.configurationManager.load();

        this.messageFormatter = PlatformType.hasComponent()
                ? new AdventureMessageFormatter<>(plugin)
                : new LegacyMessageFormatter<>(plugin);

        String loggerPrefix = this.buildLoggerPrefix();
        this.initLogger(loggerPrefix);

        TextResolverRegistry registry = new TextResolverRegistry();
        this.messageFormatter.setTextResolverRegistry(registry);
        registry.initialize();

        this.BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern(ConfigurationManager.Setting.BACKUP_DATE_FORMAT.getValue());
    }

    /**
     * Creates a MessageManager backed by a single language file mapped to the {@code "default"} language key.
     *
     * @param plugin   the plugin instance
     * @param fileName the name of the YAML file (relative to the plugin data folder)
     * @param messages an iterable collection of message definitions
     * @param <T>      the type of the plugin
     * @return a new MessageManager configured with a single language file
     */
    @NotNull
    public static <T extends Plugin> MessageManager<T, String> withSingleFile(@NotNull T plugin, @NotNull String fileName, @NotNull Iterable<? extends Message> messages) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(fileName, "File name cannot be null");
        Preconditions.checkNotNull(messages, "Messages iterable cannot be null");

        NormalLanguageConfiguration languageConfiguration = new NormalLanguageConfiguration("default");
        languageConfiguration.addLanguage("default", fileName);

        return new MessageManager<>(plugin, languageConfiguration, messages);
    }

    /**
     * Creates a MessageManager backed by a single language file mapped to the {@code "default"} language key,
     * using an enum class as the message provider.
     *
     * @param plugin           the plugin instance
     * @param fileName         the name of the YAML file (relative to the plugin data folder)
     * @param messageEnumClass the enum class containing message definitions (must implement {@link Message})
     * @param <T>              the type of the plugin
     * @param <En>             the enum type representing messages
     * @return a new MessageManager configured with a single language file
     * @throws IllegalArgumentException if the provided class is not a valid enum or does not implement Message
     */
    @NotNull
    public static <T extends Plugin, En extends Enum<En> & Message> MessageManager<T, String> withSingleFile(
            @NotNull T plugin,
            @NotNull String fileName,
            @NotNull Class<En> messageEnumClass) throws IllegalArgumentException {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(fileName, "File name cannot be null");
        Preconditions.checkNotNull(messageEnumClass, "Message enum class cannot be null");

        NormalLanguageConfiguration languageConfiguration = new NormalLanguageConfiguration("default");
        languageConfiguration.addLanguage("default", fileName);

        return new MessageManager<>(plugin, languageConfiguration, toEnumSupplier(messageEnumClass));
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
    public @NotNull ConfigurationManager<T> configurationManager() {
        return this.configurationManager;
    }

    @Override
    public @NotNull IMessageBuilder builder() {
        return new MessageBuilder(this);
    }

    @Override
    public void reload() {
        GlobalPlaceholderRegistry.getInstance().clear();
        this.formatter().clearCache();
        this.configurationManager.load();

        for (LanguageEntry languageEntry : this.languageConfiguration.getLanguagesEntries()) {
            String lang = languageEntry.language();
            String relFile = languageEntry.path();
            File file = new File(this.plugin.getDataFolder(), relFile);

            if (!file.exists() && ConfigurationManager.Setting.SYNC_AUTO_CREATE.<Boolean>getValue()) {
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

        if (!file.exists() && ConfigurationManager.Setting.SYNC_AUTO_CREATE.<Boolean>getValue()) {
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
     * Builds the logger prefix string by resolving plugin meta placeholders
     * and applying the platform-specific prefix pattern.
     *
     * @return the resolved logger prefix
     */
    private @NotNull String buildLoggerPrefix() {
        Placeholder.Builder placeholders = Placeholder.builder();
        try {
            PluginMeta pluginMeta = this.plugin.getPluginMeta();
            placeholders.register("plugin-name", pluginMeta.getName());
            placeholders.register("plugin-version", pluginMeta.getVersion());
            placeholders.register("plugin-full", pluginMeta.getName() + " " + pluginMeta.getVersion());
        } catch (Exception e) {
            placeholders.register("plugin-name", this.plugin.getDescription().getName());
            placeholders.register("plugin-version", this.plugin.getDescription().getVersion());
            placeholders.register("plugin-full", this.plugin.getDescription().getFullName());
        }

        return placeholders.build().parse(switch (PlatformType.get()) {
            case LEGACY -> ConfigurationManager.Setting.LEGACY_LOGGER_PREFIX.getValue();
            case COMPONENTS -> ConfigurationManager.Setting.ADVENTURE_LOGGER_PREFIX.getValue();
        });
    }

    /**
     * Initializes the platform-appropriate logger using the given prefix.
     *
     * @param loggerPrefix the resolved prefix string for the logger
     */
    private void initLogger(@NotNull String loggerPrefix) {
        if (this.messageFormatter instanceof AdventureMessageFormatter<?> adventureFormatter) {
            new AdventureLogger(loggerPrefix, adventureFormatter);
        } else {
            new LegacyLogger(loggerPrefix, (LegacyMessageFormatter<?>) this.messageFormatter);
        }
    }

    /**
     * Wraps an {@link Iterable} of messages in a null-checked {@link Supplier}.
     *
     * @param messages the iterable to wrap
     * @param <M>      the message type
     * @return a supplier that returns the iterable
     */
    private static <M extends Message> @NotNull Supplier<Iterable<M>> toSupplier(@NotNull Iterable<M> messages) {
        Preconditions.checkNotNull(messages, "Messages iterable cannot be null");
        return () -> messages;
    }

    /**
     * Creates a null-checked {@link Supplier} that produces an iterable view of an enum class.
     *
     * @param enumClass the enum class (must implement {@link Message})
     * @param <En>      the enum type
     * @return a supplier wrapping the enum constants
     * @throws IllegalArgumentException if the class is not a valid enum
     */
    @SuppressWarnings("unchecked")
    private static <En extends Enum<En> & Message> @NotNull Supplier<Iterable<En>> toEnumSupplier(@NotNull Class<?> enumClass) {
        Preconditions.checkNotNull(enumClass, "Message enum class cannot be null");
        Class<En> typed = (Class<En>) enumClass.asSubclass(Message.class);
        return () -> iterableEnum(typed);
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

        if (ConfigurationManager.Setting.SYNC_AUTO_REMOVE_OBSOLETE.<Boolean>getValue()) {
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
                if (ConfigurationManager.Setting.BACKUP_ENABLED.<Boolean>getValue()) {
                    this.backupFile(file, lang);
                }
                for (String k : obsolete) config.set(k, null);
                changed = true;
            }
        }

        if (ConfigurationManager.Setting.SYNC_AUTO_ADD_MISSING.<Boolean>getValue()) {
            for (Message m : this.messages.get()) {
                if (config.contains(m.key())) {
                    continue;
                }
                Object fromBundled = bundled != null ? bundled.get(m.key()) : null;
                config.set(m.key(), fromBundled != null ? fromBundled : toYamlValue(m.defaults()));
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
            Object raw = config.get(m.key());
            if (raw == null) {
                m.setLoaded(m.defaults());
                continue;
            }

            MessageSettings globalSettings = this.parseSettings(raw, m.settings());
            m.setSettings(globalSettings);

            List<MessageTypeAdapter> parsed = this.parseMessageList(raw, globalSettings);
            List<MessageTypeAdapter> filtered = parsed.stream()
                    .filter(adapter -> globalSettings.isTypeAllowed(adapter.messageType()))
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
     * Parses message settings from a raw YAML object.
     *
     * @param raw     the raw YAML object (Map or ConfigurationSection)
     * @param initial the initial settings to merge with
     * @return the parsed message settings
     */
    private @NotNull MessageSettings parseSettings(@Nullable Object raw, @NotNull MessageSettings initial) {
        Map<?, ?> map = null;
        if (raw instanceof ConfigurationSection section) {
            map = section.getValues(false);
        } else if (raw instanceof Map<?, ?> m) {
            map = m;
        }

        if (map == null) {
            return initial;
        }

        MessageSettings settings = initial;
        settings = this.applySetting(settings, map, "broadcast", MessageSettings::withBroadcast);
        settings = this.applySetting(settings, map, "send-to-console", MessageSettings::withSendToConsole);
        settings = this.applySetting(settings, map, "exclude-senders", MessageSettings::withExcludeSenders);

        return settings;
    }

    /**
     * Applies a single boolean setting from a map to a {@link MessageSettings} instance using the given function.
     *
     * @param s    the current settings
     * @param map  the map to read the value from
     * @param key  the setting key to look up
     * @param func the function to apply if the value is present
     * @return the updated settings, or the original if the key was absent or not a boolean
     */
    private MessageSettings applySetting(@NotNull MessageSettings s, @NotNull Map<?, ?> map, @NotNull String key, @NotNull BiFunction<MessageSettings, Boolean, MessageSettings> func) {
        Object val = map.get(key);
        if (val instanceof Boolean b) {
            return func.apply(s, b);
        } else if (val instanceof String str) {
            return func.apply(s, Boolean.parseBoolean(str));
        }
        return s;
    }

    /**
     * Writes all default message values to the given YAML configuration.
     *
     * @param config the configuration to populate with default values
     */
    private void writeAllDefaults(@NotNull YamlConfiguration config) {
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
    private static boolean isUnderValidRoot(@NotNull String key, @NotNull Set<String> validRoots) {
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
    private static void ensureParentExists(@NotNull File file) {
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
    private static void saveQuietly(@NotNull YamlConfiguration config, @NotNull File file) {
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
        File backupDir = new File(this.plugin.getDataFolder(), ConfigurationManager.Setting.BACKUP_DIRECTORY.getValue());
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
     * A single {@link SimpleMessage} with one line is serialized as a plain string;
     * complex messages are serialized as a list of maps or a single map if there's only one adapter.
     *
     * @param defaults the list of message type adapters to convert
     * @return a YAML-compatible value (String, List, or Map)
     */
    private static Object toYamlValue(@Nullable List<? extends MessageTypeAdapter> defaults) {
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
     * @param raw      the raw YAML object to parse
     * @param defaults default settings to use for adapters
     * @return a list of parsed message type adapters
     */
    private @NotNull List<MessageTypeAdapter> parseMessageList(@Nullable Object raw, @NotNull MessageSettings defaults) {
        switch (raw) {
            case null -> {
                return List.of();
            }
            case String str -> {
                return List.of(new SimpleMessage(MessageType.TCHAT, List.of(str),
                        defaults.broadcast(), defaults.sendToConsole(), defaults.excludeSenders()));
            }
            case List<?> list -> {
                return this.parseListValue(list, defaults);
            }
            default -> {
            }
        }

        Map<?, ?> map = toMap(raw);
        if (map != null) {
            return this.parseMapValue(map, defaults);
        }

        return List.of();
    }

    /**
     * Parses a raw YAML list value into message type adapters.
     * Handles both plain string lists (treated as a single {@link SimpleMessage}) and
     * lists of maps (each parsed as an individual adapter).
     *
     * @param list     the raw list to parse
     * @param defaults the default settings to apply
     * @return a list of parsed message type adapters
     */
    private @NotNull List<MessageTypeAdapter> parseListValue(@NotNull List<?> list, @NotNull MessageSettings defaults) {
        if (list.isEmpty()) {
            return List.of();
        }

        if (list.getFirst() instanceof String) {
            List<String> lines = list.stream()
                    .filter(e -> e instanceof String)
                    .map(e -> (String) e)
                    .toList();
            return List.of(new SimpleMessage(MessageType.TCHAT, lines,
                    defaults.broadcast(), defaults.sendToConsole(), defaults.excludeSenders()));
        }

        if (list.getFirst() instanceof Map<?, ?>) {
            List<MessageTypeAdapter> result = new ArrayList<>();
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> map)) {
                    continue;
                }
                MessageSettings adapterSettings = this.parseSettings(map, defaults);
                MessageTypeAdapter parsed = this.parseAdapterFromMap(map, adapterSettings);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
            return result;
        }

        return List.of();
    }

    /**
     * Parses a raw YAML map value into message type adapters.
     * Handles wrapped list structures (with a {@code messages} or {@code list} key)
     * as well as single-adapter maps.
     *
     * @param map      the raw map to parse
     * @param defaults the default settings to apply
     * @return a list of parsed message type adapters
     */
    private @NotNull List<MessageTypeAdapter> parseMapValue(@NotNull Map<?, ?> map, @NotNull MessageSettings defaults) {
        Object nested = map.get("messages");
        if (nested == null) {
            nested = map.get("list");
        }
        if (nested instanceof List<?>) {
            return this.parseMessageList(nested, this.parseSettings(map, defaults));
        }

        MessageSettings adapterSettings = this.parseSettings(map, defaults);
        MessageTypeAdapter parsed = this.parseAdapterFromMap(map, adapterSettings);
        return parsed != null ? List.of(parsed) : List.of();
    }

    /**
     * Converts a raw YAML object to a {@link Map}, handling both {@link ConfigurationSection}
     * and plain {@link Map} inputs.
     *
     * @param raw the raw object to convert
     * @return the map representation, or null if conversion is not possible
     */
    private static @Nullable Map<?, ?> toMap(@Nullable Object raw) {
        if (raw instanceof ConfigurationSection section) {
            return section.getValues(true);
        } else if (raw instanceof Map<?, ?> map) {
            return map;
        }
        return null;
    }

    /**
     * Parses a single message type adapter from a map of values.
     *
     * @param map      the map containing message data (type, message content, etc.)
     * @param settings the settings to apply to the adapter
     * @return the parsed message type adapter, or null if parsing failed
     */
    private @Nullable MessageTypeAdapter parseAdapterFromMap(@NotNull Map<?, ?> map, @NotNull MessageSettings settings) {
        Object rawType = map.get("type");
        MessageType type = MessageType.TCHAT;
        if (rawType instanceof String s) {
            try {
                type = MessageType.valueOf(s.toUpperCase(Locale.ROOT));
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

        values.put("broadcast", settings.broadcast());
        values.put("send-to-console", settings.sendToConsole());
        values.put("exclude-senders", settings.excludeSenders());

        try {
            return switch (type) {
                case TITLE -> TitleMessage.deserialize(values);
                case BOSS_BAR -> PlatformType.hasComponent()
                        ? AdventureBossBarMessage.deserialize(values)
                        : LegacyBossBarMessage.deserialize(values);
                case ACTION_BAR, TCHAT, NONE, WITHOUT_PREFIX, BROADCAST -> SimpleMessage.deserialize(type, values);
                case SOUND -> SoundMessage.deserialize(values);
            };
        } catch (Exception e) {
            Logger.warn("Failed to parse message of type %type%: %error%", Placeholder.of("type", type.name(), "error", e.getMessage()));
            return null;
        }
    }

    /**
     * Creates an iterable view of an enum class whose constants implement {@link Message}.
     *
     * @param enumClass the enum class to create an iterable for
     * @param <En>      the enum type
     * @return an iterable over the enum constants
     * @throws IllegalArgumentException if the class is not an enum
     */
    private static <En extends Enum<En> & Message> @NotNull Iterable<En> iterableEnum(@NotNull Class<En> enumClass) {
        En[] constants = enumClass.getEnumConstants();
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
            public En next() {
                return constants[this.i++];
            }
        };
    }
}