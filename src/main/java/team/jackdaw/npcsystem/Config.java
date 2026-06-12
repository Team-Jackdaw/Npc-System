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
    public static final long updateInterval = 1000L;
    public static final long outOfTime = 300000L;
    public static boolean enabled = true;
    public static boolean debug = false;
    public static boolean agentEnabled = false;
    public static String agentBaseUrl = "http://127.0.0.1:8765";
    public static String agentMode = "fast";
    public static String agentAuthToken = "";
    public static double range = 10.0;
    public static boolean isBubble = true;
    public static boolean isChatBar = true;
    public static TextBubbleEntity.TextBackgroundColor bubbleColor = TextBubbleEntity.TextBackgroundColor.DEFAULT;
    public static long timeLastingPerChar = 500L;

}
