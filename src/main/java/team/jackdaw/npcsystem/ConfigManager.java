package team.jackdaw.npcsystem;

import com.google.gson.Gson;
import org.slf4j.Logger;
import team.jackdaw.npcsystem.entity.TextBubbleEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigManager {
    private static final Logger logger = NPCSystem.LOGGER;
    private static final File configFile = NPCSystem.workingDirectory.resolve("config.json").toFile();

    /**
     * Load the setting from the config file.
     */
    public static void sync() {
        if (configFile.exists()) {
            try {
                String json = new String(Files.readAllBytes(configFile.toPath()));
                Gson gson = new Gson();
                _Config config = gson.fromJson(json, _Config.class);
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
            Files.write(configFile.toPath(), _Config.toJson().getBytes());
        } catch (IOException e) {
            logger.error("[npc-system] Can't write the config file.");
        }
    }

    private static final class _Config {
        private boolean enabled;
        private String dbURL;
        private String apiURL;
        private String chat_model;
        private String embedding_model;
        private double range;
        private boolean isBubble;
        private boolean isChatBar;
        private String textBackgroundColor;
        private long timeLastingPerChar;

        private static String toJson() {
            _Config config = new _Config();
            config.enabled = Config.enabled;
            config.dbURL = Config.dbURL;
            config.apiURL = Config.apiURL;
            config.chat_model = Config.chat_model;
            config.embedding_model = Config.embedding_model;
            config.range = Config.range;
            config.isBubble = Config.isBubble;
            config.isChatBar = Config.isChatBar;
            config.textBackgroundColor = Config.bubbleColor.name();
            config.timeLastingPerChar = Config.timeLastingPerChar;
            Gson gson = new Gson();
            return gson.toJson(config);
        }

        private void set() {
            Config.enabled = enabled;
            Config.dbURL = dbURL;
            Config.apiURL = apiURL;
            Config.chat_model = chat_model;
            Config.embedding_model = embedding_model;
            Config.range = range;
            Config.isBubble = isBubble;
            Config.isChatBar = isChatBar;
            Config.bubbleColor = TextBubbleEntity.TextBackgroundColor.valueOf(textBackgroundColor);
            Config.timeLastingPerChar = timeLastingPerChar;
        }

    }
}
