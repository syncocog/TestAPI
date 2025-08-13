package com.projectcrowd.bot;

import com.projectcrowd.ProjectCrowdPlugin;
import com.projectcrowd.ai.GameAI;
import com.projectcrowd.engine.ChatEngine;
import com.projectcrowd.engine.CombatEngine;
import com.projectcrowd.engine.MovementEngine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Material;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitTask;

import java.net.URL;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class BotEntity {

    private final ProjectCrowdPlugin plugin;
    private final BotManager manager;
    private final BotManager.GameType gameType;
    private final MovementEngine movementEngine;
    private final CombatEngine combatEngine;
    private final ChatEngine chatEngine;

    private final World world;
    private Location spawnLocation;

    // Backing Bukkit entity. For simplicity and compatibility we use ArmorStand with player head + armor to represent, but logic is independent.
    private ArmorStand npc;

    private final UUID botUuid;
    private final String botName;

    private BukkitTask movementTicker;
    private GameAI ai;

    private long lastPathDecisionMs = 0L;

    public BotEntity(ProjectCrowdPlugin plugin,
                     BotManager manager,
                     BotManager.GameType gameType,
                     MovementEngine movementEngine,
                     CombatEngine combatEngine,
                     ChatEngine chatEngine,
                     World world,
                     Location spawnLocation) {
        this.plugin = plugin;
        this.manager = manager;
        this.gameType = gameType;
        this.movementEngine = movementEngine;
        this.combatEngine = combatEngine;
        this.chatEngine = chatEngine;
        this.world = world;
        this.spawnLocation = spawnLocation.clone();
        this.botUuid = UUID.randomUUID();
        this.botName = generateRandomName();
    }

    public UUID getBotUuid() { return botUuid; }
    public String getBotName() { return botName; }

    public boolean isSpawned() { return npc != null && !npc.isDead(); }

    public Entity getBukkitEntity() { return npc; }

    public void setAi(GameAI ai) {
        this.ai = ai;
    }

    public void spawn() {
        Location loc = spawnLocation.clone();
        this.npc = world.spawn(loc, ArmorStand.class, stand -> {
            stand.setInvisible(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(botName);
            stand.setSmall(false);
            stand.setArms(true);
            stand.setBasePlate(false);
            stand.setGravity(true);
        });
        // Give player head with random skin appearance using PlayerProfile textures
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            PlayerProfile profile = Bukkit.createPlayerProfile(botUuid, botName);
            try {
                // Randomly pick a default Alex/Steve or leave blank to use botName skin if exists
                PlayerTextures textures = profile.getTextures();
                // Use a rotation among some default textures if desired. Here we skip external fetch.
                // textures.setSkin(new URL("https://textures.minecraft.net/texture/...");
                profile.setTextures(textures);
            } catch (Exception ignored) {}
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        }
        npc.getEquipment().setHelmet(head);

        // Basic equipment by game type
        switch (gameType) {
            case DUELS -> npc.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            case SURVIVAL -> npc.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_PICKAXE));
            case EGGWARS -> npc.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
        }

        // Add to tab list via player info add/remove simulation is not applicable to ArmorStand.
        // If desired, integrate ProtocolLib with fake player packets for tab list entries.
    }

    public void tick() {
        if (!isSpawned()) return;
        long now = System.currentTimeMillis();
        if (ai != null) ai.tick();
        // Movement throttling: recalc path every 750-1500 ms
        if (now - lastPathDecisionMs > 800) {
            lastPathDecisionMs = now;
            movementEngine.tickMovement(this);
            combatEngine.tickCombat(this);
        }
    }

    public void despawn() {
        if (movementTicker != null) {
            movementTicker.cancel();
            movementTicker = null;
        }
        if (npc != null) {
            npc.remove();
            npc = null;
        }
    }

    private static final Random RANDOM = new Random();

    private String generateRandomName() {
        String[] syllables = {"ka","zu","mi","ra","to","lo","na","ve","ri","sa","te","xo","qui","dra","mon","fel","gar","han","ion","jor"};
        StringBuilder sb = new StringBuilder();
        int parts = 2 + RANDOM.nextInt(2);
        for (int i = 0; i < parts; i++) sb.append(syllables[RANDOM.nextInt(syllables.length)]);
        String base = sb.substring(0, Math.min(12, sb.length()));
        String name = Character.toUpperCase(base.charAt(0)) + base.substring(1);
        return name + RANDOM.nextInt(100);
    }

    public MovementEngine getMovementEngine() { return movementEngine; }
    public CombatEngine getCombatEngine() { return combatEngine; }
    public ChatEngine getChatEngine() { return chatEngine; }
    public BotManager.GameType getGameType() { return gameType; }
    public World getWorld() { return world; }
    public Location getLocation() { return npc != null ? npc.getLocation() : spawnLocation.clone(); }

    public void lookAt(Location target) {
        if (npc == null) return;
        Location loc = npc.getLocation();
        Location newLoc = loc.setDirection(target.toVector().subtract(loc.toVector()));
        npc.teleport(newLoc);
    }

    public void moveTowards(Location target, double speed) {
        if (npc == null) return;
        Location loc = npc.getLocation();
        Location dir = target.clone();
        dir.subtract(loc);
        if (dir.length() < 0.2) return;
        dir.multiply(1.0 / dir.length());
        Location next = loc.add(dir.multiply(speed));
        npc.teleport(next);
    }

    public boolean isLowHealth() {
        // ArmorStand is not living; emulate health threshold based on simulated damage events (out of scope here)
        return false;
    }
}