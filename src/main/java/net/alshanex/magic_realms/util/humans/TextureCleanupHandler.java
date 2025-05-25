package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Timer;
import java.util.TimerTask;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class TextureCleanupHandler {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof RandomHumanEntity humanEntity) {
            String entityUUID = humanEntity.getUUID().toString();

            MagicRealms.LOGGER.debug("Scheduling texture cleanup for dead entity: {}", entityUUID);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Minecraft.getInstance().execute(() -> {
                        try {
                            CombinedTextureManager.removeEntityTexture(entityUUID);
                            MagicRealms.LOGGER.debug("Cleaned up texture for dead entity: {}", entityUUID);
                        } catch (Exception e) {
                            MagicRealms.LOGGER.error("Failed to cleanup texture for entity {}: {}", entityUUID, e.getMessage());
                        }
                    });
                }
            }, 5000); // 5 segundos
        }
    }
}
