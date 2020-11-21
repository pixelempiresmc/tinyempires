package dev.sucrose.tinyempires;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.sucrose.tinyempires.commands.LinkDiscordAccount;
import dev.sucrose.tinyempires.commands.Nick;
import dev.sucrose.tinyempires.commands.ProximityChat;
import dev.sucrose.tinyempires.commands.debug.CloseBot;
import dev.sucrose.tinyempires.commands.debug.RefreshCaches;
import dev.sucrose.tinyempires.commands.arena.Atlantis;
import dev.sucrose.tinyempires.commands.arena.Yggdrasil;
import dev.sucrose.tinyempires.commands.economy.Convert;
import dev.sucrose.tinyempires.commands.economy.Gift;
import dev.sucrose.tinyempires.commands.economy.Pay;
import dev.sucrose.tinyempires.commands.economy.Take;
import dev.sucrose.tinyempires.commands.empire.EmpireCommand;
import dev.sucrose.tinyempires.commands.empire.options.CreateEmpireLaw;
import dev.sucrose.tinyempires.commands.empire.options.Home;
import dev.sucrose.tinyempires.commands.godsuite.*;
import dev.sucrose.tinyempires.commands.tpa.AcceptTeleportRequest;
import dev.sucrose.tinyempires.commands.tpa.RejectTeleportRequest;
import dev.sucrose.tinyempires.commands.tpa.TeleportRequest;
import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.listeners.*;
import dev.sucrose.tinyempires.listeners.WorldBorder;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;

import java.util.Objects;

public final class TinyEmpires extends JavaPlugin {

    private static final Plugin dynmapPlugin = Bukkit.getServer().getPluginManager().getPlugin("Dynmap");
    private static final DynmapAPI dynmap = (DynmapAPI) dynmapPlugin;
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
        // assign to variable so same instance is assigned as event listener and command executor
        final Atlantis atlantis = new Atlantis();
        final Yggdrasil yggdrasil = new Yggdrasil();
        final Convert convert = new Convert();
        registerEvents(
            yggdrasil,
            atlantis,
            convert,
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
            new StructureProtection(),
            new EntityChangePotionEffect(),
            new Home(),
            new TabComplete(),
            new PreventCombatLogging(),
            new DiscordBot() // to broadcast death, join and other event messages
        );

        // load worlds
        getServer().createWorld(new WorldCreator("chess"));

        registerCommand("atlantis", atlantis);
        registerCommand("yggdrasil", yggdrasil);
        registerCommand("convert", convert);
        registerCommand("empire", new EmpireCommand());
        registerCommand("gift", new Gift());
        registerCommand("pay", new Pay());
        registerCommand("take", new Take());
        registerCommand("refreshcaches", new RefreshCaches());
        registerCommand("dimension", new Dimension());
        registerCommand("flyspeed", new Flyspeed());
        registerCommand("invisible", new Invisible());
        registerCommand("smite", new Smite());
        registerCommand("olympus", new Olympus());
        registerCommand("discord", new LinkDiscordAccount());
        registerCommand("censor", new Censor());
        registerCommand("prox", new ProximityChat());
        registerCommand("close-bot", new CloseBot());
//        registerCommand("tpa", new TeleportRequest());
//        registerCommand("tpaccept", new AcceptTeleportRequest());
//        registerCommand("tpreject", new RejectTeleportRequest());
        registerCommand("nick", new Nick());

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

        // create recipe
        addHoneyStickyPistonRecipe();

        DiscordBot.sendMessageInBridgeChat("**The server has started up!**");
    }

    @Override
    public void onDisable() {
        DiscordBot.sendMessageInBridgeChat("**The server has shut down!**");
        System.out.println("" + ChatColor.RED + ChatColor.BOLD + "+=== Disabled Tiny Empires ===+");
        DiscordBot.close();
        System.out.println(ChatColor.GREEN + "Shut down Pixel Empires Discord Bot");
        Yggdrasil.removeYggdrasilScoreboardTeams();
        System.out.println(ChatColor.GREEN + "Unregistered Yggdrasil scoreboard teams");
    }

    public static Plugin getDynmapPlugin() {
        return dynmapPlugin;
    }

    public static DynmapAPI getDynmap() {
        return dynmap;
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

    private void addHoneyStickyPistonRecipe() {
        // honey bottle sticky piston recipe
        ShapedRecipe stickyPiston = new ShapedRecipe(
            new NamespacedKey(this, NamespacedKey.BUKKIT),
            new ItemStack(Material.STICKY_PISTON)
        );

        stickyPiston.shape("***","*H*","*P*");

        stickyPiston.setIngredient('*', Material.AIR);
        stickyPiston.setIngredient('H', Material.HONEY_BOTTLE);
        stickyPiston.setIngredient('P', Material.PISTON);

        getServer().addRecipe(stickyPiston);
    }

}
