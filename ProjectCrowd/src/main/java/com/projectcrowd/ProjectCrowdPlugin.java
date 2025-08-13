package com.projectcrowd;

import com.projectcrowd.bot.BotManager;
import com.projectcrowd.commands.BotCommand;
import com.projectcrowd.integration.BungeeIntegration;
import com.projectcrowd.util.TablistService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ProjectCrowdPlugin extends JavaPlugin {

    private static ProjectCrowdPlugin instance;

    private BotManager botManager;
    private BungeeIntegration bungeeIntegration;
    private TablistService tablistService;

    public static ProjectCrowdPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        this.tablistService = new TablistService(this);
        this.botManager = new BotManager(this, tablistService);
        this.bungeeIntegration = new BungeeIntegration(this, botManager);

        if (getCommand("bot") != null) {
            BotCommand executor = new BotCommand(botManager, bungeeIntegration);
            getCommand("bot").setExecutor(executor);
            getCommand("bot").setTabCompleter(executor);
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", bungeeIntegration);

        getLogger().info("ProjectCrowd enabled.");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.removeBots(botManager.getActiveBotCount());
        }
        getLogger().info("ProjectCrowd disabled.");
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public BungeeIntegration getBungeeIntegration() {
        return bungeeIntegration;
    }

    public TablistService getTablistService() { return tablistService; }
}