package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.network.SyncEntityLevelPacket;
import net.alshanex.magic_realms.network.SyncEntityTexturePacket;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EntitySyncHandler {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof RandomHumanEntity humanEntity &&
                event.getEntity() instanceof ServerPlayer serverPlayer) {

            // Send level sync
            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
            int currentLevel = killData.getCurrentLevel();
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncEntityLevelPacket(humanEntity.getId(), humanEntity.getUUID(), currentLevel));

            // Handle texture
            if (humanEntity.hasTexture()) {
                // Send existing texture immediately
                sendExistingTextureToPlayer(humanEntity, serverPlayer);
            } else if (!humanEntity.isTextureRequested()) {
                // This is the first player to track this entity, trigger texture generation
                triggerTextureGeneration(humanEntity);
            }
            // If texture is already requested but not ready, the player will receive it when it's done
        }
    }

    private static void sendExistingTextureToPlayer(RandomHumanEntity humanEntity, ServerPlayer serverPlayer) {
        try {
            ServerLevel serverLevel = serverPlayer.serverLevel();
            Path worldDir = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
            Path texturePath = worldDir.resolve("magic_realms_textures")
                    .resolve("entity").resolve("human")
                    .resolve(humanEntity.getUUID() + "_complete.png");

            if (Files.exists(texturePath) && Files.size(texturePath) > 0) {
                byte[] textureData = Files.readAllBytes(texturePath);
                PacketDistributor.sendToPlayer(serverPlayer,
                        new SyncEntityTexturePacket(humanEntity.getUUID(), humanEntity.getId(), textureData, humanEntity.getEntityName(), false));

                MagicRealms.LOGGER.debug("Sent existing texture to new tracking player: {} for entity: {}",
                        serverPlayer.getName().getString(), humanEntity.getUUID());
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to send texture to tracking player", e);
        }
    }

    private static void triggerTextureGeneration(RandomHumanEntity humanEntity) {
        try {
            ServerLevel serverLevel = (ServerLevel) humanEntity.level();

            // Check if texture file exists
            Path worldDir = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
            Path texturePath = worldDir.resolve("magic_realms_textures")
                    .resolve("entity").resolve("human")
                    .resolve(humanEntity.getUUID() + "_complete.png");

            if (Files.exists(texturePath) && Files.size(texturePath) > 0) {
                // Texture exists, mark and distribute
                humanEntity.setHasTexture(true);
                humanEntity.distributeExistingTexture();
            } else {
                // Request generation
                humanEntity.requestTextureGeneration();
                humanEntity.setTextureRequested(true);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to trigger texture generation for entity: {}", humanEntity.getUUID(), e);
        }
    }
}
