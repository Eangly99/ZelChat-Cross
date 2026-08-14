package dev.lyy.zelchatcross.showcase;

import dev.lyy.zelchatcross.redis.payload.ShowcaseSnapshotPayload;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * In-memory representation of an item, inventory, or enderchest snapshot.
 */
public final class ShowcaseSnapshot {

    private final String snapshotId;
    private final ShowcaseSnapshotPayload.Type type;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String title;
    private final ItemStack[] items;
    private final long expiryMillis;

    public ShowcaseSnapshot(String snapshotId,
                            ShowcaseSnapshotPayload.Type type,
                            UUID ownerUuid,
                            String ownerName,
                            String title,
                            ItemStack[] items,
                            long expiryMillis) {
        this.snapshotId = snapshotId;
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.title = title;
        this.items = items;
        this.expiryMillis = expiryMillis;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public ShowcaseSnapshotPayload.Type getType() {
        return type;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getTitle() {
        return title;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public long getExpiryMillis() {
        return expiryMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryMillis;
    }
}
