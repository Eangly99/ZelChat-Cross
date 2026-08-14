package dev.lyy.zelchatcross.showcase;

import dev.lyy.zelchatcross.redis.payload.ShowcaseSnapshotPayload;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * In-memory representation of an item, inventory, or enderchest snapshot.
 * Supports thread-safe lazy deserialization to prevent CPU spikes across servers.
 */
public final class ShowcaseSnapshot {

    private final String snapshotId;
    private final ShowcaseSnapshotPayload.Type type;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String title;
    private final String serializedData;
    private volatile ItemStack[] items;
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
        this.serializedData = null;
        this.expiryMillis = expiryMillis;
    }

    public ShowcaseSnapshot(String snapshotId,
                            ShowcaseSnapshotPayload.Type type,
                            UUID ownerUuid,
                            String ownerName,
                            String title,
                            String serializedData,
                            long expiryMillis) {
        this.snapshotId = snapshotId;
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.title = title;
        this.items = null;
        this.serializedData = serializedData;
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
        if (items == null && serializedData != null) {
            synchronized (this) {
                if (items == null) {
                    if (type == ShowcaseSnapshotPayload.Type.ITEM) {
                        ItemStack item = ItemSerializer.fromBase64(serializedData);
                        this.items = new ItemStack[]{item};
                    } else {
                        this.items = ItemSerializer.itemArrayFromBase64(serializedData);
                    }
                }
            }
        }
        return items;
    }

    public long getExpiryMillis() {
        return expiryMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryMillis;
    }
}
