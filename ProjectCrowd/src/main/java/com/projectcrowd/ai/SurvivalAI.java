package com.projectcrowd.ai;

import com.projectcrowd.bot.BotEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SurvivalAI implements GameAI {

    private final BotEntity bot;
    private final Random random = new Random();

    private long lastBuildMs = 0L;

    public SurvivalAI(com.projectcrowd.ProjectCrowdPlugin plugin, BotEntity bot) {
        this.bot = bot;
    }

    @Override
    public void tick() {
        // Wander randomly
        if (random.nextDouble() < 0.5) {
            bot.getMovementEngine().randomWalk(bot, 8.0);
        } else {
            // Find nearest hostile mob and attack
            List<Entity> mobs = bot.getWorld().getNearbyEntities(bot.getLocation(), 10, 5, 10).stream()
                    .filter(e -> e instanceof Monster)
                    .collect(Collectors.toList());
            if (!mobs.isEmpty()) {
                bot.getCombatEngine().tryAttackNearestEnemy(bot, 8.0);
            }
        }

        // "Mine" resources: break exposed stone/ore blocks near feet occasionally
        if (random.nextDouble() < 0.1) {
            Location under = bot.getLocation().clone().subtract(0, 1, 0);
            Block b = under.getBlock();
            if (b.getType() == Material.STONE || b.getType().toString().endsWith("_ORE")) {
                b.breakNaturally();
            }
        }

        // Build simple structure occasionally
        long now = System.currentTimeMillis();
        if (now - lastBuildMs > 30000 && random.nextDouble() < 0.05) {
            lastBuildMs = now;
            buildPillar(bot.getLocation());
        }
    }

    private void buildPillar(Location base) {
        Location at = base.clone();
        for (int i = 0; i < 3; i++) {
            at.getBlock().setType(Material.OAK_PLANKS, false);
            at = at.add(0, 1, 0);
        }
    }
}