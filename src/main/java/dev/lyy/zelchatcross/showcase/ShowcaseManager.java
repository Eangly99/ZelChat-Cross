package dev.lyy.zelchatcross.showcase;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.config.ConfigManager;
import dev.lyy.zelchatcross.redis.payload.ShowcaseSnapshotPayload;
import dev.lyy.zelchatcross.scheduler.TaskHandle;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages item, inventory, and enderchest showcasing and snapshot distribution.
 */
public final class ShowcaseManager {

    private final ZelChatCross plugin;
    private final ShowcaseGuiManager guiManager;
    private final Map<String, ShowcaseSnapshot> snapshotCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private TaskHandle cleanupTask;

    public ShowcaseManager(ZelChatCross plugin) {
        this.plugin = plugin;
        this.guiManager = new ShowcaseGuiManager(plugin);
    }

    public void start() {
        this.cleanupTask = plugin.getScheduler().runAsyncRepeating(
                this::cleanupExpiredSnapshots,
                30,
                30,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        snapshotCache.clear();
        cooldowns.clear();
    }

    public boolean isOnCooldown(Player player) {
        Long expire = cooldowns.get(player.getUniqueId());
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public long getCooldownRemainingSeconds(Player player) {
        Long expire = cooldowns.get(player.getUniqueId());
        if (expire == null) return 0;
        long remaining = expire - System.currentTimeMillis();
        return Math.max(0, remaining / 1000L);
    }

    public void applyCooldown(Player player) {
        int cdSeconds = plugin.getConfigManager().getShowcaseCooldownSeconds();
        if (cdSeconds > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cdSeconds * 1000L));
        }
    }

