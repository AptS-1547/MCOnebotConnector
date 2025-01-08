package net.esaps.MCOnebotConnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import lombok.Getter;

class Config {
    public String connectAddress;
    public String accessToken;

    public List<serverConfig> OnebotSendListToMC;
    public List<serverConfig> MCSendListToOnebot;

    public String prefixMc;
    public String prefixOnebot;

    public String sendFormatFromMC;
    public String sendFormatFromOnebot;
}

class serverConfig {
    public String serverName;
    public int groupId;
}

public class ConfigHandler {

    private static MCOnebotConnector plugin;
    private static YamlReader reader;
    private static File configFile;
    @Getter
    private static Config config;

    /**
     * Initialize the configuration file
     * @param plugin The plugin instance
     */
    public static void initConfig(MCOnebotConnector plugin) {

        ConfigHandler.plugin = plugin;
        plugin.getLogger().info("Loading Plugin");
        configFile = plugin.getDataDirectory().resolve("config.yml").toFile();

        if (!plugin.getDataDirectory().toFile().exists()) {
            try {
                plugin.getDataDirectory().toFile().mkdir();
            } catch (Exception e) {
                plugin.getLogger().error("Failed to create the data directory: {}", plugin.getDataDirectory().toString());
                plugin.getLogger().error("{}", String.valueOf(e));
            }
        }

        if (!configFile.exists()) {
            try {
                InputStream resource = MCOnebotConnector.class.getResourceAsStream("/config.yml");

                if (resource != null) {
                    Files.copy(resource, Path.of(configFile.toURI()));
                }
                resource.close();
            } catch (Exception e) {
                plugin.getLogger().error("Error happened while creating config.yml: ", e);
                return;
            }
        }

        loadConfig(plugin);
    }

    private static void loadConfig(MCOnebotConnector plugin) {
        plugin.getLogger().info("Loading Configuration...");

        try {
            YamlReader reader = new YamlReader(new FileReader(configFile));
            config = reader.read(Config.class);
            plugin.getLogger().info("Configuration Loaded Successfully!");
        } catch (FileNotFoundException e) {
            plugin.getLogger().error("Configuration File Not Found: {}", String.valueOf(e));
        } catch (YamlException e) {
            plugin.getLogger().error("An Error Occurred While Loading Configuration File: {}", String.valueOf(e));
        } catch (Exception e) {
            plugin.getLogger().error("An Unknown Error Occurred While Loading Configuration File: {}", String.valueOf(e));
        }
    }


}