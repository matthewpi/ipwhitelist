package io.matthewp.ipwhitelist;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This enum manages the configuration for the IPWhitelist plugin.
 */
public enum Config {
    DEBUG("debug", false),
    ALLOWED_IPS("allowed_ips", new ArrayList<String>()),
    REFRESH_DELAY("refresh_delay", 5),
    KICK_MESSAGE("kick_message", "&cYou must connect through the proxy."),
    ;

    private static YamlConfiguration CONFIG;

    @Getter private final String path;
    @Getter private final Object def;

    /**
     * Adds a new config value
     *
     * @param path Path in the configuration file
     * @param def  Default value
     */
    Config(final String path, final Object def) {
        this.path = path;
        this.def = def;
    }

    /**
     * Gets the value of the config value.
     *
     * @return Configuration value
     */
    @Override
    public String toString() {
        return CONFIG.getString(this.getPath(), String.valueOf(this.getDef()));
    }

    public int intValue() {
        return CONFIG.getInt(this.getPath(), Integer.parseInt(String.valueOf(this.getDef())));
    }

    public boolean booleanValue() {
        return CONFIG.getBoolean(this.getPath(), Boolean.parseBoolean(String.valueOf(this.getDef())));
    }

    @SuppressWarnings("unchecked")
    public List<String> stringList() {
        if (!CONFIG.contains(this.getPath())) {
            return (List<String>) this.getDef();
        }

        return CONFIG.getStringList(this.getPath());
    }

    public static void init(@NonNull final JavaPlugin plugin) {
        final File dataFolder = plugin.getDataFolder();
        final File file = new File(dataFolder, "config.yml");

        if (!file.exists()) {
            try {
                // Create the data directories, ignore the return result because it is unreliable.
                dataFolder.mkdirs();

                // Log that we are creating the config.yml file.
                plugin.getLogger().info("config.yml does not exist, creating..");

                // Create the new file and check if it was created successfully.
                if (file.createNewFile()) {
                    plugin.getLogger().info("config.yml was created successfully.");
                } else {
                    plugin.getLogger().severe("Failed to create config.yml.");
                }
            } catch (final IOException ex) {
                plugin.getLogger().severe("Failed to load config.yml.");
                plugin.getLogger().severe("-------------------------------------------");
                ex.printStackTrace();
                plugin.getLogger().severe("-------------------------------------------");
            }
        }

        // Load the config.yml configuration file.
        CONFIG = YamlConfiguration.loadConfiguration(file);

        // Loop through all enum values.
        Stream.of(values()).forEach((item) -> {
            // Check if the value is null.
            if (CONFIG.getString(item.getPath()) == null) {
                CONFIG.set(item.getPath(), item.getDef());
            }
        });

        // Attempt to save the configuration.
        try {
            CONFIG.save(file);
            plugin.getLogger().info("config.yml was saved successfully.");
        } catch (final IOException ex) {
            plugin.getLogger().severe("Failed to save config.yml.");
            plugin.getLogger().severe("-------------------------------------------");
            ex.printStackTrace();
            plugin.getLogger().severe("-------------------------------------------");
        }
    }
}
