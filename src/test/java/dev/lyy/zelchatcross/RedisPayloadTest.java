package dev.lyy.zelchatcross;

import dev.lyy.zelchatcross.redis.payload.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedisPayloadTest {

    @Test
    @DisplayName("ChatMessagePayload serialization and deserialization roundtrip")
    void testChatMessagePayload() {
        UUID senderUuid = UUID.randomUUID();
        ChatMessagePayload original = new ChatMessagePayload(
                "survival-1",
                senderUuid,
                "PlayerOne",
                "<green>Survival</green>",
                "EVERYONE",
                "global",
                "Hello network! [item]",
                "<gradient:#ff0000:#00ff00>Hello network!</gradient>",
                List.of("TargetPlayer")
        );

        String json = original.toJson();
        assertNotNull(json);
        assertTrue(json.contains("survival-1"));
        assertTrue(json.contains("PlayerOne"));

        ChatMessagePayload deserialized = ChatMessagePayload.fromJson(json);
        assertNotNull(deserialized);
        assertEquals(original.getMessageId(), deserialized.getMessageId());
        assertEquals(original.getOriginServerId(), deserialized.getOriginServerId());
        assertEquals(original.getSenderUuid(), deserialized.getSenderUuid());
        assertEquals(original.getSenderName(), deserialized.getSenderName());
        assertEquals(original.getOriginServerDisplayName(), deserialized.getOriginServerDisplayName());
        assertEquals(original.getChannelType(), deserialized.getChannelType());
        assertEquals(original.getRawMessage(), deserialized.getRawMessage());
        assertEquals(original.getMiniMessageContent(), deserialized.getMiniMessageContent());
        assertEquals(1, deserialized.getMentions().size());
        assertEquals("TargetPlayer", deserialized.getMentions().get(0));
    }

    @Test
    @DisplayName("PrivateMessagePayload serialization and deserialization roundtrip")
    void testPrivateMessagePayload() {
        UUID senderUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();

        PrivateMessagePayload original = new PrivateMessagePayload(
                "lobby-1",
                senderUuid,
                "Alice",
                "<gold>Lobby</gold>",
                targetUuid,
                "Bob",
                "Hey Bob! Meet me at spawn."
        );

        String json = original.toJson();
        PrivateMessagePayload deserialized = PrivateMessagePayload.fromJson(json);

        assertNotNull(deserialized);
        assertEquals("lobby-1", deserialized.getOriginServerId());
        assertEquals(senderUuid, deserialized.getSenderUuid());
        assertEquals("Alice", deserialized.getSenderName());
        assertEquals(targetUuid, deserialized.getTargetUuid());
        assertEquals("Bob", deserialized.getTargetName());
        assertEquals("Hey Bob! Meet me at spawn.", deserialized.getMessage());
    }

    @Test
    @DisplayName("PresencePayload serialization and deserialization roundtrip")
    void testPresencePayload() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        Map<String, String> players = Map.of(player1.toString(), "Alpha", player2.toString(), "Beta");

        PresencePayload original = new PresencePayload(
                "survival-2",
                "<aqua>Survival-2</aqua>",
                PresencePayload.Type.SERVER_HEARTBEAT,
                null,
                null,
                players
        );

        String json = original.toJson();
        PresencePayload deserialized = PresencePayload.fromJson(json);

        assertNotNull(deserialized);
        assertEquals(PresencePayload.Type.SERVER_HEARTBEAT, deserialized.getType());
        assertEquals("survival-2", deserialized.getOriginServerId());
        assertEquals(2, deserialized.getOnlinePlayers().size());
        assertEquals("Alpha", deserialized.getOnlinePlayers().get(player1.toString()));
        assertEquals("Beta", deserialized.getOnlinePlayers().get(player2.toString()));
    }

    @Test
    @DisplayName("ModerationPayload serialization and deserialization roundtrip")
    void testModerationPayload() {
        ModerationPayload original = new ModerationPayload(
                "hub-1",
                ModerationPayload.Action.BROADCAST,
                "Admin",
                "<red>Server restart in 10 minutes!</red>",
                "ENTITY_PLAYER_LEVELUP",
                1.0f,
                1.2f
        );

        String json = original.toJson();
        ModerationPayload deserialized = ModerationPayload.fromJson(json);

        assertNotNull(deserialized);
        assertEquals(ModerationPayload.Action.BROADCAST, deserialized.getAction());
        assertEquals("Admin", deserialized.getSenderName());
        assertEquals("<red>Server restart in 10 minutes!</red>", deserialized.getContent());
        assertEquals("ENTITY_PLAYER_LEVELUP", deserialized.getSound());
        assertEquals(1.0f, deserialized.getSoundVolume());
        assertEquals(1.2f, deserialized.getSoundPitch());
    }

    @Test
    @DisplayName("ShowcaseSnapshotPayload serialization and deserialization roundtrip")
    void testShowcaseSnapshotPayload() {
        UUID owner = UUID.randomUUID();
        ShowcaseSnapshotPayload original = new ShowcaseSnapshotPayload(
                "survival-1",
                "snap-1234",
                ShowcaseSnapshotPayload.Type.INVENTORY,
                owner,
                "Charlie",
                "Charlie's Inventory",
                "BASE64_MOCK_DATA_HERE",
                41,
                System.currentTimeMillis() + 300000L
        );

        String json = original.toJson();
        ShowcaseSnapshotPayload deserialized = ShowcaseSnapshotPayload.fromJson(json);

        assertNotNull(deserialized);
        assertEquals("snap-1234", deserialized.getSnapshotId());
        assertEquals(ShowcaseSnapshotPayload.Type.INVENTORY, deserialized.getType());
        assertEquals(owner, deserialized.getOwnerUuid());
        assertEquals("Charlie", deserialized.getOwnerName());
        assertEquals("BASE64_MOCK_DATA_HERE", deserialized.getSerializedData());
        assertEquals(41, deserialized.getSlotCount());
    }

    @Test
    @DisplayName("StaffChatPayload and SocialSpyPayload serialization")
    void testStaffAndSpyPayloads() {
        UUID staffUuid = UUID.randomUUID();
        StaffChatPayload staffPayload = new StaffChatPayload(
                "survival-1",
                staffUuid,
                "ModAlex",
                "Survival",
                "Investigating suspicious player at x: 100 z: 200"
        );

        String staffJson = staffPayload.toJson();
        StaffChatPayload deserializedStaff = StaffChatPayload.fromJson(staffJson);
        assertNotNull(deserializedStaff);
        assertEquals("ModAlex", deserializedStaff.getSenderName());

        UUID target = UUID.randomUUID();
        SocialSpyPayload spyPayload = new SocialSpyPayload(
                "lobby-1",
                staffUuid,
                "PlayerA",
                "Lobby",
                target,
                "PlayerB",
                "Survival",
                "Secret whisper message"
        );

        String spyJson = spyPayload.toJson();
        SocialSpyPayload deserializedSpy = SocialSpyPayload.fromJson(spyJson);
        assertNotNull(deserializedSpy);
        assertEquals("PlayerA", deserializedSpy.getSenderName());
        assertEquals("PlayerB", deserializedSpy.getTargetName());
        assertEquals("Secret whisper message", deserializedSpy.getMessage());
    }
}
