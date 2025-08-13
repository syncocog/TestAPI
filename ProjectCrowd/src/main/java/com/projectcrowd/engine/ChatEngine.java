package com.projectcrowd.engine;

import com.projectcrowd.ProjectCrowdPlugin;
import com.projectcrowd.bot.BotEntity;
import com.projectcrowd.bot.BotManager;
import org.bukkit.Bukkit;

import java.util.*;

public class ChatEngine {

    private final ProjectCrowdPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> lastChat = new HashMap<>();

    private final Map<BotManager.GameType, List<String>> phrases = Map.of(
            BotManager.GameType.EGGWARS, List.of(
                    "going gen","bridge mid","def egg","push egg","need iron","buying blocks"
            ),
            BotManager.GameType.SURVIVAL, List.of(
                    "mining...","found coal","building hut","anyone seen a village?","zombies!"
            ),
            BotManager.GameType.DUELS, List.of(
                    "gl hf","nice shot","healing up","1v1 me","gg"
            )
    );

    public ChatEngine(ProjectCrowdPlugin plugin) {
        this.plugin = plugin;
    }

    public void maybeChatRandomly(BotEntity bot) {
        long now = System.currentTimeMillis();
        long last = lastChat.getOrDefault(bot.getBotUuid(), 0L);
        long interval = (60_000L + random.nextInt(180_000)); // 1-4 minutes
        if (now - last < interval) return;
        lastChat.put(bot.getBotUuid(), now);
        String msg = randomize(selectPhrase(bot.getGameType()));
        Bukkit.broadcastMessage("[" + bot.getBotName() + "] " + msg);
    }

    private String selectPhrase(BotManager.GameType type) {
        List<String> pool = phrases.getOrDefault(type, List.of("hi"));
        return pool.get(random.nextInt(pool.size()));
    }

    private String randomize(String phrase) {
        // random casing
        StringBuilder s = new StringBuilder();
        for (char c : phrase.toCharArray()) {
            if (Character.isLetter(c) && random.nextDouble() < 0.2) {
                s.append(random.nextBoolean() ? Character.toUpperCase(c) : Character.toLowerCase(c));
            } else {
                s.append(c);
            }
        }
        // typos: random insert
        if (random.nextDouble() < 0.1) {
            int idx = random.nextInt(s.length());
            s.insert(idx, (char)('a' + random.nextInt(26)));
        }
        // trailing emoji/emote sometimes
        if (random.nextDouble() < 0.15) {
            s.append(random.nextBoolean() ? " :)" : " :D");
        }
        return s.toString();
    }
}