package fr.robie.exempleplugin;

import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.configuration.lang.EnumLanguageConfiguration;
import fr.robie.messageflow.impl.MessageManager;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getLogger().info("ExamplePlugin enabled!");

        // --- Choose ONE of the configurations below ---

        // 1) Single file at plugin root (plugins/ExamplePlugin/messages.yml)
//        ConfigurationOptions<String> options = ConfigurationOptions.singleFile("messages.yml");

        // 2) Multi-language with custom paths
        EnumLanguageConfiguration<Langs> langsEnumLanguageConfiguration = new EnumLanguageConfiguration<>(Langs.class, Langs.EN_US);

        ConfigurationOptions<Langs> options = new ConfigurationOptions<>(langsEnumLanguageConfiguration);

        MessageManager<ExamplePlugin, Langs> examplePluginLangsMessageManager = new MessageManager<>(this, options, ExampleMessages.class);

        examplePluginLangsMessageManager.reload();                  // creates files + fills missing keys
        examplePluginLangsMessageManager.loadLanguage(Langs.EN_US); // switch active language (optional

        Logger.info("Loaded HELLO first line: " + ExampleMessages.HELLO.firstLine());

        // --- Logging Examples ---

        // 1) Standard logging (Only the prefix is colored by default, EXCEPT for ERROR which is colored entirely)
        Logger.info("Standard Info message");
        Logger.warn("Standard Warning message");
        Logger.error("Standard Error message (Entirely red by default)");

        // 2) Debug logging (Hidden by default)
        Logger.debug("This won't be visible yet");
        Logger.setDebug(true);
        Logger.debug("Standard Debug message (now visible)");

        // 3) Logging Message objects directly
        // This automatically uses the active language and formats the message
        Logger.info(ExampleMessages.HELLO);

        // --- Fluent Builder Examples ---
        // Allows sending messages on-the-fly without pre-defining them in an Enum
        examplePluginLangsMessageManager.builder()
                .chat("<green>Fluent Builder: <white>This message was sent using the builder!</white>")
                .placeholder("author", "1robie")
                .actionBar("<gold>Author: %author%")
                .sound("entity.experience_orb.pickup")
                .send(this.getServer().getConsoleSender());

        // 4) Global Color Toggle (Colors the entire log line)
        Logger.setColorWhole(true);
        Logger.info("This entire line is now colored for ALL log types.");
        Logger.setColorWhole(false); // Back to prefix-only

        // 5) Per-Type Color Control
        // You can change full coloring for specific log types
        Logger.LogType.WARNING.setColorWholeMessage(true);
        Logger.warn("This ENTIRE warning message is now colored!");
        Logger.LogType.ERROR.setColorWholeMessage(false);
        Logger.error("Now only the error prefix is colored.");

        // 6) Toggling and resetting
        Logger.LogType.ERROR.toggleColorWholeMessage(); // Now it's back to entirely red
        Logger.error("Error is entirely red again.");

        Logger.LogType.WARNING.resetColorWholeMessage(); // Reset to default (false)
        Logger.warn("Warning back to standard prefix coloring.");
        Logger.LogType.ERROR.resetColorWholeMessage(); // Reset to default (true)
        Logger.error("Error is still entirely red after reset.");

        // 7) Log Type Name Visibility
        // You can hide the [INFO], [WARN], etc. part of the prefix
        Logger.setShowTypeNamesGlobal(false);
        Logger.info("This info message has no [INFO] tag in the prefix.");
        Logger.setShowTypeNamesGlobal(true); // Restore

        // Per-type control
        Logger.LogType.DEBUG.setShowTypeName(false);
        Logger.debug("This debug message has no [DEBUG] tag.");
        Logger.LogType.DEBUG.toggleShowTypeName(); // Restore to true

        // 8) Exception Logging
        // Stacktraces are automatically colored red for better visibility
        try {
            throw new RuntimeException("Example Exception");
        } catch (RuntimeException e) {
            Logger.error("An error occurred during the example", e);
            Logger.debug("Debug information with stacktrace", e);
        }

        Logger.info("ExamplePlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        Logger.info("ExamplePlugin disabled!");
    }
}
