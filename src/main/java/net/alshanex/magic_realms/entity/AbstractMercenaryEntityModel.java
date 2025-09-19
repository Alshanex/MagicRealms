package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public abstract class AbstractMercenaryEntityModel extends AbstractSpellCastingMobModel {
    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (entity instanceof RandomHumanEntity human && human.isSittingInChair()) {
            GeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
            GeoBone leftLeg = getAnimationProcessor().getBone("left_leg");

            if (rightLeg != null && leftLeg != null) {
                // 90-degree forward rotation for sitting
                rightLeg.setRotX((float) Math.toRadians(90));
                leftLeg.setRotX((float) Math.toRadians(90));

                // Add leg separation (spread them apart slightly)
                rightLeg.setRotY((float) Math.toRadians(-10)); // Rotate right leg slightly outward
                leftLeg.setRotY((float) Math.toRadians(10));   // Rotate left leg slightly outward

                // Optional: slight Z rotation for more natural pose
                rightLeg.setRotZ((float) Math.toRadians(5));
                leftLeg.setRotZ((float) Math.toRadians(-5));
            }
        }
    }
}
