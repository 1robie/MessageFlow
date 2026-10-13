# MessageFlow

[![](https://jitpack.io/v/1robie/MessageFlow.svg)](https://jitpack.io/#1robie/MessageFlow)

## 🌟 Overview

MessageFlow is a robust, multi-module Java framework for Minecraft plugins (Paper/Bukkit) designed to simplify message
management. It provides a centralized way to handle multi-language support, automatic configuration updates, and rich
message formatting using both modern **Adventure** (MiniMessage) and **Legacy** (ChatColor) systems.

## 🚀 Key Features

- **Platform Agnostic**: Automatically detects the environment (Paper vs. Spigot) and chooses the best formatting
  strategy (Adventure if available, Legacy otherwise).
- **Rich Message Types**: Built-in support for Chat messages, Titles, Action Bars, broadcast, sound, boss bars, and without prefix messages.
- **Fluent Message Builder**: Create complex messages (combining chat, title, sounds) on-the-fly.
- **Dynamic Configuration**: Automatically manages YAML language files—adding missing keys, removing obsolete ones, and creating backups.
- **Flexible Placeholders**: A powerful placeholder system that supports static values, dynamic suppliers, player-specific placeholders, **Global Placeholders**, and **modifiers** (`%coins.comma%`, `%count.plural:item:items%`).
- **Fast**: messages are parsed once and reused, even with placeholders; a broadcast is formatted once for everyone unless a placeholder depends on the player.
- **Folia and async**: runs on Folia, and `sendMessageAsync` sends from any thread.
- **Granular Delivery Settings**: Control broadcast, console logging, and sender exclusion per-message or per-component.
- **Advanced Logging**: A flexible logging system with:
  - Global and instance-based access.
  - Integrated support for Adventure/Legacy formatting.
  - Customizable visibility for log type tags (e.g., `[INFO]`).
  - Exception logging with red-colored stacktraces for maximum visibility.
- **Clean Architecture**: Highly modular structure separating API, implementation, and models.
- **Hooks System**: Hook into `PlaceholderAPI` so each message can be parsed by PAPI before being sent.

---

## 🏗️ Project Structure

- **`core/`**: The main library containing all logic for message parsing, configuration, and sending.
- **`example/`**: A sample implementation showing how to integrate the library into a standard `JavaPlugin`.

---

## Breaking Changes

Version 1.0.0 introduced major changes to the API, including:

- **New Placeholder System**: The old `Object[]` or `Placeholders` system has been replaced by a dedicated `Placeholder` class.
  `formatter.sendMessage(player, MyMessages.WELCOME, "player", player.getName())` becomes
  `formatter.sendMessage(MyMessages.WELCOME, player, Placeholder.of("player", player.getName()))`.
- **Simplified Initialization**: `ConfigurationOptions.singleFile(...)` is deprecated in favor of `MessageManager.withSingleFile(...)`.
- **Global Placeholders**: Register placeholders once via `GlobalPlaceholderRegistry` to use them across all messages.

```java
// Modern Placeholder usage
Placeholder p = Placeholder.builder()
    .register("player", player.getName())
    .register("server_version", Bukkit.getVersion())
    .register("dynamic_value", () -> "someValue")
    .build();
```

---

## ⚙️ Library Configuration

You can customize the library's behavior using `ConfigurationManager.Setting` **before** initializing your `MessageManager`.

```java
// Example: Customize settings before initialization
ConfigurationManager.Setting.SYNC_AUTO_ADD_MISSING.setDefaultValue(true);
ConfigurationManager.Setting.BACKUP_ENABLED.setDefaultValue(false);

// If false (default), MessageFlow creates a 'messageflow.yml' file 
// allowing users to customize these settings.
// If true, file operations are skipped and only code defaults are used.
ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(false); 
```

### User Customization
By default (`BYPASS_EXTERNAL_CONFIG = false`), MessageFlow automatically generates a configuration file at `plugins/YourPlugin/messageflow/messageflow.yml`. This allows end-users to modify library behavior (like cache sizes or sync options) without you needing to create a custom configuration bridge.

Key Settings available in `messageflow.yml`:
- `sync.auto-add-missing`: Automatically adds missing keys to YAML files.
- `sync.auto-remove-obsolete`: Removes keys from YAML that are no longer defined in code.
- `backup.enabled`: Creates backups before modifying files.
- `cache.messages.maximum-size`: Control message caching for performance.



---

## 💻 Installation

### 1. Add Dependency

MessageFlow is available via JitPack.

#### **Maven**

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.1robie.MessageFlow</groupId>
    <artifactId>message-flow</artifactId>
    <version>VERSION_TAG</version>
    <scope>compile</scope>
</dependency>
```

#### **Gradle (Groovy)**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.1robie.MessageFlow:message-flow:VERSION_TAG'
}
```

#### **Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.1robie.MessageFlow:message-flow:VERSION_TAG")
}
```

### 2. Relocation (Required)

To avoid conflicts with other plugins using different versions of MessageFlow, you **MUST** relocate (shade) the library
into your plugin's package.

#### **Maven (using Maven Shade Plugin)**

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>fr.robie.messageflow</pattern>
                        <shadedPattern>your.package.messageflow</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### **Gradle (using Shadow Plugin)**

```groovy
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

tasks.withType(ShadowJar) {
    relocate 'fr.robie.messageflow', 'your.package.messageflow'
}
```

---

## 🛠️ Usage Examples

### 1. Initializing MessageManager

Define your messages in an Enum:

```java
public enum MyMessages implements Message {
  WELCOME("welcome", "Welcome to the server, %player%!"),
  ALERT("alert", "This is an alert!");

  private final String key;
  private final List<? extends MessageTypeAdapter> defaults;
  private final MessageSettings defaultSettings;
  private List<? extends MessageTypeAdapter> loaded;
  private MessageSettings loadedSettings;

  MyMessages(String key, String defaultValue) {
      this.key = key;
      this.defaults = Message.chat(defaultValue);
      this.defaultSettings = MessageSettings.DEFAULT;
  }

  @Override
  public @NotNull String key() {
      return this.key;
  }

  @Override
  public @NotNull List<? extends MessageTypeAdapter> defaults() {
      return this.defaults;
  }

  @Override
  public @NotNull List<? extends MessageTypeAdapter> loaded() {
      return this.loaded != null ? this.loaded : this.defaults;
  }

  @Override
  public void setLoaded(@NotNull List<? extends MessageTypeAdapter> loaded) {
      this.loaded = loaded;
  }

  @Override
  public void setSettings(@NotNull MessageSettings settings) {
    this.loadedSettings = settings;
  }

  @Override
  public @NotNull MessageSettings settings() {
    return this.loadedSettings != null ? this.loadedSettings : this.defaultSettings;
  }
}
```

Initialize the manager in your plugin:

1) **Single language setup (Recommended)**:

```java
// Initialize Manager with a single file
MessageManager<MyPlugin, String> messageManager = MessageManager.withSingleFile(this, "messages.yml", MyMessages.class);
messageManager.reload(); // Creates file and fills missing keys
```

Call `messageManager.close()` in `onDisable` (or before building a new manager on reload): it releases the global logger.

2) **Multi-language setup**:

With an enum for languages:

```java
// Define languages
EnumLanguageConfiguration<Langs> langConfig = new EnumLanguageConfiguration<>(Langs.class, Langs.EN_US);

// Initialize Manager
MessageManager<MyPlugin, Langs> messageManager = new MessageManager<>(this, langConfig, MyMessages.class);
messageManager.reload(); 

messageManager.setActiveLanguage(Langs.FR_FR); // Switch to French and load french messages
```

With string-based language keys:

```java
// Define languages
NormalLanguageConfiguration langConfig = new NormalLanguageConfiguration("en_us"); // Default language key
langConfig.addLanguage("en_us","messages_en.yml");
langConfig.addLanguage("fr_fr","messages_fr.yml");

MessageManager<MyPlugin, String> messageManager = new MessageManager<>(this, langConfig, MyMessages.class);
messageManager.reload();

messageManager.setActiveLanguage("fr_fr"); // Switch to French and load french messages
```


3) **Per-player languages** (each player reads their client's language):

```java
NormalLanguageConfiguration langConfig = new NormalLanguageConfiguration("en_us");
langConfig.addLanguage("en_us", "lang/en_us.yml");
langConfig.addLanguage("fr_fr", "lang/fr_fr.yml");

MessageManager<MyPlugin, String> messageManager = new MessageManager<>(this, langConfig, MyMessages.class);
messageManager.setPerPlayerLanguages(true);
messageManager.reload();
messageManager.setActiveLanguage("en_us"); // the default language: console, unknown client languages
```

A `fr_ca` client reads `fr_fr`; a language without a file, or a key missing from it, falls back to the default
language. Missing keys are only written into the default language's file.

To let code that only has Adventure send your messages, register a translator: every
`Component.translatable("myplugin.<key>", args...)` is then rendered from your files, in each player's language,
with argument `i` in placeholder `%i%`.

```java
messageManager.registerTranslator("myplugin.");
player.sendMessage(Component.translatable("myplugin.welcome", Component.text(player.getName())));
```

### 2. Powerful Logging

```java
// 1. Basic logging (INFO, WARN, ERROR, DEBUG)
Logger.info("Server started!");
Logger.error("A critical error occurred",someThrowable); // Stacktrace is red!

// 2. Formatting control
Logger.setColorWhole(true); // Entire line becomes colored
Logger.LogType.ERROR.setColorWholeMessage(true); // Default: true for Errors

// 3. Prefix customization
Logger.setShowTypeNamesGlobal(false); // Hides [INFO], [WARN], etc.
Logger.LogType.DEBUG.setShowTypeName(true); // Show only for debug

// 4. Instance-based logging
Logger myLogger = Logger.getLogger();
myLogger.logInfo("Local log message");

// 5. Debug lines that are only built when debug is on
Logger.debug("[tick]", () -> "Took " + nanos + " ns");

// 6. Without a MessageManager (unit tests): plain lines to a java.util.logging logger
Logger testLogger = Logger.jul(java.util.logging.Logger.getLogger("Test"));

// 7. For an API that takes a java.util.logging.Logger
someApi.load(path, myLogger.toJul());
```

Exceptions go to the plugin's `java.util.logging` logger with the exception attached, so log files show a
normal stack trace. Calls made before the `MessageManager` exists go to a `java.util.logging` logger named
`MessageFlow`.

### 3. Sending Messages

```java
MessageManager<MyPlugin, String> messageManager = ...; // Initialized as shown above

@SuppressWarnings("DataFlowIssue")
MessageFormatter<MyPlugin, ?> formatter = this.messageManager.formatter();

Placeholder placeholders = Placeholder.of("player", player.getName());

formatter.sendMessage(MyMessages.WELCOME, player, placeholders); // Sends "Welcome to the server, PlayerName!" to the player
```

Placeholder values are inserted as plain text: tags or color codes inside a value are shown as typed, so a
player name can't inject `<click:...>`. To insert a styled value, pass a component:

```java
formatter.sendMessage(MyMessages.WELCOME, player, Placeholder.of("player", player.displayName()));
```

### Formatting without sending

```java
// One Component (text lines joined by line breaks), or one per line (for item lore)
Component text = messageManager.adventure().formatMessage(MyMessages.WELCOME, player, placeholders);
List<Component> lore = messageManager.adventure().formatLines(MyMessages.WELCOME, player, placeholders);
```

### Placeholder modifiers

Add modifiers after a dot to change a value, in local and global placeholders. Arguments follow colons, and
modifiers chain:

```yaml
balance: "<gold>%coins.comma% coins"              # 1234567 -> 1,234,567
items: "%count% %count.plural:item:items%"          # 1 item, 3 items
objets: "%count% %count.plural:objet:objet:objets%" # zero form: 0 objet, 1 objet, 2 objets
name: "%name.default:Nobody.upper%"                 # NOBODY when the name is blank
```

Built in: `upper`, `lower`, `capitalize`, `trim`, `length`, `empty`, `isnumber`, `safe`, `default:text`, `max:n`,
`pad:n`, `int`, `round`, `floor`, `ceil`, `abs`, `0f`-`3f`, `comma`, `add:n`, `sub:n`, `add_one`, `remove_one`,
`negative`, `even`, `plural`. An unknown or failing modifier leaves the token as typed, so mistakes are visible.
Register your own with `PlaceholderModifiers.register("name", (value, args) -> ...)`.

### Sending off the main thread

```java
// Formats and sends from an async thread; sounds and boss bars are handed to each player's thread.
formatter.sendMessageAsync(MyMessages.EVENT_START, Bukkit.getOnlinePlayers(), placeholders)
        .exceptionally(error -> { getLogger().warning("Send failed: " + error); return null; });
```

Placeholder values (and PlaceholderAPI expansions) are evaluated on the async thread: they must not change the world.

MessageFlow runs on **Folia**: sounds and boss bars use each player's region scheduler.

### 4. Global Placeholders

Register placeholders that can be used in **any** message across the plugin.

```java
GlobalPlaceholderRegistry registry = GlobalPlaceholderRegistry.getInstance();

// Static
registry.register("server_name", "My Awesome Server");

// Dynamic (Supplier)
registry.register("online_players", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));

