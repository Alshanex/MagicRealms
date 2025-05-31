package net.alshanex.magic_realms.traits;

import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class TrueEye extends LegendaryTrait {
    int range = 10;
    List<Holder<MobEffect>> effects = List.of(MobEffectRegistry.TRUE_INVISIBILITY, MobEffectRegistry.EVASION, MobEffectRegistry.ABYSSAL_SHROUD, MobEffects.INVISIBILITY);

    public TrueEye(ChatFormatting format) {
        super(format);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(range, range, range), (target) ->
                target.distanceToSqr(mob) < range * range
                        && target != mob
        ).forEach(target -> {
            for(Holder<MobEffect> effect : effects){
                if(!target.level().isClientSide && target.hasEffect(effect)){
                    target.removeEffect(effect);
                    MagicManager.spawnParticles(target.level(), new BlastwaveParticleOptions(SchoolRegistry.EVOCATION.get().getTargetingColor(), 3), target.getX(), target.getY() + .165f, target.getZ(), 1, 0, 0, 0, 0, true);
                }
            }
        });
    }
}
