package dev.sucrose.tinyempires.discord;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.DiscordLinkRequest;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.CensorUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class DiscordBot extends ListenerAdapter {

    private static final String GUILD_ID = "739978697041248296";
    private static final String BRIDGE_CHANNEL_ID = "774537181049257984";
    private static final String PRAYER_CHANNEL_ID = "763642000049700875";

    private static JDA bot;
    private static TextChannel bridgeChannel;
    private static TextChannel prayerChannel;
    private static Guild discordServer;
    private static Role empireOwnerRole;
    private static final Random random = new Random();

    private static final Map<String, DiscordLinkRequest> minecraftDiscordAccountLinkRequests = new HashMap<>();

    public static void init() throws LoginException, InterruptedException {
        String token = "";
        try {
            File file = new File("plugins/TinyEmpires/discord_bot_token.txt");
            System.out.println("Full file path: " + file.getAbsolutePath());
            Scanner reader = new Scanner(file);
            token = reader.nextLine();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        bot = JDABuilder.createLight(token)
            .addEventListeners(new DiscordBot())
            .setActivity(Activity.playing("Bridging the Pixel Empires Discord and Minecraft servers"))
            .build();
        bot.awaitReady();

        discordServer = bot.getGuildById(GUILD_ID);
        if (discordServer == null)
            throw new NullPointerException("Could not get JDA Guild for guild ID " + GUILD_ID);

        empireOwnerRole = discordServer.getRoleById("775925631577096242");
        if (empireOwnerRole == null)
            throw new NullPointerException("Could not get empire owner role in Discord server");

        bridgeChannel = discordServer.getTextChannelById(BRIDGE_CHANNEL_ID);
        prayerChannel = discordServer.getTextChannelById(PRAYER_CHANNEL_ID);

        // success
        System.out.println(ChatColor.LIGHT_PURPLE + "Initialized Pixel Empires Discord Bot");
    }

    public static void sendMessageInBridgeChat(String content) {
        bridgeChannel
            .sendMessage(CensorUtils.censorCurses(content))
            .queue(response -> System.out.println("Successfully sent message to Discord"));
    }

    public static void sendPrayerMessage(UUID playerId, Location burnLocation, ItemStack burntItems) {
        final TEPlayer player = TEPlayer.getTEPlayer(playerId);
        if (player == null) {
            final Player gamePlayer = Bukkit.getPlayer(playerId);
            if (gamePlayer != null)
                gamePlayer.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final StringBuilder message = new StringBuilder(String.format(
            "**%s** of empire *%s* burnt **%d %s** at %.1f, %.1f, %.1f",
            player.getName(),
            player.getEmpire() == null
                ? "[Unaffiliated]"
                : player.getEmpire().getName(),
            burntItems.getAmount(),
            burntItems.getType()
                .toString()
                .replace('_', ' '),
            burnLocation.getX(),
            burnLocation.getY(),
            burnLocation.getZ()
        ));

        if (burntItems.getItemMeta() instanceof BookMeta) {
            final BookMeta book = ((BookMeta) burntItems.getItemMeta());
            final List<String> content = book.getPages();
            message.append("\n\n**Book Contents**")
                .append("\nTitle: ")
                .append(book.getTitle());
            if (content.size() == 1 && content.get(0).length() == 0) {
                message.append("\n*Empty Contents*");
            } else {
                message.append("```");
                for (String page : content)
                    message.append("\n")
                        .append(page);
                message.append("```");
            }
        }

        prayerChannel.sendMessage(
            new EmbedBuilder()
                .setThumbnail("https://i.imgur.com/3TNDQqJ.png")
                .setColor(new Color(0, 158, 21))
                .setTitle("Prayer from " + player.getName())
                .setDescription(message.toString())
                .build()
        ).queue(response -> System.out.println("Successfully sent prayer message"));
    }

    public static void close() {
        bot.shutdownNow();
    }

    private static String generateLinkCode() {
        final StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++)
            codeBuilder
                .append(random.nextInt(10))
                .append(i == 2 ? "-" : "");
        return codeBuilder.toString();
    }

    @Nullable
    public static String getPlayerLinkCode(UUID uuid) {
        for (final Map.Entry<String, DiscordLinkRequest> entry : minecraftDiscordAccountLinkRequests.entrySet()) {
            if (entry.getValue().getPlayerId().equals(uuid))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Generates a six-digit code (e.g. 123-456) that the user can direct message the bot to link their
     * Minecraft and Discord
     * accounts and stores it
     * @return Link code
     */
    public static String addPendingLinkCode(UUID linker) {
        final String code = generateLinkCode();
        minecraftDiscordAccountLinkRequests.put(
            code,
            new DiscordLinkRequest(
                linker,
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                    TinyEmpires.getInstance(),
                    () -> {
                        minecraftDiscordAccountLinkRequests.remove(code);
                        final Player player = Bukkit.getPlayer(linker);
                        if (player != null)
                            player.sendMessage(ChatColor.RED +
                                String.format(
                                    "You took too long to link your Discord account and the code %s is now void",
                                    ChatColor.BOLD + code + ChatColor.RED
                                )
                            );
                    },
                    60 * 20
                )
            )
        );
        return code;
    }

    public static RoleAction createRoleAction(String name, String colorName) {
        return discordServer
            .createRole()
            .setName(name)
            .setHoisted(true)
            .setMentionable(true)
            .setColor(Color.getColor(colorName));
    }

    public static void deleteEmpireRole(Empire empire) {
        final Role role = discordServer.getRoleById(empire.getDiscordRoleId());
        if (role == null)
            throw new NullPointerException("Could not get empire's Discord role from ID: " + empire.getDiscordRoleId());
        role.delete().queue();
    }

    public static void removeEmpireDiscordRoleFromUser(TEPlayer player, Empire empire) {
        // assumes player has a nonnull discord ID
        final Role role = discordServer.getRoleById(empire.getDiscordRoleId());
        if (role == null)
            throw new NullPointerException("Could not get empire's Discord role from ID: " + empire.getDiscordRoleId());
        if (player.getDiscordId() != null)
            discordServer.removeRoleFromMember(player.getDiscordId(), role).queue();
    }

    public static void giveUserEmpireDiscordRole(TEPlayer player, Empire empire) {
        // assumes player has a nonnull discord ID
        final Role role = discordServer.getRoleById(empire.getDiscordRoleId());
        if (role == null)
            throw new NullPointerException("Could not get empire's Discord role from ID: " + empire.getDiscordRoleId());
        if (player.getDiscordId() != null)
            discordServer.addRoleToMember(player.getDiscordId(), role).queue();
    }

    public static void giveUserEmpireOwnerRole(TEPlayer tePlayer) {
        if (tePlayer.getDiscordId() != null)
            discordServer
                .addRoleToMember(tePlayer.getDiscordId(), empireOwnerRole)
                .queue();
    }

    public static void removeEmpireOwnerRoleFromUser(TEPlayer tePlayer) {
        if (tePlayer.getDiscordId() != null)
            discordServer
                .removeRoleFromMember(tePlayer.getDiscordId(), empireOwnerRole)
                .queue();
    }

    private static final Pattern codePattern = Pattern.compile("[0-9]{3}-[0-9]{3}");
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        final Message msg = event.getMessage();
        if (msg.getAuthor().isBot())
            return;
        final MessageChannel channel = event.getChannel();
        final String content = msg.getContentRaw();
        if (channel.getType() == ChannelType.PRIVATE) {
            if (!codePattern.matcher(content).find()) {
                channel
                    .sendMessage("For linking a Minecraft account with a Pixel Empires Discord one, only a " +
                    "verbatim verification code of the form `123-456` can be used.")
                    .queue();
                return;
            }

            final DiscordLinkRequest linkRequest = minecraftDiscordAccountLinkRequests.get(content);
            if (linkRequest.getPlayerId() == null) {
                channel
                    .sendMessage(String.format(
                        "'%s' is not an existing link code",
                        content
                    ))
                    .queue();
                return;
            }

            final TEPlayer tePlayer = TEPlayer.getTEPlayer(linkRequest.getPlayerId());
            if (tePlayer == null) {
                channel
                    .sendMessage("Unable to find your account in the database. Please contact a developer and we will" +
                        " attend to this promptly.")
                    .queue();
                return;
            }

            // run task on main Spigot thread to avoid changing player scoreboard asynchronously
            Bukkit.getScheduler().runTask(
                TinyEmpires.getInstance(),
                () -> tePlayer.setDiscordId(msg.getAuthor().getId())
            );
            final Player player = Bukkit.getPlayer(linkRequest.getPlayerId());
            if (player != null) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + String.format(
                    "Success! Your Minecraft account was linked to %s",
                    ChatColor.BOLD + msg.getAuthor().getAsTag()
                ));
                channel.sendMessage(String.format(
                    "Successfully linked account to *%s*",
                    player.getName()
                )).queue();
                if (tePlayer.isInEmpire()) {
                    giveUserEmpireDiscordRole(tePlayer, tePlayer.getEmpire());
                    if (tePlayer.isOwner())
                        giveUserEmpireOwnerRole(tePlayer);
                }
            }
            return;
        }

        // TODO: Op commands
//        final Member discordMember = discordServer.getMember(msg.getAuthor());
        // Discord opped Minecraft commands
//        if (discordMember != null
//                && discordMember.getRoles().contains(discordServer.getRoleById("739979260856631368"))
//                && content.charAt(0) == '/') {
//            final String command = content.substring(1);
//            final String[] words = content.split(" ");
//            final String[] args = Arrays.copyOfRange(words, 1, words.length);
//            switch (command) {
//                case "give":
//                    if (args.length < 3) {
//                        channel.sendMessage("/give <player> <item> <amount>").queue();
//                        return;
//                    }
//
//                    final String playerName = args[0];
//                    final Player player = Bukkit.getPlayer(playerName);
//                    if (player == null) {
//                        channel.sendMessage(String.format("'%s' is not currently in the game! (/list to see players)"));
//                        return;
//                    }
//
//
//            }
//        }

        if (channel.getId().equals(BRIDGE_CHANNEL_ID)) {
            if (content.charAt(0) == '/') {
                final String command = content.substring(1);
                switch (command) {
                    case "list":
                        final Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                        final StringBuilder messageBuilder = new StringBuilder();
                        messageBuilder.append(
                            players.size() == 0
                                ? "No players are currently online"
                                : String.format(
                                    "%d players are currently online:",
                                    players.size()
                                )
                        );
                        for (final Player p : players) {
                            final UUID pUUID = p.getUniqueId();
                            final TEPlayer teP = TEPlayer.getTEPlayer(pUUID);
                            if (teP == null)
                                throw new NullPointerException("Could not get TEPlayer for UUID " + pUUID);
                            messageBuilder.append(String.format(
                                "\n - [%s] %s",
                                teP.getEmpire() == null
                                    ? "Unaffiliated"
                                    : teP.getEmpire().getName(),
                                p.getName()
                            ));
                        }
                        channel.sendMessage("```" + messageBuilder.toString() + "```").queue();
                        return;
//                    case "tps":
//                        Bukkit.dispatchCommand(
//                            TinyEmpires.getCommandSenderExtractor(),
//                            "tps"
//                        );
//                        channel
//                            .sendMessage(
//                                '`'
//                                + TinyEmpires.getCommandSenderExtractor().getLastMessage()
//                                + '`'
//                            )
//                            .queue();
//                        return;
                    default:
                        channel
                            .sendMessage(
                            "Invalid command (list)"
                            )
                            .queue();
                        return;
                }
            }
            Bukkit.broadcastMessage("" + ChatColor.GRAY + ChatColor.BOLD + String.format(
                "[DISCORD]%s %s » %s",
                ChatColor.RESET,
                CensorUtils.censorCurses(msg.getAuthor().getAsTag()),
                ChatColor.YELLOW + content
            ));
            // acknowledge with a mailbox emoji
            try {
                // Couldn't solve IllegalStateException error with JDA dependency so wrapped in try/catch
                // to avoid clogging logs
                msg.addReaction("U+1F4EC")
                    .queue(response -> System.out.println("Successfully sent message from Discord"));
            } catch (Exception ignore) {}
        }
    }

    public static User getDiscordUsernameFromId(String discordId) {
        return bot.getUserById(discordId);
    }

}