// Player-specific (Function)
registry.registerPlayer("ping", player -> String.valueOf(player.getPing()));

// Cached dynamic placeholder
registry.registerCached("tps", () -> String.format("%.2f", getServerTPS()), 5, TimeUnit.SECONDS);

// Cached player-specific placeholder
registry.registerPlayerCached("health", player -> String.valueOf(player.getHealth()), 1, TimeUnit.SECONDS);
```

### 5. Fluent Message Builder

Create and send multi-component messages on-the-fly without pre-defining them in an Enum.

```java
messageManager.builder()
    .chat("<green>Success!</green> You received a reward.")
    .actionBar("<gold>+%reward%</gold>")
    .sound("entity.experience_orb.pickup")
    .placeholder("reward", "100 Coins")
    .broadcast(true) // Broadcast to everyone
    .send(player);
```


And more choices for sending message, broadcasting, sending titles, action bars, boss bars, etc. Check the Javadoc for
`MessageFormatter` for all available methods.

---

## 🔨 Building the Project

- **Build All**: `mvn clean package`
- **Install to Local Repo**: `mvn install`
- **Dev Build**: `mvn clean package -Pdev-build`
- **Benchmarks**: `mvn -P benchmarks package -DskipTests` then `java -jar benchmarks/target/benchmarks.jar`

Outputs:

- `core/target/message-flow.jar`
- `example/target/ExamplePlugin.jar`

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](./LICENSE) file for details.

---

## 🙌 Acknowledgments

- **JitPack.io**: Distribution made simple.
- **Papermc.io**: Optimized for modern Paper servers.

### Authors

* [1robie](https://github.com/1robie)
