package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.commands.empire.EmpireCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TabComplete implements Listener {

    private static final List<String> hiddenCommands = new ArrayList<>();

    static {
        hiddenCommands.add("dynmap");
        hiddenCommands.add("/");
        hiddenCommands.add("close-bot");
        hiddenCommands.add("refreshcaches");
        hiddenCommands.add("dumpcaches");
        hiddenCommands.add("olympus");
        hiddenCommands.add("silent");
        hiddenCommands.add("s");
        hiddenCommands.add("smite");
        hiddenCommands.add("invisible");
        hiddenCommands.add("flyspeed");
        hiddenCommands.add("censor");
        hiddenCommands.add("dimension");
    }

    @EventHandler
    public static void onTabComplete(TabCompleteEvent event) {
        if(event.isCancelled()) {
            // hide illegal commands to unopped players
            event.setCompletions(
                    event.getCompletions()
                            .stream()
                            .filter(str -> !hiddenCommands.contains(str))
                            .collect(Collectors.toList())
            );

            final String[] currentArgs = event
                    .getBuffer()
                    .replaceAll(" +", " ")
                    .trim()
                    .split(" ");
            final String command = currentArgs[0];
            if (command.equals("/e")
                    || command.equals("/empire")) {
                if (currentArgs.length == 1) {
                    event.setCompletions(new ArrayList<>(EmpireCommand.getOptions()));
                } else if (currentArgs.length == 2) {
                    event.setCompletions(
                            (new ArrayList<>(EmpireCommand.getOptions()))
                                    .stream()
                                    .filter(option -> EmpireCommand.getOptions().contains(currentArgs[1]))
                                    .collect(Collectors.toList())
                    );
                }
            }
        }
    }

}
