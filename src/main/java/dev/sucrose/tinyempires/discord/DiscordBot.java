package dev.sucrose.tinyempires.discord;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class DiscordBot extends ListenerAdapter {

    private static final String GUILD_ID = "739978697041248296";
    private static final String BRIDGE_CHANNEL_ID = "774537181049257984";
    private static final String PRAYER_CHANNEL_ID = "763642000049700875";

    private static JDA bot;
    private static TextChannel bridgeChannel;
    private static TextChannel prayerChannel;

    public static void init() throws LoginException, InterruptedException {
        String token = "";
        try {
            File file = new File("plugins/TinyEmpires/discord_bot_token.txt");
            System.out.println("Full file path: " + file.getAbsolutePath());
            Scanner reader = new Scanner(file);
            token = reader.nextLine();
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        bot = JDABuilder.createLight(token)
            .addEventListeners(new DiscordBot())
            .setActivity(Activity.playing("Bridging the Pixel Empires Discord and Minecraft servers"))
            .build();
        bot.awaitReady();

        Guild discordServer = bot.getGuildById(GUILD_ID);
        assert discordServer != null;
        System.out.println(discordServer);
        bridgeChannel = discordServer.getTextChannelById(BRIDGE_CHANNEL_ID);
        prayerChannel = discordServer.getTextChannelById(PRAYER_CHANNEL_ID);

        // success
        System.out.println(ChatColor.LIGHT_PURPLE + "Initialized Pixel Empires Discord Bot");
    }

    public static void sendMessageInBridgeChat(String content) {
        bridgeChannel.sendMessage(content)
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
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (msg.getAuthor().isBot())
            return;
        MessageChannel channel = event.getChannel();
        if (channel.getId().equals(BRIDGE_CHANNEL_ID)) {
            String content = msg.getContentRaw();
            Bukkit.broadcastMessage("" + ChatColor.GRAY + ChatColor.BOLD + String.format(
                "[DISCORD]%s %s » %s",
                ChatColor.RESET,
                msg.getAuthor().getAsTag(),
                ChatColor.YELLOW + content
            ));
            // acknowledge with a mailbox emoji
            try {
                // Couldn't solve IllegalStateException error with JDA dependency so wrapped in try/catch
                // to avoid clogging logs
                msg.addReaction("U+1F4EC")
                    .queue(response -> System.out.println("Successfully sent message from Discord"));
            } catch (Exception ignore) {}
            channel.sendMessage(String.format(
                "Sent `%s` in chat",
                content
            )).queue(response -> System.out.println("Successfully sent message from Discord"));
        }
    }
}