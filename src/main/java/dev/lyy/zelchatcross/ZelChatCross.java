package dev.lyy.zelchatcross;

import dev.lyy.zelchatcross.chat.CrossChatManager;
import dev.lyy.zelchatcross.commands.*;
import dev.lyy.zelchatcross.config.ConfigManager;
import dev.lyy.zelchatcross.hook.ZelCrossExpansion;
import dev.lyy.zelchatcross.listeners.PlayerConnectionListener;
import dev.lyy.zelchatcross.listeners.ShowcaseInventoryListener;
import dev.lyy.zelchatcross.messaging.PrivateMessageManager;
import dev.lyy.zelchatcross.moderation.ModerationManager;
import dev.lyy.zelchatcross.module.ZelCrossChatModule;
import dev.lyy.zelchatcross.presence.NetworkPresenceManager;
import dev.lyy.zelchatcross.redis.RedisManager;
import dev.lyy.zelchatcross.scheduler.PlatformScheduler;
import dev.lyy.zelchatcross.showcase.ShowcaseManager;
import it.pino.zelchat.api.ZelChatAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Main plugin class for ZelChat-Cross (ZelCross).
 * Enterprise-grade Redis/DragonflyDB cross-server communication expansion for ZelChat on Paper and Folia.
 */
public final class ZelChatCross extends JavaPlugin {

    private static ZelChatCross instance;

    private PlatformScheduler scheduler;
    private ConfigManager configManager;
    private RedisManager redisManager;
    private NetworkPresenceManager presenceManager;
    private ShowcaseManager showcaseManager;
    private CrossChatManager chatManager;
    private PrivateMessageManager privateMessageManager;
    private ModerationManager moderationManager;
    private ZelCrossChatModule chatModule;
    private ZelCrossExpansion papiExpansion;

    public static ZelChatCross get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("==================================================");
        getLogger().info("           ZelChat-Cross (ZelCross) v" + getPluginMeta().getVersion());
        getLogger().info("   Redis Cross-Server Bridge for ZelChat Networks");
        getLogger().info("   Platform: " + (PlatformScheduler.isFolia() ? "Folia (Regionized)" : "Paper/Spigot"));
        getLogger().info("==================================================");

        // 1. Initialize platform scheduler
        this.scheduler = PlatformScheduler.create(this);

        // 2. Initialize configuration manager
        this.configManager = new ConfigManager(this);

        // 3. Initialize Redis connection & PubSub
        this.redisManager = new RedisManager(this);
        this.redisManager.connect();

        // 4. Initialize Core Managers
        this.presenceManager = new NetworkPresenceManager(this);
        this.showcaseManager = new ShowcaseManager(this);
        this.chatManager = new CrossChatManager(this);
        this.privateMessageManager = new PrivateMessageManager(this);
        this.moderationManager = new ModerationManager(this);

        this.presenceManager.start();
        this.showcaseManager.start();

        // 5. Register Event Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShowcaseInventoryListener(), this);

        // 6. Register Commands
        registerCommands();

        // 7. Register ZelChat ChatModule
        registerZelChatModule();

        // 8. Register PlaceholderAPI Hook
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.papiExpansion = new ZelCrossExpansion(this);
            this.papiExpansion.register();
            getLogger().info("[PAPI] Registered ZelCross PlaceholderAPI expansion.");
        }

        getLogger().info("[ZelCross] Successfully enabled for server: " + configManager.getServerId());
    }

    @Override
    public void onDisable() {
        getLogger().info("[ZelCross] Disabling ZelChat-Cross...");

        // Unregister PAPI
        if (papiExpansion != null) {
            try {
                papiExpansion.unregister();
            } catch (Exception ignored) {}
            papiExpansion = null;
        }

        // Unregister ChatModule from ZelChat
        unregisterZelChatModule();

        // Stop Presence and Showcase
        if (presenceManager != null) {
            presenceManager.stop();
        }
        if (showcaseManager != null) {
            showcaseManager.stop();
        }

        // Disconnect Redis
        if (redisManager != null) {
            redisManager.disconnect();
        }

        // Cancel scheduled tasks
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        instance = null;
        getLogger().info("[ZelCross] Disabled gracefully.");
    }

    public void reload() {
        configManager.loadConfigurations();

        // Reconnect Redis with new configs
        if (redisManager != null) {
            redisManager.connect();
        }

        if (chatModule != null) {
            chatModule.reload();
        }
    }

    private void registerCommands() {
        registerCmd("zelcross", new ZelCrossCommand(this));
        registerCmd("msg", new GlobalMsgCommand(this));
        registerCmd("reply", new GlobalReplyCommand(this));
        registerCmd("gbroadcast", new GlobalBroadcastCommand(this));
        registerCmd("gchatmute", new GlobalChatMuteCommand(this));
        registerCmd("gclearchat", new GlobalClearChatCommand(this));
        registerCmd("gspy", new GlobalSpyCommand(this));
        registerCmd("gstaff", new GlobalStaffChatCommand(this));
    }

    private void registerCmd(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            if (executor instanceof org.bukkit.command.CommandExecutor ce) {
                cmd.setExecutor(ce);
            }
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
        } else {
            getLogger().warning("Could not find command registration for: " + name);
        }
    }

    private void registerZelChatModule() {
        try {
            if (ZelChatAPI.get() != null) {
                this.chatModule = new ZelCrossChatModule(this);
                ZelChatAPI.get().getModuleManager().register(this, chatModule);
                this.chatModule.load();
                getLogger().info("[ZelChat] ZelCross ChatModule successfully hooked into ZelChat API.");
                return;
            }
        } catch (Throwable ignored) {}

        // Retry hook after short delay to allow ZelChat initialization
        scheduler.runAsyncDelayed(() -> {
            try {
                if (ZelChatAPI.get() != null) {
                    this.chatModule = new ZelCrossChatModule(this);
                    ZelChatAPI.get().getModuleManager().register(this, chatModule);
                    this.chatModule.load();
                    getLogger().info("[ZelChat] ZelCross ChatModule successfully hooked into ZelChat API.");
                }
            } catch (Throwable t) {
                getLogger().info("[ZelChat] Standalone mode: ZelChat API not detected (" + t.getMessage() + ").");
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void unregisterZelChatModule() {
        if (chatModule != null) {
            try {
                chatModule.unload();
                ZelChatAPI.get().getModuleManager().unregister(this, chatModule);
            } catch (Throwable ignored) {}
            chatModule = null;
        }
    }

    // Getters
    public PlatformScheduler getScheduler() { return scheduler; }
    public ConfigManager getConfigManager() { return configManager; }
    public RedisManager getRedisManager() { return redisManager; }
    public NetworkPresenceManager getPresenceManager() { return presenceManager; }
    public ShowcaseManager getShowcaseManager() { return showcaseManager; }
    public CrossChatManager getChatManager() { return chatManager; }
    public PrivateMessageManager getPrivateMessageManager() { return privateMessageManager; }
    public ModerationManager getModerationManager() { return moderationManager; }
}
