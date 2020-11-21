package dev.sucrose.tinyempires.commands.empire;

import dev.sucrose.tinyempires.commands.empire.options.*;
import dev.sucrose.tinyempires.models.CommandOption;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class EmpireCommand implements CommandExecutor, Listener {

    final private static Map<String, CommandOption> options = new HashMap<>();
    final private static int HELP_PAGE_SIZE = 6;

    static {
        options.put("claim", new ClaimEmpireChunk());
        options.put("create", new CreateEmpire());
        options.put("color", new ChangeEmpireColor());
        options.put("join", new JoinEmpire());
        options.put("position", new CreateEmpirePosition());
        options.put("perm", new EditEmpirePosition());
        options.put("info", new Info());
        options.put("war", new DeclareWar());
        options.put("type", new SetChunkType());
        options.put("claimfill", new EmpireClaimFill());
        options.put("leave", new LeaveEmpire());
        options.put("contribute", new ContributeToEmpire());
        options.put("desc", new ChangeEmpireDescription());
        options.put("delpos", new DeleteEmpirePosition());
        options.put("withdraw", new WithdrawEmpireReserve());
        options.put("autoclaim", new AutoClaimEmpireChunk());
        options.put("unclaim", new UnclaimEmpireChunk());
        options.put("owner", new TransferEmpireOwner());
        options.put("assign", new AssignEmpirePosition());
        options.put("law", new OpenEmpireLaw());
        options.put("editlaw", new EditEmpireLaw());
        options.put("newlaw", new CreateEmpireLaw());
        options.put("repeal", new DeleteEmpireLaw());
        options.put("tax", new Tax());
        options.put("forgive", new Forgive());
        options.put("fine", new Fine());
        options.put("home", new Home());
        options.put("sethome", new SetHome());
        options.put("name", new ChangeEmpireName());
        options.put("kick", new KickEmpireMember());
        options.put("accept", new AcceptEmpireMember());
        options.put("reject", new RejectEmpireMember());
        options.put("renamelaw", new RenameEmpireLaw());
        options.put("endwar", new EndWar());
        options.put("chat", new Chat());
        options.put("ally", new Ally());
        options.put("allyaccept", new AllyAcceptRequest());
        options.put("allyreject", new AllyRejectRequest());
        options.put("unally", new Unally());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // e <option> <args>
        if (args.length < 1) {
            sender.sendMessage(ChatColor.GREEN + String.format(
                "Run %s/e help <page>%s to get command usage",
                ChatColor.BOLD,
                ChatColor.GREEN
            ));
            return false;
        }

        final String option = args[0];
        if (option.equals("help")) {
            int page = 1;
            final int highestPage = options.size() / HELP_PAGE_SIZE;
            if (args.length > 1) {
                try {
                    page = Math.min(highestPage, Integer.parseInt(args[1]));
                    if (page <= 0) {
                        sender.sendMessage(ChatColor.RED + "Page number must be positive");
                        return false;
                    }
                } catch (Exception ignore) {
                    sender.sendMessage(ChatColor.RED + "/help <page>");
                    return false;
                }
            }

            sender.sendMessage(ChatColor.YELLOW + String.format(
                "---- %sEmpire Help (Page %s/%s)%s ----",
                ChatColor.GREEN,
                page,
                highestPage,
                ChatColor.YELLOW
            ));

            final List<CommandOption> commands = new ArrayList<>(options.values());
            for (int i = 0; i < HELP_PAGE_SIZE; i++) {
                if (i == options.size() - 1)
                    break;
                final CommandOption commandOption = commands.get(i + HELP_PAGE_SIZE * (page - 1));
                sender.sendMessage(ChatColor.GOLD + String.format(
                    "%s %s %s",
                    commandOption.getUsage() + ":" + ChatColor.WHITE,
                    commandOption.getDescription(),
                    commandOption.getPermissionRequired() != null
                        ? ChatColor.YELLOW
                            + "("
                            + commandOption.getPermissionRequired().name().toLowerCase()
                            + ")"
                        : ""
                ));
            }
            return true;
        }

        if (options.containsKey(option)) {
            final String[] argsToPass = new String[args.length - 1];
            System.arraycopy(args, 1, argsToPass, 0, args.length - 1);
            options.get(option).execute((Player) sender, argsToPass);
            return true;
        }

        sender.sendMessage(ChatColor.RED + String.format(
            "'%s' is not a valid option (%s)",
            option,
            String.join("/", options.keySet())
        ));
        return false;
    }

    public static Set<String> getOptions() {
        return options.keySet();
    }

}
