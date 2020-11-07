package dev.sucrose.tinyempires;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.sucrose.tinyempires.commands.RefreshCaches;
import dev.sucrose.tinyempires.commands.arena.Arena;
import dev.sucrose.tinyempires.commands.arena.Yggdrasil;
import dev.sucrose.tinyempires.commands.economy.Convert;
import dev.sucrose.tinyempires.commands.economy.Gift;
import dev.sucrose.tinyempires.commands.economy.Pay;
import dev.sucrose.tinyempires.commands.economy.Take;
import dev.sucrose.tinyempires.commands.empire.EmpireCommand;
import dev.sucrose.tinyempires.commands.empire.options.CreateEmpireLaw;
import dev.sucrose.tinyempires.commands.godsuite.*;
import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.listeners.*;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class TinyEmpires extends JavaPlugin {

    private static final MongoClient mongoClient = MongoClients.create();
    private static final MongoDatabase database = mongoClient.getDatabase("tinyempires");
    private static Plugin instance;

    // global world-border constants
    public static final int WORLD_BORDER_LEFT_X = -10752;
    public static final int WORLD_BORDER_RIGHT_X = 10752;
    public static final int WORLD_BORDER_TOP_Z = -5376;
    public static final int WORLD_BORDER_BOTTOM_Z = 5377;
    public static final int MARGIN = 20;

    @Override
    public void onEnable() {
        instance = this;
        System.out.println("" + ChatColor.GREEN + ChatColor.BOLD + "+=== Initialized Tiny Empires ===+");
        DrawEmpire.draw();
        DrawEmpire.drawBorders(WORLD_BORDER_LEFT_X, WORLD_BORDER_RIGHT_X, WORLD_BORDER_BOTTOM_Z, WORLD_BORDER_TOP_Z);
        final Yggdrasil yggdrasil = new Yggdrasil();
        registerEvents(
            new ChestShopListener(),
            new EndPortal(),
            new PlayerChat(),
            new PlayerJoin(),
            new PlayerMove(),
            new TerritoryProtection(),
            new PreventForeignTNTAndPistons(),
            new PlayerLeave(),
            new TempleBurnListener(),
            new CreateEmpireLaw(),
            new WorldBorder(),
            new Arena(),
            new StructureProtection(),
            yggdrasil
        );

        // load worlds
        getServer().createWorld(new WorldCreator("chess"));

        registerCommand("convert", new Convert());
        registerCommand("gift", new Gift());
        registerCommand("pay", new Pay());
        registerCommand("take", new Take());
        registerCommand("empire", new EmpireCommand());
        registerCommand("clearcache", new RefreshCaches());
        registerCommand("dimension", new Dimension());
        registerCommand("flyspeed", new Flyspeed());
        registerCommand("invisible", new Invisible());
        registerCommand("smite", new Smite());
        registerCommand("arena", new Arena());
        registerCommand("olympus", new Olympus());
        registerCommand("yggdrasil", yggdrasil);

        try {
            DiscordBot.init();
        } catch (Exception e) {
            System.out.println(ChatColor.DARK_RED + "Failure in initializing discord bot");
            e.printStackTrace();
        }

        // update player scoreboards
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
            if (tePlayer == null)
                throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            tePlayer.updatePlayerScoreboard();
        }
    }

    @Override
    public void onDisable() {
        System.out.println("" + ChatColor.RED + ChatColor.BOLD + "+=== Disabled Tiny Empires ===+");
        DiscordBot.close();
        System.out.println(ChatColor.GREEN + "Shut down Pixel Empires Discord Bot");
        Yggdrasil.removeYggdrasilScoreboardTeams();
        System.out.println(ChatColor.GREEN + "Unregistered Yggdrasil scoreboard teams");
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
