package dev.lyy.zelchatcross.redis.payload;

/**
 * Payload for network-wide moderation actions (mute, broadcast, clear chat).
 */
public final class ModerationPayload extends RedisPayload {

    public enum Action {
        CHAT_MUTE,
        CHAT_UNMUTE,
        CLEAR_CHAT,
        BROADCAST
    }

    private final Action action;
    private final String senderName;
    private final String content;
    private final String sound;
    private final float soundVolume;
    private final float soundPitch;

    public ModerationPayload(String originServerId,
                             Action action,
                             String senderName,
                             String content,
                             String sound,
                             float soundVolume,
                             float soundPitch) {
        super(originServerId);
        this.action = action;
        this.senderName = senderName;
        this.content = content;
        this.sound = sound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }

    public Action getAction() {
        return action;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public String getSound() {
        return sound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public static ModerationPayload fromJson(String json) {
        return GSON.fromJson(json, ModerationPayload.class);
    }
}
