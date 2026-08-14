package dev.lyy.zelchatcross;

import dev.lyy.zelchatcross.presence.NetworkPlayer;
import dev.lyy.zelchatcross.redis.payload.ShowcaseSnapshotPayload;
import dev.lyy.zelchatcross.showcase.ShowcaseSnapshot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShowcaseSnapshotTest {

    @Test
    @DisplayName("ShowcaseSnapshot expiry calculation")
    void testSnapshotExpiry() {
        UUID owner = UUID.randomUUID();
        ItemStack[] items = new ItemStack[0];

        long future = System.currentTimeMillis() + 10000L;
        ShowcaseSnapshot snapshot = new ShowcaseSnapshot(
                "snap-1",
                ShowcaseSnapshotPayload.Type.ITEM,
                owner,
                "Alex",
                "Diamond Item",
                items,
                future
        );

        assertFalse(snapshot.isExpired());
        assertEquals("snap-1", snapshot.getSnapshotId());
        assertEquals(ShowcaseSnapshotPayload.Type.ITEM, snapshot.getType());
        assertEquals(0, snapshot.getItems().length);

        long past = System.currentTimeMillis() - 1000L;
        ShowcaseSnapshot expiredSnapshot = new ShowcaseSnapshot(
                "snap-2",
                ShowcaseSnapshotPayload.Type.ITEM,
                owner,
                "Alex",
                "Expired Item",
                items,
                past
        );
        assertTrue(expiredSnapshot.isExpired());
    }

    @Test
    @DisplayName("NetworkPlayer data integrity and updateLastSeen")
    void testNetworkPlayer() {
        UUID uuid = UUID.randomUUID();
        NetworkPlayer player = new NetworkPlayer(uuid, "PlayerX", "survival-1", "<green>Survival</green>");

        assertEquals(uuid, player.getUniqueId());
        assertEquals("PlayerX", player.getUsername());
        assertEquals("survival-1", player.getServerId());
        assertEquals("<green>Survival</green>", player.getServerDisplayName());

        long initialSeen = player.getLastSeen();
        assertTrue(initialSeen > 0);

        player.updateLastSeen();
        assertTrue(player.getLastSeen() >= initialSeen);
    }
}
