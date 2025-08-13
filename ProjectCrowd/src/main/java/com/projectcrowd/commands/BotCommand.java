package com.projectcrowd.commands;

import com.projectcrowd.bot.BotManager;
import com.projectcrowd.integration.BungeeIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BotCommand implements CommandExecutor, TabCompleter {

    private final BotManager botManager;
    private final BungeeIntegration bungeeIntegration;

    public BotCommand(BotManager botManager, BungeeIntegration bungeeIntegration) {
        this.botManager = botManager;
        this.bungeeIntegration = bungeeIntegration;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " <spawn|remove|count|send> ...");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn" -> {
                if (!sender.hasPermission("bot.admin")) { sender.sendMessage("No permission."); return true; }
                if (args.length < 3) { sender.sendMessage("Usage: /" + label + " spawn <amount> <game>"); return true; }
                int amount;
                try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage("Invalid amount"); return true; }
                BotManager.GameType type;
                try { type = BotManager.GameType.valueOf(args[2].toUpperCase()); } catch (IllegalArgumentException e) { sender.sendMessage("Game must be EGGWARS|SURVIVAL|DUELS"); return true; }
                World world;
                Location loc;
                if (sender instanceof Player p) {
                    world = p.getWorld();
                    loc = p.getLocation();
                } else {
                    world = Bukkit.getWorlds().get(0);
                    loc = world.getSpawnLocation();
                }
                botManager.spawnBots(amount, type, world, loc);
                sender.sendMessage("Spawned " + amount + " " + type + " bot(s).");
            }
            case "remove" -> {
                if (!sender.hasPermission("bot.admin")) { sender.sendMessage("No permission."); return true; }
                if (args.length < 2) { sender.sendMessage("Usage: /" + label + " remove <amount>"); return true; }
                int amount;
                try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage("Invalid amount"); return true; }
                botManager.removeBots(amount);
                sender.sendMessage("Removed up to " + amount + " bot(s).");
            }
            case "count" -> {
                if (!sender.hasPermission("bot.view")) { sender.sendMessage("No permission."); return true; }
                sender.sendMessage("Active bots: " + botManager.getActiveBotCount());
            }
            case "send" -> {
                if (!sender.hasPermission("bot.admin")) { sender.sendMessage("No permission."); return true; }
                if (args.length < 2) { sender.sendMessage("Usage: /" + label + " send <server>"); return true; }
                String server = args[1];
                int count = botManager.getActiveBotCount();
                bungeeIntegration.forwardBotsToServer(server, count);
                sender.sendMessage("Sent " + count + " bot(s) to server " + server + ".");
            }
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result = Arrays.asList("spawn","remove","count","send");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            result = Arrays.asList("EGGWARS","SURVIVAL","DUELS");
        }
        return result;
    }
}