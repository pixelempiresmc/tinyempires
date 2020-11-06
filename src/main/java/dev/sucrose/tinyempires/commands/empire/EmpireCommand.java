package dev.sucrose.tinyempires.commands.empire;

import dev.sucrose.tinyempires.commands.empire.options.*;
import dev.sucrose.tinyempires.models.EmpireCommandOption;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EmpireCommand implements CommandExecutor {

    final private static Map<String, EmpireCommandOption> options = new HashMap<>();

    static {
        options.put("claim", new ClaimEmpireChunk());
        options.put("create", new CreateEmpire());
        options.put("color", new ChangeEmpireColor());
        options.put("join", new JoinEmpire());
        options.put("position", new CreateEmpirePosition());
        options.put("perm", new EditEmpirePosition());
        options.put("info", new Info());
        options.put("war", new DeclareWar());
        options.put("warclaim", new WarClaimChunk());
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
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // e <option> <args>
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e <option> <args>");
            return false;
        }

        String option = args[0];
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
}
