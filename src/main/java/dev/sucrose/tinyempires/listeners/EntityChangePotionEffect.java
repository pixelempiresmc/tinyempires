package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.TinyEmpires;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.dynmap.DynmapAPI;

public class EntityChangePotionEffect implements Listener {

    @EventHandler
    public static void onPotionEntityEffect(EntityPotionEffectEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        final Player player = (Player) event.getEntity();
        // make player invisible on the Dynmap if player just gained the invisibility potion effect and make them
        // lose invisibility if they just lost the effect
        if (event.getNewEffect() != null
                && event.getNewEffect().getType() == PotionEffectType.INVISIBILITY) {
            assertPlayerDynmapInvisibility(player, true);
        } else if (event.getOldEffect() != null
                && event.getNewEffect() == null
                && event.getOldEffect().getType() == PotionEffectType.INVISIBILITY) {
            assertPlayerDynmapInvisibility(player, false);
        }
    }

    private static void assertPlayerDynmapInvisibility(Player player, boolean isInvisible) {
        TinyEmpires.getDynmap().assertPlayerInvisibility(
            player,
            isInvisible,
            TinyEmpires.getInstance()
        );
    }

}
