package fr.robie.exempleplugin;

import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.configuration.lang.EnumLanguageConfiguration;
import fr.robie.messageflow.impl.MessageManager;
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

        this.getLogger().info("Loaded HELLO first line: " + ExampleMessages.HELLO.firstLine());
    }

    @Override
    public void onDisable() {
        this.getLogger().info("ExamplePlugin disabled!");
    }
}
