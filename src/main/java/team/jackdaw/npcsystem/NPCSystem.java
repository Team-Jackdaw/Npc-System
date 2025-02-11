package team.jackdaw.npcsystem;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NPCSystem implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("npc-system");
    public static final Path workingDirectory = Paths.get(System.getProperty("user.dir"), "config", "npc-system");
    public static final boolean debug = false;
    @Override
    public void onInitialize() {
        // Create the working directory if it does not exist
        if (!Files.exists(workingDirectory)) {
            try {
                Files.createDirectories(workingDirectory);
            } catch (IOException e) {
                LOGGER.error("[npc-system] Failed to create the working directory");
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        SettingManager.sync();
    }
}
