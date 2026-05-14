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

        // 1) Standard logging (Only the prefix is colored by default)
        Logger.info("Standard Info message");
        Logger.warn("Standard Warning message");
        Logger.error("Standard Error message");

        // 2) Debug logging (Hidden by default)
        Logger.debug("This won't be visible yet");
        Logger.setDebug(true);
        Logger.debug("Standard Debug message (now visible)");

        // 3) Logging Message objects directly
        // This automatically uses the active language and formats the message
        Logger.info(ExampleMessages.HELLO);

        // 4) Global Color Toggle (Colors the entire log line)
        Logger.setColorWhole(true);
        Logger.info("This entire line is now colored for ALL log types.");
        Logger.setColorWhole(false); // Back to prefix-only

        // 5) Per-Type Color Control
        // You can enable full coloring for specific log types only
        Logger.LogType.ERROR.setColorWholeMessage(true);
        Logger.info("Only the prefix is colored here.");
        Logger.error("This ENTIRE error message is colored!");

        // 6) Toggling and resetting
        Logger.LogType.ERROR.toggleColorWholeMessage(); // Now it's off (prefix only)
        Logger.error("Error with only colored prefix again.");

        Logger.LogType.WARNING.setColorWholeMessage(true);
        Logger.warn("Warning with colored whole message.");
        Logger.LogType.WARNING.resetColorWholeMessage(); // Reset to default (false)
        Logger.warn("Warning back to standard prefix coloring.");

        Logger.info("ExamplePlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        Logger.info("ExamplePlugin disabled!");
    }
}
