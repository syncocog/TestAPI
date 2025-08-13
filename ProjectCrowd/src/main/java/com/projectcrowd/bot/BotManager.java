package com.projectcrowd.bot;

import com.projectcrowd.ProjectCrowdPlugin;
import com.projectcrowd.ai.DuelsAI;
import com.projectcrowd.ai.EggWarsAI;
import com.projectcrowd.ai.SurvivalAI;
import com.projectcrowd.engine.ChatEngine;
import com.projectcrowd.engine.CombatEngine;
import com.projectcrowd.engine.MovementEngine;
import com.projectcrowd.util.TablistService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BotManager {

    private final ProjectCrowdPlugin plugin;
    private final Map<UUID, BotEntity> idToBot = new ConcurrentHashMap<>();

    private final MovementEngine movementEngine;
    private final CombatEngine combatEngine;
    private final ChatEngine chatEngine;
    private final TablistService tablistService;

    private int aiTaskId = -1;

    public enum GameType { EGGWARS, SURVIVAL, DUELS }

    public BotManager(ProjectCrowdPlugin plugin, TablistService tablistService) {
        this.plugin = plugin;
        this.tablistService = tablistService;
        this.movementEngine = new MovementEngine(plugin);
        this.combatEngine = new CombatEngine(plugin);
        this.chatEngine = new ChatEngine(plugin);

        startAiScheduler();
    }

    private void startAiScheduler() {
        // Run AI tick at 10 ticks (0.5s) with internal throttling per bot.
        this.aiTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (idToBot.isEmpty()) return;
            // Performance: Skip bots far from players
            Set<Player> online = new HashSet<>(Bukkit.getOnlinePlayers());
            for (BotEntity bot : idToBot.values()) {
                if (!bot.isSpawned()) continue;
                if (!isNearAnyPlayer(bot, online, 64.0)) {
                    continue;
                }
                bot.tick();
            }
        }, 20L, 10L);

        // Chat schedule independently every 20s, engine handles per-bot jitter window
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            idToBot.values().forEach(chatEngine::maybeChatRandomly);
        }, 200L, 200L);
    }

    private boolean isNearAnyPlayer(BotEntity bot, Set<Player> players, double radius) {
        Location botLoc = bot.getBukkitEntity().getLocation();
        for (Player p : players) {
            if (p.getWorld() == botLoc.getWorld() && p.getLocation().distanceSquared(botLoc) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    public int getActiveBotCount() {
        return (int) idToBot.values().stream().filter(BotEntity::isSpawned).count();
    }

    public List<BotEntity> listBots() {
        return new ArrayList<>(idToBot.values());
    }

    public void spawnBots(int amount, GameType gameType, World world, Location spawnLocation) {
        for (int i = 0; i < amount; i++) {
            BotEntity bot = new BotEntity(plugin, this, gameType, movementEngine, combatEngine, chatEngine, world, spawnLocation);
            idToBot.put(bot.getBotUuid(), bot);
            bot.spawn();
            // Add to tab list if available
            tablistService.addFakePlayer(bot.getBotUuid(), bot.getBotName());
            switch (gameType) {
                case EGGWARS -> bot.setAi(new EggWarsAI(plugin, bot));
                case SURVIVAL -> bot.setAi(new SurvivalAI(plugin, bot));
                case DUELS -> bot.setAi(new DuelsAI(plugin, bot));
            }
        }
    }

    public void removeBots(int amount) {
        List<UUID> toRemove = idToBot.values().stream()
                .filter(BotEntity::isSpawned)
                .limit(amount)
                .map(BotEntity::getBotUuid)
                .collect(Collectors.toList());
        for (UUID id : toRemove) {
            BotEntity bot = idToBot.remove(id);
            if (bot != null) {
                tablistService.removeFakePlayer(bot.getBotUuid(), bot.getBotName());
                bot.despawn();
            }
        }
    }

    public void removeAll() {
        removeBots(idToBot.size());
    }
}