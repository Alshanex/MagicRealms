package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class EntityCleanupHandler {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof RandomHumanEntity humanEntity) {
            String entityUUID = humanEntity.getUUID().toString();
            MagicRealms.LOGGER.debug("RandomHumanEntity joined level: {}", entityUUID);
            // Entity will be tracked when texture is first requested
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof RandomHumanEntity humanEntity) {
            String entityUUID = humanEntity.getUUID().toString();

            // Check if this is a permanent removal (despawn/death) or temporary (chunk unload)
            boolean isPermanentRemoval = humanEntity.isRemoved() &&
                    (humanEntity.getRemovalReason() == Entity.RemovalReason.KILLED ||
                            humanEntity.getRemovalReason() == Entity.RemovalReason.DISCARDED ||
                            (!humanEntity.requiresCustomPersistence() && !humanEntity.hasBeenInteracted()));

            if (isPermanentRemoval) {
                MagicRealms.LOGGER.debug("RandomHumanEntity permanently removed: {}, cleaning up texture", entityUUID);
                CombinedTextureManager.removeEntityTexture(entityUUID, true);
            } else {
                MagicRealms.LOGGER.debug("RandomHumanEntity temporarily removed: {} (reason: {}), preserving texture",
                        entityUUID, humanEntity.getRemovalReason());
                CombinedTextureManager.markEntityInactive(entityUUID);
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof RandomHumanEntity humanEntity) {
            String entityUUID = humanEntity.getUUID().toString();
            MagicRealms.LOGGER.debug("RandomHumanEntity died: {}, marking for texture cleanup", entityUUID);

            // Mark for cleanup, but don't immediately delete in case of resurrection or other edge cases
            CombinedTextureManager.markEntityInactive(entityUUID);
        }
    }
}
