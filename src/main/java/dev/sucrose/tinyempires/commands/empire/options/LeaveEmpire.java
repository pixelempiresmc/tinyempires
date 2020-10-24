package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.EmpireCommandOption;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LeaveEmpire implements EmpireCommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e leave
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to leave");
            return;
        }

        // leave empire
        tePlayer.leaveEmpire();
        empire.removeMember(tePlayer);
        Bukkit.broadcastMessage(ChatColor.YELLOW + String.format(
            "%s has left the empire of %s!",
            tePlayer.getName(),
            empire.getName()
        ));

        // delete empire if no-one is left
        if (empire.getMembers().size() == 0) {
            Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + String.format(
                "Everyone has left the empire of %s and it has now disbanded!",
                empire.getName()
            ));
            // erase chunk markers
            for (final TEChunk chunk : TEChunk.getEmpireChunks(empire.getId()))
                DrawEmpire.removeChunk(chunk);
            // delete chunks in mongo
            TEChunk.deleteChunks(empire.getId());
            // delete empire in mongo
            empire.delete();
        }
    }

}
