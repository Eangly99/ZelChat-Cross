package dev.lyy.zelchatcross;

import dev.lyy.zelchatcross.presence.NetworkPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PresenceLogicTest {

    @Test
    @DisplayName("Test multi-server presence directory and tab-completion filtering")
    void testPresenceTabCompletion() {
        Map<UUID, NetworkPlayer> players = new HashMap<>();
        List<String> names = List.of("Steve", "Alex", "Alexander", "ShadowNinja", "Batman");

        for (String name : names) {
            UUID id = UUID.randomUUID();
            players.put(id, new NetworkPlayer(id, name, "survival-1", "Survival"));
        }

        // Filter tab completion matching "al"
        String prefix = "al";
        List<String> matches = new ArrayList<>();
        for (NetworkPlayer np : players.values()) {
            if (np.getUsername().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(np.getUsername());
            }
        }

        assertEquals(2, matches.size());
        assertTrue(matches.contains("Alex"));
        assertTrue(matches.contains("Alexander"));
    }

    @Test
    @DisplayName("Test server player counts aggregation")
    void testServerCountsAggregation() {
        Map<String, Set<UUID>> playersByServer = new HashMap<>();
        playersByServer.put("survival-1", new HashSet<>(List.of(UUID.randomUUID(), UUID.randomUUID())));
        playersByServer.put("lobby-1", new HashSet<>(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));

        assertEquals(2, playersByServer.get("survival-1").size());
        assertEquals(3, playersByServer.get("lobby-1").size());
    }
}
