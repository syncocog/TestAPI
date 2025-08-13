package com.projectcrowd.util;

import com.projectcrowd.ProjectCrowdPlugin;

import java.util.UUID;

public class TablistService {

    private final ProjectCrowdPlugin plugin;

    public TablistService(ProjectCrowdPlugin plugin) {
        this.plugin = plugin;
    }

    public void addFakePlayer(UUID uuid, String name) {
        // No-op without NMS or ProtocolLib. Left as extension point.
        plugin.getLogger().fine("Tablist add requested for " + name + " (" + uuid + ")");
    }

    public void removeFakePlayer(UUID uuid, String name) {
        // No-op
        plugin.getLogger().fine("Tablist remove requested for " + name + " (" + uuid + ")");
    }
}