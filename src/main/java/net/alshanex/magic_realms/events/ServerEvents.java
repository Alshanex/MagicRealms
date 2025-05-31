package net.alshanex.magic_realms.events;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.WardenCloneEntity;
import net.alshanex.magic_realms.registry.TraitRegistry;
import net.alshanex.magic_realms.util.HumanEntityCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

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

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event){
        LivingEntity caster = event.getEntity();
        caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(15, 4, 15), (target) ->
                target.distanceToSqr(caster) < 15 * 15
                && target != caster
        ).forEach(target -> {
            var opt = LHMiscs.MOB.type().getExisting(target);
            if (opt.isEmpty()) return;
            var cap = opt.get();

            if(cap.traits.containsKey(TraitRegistry.DISRUPT.get())){
                Random random = new Random();
                int roll = random.nextInt(100);

                if(roll < 50){
                    if(!caster.level().isClientSide){
                        DamageSources.applyDamage(caster, caster.getMaxHealth() * .1f, SpellRegistry.FIREBALL_SPELL.get().getDamageSource(target));
                        MagicManager.spawnParticles(caster.level(), new BlastwaveParticleOptions(SchoolRegistry.FIRE.get().getTargetingColor(), 3), caster.getX(), caster.getY() + .165f, caster.getZ(), 1, 0, 0, 0, 0, true);
                        PacketDistributor.sendToPlayersTrackingEntityAndSelf(caster, new ShockwaveParticlesPacket(new Vec3(caster.getX(), caster.getY() + .165f, caster.getZ()), 3, ParticleRegistry.FIRE_PARTICLE.get()));
                    }

                    event.setCanceled(true);
                }
            }
        });

        if(event.getSchoolType() == SchoolRegistry.ELDRITCH.get()){
            caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(15, 4, 15), (target) ->
                    target.distanceToSqr(caster) < 15 * 15
                    && target != caster
            ).forEach(target -> {
                var opt = LHMiscs.MOB.type().getExisting(target);
                if (opt.isEmpty()) return;
                var cap = opt.get();

                if(cap.traits.containsKey(TraitRegistry.DEPHT_RULER.get())){
                    event.setCanceled(true);

                    Vec3 randomOffset = getRandomPositionWithinRadius(10);
                    Vec3 spawn = target.position().add(randomOffset);

                    spawn = Utils.moveToRelativeGroundLevel(caster.level(), spawn, 8);
                    if (!caster.level().getBlockState(BlockPos.containing(spawn).below()).isAir()) {
                        WardenCloneEntity clone = new WardenCloneEntity(caster.level(), target, caster);

                        clone.setPos(spawn.x, spawn.y, spawn.z);
                        clone.finalizeSpawn((ServerLevel) caster.level(), caster.level().getCurrentDifficultyAt(caster.blockPosition()), MobSpawnType.TRIGGERED, null);
                        caster.level().addFreshEntity(clone);
                        clone.getAngerManagement().increaseAnger(caster, 150);
                        clone.setAttackTarget(caster);
                    }
                }
            });
        }
    }

    public static Vec3 getRandomPositionWithinRadius(double radius) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = distance * Math.cos(angle);
        double z = distance * Math.sin(angle);
        return new Vec3(x, 0, z);
    }
}
