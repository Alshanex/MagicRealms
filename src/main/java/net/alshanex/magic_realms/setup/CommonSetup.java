package net.alshanex.magic_realms.setup;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.enderman.WizardEndermanEntity;
import net.alshanex.magic_realms.entity.exclusive.ace.AceEntity;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.exclusive.alshanex.AlshanexEntity;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntity;
import net.alshanex.magic_realms.entity.exclusive.catas.CatasEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.alshanex.magic_realms.entity.slime.MagicSlimeEntity;
import net.alshanex.magic_realms.entity.slime.SummonedMagicSlimeEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.entity.tim.TimEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CommonSetup {
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(MREntityRegistry.HUMAN.get(), RandomHumanEntity.prepareAttributes().build());
        event.put(MREntityRegistry.HOSTILE_HUMAN.get(), HostileRandomHumanEntity.prepareAttributes().build());
        event.put(MREntityRegistry.ALSHANEX.get(), AlshanexEntity.prepareAttributes().build());
        event.put(MREntityRegistry.ALIANA.get(), AlianaEntity.prepareAttributes().build());
        event.put(MREntityRegistry.CATAS.get(), CatasEntity.prepareAttributes().build());
        event.put(MREntityRegistry.AMADEUS.get(), AmadeusEntity.prepareAttributes().build());
        event.put(MREntityRegistry.ACE.get(), AceEntity.prepareAttributes().build());
        event.put(MREntityRegistry.TAVERNKEEP.get(), TavernKeeperEntity.prepareAttributes().build());
        event.put(MREntityRegistry.MAGIC_SLIME.get(), MagicSlimeEntity.prepareAttributes().build());
        event.put(MREntityRegistry.SUMMONED_MAGIC_SLIME.get(), MagicSlimeEntity.prepareAttributes().build());
        event.put(MREntityRegistry.MAGIC_CREEPER.get(), Creeper.createAttributes().build());
        event.put(MREntityRegistry.TIM.get(), TimEntity.createAttributes().build());
        event.put(MREntityRegistry.WIZARD_ENDERMAN.get(), WizardEndermanEntity.createAttributes().build());
    }
}
