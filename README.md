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
- **Flexible Placeholders**: A powerful placeholder system that supports static values, dynamic suppliers, player-specific placeholders, and **Global Placeholders**.
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
  `formatter.sendMessage(player, MyMessages.WELCOME, Placeholder.of("player", player.getName()))`.
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
    <groupId>com.github.1robie</groupId>
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
    implementation 'com.github.1robie:message-flow:VERSION_TAG'
}
```

#### **Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.1robie:message-flow:VERSION_TAG")
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
  private final MessageSettings settings;
  private List<? extends MessageTypeAdapter> loaded;

  MyMessages(String key, String defaultValue) {
      this.key = key;
      this.defaults = Message.chat(defaultValue);
      this.settings = MessageSettings.DEFAULT;
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
    return this.loadedSettings != null ? this.loadedSettings : this.staticSettings;
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

2) **Multi-language setup**:

With an enum for languages:

```java
// Define languages
EnumLanguageConfiguration<Langs> langConfig = new EnumLanguageConfiguration<>(Langs.class, Langs.EN_US);

// Initialize Manager
MessageManager<MyPlugin, Langs> messageManager = new MessageManager<>(this, this.langConfig, MyMessages.class);
messageManager.reload(); 

messageManager.setActiveLanguage(Langs.FR_FR); // Switch to French and load french messages
```

With string-based language keys:

```java
// Define languages
NormalLanguageConfiguration langConfig = new NormalLanguageConfiguration("en_us"); // Default language key
langConfig.addLanguage("en_us","messages_en.yml");
langConfig.addLanguage("fr_fr","messages_fr.yml");

MessageManager<MyPlugin, String> messageManager = new MessageManager<>(this, this.langConfig, MyMessages.class);
messageManager.reload();

messageManager.setActiveLanguage("fr_fr"); // Switch to French and load french messages
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
```

### 3. Sending Messages

```java
MessageManager<MyPlugin, String> messageManager = ...; // Initialized as shown above

@SuppressWarnings("DataFlowIssue")
MessageFormatter<MyPlugin, ?> formatter = this.messageManager.formatter();

Placeholders.Builder placeholders = Placeholders.builder();
placeholders.register("player", player.getName());

formatter.sendMessage(player, MyMessages.WELCOME, placeholders.build()); // Sends "Welcome to the server, PlayerName!" to the player
```

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
