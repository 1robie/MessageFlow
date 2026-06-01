# Changelog

## [Unreleased]
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