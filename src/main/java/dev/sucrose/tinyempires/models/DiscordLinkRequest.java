package dev.sucrose.tinyempires.models;

import java.util.UUID;

public class DiscordLinkRequest {

    private final int removalTaskId;
    private final UUID player;

    public DiscordLinkRequest(UUID player, Integer removalTaskId) {
        this.removalTaskId = removalTaskId;
        this.player = player;
    }

    public int getRemovalTaskId() {
        return removalTaskId;
    }

    public UUID getPlayerId() {
        return player;
    }

}
