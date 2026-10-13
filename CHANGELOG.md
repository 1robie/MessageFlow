# Changelog

## Unreleased

- **Logger without a manager.** Static `Logger` calls made before a `MessageManager` exists (or after `close()`) go to a plain `java.util.logging` logger named `MessageFlow` instead of being dropped. `Logger.jul(java.util.logging.Logger)` makes such a logger for code that runs without a manager, such as unit tests: placeholders are replaced and tags removed.
- **Exceptions** are logged through the plugin's `java.util.logging` logger with the exception attached to the record (log files and panels show a real stack trace), instead of a red stack trace in console text.
- New: `Logger#toJul()`, a `java.util.logging.Logger` view that forwards to a MessageFlow logger, for APIs that require one. Record text is a value, never parsed as tags.
- New: `Logger.debug(Supplier<String>)` and `Logger#logDebug(Supplier<String>)`: the message is only built when debug is on.
- New: `MessageFormatter#formatPlain` and `#formatPlainLines`.
- **Per-player languages.** `MessageManager#setPerPlayerLanguages(true)` loads every language file and sends each recipient the message in their client's language: the exact language (`fr_ca`), else the first registered language of the same language (`fr_*`), else the active language, which is the default language. The console uses the default language. A message missing from a language file falls back to the default language; missing keys are only added to the default language's file, and other language files are only created from bundled resources.
- **Translator bridge.** `MessageManager#registerTranslator(keyPrefix)` renders `Component.translatable(key, args...)` server-side, in each receiver's language, for message keys starting with the prefix (argument `i` fills `%i%`). Works with the Adventure of Paper 1.20.6 (4.17) and of Paper 26 (5.x). `close()` removes it.
- New: `MessageManager#message(key)`, `MessageFormatter#formatLines(List<MessageTypeAdapter>, Player, Placeholder)`, the `MessageLocalizer` interface (`MessageFormatter#setLocalizer`).
- `MessageFormatter#sendMessage(Message, Collection, boolean, Placeholder)` is no longer abstract: subclasses implement `dispatch(...)` instead.
- `NormalLanguageConfiguration` keeps languages in registration order, and `getRelativePath` accepts any spelling of a language code.
- **Behaviour change (Adventure):** local placeholder values (`Placeholder.of(...)`) are inserted as plain text after the message is parsed. A value can no longer add MiniMessage tags or legacy color codes, so a player name or any typed text can't inject `<click:run_command:...>`. Pass a `Component` (`Placeholder.of(key, component)`) for a styled value. Inside a tag argument (`<click:run_command:'/tp %player%'>`) the value is still text, minus `< > ' " \`. Global placeholders and PlaceholderAPI are unchanged.
- Local player placeholders (`Placeholder.of(key, Function<Player, String>)`) get the player the message is for. Before, they always resolved to `""`.
- New: `MessageFormatter#formatLines(Message, Player, Placeholder)` and `#formatMessage(...)` format a message's text lines without sending them (item lore, menus, any API that takes a component). `MessageManager#adventure()` returns the formatter typed for `Component`.
- New: `Placeholder.of(key, Component)`, `Placeholder.Builder#register(key, Component)`, `#registerAll(Placeholder)` and `Placeholder#get(key)`.
- Runs on Paper 1.20.6: `SoundMessage` no longer calls `Sound#key()` (an interface method only since 1.21.3).
- On servers without `getPluginMeta()` (Spigot) the logger prefix falls back to the plugin description instead of failing.
- The published `message-flow` pom no longer declares `common` and `BOM`: their classes are already in the jar.
- JitPack builds with JDK 21 (`jitpack.yml`). The README shows the real coordinates, `com.github.1robie.MessageFlow:message-flow`.
- A plugin can build a new `MessageManager` (on reload, for example) without an `IllegalStateException`: the new logger replaces the old one. A logger of *another* plugin still throws (the library is not relocated).
- New `IMessageManager#close()` and `Logger#close()`: release the global logger (call it in `onDisable`).
- `reload()` no longer clears the global placeholders (`GlobalPlaceholderRegistry`): every reload, the first one included, used to wipe what the plugin registered.
- Text resolvers run in the documented order (global placeholders, then PlaceholderAPI, then later ones) and can be registered from any thread.
- The parsed-message cache is used again: it was skipped whenever any resolver was registered, which was always. `clearCache()` now really clears it.
- The legacy logger prefix placeholder `%plugin-full-name%` is resolved.
- `BROADCAST` messages honour `send-to-console` and `exclude-senders`.
- Legacy (Spigot) formatter: sounds play, boss bar durations are in ticks like the Adventure one, and `getLegacyColoredMessage` no longer converts twice.
- README samples match the API.
- Change all `toLowerCase()` and `toUpperCase()` calls to use `Locale.ROOT` to avoid locale-specific issues.

## Releases:

### 1.0.0
- Refactored `Logger` system to centralize logging logic and eliminate code duplication between `AdventureLogger` and `LegacyLogger`.
- Added support for a per-method `context` parameter in all logging methods (info, warn, error, debug).
- New function `String getLegacyColoredMessage(@NotNull String message, @Nullable Player player, @NotNull Placeholder placeholders)` inside `MessageFormatter` to get a legacy colored message with support for placeholders and player.
- New function `Component getComponent(@Nullable String message, @Nullable Player player, @NotNull Placeholder placeholders)` inside `AdventureMessageFormatter` to get a component from a string message, with support for legacy color codes, placeholders and player.
- Deprecate all setter/getter methods inside `ConfigurationOptions` in favor of a better way to configurate options using `ConfigurationManager.Setting.<name>.setDefault(value)` and `ConfigurationManager.Setting.<name>.getDefaultValue()`.
- Allow if the programmer wants to let user customize some settings for the library inside a yml file. Automatically create the file if it doesn't exist and fill missing keys with default values. 
- New `Placeholder` class to represent placeholders in messages. It has a name and a value, and can be used to replace placeholders in messages with their corresponding values.
- Support for `PlaceholderAPI` in each message.
- New `GlobalPlaceholderRegistry` class to register global placeholders that can be used in any message. Support for static placeholders, dynamic values and player dynamic placeholders.
- New `IMessageBuilder` class to create messages using a builder pattern. It allows to set the message type, the content, the placeholders and other options in a fluent way.
- New granular message delivery settings: `broadcast`, `send-to-console`, and `exclude-senders`.
- Settings can now be configured per-component (e.g., specific title or chat line) or globally for a message.
- Refactored message model from records to classes with an `AbstractMessageTypeAdapter` to centralize common delivery logic and settings.
- Added fluent builder methods: `withBroadcast()`, `withSendToConsole()`, and `withExcludeSenders()` for per-component configuration.
- New `SoundMessage` class to represent messages that play a sound when sent. It has a sound, a volume and a pitch, and can be used to play sounds to players when sending messages.
```yaml
sound: "entity.experience_orb.pickup"
# category: MASTER
# volume: 1.0
# pitch: 1.0
# broadcast: false
# send-to-console: false
# exclude-senders: false
```