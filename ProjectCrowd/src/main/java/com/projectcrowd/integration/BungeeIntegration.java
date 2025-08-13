package com.projectcrowd.integration;

import com.projectcrowd.ProjectCrowdPlugin;
import com.projectcrowd.bot.BotManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;

public class BungeeIntegration implements PluginMessageListener {

    private final ProjectCrowdPlugin plugin;
    private final BotManager botManager;

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String SUBCHANNEL = "ProjectCrowd"; // our custom subchannel

    public BungeeIntegration(ProjectCrowdPlugin plugin, BotManager botManager) {
        this.plugin = plugin;
        this.botManager = botManager;
    }

    public void forwardBotsToServer(String serverName, int count) {
        Player sender = Bukkit.getOnlinePlayers().stream().findAny().orElse(null);
        if (sender == null) {
            plugin.getLogger().warning("No online player to send BungeeCord message. Bots not forwarded.");
            return;
        }
        try {
            // Prepare payload: [int count][String gameType]
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeInt(count);
            msgOut.writeUTF("SURVIVAL"); // default type; could be extended per-bot

            // Wrap into Forward command
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outBytes);
            out.writeUTF("Forward");
            out.writeUTF(serverName);
            out.writeUTF(SUBCHANNEL);
            out.writeShort(msgBytes.toByteArray().length);
            out.write(msgBytes.toByteArray());

            sender.sendPluginMessage(plugin, BUNGEE_CHANNEL, outBytes.toByteArray());
            plugin.getLogger().info("Forwarded request to spawn " + count + " bot(s) on server " + serverName + ".");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send BungeeCord message: " + e.getMessage());
        }
        // Despawn locally to simulate transfer
        botManager.removeBots(count);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(BUNGEE_CHANNEL)) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String sub = in.readUTF();
            if (!SUBCHANNEL.equals(sub)) return;
            short len = in.readShort();
            byte[] payload = new byte[len];
            in.readFully(payload);
            try (DataInputStream pin = new DataInputStream(new ByteArrayInputStream(payload))) {
                int count = pin.readInt();
                String type = pin.readUTF();
                BotManager.GameType gameType;
                try { gameType = BotManager.GameType.valueOf(type.toUpperCase()); } catch (Exception e) { gameType = BotManager.GameType.SURVIVAL; }
                World world = Bukkit.getWorlds().get(0);
                botManager.spawnBots(count, gameType, world, world.getSpawnLocation());
                plugin.getLogger().info("Received transfer: spawned " + count + " " + gameType + " bot(s).");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to parse incoming Bungee message: " + e.getMessage());
        }
    }
}