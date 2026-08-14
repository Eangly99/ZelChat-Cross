package dev.lyy.zelchatcross.config;

import dev.lyy.zelchatcross.ZelChatCross;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages plugin configuration files (config.yml, messages.yml) and MiniMessage deserialization.
 */
public final class ConfigManager {

    private final ZelChatCross plugin;
    private final MiniMessage miniMessage;

    private FileConfiguration config;
    private FileConfiguration messages;

    // Cached Config Values
    private boolean debug;
    private String serverId;
    private String serverDisplayName;
    private String redisHost;
    private int redisPort;
    private String redisUsername;
    private String redisPassword;
    private int redisDatabase;
    private boolean redisSsl;
    private int redisTimeoutMs;
    private String redisChannelPrefix;
    private int redisPoolMaxTotal;
    private int redisPoolMaxIdle;
    private int redisPoolMinIdle;

    private boolean syncPublic;
    private boolean syncStaff;
    private String publicIncomingPrefix;
    private String staffIncomingPrefix;

    private boolean showcaseEnabled;
    private int showcaseCooldownSeconds;
    private boolean showcaseItemEnabled;
    private List<String> showcaseItemPlaceholders;
    private String showcaseItemFormat;
    private String showcaseItemAirFormat;

    private boolean showcaseInvEnabled;
    private List<String> showcaseInvPlaceholders;
    private String showcaseInvFormat;
    private String showcaseInvGuiTitle;
    private int showcaseInvTtlSeconds;

    private boolean showcaseEcEnabled;
    private List<String> showcaseEcPlaceholders;
    private String showcaseEcFormat;
    private String showcaseEcGuiTitle;
    private int showcaseEcTtlSeconds;

    private boolean pmEnabled;
    private String pmFormatSend;
    private String pmFormatReceive;
    private String pmFormatSpy;

    private int presenceHeartbeatInterval;
    private int presenceServerTimeout;
    private boolean presenceTabComplete;

    private String broadcastPrefix;
    private String broadcastSound;
    private float broadcastSoundVolume;
    private float broadcastSoundPitch;
    private String staffChatPrefix;

    public ConfigManager(ZelChatCross plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        loadConfigurations();
    }

