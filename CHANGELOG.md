# Changelog

## [Unreleased]
- Deprecate all setter/getter methods inside `ConfigurationOptions` in favor of a better way to configurate options using `ConfigurationManager.Setting.<name>.setDefault(value)` and `ConfigurationManager.Setting.<name>.getDefaultValue()`.
- Allow if the programmer wants to let user customize some settings for the library inside a yml file. Automatically create the file if it doesn't exist and fill missing keys with default values. 
- New `Placeholder` class to represent placeholders in messages. It has a name and a value, and can be used to replace placeholders in messages with their corresponding values.
- Support for `PlaceholderAPI` in each message.
- New `GlobalPlaceholderRegistry` class to register global placeholders that can be used in any message. Support for static placeholders, dynamic values and player dynamic placeholders.
- New `IMessageBuilder` class to create messages using a builder pattern. It allows to set the message type, the content, the placeholders and other options in a fluent way.
- New `SoundMessage` class to represent messages that play a sound when sent. It has a sound, a volume and a pitch, and can be used to play sounds to players when sending messages.
```yaml
sound: "entity.experience_orb.pickup"
# category: MASTER
# volume: 1.0
# pitch: 1.0
```