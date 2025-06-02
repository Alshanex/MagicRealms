package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.SummoningUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SummonEventHandler {

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        // Verificar si la entidad que murió es una RandomHumanEntity invocada
        if (event.getEntity() instanceof RandomHumanEntity humanEntity) {
            // Verificar si tiene un summoner (es una entidad invocada)
            if (humanEntity.getSummoner() != null) {
                MagicRealms.LOGGER.debug("Summoned entity {} died", humanEntity.getEntityName());

                SummoningUtils.onSummonedEntityDeath(humanEntity);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Limpiar invocaciones cuando el jugador se desconecta
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MagicRealms.LOGGER.debug("Player {} disconnected, cleaning up summons", serverPlayer.getName().getString());
            SummoningUtils.onPlayerDisconnect(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // También limpiar invocaciones cuando el jugador respawnea (por si murió)
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MagicRealms.LOGGER.debug("Player {} respawned, cleaning up summons", serverPlayer.getName().getString());
            SummoningUtils.onPlayerDisconnect(serverPlayer);
        }
    }
}
