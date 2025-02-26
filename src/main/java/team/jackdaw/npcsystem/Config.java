package team.jackdaw.npcsystem;

import team.jackdaw.npcsystem.entity.TextBubbleEntity;

/**
 * Manages the config files for the modules provided by this plugin.
 *
 * <p>
 * Configure the setting if a specific module is enabled or disabled.
 *
 * @author WDRshadow
 * @version v1.0
 */
public class Config {
    public static final long updateInterval = 30000L;
    public static final long outOfTime = 300000L;
    public static boolean enabled = true;
    public static String dbURL = "http://localhost:8080";
    public static String apiURL = "http://localhost:11434";
    public static String chat_model = "qwen2.5:7b";
    public static String embedding_model = "nomic-embed-text";
    public static double range = 10.0;
    public static boolean isBubble = true;
    public static boolean isChatBar = true;
    public static TextBubbleEntity.TextBackgroundColor bubbleColor = TextBubbleEntity.TextBackgroundColor.DEFAULT;
    public static long timeLastingPerChar = 500L;

}
