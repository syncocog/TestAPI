package com.projectcrowd.engine;

import com.projectcrowd.ProjectCrowdPlugin;
import com.projectcrowd.bot.BotEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

public class CombatEngine {

    private final ProjectCrowdPlugin plugin;
    private final Random random = new Random();

    public CombatEngine(ProjectCrowdPlugin plugin) {
        this.plugin = plugin;
    }

    public void tickCombat(BotEntity bot) {
        tryAttackNearestEnemy(bot, 6.0);
    }

    public void tryAttackNearestEnemy(BotEntity bot, double radius) {
        Optional<? extends Player> nearest = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld() == bot.getWorld())
                .filter(p -> p.getLocation().distanceSquared(bot.getLocation()) <= radius * radius)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(bot.getLocation())));
        if (nearest.isEmpty()) return;
        Player target = nearest.get();

        // Strafe randomly
        if (random.nextBoolean()) {
            Vector dir = target.getLocation().toVector().subtract(bot.getLocation().toVector()).normalize();
            Vector left = new Vector(-dir.getZ(), 0, dir.getX());
            Location strafe = bot.getLocation().clone().add(left.multiply(random.nextBoolean() ? 0.5 : -0.5));
            bot.getMovementEngine().navigateAStar(bot, strafe, 8, 200);
        }

        bot.lookAt(target.getLocation());

        // Simulate CPS: attack only sometimes
        double cps = 4 + random.nextInt(5); // 4-8 cps
        long now = System.currentTimeMillis();
        if (now % Math.max(1, (int)(1000 / cps)) < 50) {
            target.damage(1.0);
        }

        // Retreat if low health simulated
        if (bot.isLowHealth() && random.nextBoolean()) {
            Location retreat = bot.getLocation().clone().add(random.nextInt(4) - 2, 0, random.nextInt(4) - 2);
            bot.getMovementEngine().navigateAStar(bot, retreat, 12, 300);
        }
    }
}