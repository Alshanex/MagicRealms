package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.EntityLinkingHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = "magic_realms")
public class EntityLinkingEventHandler {
    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;

        // Si el jugador está agachado e interactúa con una RandomHumanEntity
        if (event.getEntity().isShiftKeyDown() && event.getTarget() instanceof RandomHumanEntity humanEntity) {
            var result = EntityLinkingHelper.tryLinkEntityToItem(humanEntity, event.getEntity(), event.getHand());
            if (result.consumesAction()) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;

        if (event.getEntity() instanceof RandomHumanEntity humanEntity) {
            // Actualizar cada 100 ticks (5 segundos)
            if (humanEntity.tickCount % 100 == 0) {
                EntityLinkingHelper.updateLinkedItems(humanEntity);
            }
        }
    }
}