    public void loadConfigurations() {
        // Save defaults if not present
        plugin.saveDefaultConfig();
        saveDefaultResource("messages.yml");

        plugin.reloadConfig();
        this.config = plugin.getConfig();

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Check defaults for messages
        InputStream defMessagesStream = plugin.getResource("messages.yml");
        if (defMessagesStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defMessagesStream, StandardCharsets.UTF_8));
            messages.setDefaults(defConfig);
        }

        // Cache all config parameters
        this.debug = config.getBoolean("debug", false);
        this.serverId = config.getString("server-id", "server-1");
        this.serverDisplayName = config.getString("server-display-name", "<yellow>" + serverId + "</yellow>");

        this.redisHost = config.getString("redis.host", "127.0.0.1");
        this.redisPort = config.getInt("redis.port", 6379);
        this.redisUsername = config.getString("redis.username", "");
        this.redisPassword = config.getString("redis.password", "");
        this.redisDatabase = config.getInt("redis.database", 0);
        this.redisSsl = config.getBoolean("redis.ssl", false);
        this.redisTimeoutMs = config.getInt("redis.timeout-ms", 5000);
        this.redisChannelPrefix = config.getString("redis.channel-prefix", "zelcross");
        this.redisPoolMaxTotal = config.getInt("redis.pool.max-total", 32);
        this.redisPoolMaxIdle = config.getInt("redis.pool.max-idle", 16);
        this.redisPoolMinIdle = config.getInt("redis.pool.min-idle", 4);

        this.syncPublic = config.getBoolean("channels.sync-public", true);
        this.syncStaff = config.getBoolean("channels.sync-staff", true);
        this.publicIncomingPrefix = config.getString("channels.public-incoming-prefix", "<dark_gray>[<aqua><origin_server_display></aqua>]</dark_gray> <gray><sender></gray><dark_gray>:</dark_gray> ");
        this.staffIncomingPrefix = config.getString("channels.staff-incoming-prefix", "<red>[STAFF]</red> <dark_gray>[<yellow><origin_server_display></yellow>]</dark_gray> ");

        this.showcaseEnabled = config.getBoolean("showcase.enabled", true);
        this.showcaseCooldownSeconds = config.getInt("showcase.cooldown-seconds", 3);

        this.showcaseItemEnabled = config.getBoolean("showcase.item.enabled", true);
        this.showcaseItemPlaceholders = config.getStringList("showcase.item.placeholders");
        if (showcaseItemPlaceholders.isEmpty()) showcaseItemPlaceholders = List.of("[item]", "[i]", "[hand]");
        this.showcaseItemFormat = config.getString("showcase.item.format", "<dark_gray>[<aqua>{item_name} <gray>x{item_count}</gray></aqua>]</dark_gray>");
        this.showcaseItemAirFormat = config.getString("showcase.item.air-format", "<dark_gray>[<gray>Air</gray>]</dark_gray>");

        this.showcaseInvEnabled = config.getBoolean("showcase.inventory.enabled", true);
        this.showcaseInvPlaceholders = config.getStringList("showcase.inventory.placeholders");
        if (showcaseInvPlaceholders.isEmpty()) showcaseInvPlaceholders = List.of("[inv]", "[inventory]");
        this.showcaseInvFormat = config.getString("showcase.inventory.format", "<gold>[Inventory]</gold>");
        this.showcaseInvGuiTitle = config.getString("showcase.inventory.gui-title", "<dark_gray>{player}'s Inventory Snapshot</dark_gray>");
        this.showcaseInvTtlSeconds = config.getInt("showcase.inventory.snapshot-ttl-seconds", 300);

        this.showcaseEcEnabled = config.getBoolean("showcase.enderchest.enabled", true);
        this.showcaseEcPlaceholders = config.getStringList("showcase.enderchest.placeholders");
        if (showcaseEcPlaceholders.isEmpty()) showcaseEcPlaceholders = List.of("[ec]", "[enderchest]", "[echest]");
        this.showcaseEcFormat = config.getString("showcase.enderchest.format", "<light_purple>[Ender Chest]</light_purple>");
        this.showcaseEcGuiTitle = config.getString("showcase.enderchest.gui-title", "<dark_purple>{player}'s Ender Chest Snapshot</dark_purple>");
        this.showcaseEcTtlSeconds = config.getInt("showcase.enderchest.snapshot-ttl-seconds", 300);

        this.pmEnabled = config.getBoolean("private-messaging.enabled", true);
        this.pmFormatSend = config.getString("private-messaging.format-send", "<gray>[<gold>You</gold> <dark_gray>→</dark_gray> <aqua><target></aqua> <dark_gray>(<yellow><target_server_display></yellow>)</dark_gray>]</gray> <white><message></white>");
        this.pmFormatReceive = config.getString("private-messaging.format-receive", "<gray>[<aqua><sender></aqua> <dark_gray>(<yellow><sender_server_display></yellow>)</dark_gray> <dark_gray>→</dark_gray> <gold>You</gold>]</gray> <white><message></white>");
        this.pmFormatSpy = config.getString("private-messaging.format-spy", "<dark_gray>[SPY]</dark_gray> <gray>[<aqua><sender></aqua> <dark_gray>(<yellow><sender_server_display></yellow>)</dark_gray> <dark_gray>→</dark_gray> <gold><target></gold> <dark_gray>(<yellow><target_server_display></yellow>)</dark_gray>]</gray> <white><message></white>");

        this.presenceHeartbeatInterval = config.getInt("presence.heartbeat-interval-seconds", 10);
        this.presenceServerTimeout = config.getInt("presence.server-timeout-seconds", 30);
        this.presenceTabComplete = config.getBoolean("presence.enable-network-tab-complete", true);

        this.broadcastPrefix = config.getString("moderation.broadcast-prefix", "<gradient:#ff512f:#dd2476><bold>NETWORK BROADCAST</bold></gradient> <dark_gray>»</dark_gray> ");
        this.broadcastSound = config.getString("moderation.broadcast-sound", "ENTITY_PLAYER_LEVELUP");
        this.broadcastSoundVolume = (float) config.getDouble("moderation.broadcast-sound-volume", 1.0);
        this.broadcastSoundPitch = (float) config.getDouble("moderation.broadcast-sound-pitch", 1.0);
        this.staffChatPrefix = config.getString("moderation.staffchat-prefix", "<red>[STAFF]</red> <dark_gray>[<yellow><origin_server_display></yellow>]</dark_gray> <aqua><sender></aqua><dark_gray>:</dark_gray> ");
    }

    private void saveDefaultResource(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public Component parse(String miniMessageString) {
        if (miniMessageString == null || miniMessageString.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(miniMessageString);
    }

    public Component parse(String miniMessageString, TagResolver... resolvers) {
        if (miniMessageString == null || miniMessageString.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(miniMessageString, resolvers);
    }

    public Component getMessage(String key) {
        String raw = messages.getString(key, "<red>Missing message: " + key + "</red>");
        String prefix = messages.getString("prefix", "");
        return miniMessage.deserialize(prefix + raw);
    }

    public Component getMessageRaw(String key) {
        String raw = messages.getString(key, "<red>Missing message: " + key + "</red>");
        return miniMessage.deserialize(raw);
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key, "<red>Missing message: " + key + "</red>");
        String prefix = messages.getString("prefix", "");
        String text = prefix + raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String safeVal = entry.getValue() != null ? miniMessage.escapeTags(entry.getValue()) : "";
            text = text.replace("{" + entry.getKey() + "}", safeVal);
        }
        return miniMessage.deserialize(text);
    }

    public List<String> getMessageList(String key) {
        return messages.getStringList(key);
    }

    // Getters & Setters
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    public String getServerId() { return serverId; }
    public String getServerDisplayName() { return serverDisplayName; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisUsername() { return redisUsername; }
    public String getRedisPassword() { return redisPassword; }
    public int getRedisDatabase() { return redisDatabase; }
    public boolean isRedisSsl() { return redisSsl; }
    public int getRedisTimeoutMs() { return redisTimeoutMs; }
    public String getRedisChannelPrefix() { return redisChannelPrefix; }
    public int getRedisPoolMaxTotal() { return redisPoolMaxTotal; }
    public int getRedisPoolMaxIdle() { return redisPoolMaxIdle; }
    public int getRedisPoolMinIdle() { return redisPoolMinIdle; }

    public boolean isSyncPublic() { return syncPublic; }
    public boolean isSyncStaff() { return syncStaff; }
    public String getPublicIncomingPrefix() { return publicIncomingPrefix; }
    public String getStaffIncomingPrefix() { return staffIncomingPrefix; }

    public boolean isShowcaseEnabled() { return showcaseEnabled; }
    public int getShowcaseCooldownSeconds() { return showcaseCooldownSeconds; }
    public boolean isShowcaseItemEnabled() { return showcaseItemEnabled; }
    public List<String> getShowcaseItemPlaceholders() { return showcaseItemPlaceholders; }
    public String getShowcaseItemFormat() { return showcaseItemFormat; }
    public String getShowcaseItemAirFormat() { return showcaseItemAirFormat; }

    public boolean isShowcaseInvEnabled() { return showcaseInvEnabled; }
    public List<String> getShowcaseInvPlaceholders() { return showcaseInvPlaceholders; }
    public String getShowcaseInvFormat() { return showcaseInvFormat; }
    public String getShowcaseInvGuiTitle() { return showcaseInvGuiTitle; }
    public int getShowcaseInvTtlSeconds() { return showcaseInvTtlSeconds; }

    public boolean isShowcaseEcEnabled() { return showcaseEcEnabled; }
    public List<String> getShowcaseEcPlaceholders() { return showcaseEcPlaceholders; }
    public String getShowcaseEcFormat() { return showcaseEcFormat; }
    public String getShowcaseEcGuiTitle() { return showcaseEcGuiTitle; }
    public int getShowcaseEcTtlSeconds() { return showcaseEcTtlSeconds; }

    public boolean isPmEnabled() { return pmEnabled; }
    public String getPmFormatSend() { return pmFormatSend; }
    public String getPmFormatReceive() { return pmFormatReceive; }
    public String getPmFormatSpy() { return pmFormatSpy; }

    public int getPresenceHeartbeatInterval() { return presenceHeartbeatInterval; }
    public int getPresenceServerTimeout() { return presenceServerTimeout; }
    public boolean isPresenceTabComplete() { return presenceTabComplete; }

    public String getBroadcastPrefix() { return broadcastPrefix; }
    public String getBroadcastSound() { return broadcastSound; }
    public float getBroadcastSoundVolume() { return broadcastSoundVolume; }
    public float getBroadcastSoundPitch() { return broadcastSoundPitch; }
    public String getStaffChatPrefix() { return staffChatPrefix; }
}
