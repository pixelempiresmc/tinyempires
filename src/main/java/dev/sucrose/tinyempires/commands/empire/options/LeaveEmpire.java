package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LeaveEmpire implements CommandOption {

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

        // owners in an empire with other people can't leave until they transfer ownership
        if (tePlayer.isOwner()
                && empire.getMembers().size() > 1) {
            sender.sendMessage(ChatColor.RED + "You must transfer empire ownership to another member before leaving");
            return;
        }

        if (empire.getMembers().size() == 1
                && empire.getReserve() > 0.1) {
            sender.sendMessage(ChatColor.RED + String.format(
                "The empire has %.1f coins remaining in its reserve; you cannot leave if you are the only remaining " +
                    "member and coins still remain in the reserve. (Withdraw coins with /e withdraw %.1f)",
                empire.getReserve(),
                empire.getReserve()
            ));
            return;
        }

        // leave empire
        DiscordBot.removeEmpireDiscordRoleFromUser(tePlayer, empire);

        tePlayer.leaveEmpire();
        empire.removeMember(tePlayer);
        Bukkit.broadcastMessage(ChatColor.YELLOW + String.format(
            "%s has left the empire of %s!",
            tePlayer.getName(),
            empire.getName()
        ));

        // delete empire if no-one is left
        if (empire.getMembers().size() == 0) {
            Bukkit.broadcastMessage(ChatColor.BOLD + String.format(
                "Everyone has left the empire of %s and it has now disbanded!",
                empire.getName()
            ));

            if (empire.getAtWarWith() != null) {
                DeclareWar.endWar(
                    empire.isAttackerInWar() ? empire : empire.getAtWarWith(),
                    empire.isAttackerInWar() ? empire.getAtWarWith() : empire
                );
                empire.getAtWarWith().broadcastText(ChatColor.GREEN +
                    "The enemy has disbanded and the war is now over!"
                );
            }

            // erase chunk markers + delete in mongo
            for (final TEChunk chunk : TEChunk.getEmpireChunks(empire.getId())) {
                TEChunk.deleteChunk(chunk);
                DrawEmpire.removeChunk(chunk, empire);
            }

            // delete flag marker
            DrawEmpire.deleteEmpireHomeMarker(empire.getId());

            // delete empire alliances
            for (final ObjectId empireId : empire.getAllies()) {
                final Empire ally = Empire.getEmpire(empireId);
                if (ally == null)
                    throw new NullPointerException("Could not get ally with object id " + empireId);
                ally.removeAlliedEmpire(empire.getId());
                ally.broadcastText(ChatColor.RED + String.format(
                    "The empire is no longer allies with %s",
                    ChatColor.BOLD + ally.getName()
                ));
            }

            // delete empire in mongo
            empire.delete();

            // update player scoreboards to account for deleted chunks
            Bukkit.getOnlinePlayers().forEach(p -> {
                final TEPlayer teP = TEPlayer.getTEPlayer(p.getUniqueId());
                if (teP == null)
                    throw new NullPointerException("Could not get TEPlayer instance when updating player " +
                        "scoreboards for user with ID: " + p.getUniqueId());
                teP.updatePlayerScoreboard();
            });

            // delete empire Discord role
            DiscordBot.deleteEmpireRole(empire);
        }
    }

    @Override
    public String getDescription() {
        return "Leave empire (will delete if last member)";
    }

    @Override
    public Permission getPermissionRequired() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/e leave";
    }

}
