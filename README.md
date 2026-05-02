# MessageFlow

[![](https://jitpack.io/v/1robie/MessageFlow.svg)](https://jitpack.io/#1robie/MessageFlow)

## 🌟 Overview

MessageFlow is a dynamic and modular Java library specifically designed for flexible message management, localization,
and formatting. The library is ideal for integrating Adventure or Legacy text systems, making it an excellent choice for
Java developers looking to support styled and localized messaging.

## 🚀 Features

- **Multi-Formatter Support**: Easily use the built-in `MessageFormatter` for dynamic text rendering.
- **Extensibility**: Architected with modularity to allow custom enhancements while keeping core features available.
- **Out-of-the-box Localization**: Supports YAML-based language files for seamless localization.
- **Integration Ready**: Plug-and-play support for Bukkit and Paper plugins.

### Core Components

1. **`MessageFormatter` Class**: A comprehensive base class for managing custom message handling and caching.
2. **`ConfigurationOptions`**: Simplifies the tuning of caching behavior for message formatting.
3. **YAML Configuration Support**: Intuitive and concise message file structures for localization.

---

## 🛠️ Project Modules

1. **Core Module**
    - Defines message formatting, types (e.g., `TitleMessage`, `BossBarMessage`), and plugin hooks.
    - Produces the main `message-flow.jar` artifact.

2. **Example Module**
    - Demonstrates practical integrations of `MessageFlow` through a simple plugin.
    - Produces `ExamplePlugin.jar` output.
    - Example language files located under `example/src/main/resources/lang/`.

---

## 📋 Table of Contents

1. [Installation](#-installation)
2. [Build & Test](#-build--test)

---

## 💻 Installation

### Maven Dependency

MessageFlow can be added to your project via JitPack:

Add JitPack to the repositories in your `pom.xml` file:

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then, include the following dependency into your project configuration:

```xml

<dependency>
    <groupId>com.github.1robie</groupId>
    <artifactId>MessageFlow</artifactId>
    <version>VERSION_TAG</version>
</dependency>
```

Replace `VERSION_TAG` with the latest version displayed at the badge above.

---

## 🏗️ Build & Test

Run the following commands to build the library and validate everything functions as intended:

- **Build the Project**:

```bash
mvn clean package
```

- **Run Unit Tests**:

```bash
mvn test
```

- **Developer Build (Dev Suffix):**

```bash
mvn clean package -Pdev-build
```

---

## 📖 Example Usage

### 1. YAML Configurations

The following is an example of YAML-based message definitions for localization:

```yaml
example_message: "Hello, {player}!"
boss_bar.title: "Loading..."
title_message.header: "Welcome Back, Adventurer"
```

Modify `lang/<locale>.yml` files to manage other languages dynamically.

//TODO: Continue that

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](./LICENSE) file for details.

---

## 🙌 Acknowledgments

- **JitPack.io**: Distribution made simple through JitPack.
- **Papermc.io**: Example plugin powered by Paper API.

### Authors

* [1robie](https://github.com/1robie)

For further questions or support, please open an issue or reach out!