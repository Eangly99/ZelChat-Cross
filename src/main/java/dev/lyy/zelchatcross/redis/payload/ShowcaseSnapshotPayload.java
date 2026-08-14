package dev.lyy.zelchatcross.redis.payload;

import java.util.UUID;

/**
 * Payload for sharing showcase snapshots (Item, Inventory, Enderchest) across instances.
 */
public final class ShowcaseSnapshotPayload extends RedisPayload {

    public enum Type {
        ITEM,
        INVENTORY,
        ENDER_CHEST
    }

    private final String snapshotId;
    private final Type type;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String title;
    private final String serializedData; // Base64 ItemStack or array
    private final int slotCount;
    private final long expiryMillis;

    public ShowcaseSnapshotPayload(String originServerId,
                                   String snapshotId,
                                   Type type,
                                   UUID ownerUuid,
                                   String ownerName,
                                   String title,
                                   String serializedData,
                                   int slotCount,
                                   long expiryMillis) {
        super(originServerId);
        this.snapshotId = snapshotId;
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.title = title;
        this.serializedData = serializedData;
        this.slotCount = slotCount;
        this.expiryMillis = expiryMillis;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public Type getType() {
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

    public String getSerializedData() {
        return serializedData;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public long getExpiryMillis() {
        return expiryMillis;
    }

    public static ShowcaseSnapshotPayload fromJson(String json) {
        return GSON.fromJson(json, ShowcaseSnapshotPayload.class);
    }
}
