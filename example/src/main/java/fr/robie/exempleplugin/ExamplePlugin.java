package fr.robie.exempleplugin;

import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.impl.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {
    private MessageManager<ExamplePlugin> messages;

    @Override
    public void onEnable() {
        this.getLogger().info("ExamplePlugin enabled!");

        // --- Choose ONE of the configurations below ---

        // 1) Single file at plugin root (plugins/ExamplePlugin/messages.yml)
        // ConfigurationOptions options = ConfigurationOptions.singleFile("messages.yml");

        // 2) Multi-language with custom paths
        ConfigurationOptions options = new ConfigurationOptions()
                .addLanguage("en_US", "lang/en_us.yml")
                .addLanguage("fr_FR", "lang/fr_fr.yml")
                .defaultLanguage("en_US");

        this.messages = new MessageManager<>(this, options, ExampleMessages.class);
        this.messages.reload();              // creates files + fills missing keys
        this.messages.loadLanguage("en_US"); // switch active language (optional)

        this.getLogger().info("Loaded HELLO first line: " + ExampleMessages.HELLO.firstLine());
    }

    @Override
    public void onDisable() {
        this.getLogger().info("ExamplePlugin disabled!");
    }
}
