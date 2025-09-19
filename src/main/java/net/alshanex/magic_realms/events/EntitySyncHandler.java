package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.network.SyncEntityLevelPacket;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.DynamicTextureManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EntitySyncHandler {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AbstractMercenaryEntity humanEntity &&
                event.getEntity() instanceof ServerPlayer serverPlayer) {

            // Send level sync - texture metadata is automatically synced via entity data
            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
            int currentLevel = killData.getCurrentLevel();
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncEntityLevelPacket(humanEntity.getId(), humanEntity.getUUID(), currentLevel));

            MagicRealms.LOGGER.debug("Synced entity {} (level {}) to player {}",
                    humanEntity.getEntityName(), currentLevel, serverPlayer.getName().getString());
        }
    }
}
