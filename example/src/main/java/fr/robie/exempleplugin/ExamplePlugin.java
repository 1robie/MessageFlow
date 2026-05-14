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

        // 1) Standard logging (getPrefix only colored)
        Logger.info("Standard Info message");
        Logger.warn("Standard Warning message");
        Logger.error("Standard Error message");
        Logger.debug("Standard Debug message (hidden by default)");

        // 2) Debug control
        Logger.setDebug(true);
        Logger.debug("Standard Debug message (now visible)");

        // 3) Colored logging (whole message colored)
        Logger.setColorWhole(true);
        Logger.info("Colored Info message");
        Logger.warn("Colored Warning message");
        Logger.error("Colored Error message");
        Logger.debug("Colored Debug message");

        // 4) Toggling and resetting
        Logger.toggleDebug();
        Logger.debug("Debug message (hidden again)");
        Logger.setColorWhole(false);
        Logger.info("Back to standard coloring");

        Logger.info(ExampleMessages.HELLO);
    }

    @Override
    public void onDisable() {
        Logger.info("ExamplePlugin disabled!");
    }
}
