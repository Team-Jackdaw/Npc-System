package team.jackdaw.npcsystem;

import com.google.gson.Gson;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Manages the config files for the modules provided by this plugin.
 *
 * <p>
 * Configure the setting if a specific module is enabled or disabled.
 *
 * @author WDRshadow, Lokeyli
 * @version v1.0
 */
public class SettingManager {
    private static final Logger logger = NPCSystem.LOGGER;
    private static final File configFile = NPCSystem.workingDirectory.resolve("config.json").toFile();
    public static boolean enabled = true;
    public static String apiURL = "localhost:11434";
    public static String model = "qwen2.5:14b";
    public static String apiKey = "";
    public static double range = 10.0;

    /**
     * Load the setting from the config file.
     */
    public static void sync() {
        if (configFile.exists()) {
            try {
                String json = new String(Files.readAllBytes(configFile.toPath()));
                Gson gson = new Gson();
                Config config = gson.fromJson(json, Config.class);
                config.set();
            } catch (IOException e) {
                logger.error("[npc-system] Can't open the config file.");
            }
        } else {
            save();
        }
    }

    /**
     * Write the setting to the config file.
     */
    public static void save() {
        try {
            if (!configFile.exists()) {
                if (!configFile.createNewFile()) {
                    logger.error("[npc-system] Can't create the config file.");
                    return;
                }
            }
            Files.write(configFile.toPath(), Config.toJson().getBytes());
        } catch (IOException e) {
            logger.error("[npc-system] Can't write the config file.");
        }
    }

    private static final class Config {
        private boolean enabled = true;
        private String apiURL = "localhost:11434";
        private String model = "qwen2.5:14b";
        private String apiKey = "";
        private double range = 10.0;

        private static String toJson() {
            Config config = new Config();
            config.enabled = SettingManager.enabled;
            config.apiURL = SettingManager.apiURL;
            config.model = SettingManager.model;
            config.apiKey = SettingManager.apiKey;
            config.range = SettingManager.range;
            Gson gson = new Gson();
            return gson.toJson(config);
        }

        private void set() {
            SettingManager.enabled = enabled;
            SettingManager.apiURL = apiURL;
            SettingManager.model = model;
            SettingManager.apiKey = apiKey;
            SettingManager.range = range;
        }

    }

}
