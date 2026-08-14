package dev.lyy.zelchatcross.showcase;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom InventoryHolder marking snapshot viewer GUIs so that players cannot take or move items.
 */
public final class ShowcaseInventoryHolder implements InventoryHolder {

    private final ShowcaseSnapshot snapshot;
    private Inventory inventory;

    public ShowcaseInventoryHolder(ShowcaseSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ShowcaseSnapshot getSnapshot() {
        return snapshot;
    }
}