    /**
     * Replaces showcase tokens in a player's chat message with interactive MiniMessage links.
     */
    public String processChatShowcases(Player player, String message) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isShowcaseEnabled()) {
            return message;
        }

        String result = message;
        boolean usedShowcase = false;

        // Held Item Showcasing
        if (cfg.isShowcaseItemEnabled() && player.hasPermission("zelcross.showcase.item")) {
            for (String placeholder : cfg.getShowcaseItemPlaceholders()) {
                if (result.contains(placeholder)) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    String replacement;
                    if (hand == null || hand.getType() == Material.AIR) {
                        replacement = cfg.getShowcaseItemAirFormat();
                    } else {
                        String snapshotId = createItemSnapshot(player, hand);
                        String itemName = getItemDisplayName(hand);
                        replacement = cfg.getShowcaseItemFormat()
                                .replace("{snapshot_id}", snapshotId)
                                .replace("{item_name}", itemName)
                                .replace("{item_count}", String.valueOf(hand.getAmount()))
                                .replace("{item_type}", hand.getType().name().toLowerCase(Locale.ROOT));
                    }
                    result = result.replace(placeholder, replacement);
                    usedShowcase = true;
                }
            }
        }

        // Inventory Showcasing
        if (cfg.isShowcaseInvEnabled() && player.hasPermission("zelcross.showcase.inventory")) {
            for (String placeholder : cfg.getShowcaseInvPlaceholders()) {
                if (result.contains(placeholder)) {
                    String snapshotId = createInventorySnapshot(player);
                    String replacement = cfg.getShowcaseInvFormat()
                            .replace("{snapshot_id}", snapshotId)
                            .replace("{player}", player.getName());
                    result = result.replace(placeholder, replacement);
                    usedShowcase = true;
                }
            }
        }

        // Ender Chest Showcasing
        if (cfg.isShowcaseEcEnabled() && player.hasPermission("zelcross.showcase.enderchest")) {
            for (String placeholder : cfg.getShowcaseEcPlaceholders()) {
                if (result.contains(placeholder)) {
                    String snapshotId = createEnderChestSnapshot(player);
                    String replacement = cfg.getShowcaseEcFormat()
                            .replace("{snapshot_id}", snapshotId)
                            .replace("{player}", player.getName());
                    result = result.replace(placeholder, replacement);
                    usedShowcase = true;
                }
            }
        }

        if (usedShowcase) {
            applyCooldown(player);
        }

        return result;
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        String typeName = item.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        String[] words = typeName.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public String createItemSnapshot(Player player, ItemStack item) {
        String snapshotId = UUID.randomUUID().toString().substring(0, 8);
        int ttl = plugin.getConfigManager().getShowcaseInvTtlSeconds();
        long expiry = System.currentTimeMillis() + (ttl * 1000L);
        String title = "<dark_gray>Item: " + getItemDisplayName(item) + "</dark_gray>";

        ItemStack[] items = new ItemStack[]{item.clone()};
        ShowcaseSnapshot snapshot = new ShowcaseSnapshot(
                snapshotId,
                ShowcaseSnapshotPayload.Type.ITEM,
                player.getUniqueId(),
                player.getName(),
                title,
                items,
                expiry
        );
        registerAndPublishSnapshot(snapshot, ItemSerializer.toBase64(item), 1, ttl);
        return snapshotId;
    }

    public String createInventorySnapshot(Player player) {
        String snapshotId = UUID.randomUUID().toString().substring(0, 8);
        int ttl = plugin.getConfigManager().getShowcaseInvTtlSeconds();
        long expiry = System.currentTimeMillis() + (ttl * 1000L);
        String title = plugin.getConfigManager().getShowcaseInvGuiTitle()
                .replace("{player}", player.getName());

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            cloned[i] = contents[i] != null ? contents[i].clone() : null;
        }

        ShowcaseSnapshot snapshot = new ShowcaseSnapshot(
                snapshotId,
                ShowcaseSnapshotPayload.Type.INVENTORY,
                player.getUniqueId(),
                player.getName(),
                title,
                cloned,
                expiry
        );
        registerAndPublishSnapshot(snapshot, ItemSerializer.itemArrayToBase64(cloned), cloned.length, ttl);
        return snapshotId;
    }

    public String createEnderChestSnapshot(Player player) {
        String snapshotId = UUID.randomUUID().toString().substring(0, 8);
        int ttl = plugin.getConfigManager().getShowcaseEcTtlSeconds();
        long expiry = System.currentTimeMillis() + (ttl * 1000L);
        String title = plugin.getConfigManager().getShowcaseEcGuiTitle()
                .replace("{player}", player.getName());

        ItemStack[] contents = player.getEnderChest().getContents();
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            cloned[i] = contents[i] != null ? contents[i].clone() : null;
        }

        ShowcaseSnapshot snapshot = new ShowcaseSnapshot(
                snapshotId,
                ShowcaseSnapshotPayload.Type.ENDER_CHEST,
                player.getUniqueId(),
                player.getName(),
                title,
                cloned,
                expiry
        );
        registerAndPublishSnapshot(snapshot, ItemSerializer.itemArrayToBase64(cloned), cloned.length, ttl);
        return snapshotId;
    }

    private void registerAndPublishSnapshot(ShowcaseSnapshot snapshot, String serializedData, int slotCount, int ttlSeconds) {
        snapshotCache.put(snapshot.getSnapshotId(), snapshot);

        String serverId = plugin.getConfigManager().getServerId();
        ShowcaseSnapshotPayload payload = new ShowcaseSnapshotPayload(
                serverId,
                snapshot.getSnapshotId(),
                snapshot.getType(),
                snapshot.getOwnerUuid(),
                snapshot.getOwnerName(),
                snapshot.getTitle(),
                serializedData,
                slotCount,
                snapshot.getExpiryMillis()
        );

        // Publish to other servers
        plugin.getRedisManager().getPublisher().publishShowcase(payload);

        // Store snapshot in Redis key with TTL for persistent retrieval
        plugin.getScheduler().runAsync(() -> {
            try {
                if (plugin.getRedisManager().isConnected()) {
                    try (Jedis jedis = plugin.getRedisManager().getResource()) {
                        String key = plugin.getConfigManager().getRedisChannelPrefix() + ":snapshot:" + snapshot.getSnapshotId();
                        jedis.setex(key, ttlSeconds, payload.toJson());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Showcase] Error caching snapshot to Redis: " + e.getMessage());
            }
        });
    }

    public void handleIncomingSnapshot(ShowcaseSnapshotPayload payload) {
        ItemStack[] items;
        if (payload.getType() == ShowcaseSnapshotPayload.Type.ITEM) {
            ItemStack item = ItemSerializer.fromBase64(payload.getSerializedData());
            items = new ItemStack[]{item};
        } else {
            items = ItemSerializer.itemArrayFromBase64(payload.getSerializedData());
        }

        ShowcaseSnapshot snapshot = new ShowcaseSnapshot(
                payload.getSnapshotId(),
                payload.getType(),
                payload.getOwnerUuid(),
                payload.getOwnerName(),
                payload.getTitle(),
                items,
                payload.getExpiryMillis()
        );
        snapshotCache.put(snapshot.getSnapshotId(), snapshot);
    }

    public ShowcaseSnapshot getSnapshot(String snapshotId) {
        ShowcaseSnapshot snapshot = snapshotCache.get(snapshotId);
        if (snapshot != null && !snapshot.isExpired()) {
            return snapshot;
        }

        // Try to fetch from Redis
        if (plugin.getRedisManager().isConnected()) {
            try (Jedis jedis = plugin.getRedisManager().getResource()) {
                String key = plugin.getConfigManager().getRedisChannelPrefix() + ":snapshot:" + snapshotId;
                String json = jedis.get(key);
                if (json != null) {
                    ShowcaseSnapshotPayload payload = ShowcaseSnapshotPayload.fromJson(json);
                    if (payload != null) {
                        handleIncomingSnapshot(payload);
                        return snapshotCache.get(snapshotId);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "[Showcase] Snapshot lookup in Redis failed: " + e.getMessage());
            }
        }
        return null;
    }

    public void openSnapshotGui(Player player, String snapshotId) {
        ShowcaseSnapshot snapshot = getSnapshot(snapshotId);
        if (snapshot == null || snapshot.isExpired()) {
            player.sendMessage(plugin.getConfigManager().getMessage("showcase-not-found"));
            return;
        }
        guiManager.openSnapshot(player, snapshot);
    }

    private void cleanupExpiredSnapshots() {
        snapshotCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public ShowcaseGuiManager getGuiManager() {
        return guiManager;
    }
}
