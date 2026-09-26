package net.alshanex.magic_realms.entity.humans;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.client.HeldItemPoser;
import net.alshanex.magic_realms.entity.humans.client.HeldItemPosers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public abstract class AbstractMercenaryEntityModel extends AbstractSpellCastingMobModel {

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "geo/slim_abstract_casting_mob.geo.json");
    }

    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (entity instanceof AbstractMercenaryEntity human) {
            if(human.isSittingInChair()){
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
            } else {
                applyHeldItemPose(human, animationState);
            }
        }
    }

    private void applyHeldItemPose(AbstractMercenaryEntity human,
                                   AnimationState<AbstractSpellCastingMob> animationState) {
        if (HeldItemPosers.isEmpty()) return;
        if (human.isCasting()) return;

        ItemStack main = human.getMainHandItem();
        HeldItemPoser poser = HeldItemPosers.find(main);
        if (poser == null) return;

        boolean rightHanded = !human.isLeftHanded();
        GeoBone right = getAnimationProcessor().getBone("right_arm");
        GeoBone left = getAnimationProcessor().getBone("left_arm");

        poser.pose(new HeldItemPoser.Context(
                human, main, rightHanded,
                rightHanded ? right : left,
                rightHanded ? left : right,
                getAnimationProcessor().getBone("head"),
                animationState.getPartialTick()
        ));
    }
}
