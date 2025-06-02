package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.WardenCloneEntity;
import net.alshanex.magic_realms.util.HumanEntityCommands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HumanEntityCommands.register(event.getDispatcher());
        MagicRealms.LOGGER.info("Registered Magic Realms commands");
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Post event){
        if(event.getSource().getEntity() instanceof WardenCloneEntity warden){
            LivingEntity target = event.getEntity();

            if(target.hasEffect(MobEffects.DARKNESS)){
                target.removeEffect(MobEffects.DARKNESS);
            }
            if(target.hasEffect(MobEffects.WEAKNESS)){
                target.removeEffect(MobEffects.WEAKNESS);
            }
            if(target.hasEffect(MobEffects.DIG_SLOWDOWN)){
                target.removeEffect(MobEffects.DIG_SLOWDOWN);
            }
            if(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)){
                target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }

            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));

            if (!warden.level().isClientSide) {
                MagicManager.spawnParticles(warden.level(), ParticleTypes.POOF, warden.getX(), warden.getY(), warden.getZ(), 25, .4, .8, .4, .03, false);
                warden.discard();
            }
        }
    }
}
