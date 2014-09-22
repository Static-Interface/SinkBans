package de.static_interface.banplugin.model;

import java.util.UUID;

public class Account {

    private final UUID uuid;
    private final String playername;

    public Account(UUID uuid, String playername) {
        this.uuid = uuid;
        this.playername = playername;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getPlayername() {
        return playername;
    }
}
