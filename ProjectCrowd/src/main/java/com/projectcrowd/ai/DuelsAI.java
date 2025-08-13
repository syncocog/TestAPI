package com.projectcrowd.ai;

import com.projectcrowd.bot.BotEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

public class DuelsAI implements GameAI {

    private final BotEntity bot;
    private final Random random = new Random();

    private enum State { SEEK_OPPONENT, FIGHT, RETREAT }
    private State state = State.SEEK_OPPONENT;

    private Location lastKnownOpponent;

    public DuelsAI(com.projectcrowd.ProjectCrowdPlugin plugin, BotEntity bot) {
        this.bot = bot;
    }

    @Override
    public void tick() {
        Optional<? extends Player> nearest = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld() == bot.getWorld())
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(bot.getLocation())));

        switch (state) {
            case SEEK_OPPONENT -> {
                if (nearest.isPresent()) {
                    lastKnownOpponent = nearest.get().getLocation();
                    bot.getMovementEngine().navigateAStar(bot, lastKnownOpponent, 32, 1000);
                    if (bot.getLocation().distanceSquared(lastKnownOpponent) < 25) state = State.FIGHT;
                } else {
                    bot.getMovementEngine().randomWalk(bot, 6.0);
                }
            }
            case FIGHT -> {
                bot.getCombatEngine().tryAttackNearestEnemy(bot, 6.0);
                if (bot.isLowHealth() || random.nextDouble() < 0.02) {
                    state = State.RETREAT;
                }
            }
            case RETREAT -> {
                Location retreat = bot.getLocation().clone().add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
                bot.getMovementEngine().navigateAStar(bot, retreat, 16, 800);
                // Simulate healing by waiting a bit then go back
                if (random.nextDouble() < 0.1) state = State.SEEK_OPPONENT;
            }
        }
    }
}