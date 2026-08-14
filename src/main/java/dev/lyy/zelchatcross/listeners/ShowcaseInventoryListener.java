package dev.lyy.zelchatcross.listeners;

import dev.lyy.zelchatcross.showcase.ShowcaseInventoryHolder;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Prevents players from interacting with or taking items from snapshot preview GUIs.
 * Covers both top and bottom inventory interactions, shift-clicking, number-key swaps, and dragging.
 */
public final class ShowcaseInventoryListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShowcaseInventoryHolder ||
                (event.getInventory() != null && event.getInventory().getHolder() instanceof ShowcaseInventoryHolder)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShowcaseInventoryHolder ||
                (event.getInventory() != null && event.getInventory().getHolder() instanceof ShowcaseInventoryHolder)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }
}
