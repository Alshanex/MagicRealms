package net.alshanex.magic_realms.events;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.WardenCloneEntity;
import net.alshanex.magic_realms.registry.TraitRegistry;
import net.alshanex.magic_realms.util.HumanEntityCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HumanEntityCommands.register(event.getDispatcher());
        MagicRealms.LOGGER.info("Registered Magic Realms commands");
    }

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event){
        LivingEntity caster = event.getEntity();
        caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(15, 4, 15), (target) ->
                !DamageSources.isFriendlyFireBetween(target, caster)
                        && Utils.hasLineOfSight(caster.level(), caster, target, true)
                        && target.distanceToSqr(caster) < 15 * 15
        ).forEach(target -> {
            boolean hasCap = LHMiscs.MOB.type().getExisting(target).isPresent();
            if(hasCap){
                MobTraitCap cap = LHMiscs.MOB.type().getOrCreate(target);
                if(cap.traits.containsKey(TraitRegistry.DEPHT_RULER)){
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
            }
        });
    }

    public static Vec3 getRandomPositionWithinRadius(double radius) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = distance * Math.cos(angle);
        double z = distance * Math.sin(angle);
        return new Vec3(x, 0, z);
    }
}
