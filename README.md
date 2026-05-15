# MessageFlow

[![](https://jitpack.io/v/1robie/MessageFlow.svg)](https://jitpack.io/#1robie/MessageFlow)

## 🌟 Overview

MessageFlow is a robust, multi-module Java framework for Minecraft plugins (Paper/Bukkit) designed to simplify message
management. It provides a centralized way to handle multi-language support, automatic configuration updates, and rich
message formatting using both modern **Adventure** (MiniMessage) and **Legacy** (ChatColor) systems.

## 🚀 Key Features

- **Platform Agnostic**: Automatically detects the environment (Paper vs. Spigot) and chooses the best formatting
  strategy.
- **Rich Message Types**: Built-in support for Chat messages, Titles, Action Bars, and Boss Bars.
- **Dynamic Configuration**: Automatically manages YAML language files—adding missing keys, removing obsolete ones, and
  creating backups.
- **Advanced Logging**: A flexible logging system with:
  - Global and instance-based access.
  - Integrated support for Adventure/Legacy formatting.
  - Customizable visibility for log type tags (e.g., `[INFO]`).
  - Exception logging with red-colored stacktraces for maximum visibility.
- **Clean Architecture**: Highly modular structure separating API, implementation, and models.

---

## 🏗️ Project Structure

- **`core/`**: The main library containing all logic for message parsing, configuration, and sending.
- **`example/`**: A sample implementation showing how to integrate the library into a standard `JavaPlugin`.

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
    private List<? extends MessageTypeAdapter> loaded;

    MyMessages(String key, String defaultValue) {
        this.key = key;
        this.defaults = Message.chat(defaultValue);
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
}
```

Initialize the manager in your plugin:

1) Single language setup:

```java

ConfigurationOptions<String> options = ConfigurationOptions.singleFile("messages.yml");

// Initialize Manager
MessageManager<MyPlugin, String> messageManager = new MessageManager<>(this, this.options, MyMessages.class);
messageManager.reload(); // Creates file and fills missing keys

```

2) Multi-language setup:

With an enum for languages:

```java
// Define languages
EnumLanguageConfiguration<Langs> langConfig = new EnumLanguageConfiguration<>(Langs.class, Langs.EN_US);
ConfigurationOptions<Langs> options = new ConfigurationOptions<>(this.langConfig);

// Initialize Manager
MessageManager<MyPlugin, Langs> messageManager = new MessageManager<>(this, this.options, MyMessages.class);
messageManager.reload(); // Creates files and fills missing keys
```

With string-based language keys:

```java
// Define languages
NormalLanguageConfiguration langConfig = new NormalLanguageConfiguration("en_us"); // -> Default language key
langConfig.addLanguage("en_us","messages_en.yml");
langConfig.addLanguage("fr_fr","messages_fr.yml");

MessageManager<MyPlugin, String> messageManager = new MessageManager<>(this, this.langConfig, MyMessages.class);
messageManager.reload(); // Creates files and fills missing keys

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

formatter.sendMessage(player, MyMessages.WELCOME, "player",player.getName()); // Sends "Welcome to the server, PlayerName!" to the player

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
