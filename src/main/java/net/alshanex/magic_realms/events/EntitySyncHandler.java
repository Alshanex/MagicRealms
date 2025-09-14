package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.network.SyncEntityLevelPacket;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EntitySyncHandler {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof RandomHumanEntity humanEntity &&
                event.getEntity() instanceof ServerPlayer serverPlayer) {

            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
            int currentLevel = killData.getCurrentLevel();

            // Use both entity ID (for client lookup) and UUID (for server communication)
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncEntityLevelPacket(humanEntity.getId(), humanEntity.getUUID(), currentLevel));

            MagicRealms.LOGGER.debug("Sent level sync to player {} for entity {} (ID: {}, level: {})",
                    serverPlayer.getName().getString(), humanEntity.getUUID(), humanEntity.getId(), currentLevel);
        }
    }
}
