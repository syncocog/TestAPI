package com.projectcrowd.ai;

import com.projectcrowd.bot.BotEntity;
import com.projectcrowd.engine.MovementEngine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

public class EggWarsAI implements GameAI {

    private final BotEntity bot;
    private final Random random = new Random();

    private enum State { TO_GENERATOR, BUY_BLOCKS, BUILD_TO_MID, FARM_MID, UPGRADE_GEAR, TARGET_EGG, DEFEND_EGG }
    private State state = State.TO_GENERATOR;

    private Location teamBase;
    private Location generator;
    private Location mid;

    public EggWarsAI(com.projectcrowd.ProjectCrowdPlugin plugin, BotEntity bot) {
        this.bot = bot;
        Location base = bot.getLocation();
        this.teamBase = base.clone();
        this.generator = base.clone().add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
        this.mid = base.getWorld().getSpawnLocation();
    }

    @Override
    public void tick() {
        switch (state) {
            case TO_GENERATOR -> {
                bot.getMovementEngine().navigateAStar(bot, generator, 24, 1000);
                if (bot.getLocation().distanceSquared(generator) < 4) state = State.BUY_BLOCKS;
            }
            case BUY_BLOCKS -> {
                // Simulate shop interaction delay
                if (random.nextDouble() < 0.2) state = State.BUILD_TO_MID;
            }
            case BUILD_TO_MID -> {
                // Place blocks in a straight-ish line towards mid by teleport stepping and simulating placement
                Location current = bot.getLocation();
                Location next = stepTowards(current, mid, 1.0);
                placeBridgeUnder(next);
                bot.moveTowards(next, 0.6);
                if (current.distanceSquared(mid) < 100) state = State.FARM_MID;
            }
            case FARM_MID -> {
                // Circle around mid collecting imaginary resources
                bot.getMovementEngine().randomWalk(bot, 6.0);
                if (random.nextDouble() < 0.05) state = State.UPGRADE_GEAR;
            }
            case UPGRADE_GEAR -> {
                // Simulate upgrades then go for target egg
                if (random.nextDouble() < 0.2) state = State.TARGET_EGG;
            }
            case TARGET_EGG -> {
                // Find nearest player base (approx by nearest player) and move/attack
                Optional<? extends Player> nearest = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getWorld() == bot.getWorld())
                        .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(bot.getLocation())));
                if (nearest.isPresent()) {
                    Location target = nearest.get().getLocation();
                    bot.getMovementEngine().navigateAStar(bot, target, 32, 1200);
                    bot.getCombatEngine().tryAttackNearestEnemy(bot, 5.0);
                    if (random.nextDouble() < 0.02) state = State.DEFEND_EGG;
                }
            }
            case DEFEND_EGG -> {
                bot.getMovementEngine().navigateAStar(bot, teamBase, 24, 1000);
                bot.getCombatEngine().tryAttackNearestEnemy(bot, 6.0);
                if (bot.getLocation().distanceSquared(teamBase) < 9) state = State.TARGET_EGG;
            }
        }
    }

    private void placeBridgeUnder(Location loc) {
        Location under = loc.clone().subtract(0, 1, 0);
        Block block = under.getBlock();
        if (block.getType() == Material.AIR) block.setType(Material.OAK_PLANKS, false);
    }

    private Location stepTowards(Location from, Location to, double step) {
        Location dir = to.clone().subtract(from);
        double len = dir.length();
        if (len < step) return to.clone();
        dir.multiply(step / len);
        return from.clone().add(dir);
    }
}