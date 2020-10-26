package dev.sucrose.tinyempires;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.sucrose.tinyempires.commands.DebugClearCache;
import dev.sucrose.tinyempires.commands.economy.Convert;
import dev.sucrose.tinyempires.commands.economy.Gift;
import dev.sucrose.tinyempires.commands.economy.Pay;
import dev.sucrose.tinyempires.commands.economy.Take;
import dev.sucrose.tinyempires.commands.empire.EmpireCommand;
import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.listeners.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class TinyEmpires extends JavaPlugin {

    private static final MongoClient mongoClient = MongoClients.create();
    private static final MongoDatabase database = mongoClient.getDatabase("tinyempires");
    private static Plugin instance;

    @Override
    public void onEnable() {
        instance = this;
        System.out.println("" + ChatColor.GREEN + ChatColor.BOLD + "+=== Initialized Tiny Empires ===+");
        DrawEmpire.drawChunks();
        registerEvents(
            new ChestShopListener(),
            new EndPortal(),
            new PlayerChat(),
            new PlayerJoin(),
            new PlayerMove(),
            new TerritoryProtection(),
            new PreventForeignTNTAndPistons(),
            new PlayerLeave()
        );

        registerCommand("convert", new Convert());
        registerCommand("gift", new Gift());
        registerCommand("pay", new Pay());
        registerCommand("take", new Take());
        registerCommand("empire", new EmpireCommand());
        registerCommand("clearcache", new DebugClearCache());

        try {
            DiscordBot.init();
        } catch (Exception e) {
            System.out.println(ChatColor.DARK_RED + "Failure in initializing discord bot");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        System.out.println("" + ChatColor.RED + ChatColor.BOLD + "+=== Disabled Tiny Empires ===+");
        DiscordBot.close();
        System.out.println(ChatColor.GREEN + "Shut down Pixel Empires Discord Bot");
    }

    public static Plugin getInstance() {
        return instance;
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    private void registerCommand(String name, CommandExecutor commandExecutor) {
        Objects.requireNonNull(getCommand(name)).setExecutor(commandExecutor);
    }

    private void registerEvents(Listener ...listeners) {
        final PluginManager pluginManager = getServer().getPluginManager();
        for (final Listener listener : listeners)
            pluginManager.registerEvents(listener, this);
    }

}
