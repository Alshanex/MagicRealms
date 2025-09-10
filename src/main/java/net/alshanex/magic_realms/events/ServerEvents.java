package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.HumanEntityCommands;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HumanEntityCommands.register(event.getDispatcher());
        MagicRealms.LOGGER.info("Registered Magic Realms commands");
    }

    @SubscribeEvent
    public static void onEquipmentChangeEvent(LivingEquipmentChangeEvent event){
        if (event.getEntity() instanceof RandomHumanEntity human && !human.level().isClientSide) {
            if(human.getEntityClass() == EntityClass.MAGE && event.getSlot() == EquipmentSlot.OFFHAND){
                human.updateSpellbookSpells();
            }
            human.refreshSpellsAfterEquipmentChange();
        }
    }
}
