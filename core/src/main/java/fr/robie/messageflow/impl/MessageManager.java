package fr.robie.messageflow.impl;

import fr.robie.messageflow.api.IMessageManager;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.formatter.LegacyMessageFormatter;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.model.AdventureBossBarMessage;
import fr.robie.messageflow.model.LegacyBossBarMessage;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.MessageType;
import fr.robie.messageflow.model.SimpleMessage;
import fr.robie.messageflow.model.TitleMessage;
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

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MessageManager<T extends Plugin> implements IMessageManager<T> {
    private final T plugin;
    private final ConfigurationOptions options;
    private final Supplier<? extends Iterable<? extends Message>> messages;
    private final MessageFormatter<T> messageFormatter;

    private String activeLanguage;

    public MessageManager(@NotNull T plugin, @NotNull ConfigurationOptions options, @NotNull Iterable<? extends Message> messages) {
        this.plugin = plugin;
        this.options = options;
        this.messages = () -> messages;
        this.activeLanguage = options.defaultLanguage();
        this.messageFormatter = PlatformType.hasComponent()
                ? new AdventureMessageFormatter<>(plugin)
                : new LegacyMessageFormatter<>(plugin);
    }

    public <E extends Enum<E> & Message> MessageManager(@NotNull T plugin, @NotNull ConfigurationOptions options, @NotNull Class<E> messageEnumClass) {
        this.plugin = plugin;
        this.options = options;
        this.messages = () -> iterableEnum(messageEnumClass);
        this.activeLanguage = options.defaultLanguage();
        this.messageFormatter = PlatformType.hasComponent()
                ? new AdventureMessageFormatter<>(plugin)
                : new LegacyMessageFormatter<>(plugin);
    }

    @Override
    public @NotNull MessageFormatter<T> formatter() {
        return this.messageFormatter;
    }

    @Override
    public void reload() {
        for (Map.Entry<String, String> e : this.options.languageFiles().entrySet()) {
            String lang = e.getKey();
            String relFile = e.getValue();
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

        this.loadLanguage(this.activeLanguage);
    }

    @Override
    public void loadLanguage(@NotNull String languageCode) {
        String lang = ConfigurationOptions.normalizeLanguage(languageCode);
        String rel = this.options.languageFiles().get(lang);
        if (rel == null) {
            lang = this.options.defaultLanguage();
            rel = this.options.languageFiles().get(lang);
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
            this.activeLanguage = lang;
        } else {
            this.activeLanguage = this.options.defaultLanguage();
        }
    }

    @Override
    public @NotNull String activeLanguage() {
        return this.activeLanguage;
    }

    @Override
    public @NotNull List<MessageTypeAdapter> resolve(@NotNull Message message) {
        Objects.requireNonNull(message, "message");
        List<? extends MessageTypeAdapter> loaded = message.loaded();
        if (loaded.isEmpty()) {
            return message.defaults().stream().map(m -> (MessageTypeAdapter) m).toList();
        }
        return loaded.stream().map(m -> (MessageTypeAdapter) m).toList();
    }

    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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

    private void loadLanguageFileIntoMessages(@NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (Message m : this.messages.get()) {
            if (!config.contains(m.key())) {
                m.setLoaded(m.defaults());
                continue;
            }
            m.setLoaded(this.parseMessageList(config, m.key()));
        }
    }

    private void writeAllDefaults(YamlConfiguration config) {
        for (Message m : this.messages.get()) {
            config.set(m.key(), toYamlValue(m.defaults()));
        }
    }

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

    private static void ensureParentExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private static void saveQuietly(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    private boolean tryCopyBundledResource(@NotNull String relativePath) {
        try {
            if (this.plugin.getResource(relativePath) == null) {
                return false;
            }
            this.plugin.saveResource(relativePath, false);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private @Nullable YamlConfiguration loadBundledYaml(@NotNull String relativePath) {
        try (InputStream is = this.plugin.getResource(relativePath)) {
            if (is == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    private void backupFile(@NotNull File file, @NotNull String lang) {
        File backupDir = new File(this.plugin.getDataFolder(), this.options.backupFolder());
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return;
        }

        String baseName = file.getName().replaceAll("\\.yml$", "");
        String stamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
        File dest = new File(backupDir, lang + "_" + baseName + "_" + stamp + ".yml");

        try {
            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

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
            default -> {
            }
        }

        if (raw instanceof ConfigurationSection section) {
            MessageTypeAdapter parsed = this.parseAdapterFromMap(section.getValues(true));
            return parsed != null ? List.of(parsed) : List.of();
        }

        if (raw instanceof Map<?, ?> map) {
            MessageTypeAdapter parsed = this.parseAdapterFromMap(map);
            return parsed != null ? List.of(parsed) : List.of();
        }

        return List.of();
    }

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

        return switch (type) {
            case TITLE -> TitleMessage.deserialize(values);
            case BOSS_BAR ->
                    PlatformType.hasComponent() ? AdventureBossBarMessage.deserialize(values) : LegacyBossBarMessage.deserialize(values);
            case ACTION_BAR, TCHAT, NONE, WITHOUT_PREFIX, BROADCAST -> SimpleMessage.deserialize(type, values);
        };
    }

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
