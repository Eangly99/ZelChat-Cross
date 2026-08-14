package dev.lyy.zelchatcross.showcase;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.ShowcaseSnapshotPayload;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Creates and opens interactive read-only GUI snapshots on Folia and Paper.
 */
public final class ShowcaseGuiManager {

    private final ZelChatCross plugin;

    public ShowcaseGuiManager(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    public void openSnapshot(Player player, ShowcaseSnapshot snapshot) {
        if (snapshot == null || snapshot.isExpired()) {
            player.sendMessage(plugin.getConfigManager().getMessage("showcase-not-found"));
            return;
        }

        plugin.getScheduler().runEntity(player, () -> {
            Component title = plugin.getConfigManager().parse(snapshot.getTitle());
            ShowcaseInventoryHolder holder = new ShowcaseInventoryHolder(snapshot);

            Inventory inv;
            ItemStack[] items = snapshot.getItems();

            if (snapshot.getType() == ShowcaseSnapshotPayload.Type.ITEM) {
                inv = Bukkit.createInventory(holder, 9 * 3, title);
                holder.setInventory(inv);

                ItemStack glass = createGlassPane("<dark_gray> </dark_gray>");
                for (int i = 0; i < 27; i++) {
                    inv.setItem(i, glass);
                }
                if (items.length > 0 && items[0] != null) {
                    inv.setItem(13, items[0]);
                }
            } else if (snapshot.getType() == ShowcaseSnapshotPayload.Type.ENDER_CHEST) {
                inv = Bukkit.createInventory(holder, 27, title);
                holder.setInventory(inv);
                for (int i = 0; i < Math.min(27, items.length); i++) {
                    inv.setItem(i, items[i]);
                }
            } else { // INVENTORY
                inv = Bukkit.createInventory(holder, 54, title);
                holder.setInventory(inv);

                // Row 1 to 4: main inventory (slots 9-35 + hotbar 0-8)
                // Standard player inventory layout in a 54-slot chest:
                // Chest Slots 0-26: Main inventory (9-35)
                // Chest Slots 27-35: Hotbar (0-8)
                // Chest Slots 36-44: Separator glass
                // Chest Slots 45-48: Armor (Helmet, Chestplate, Leggings, Boots)
                // Chest Slot 53: Offhand

                for (int i = 9; i < 36 && i < items.length; i++) {
                    inv.setItem(i - 9, items[i]);
                }
                for (int i = 0; i < 9 && i < items.length; i++) {
                    inv.setItem(27 + i, items[i]);
                }

                ItemStack separator = createGlassPane("<dark_gray>--- Equipment ---</dark_gray>");
                for (int i = 36; i < 45; i++) {
                    inv.setItem(i, separator);
                }

                // Armor: items[36] Helmet, items[37] Chest, items[38] Legs, items[39] Boots
                if (items.length > 39 && items[39] != null) inv.setItem(45, items[39]); // Helmet
                if (items.length > 38 && items[38] != null) inv.setItem(46, items[38]); // Chest
                if (items.length > 37 && items[37] != null) inv.setItem(47, items[37]); // Legs
                if (items.length > 36 && items[36] != null) inv.setItem(48, items[36]); // Boots

                // Offhand: items[40]
                if (items.length > 40 && items[40] != null) {
                    inv.setItem(53, items[40]);
                }
            }

            player.openInventory(inv);
        });
    }

    private ItemStack createGlassPane(String name) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getConfigManager().parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
