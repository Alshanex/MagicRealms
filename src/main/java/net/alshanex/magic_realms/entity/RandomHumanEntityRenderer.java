package net.alshanex.magic_realms.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class RandomHumanEntityRenderer extends AbstractSpellCastingMobRenderer {
    private static final String LEFT_HAND = "bipedHandLeft";
    private static final String RIGHT_HAND = "bipedHandRight";

    protected ItemStack mainHandItem;
    protected ItemStack offhandItem;
    protected ItemStack hiddenShield = ItemStack.EMPTY;
    private boolean shieldWasHidden = false;

    public RandomHumanEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RandomHumanEntityModel());

        addRenderLayer(new CustomItemGeoLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob animatable) {
        return ((RandomHumanEntity)animatable).getTextureConfig().getSkinTexture();
    }

    @Override
    public void preRender(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        this.mainHandItem = animatable.getMainHandItem();
        this.offhandItem = animatable.getOffhandItem();

        if (this.offhandItem.getItem() instanceof ShieldItem) {
            this.hiddenShield = this.offhandItem.copy();
            animatable.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            this.shieldWasHidden = true;
        }
    }

    private class CustomItemGeoLayer extends BlockAndItemGeoLayer<AbstractSpellCastingMob> {

        public CustomItemGeoLayer(RandomHumanEntityRenderer renderer) {
            super(renderer);
        }

        @Nullable
        @Override
        protected ItemStack getStackForBone(GeoBone bone, AbstractSpellCastingMob animatable) {
            return switch (bone.getName()) {
                case LEFT_HAND -> {
                    ItemStack leftItem = animatable.isLeftHanded() ?
                            RandomHumanEntityRenderer.this.mainHandItem :
                            RandomHumanEntityRenderer.this.offhandItem;

                    if (shouldRenderCustom(leftItem)) {
                        yield leftItem;
                    }
                    yield null;
                }
                case RIGHT_HAND -> {
                    ItemStack rightItem = animatable.isLeftHanded() ?
                            RandomHumanEntityRenderer.this.offhandItem :
                            RandomHumanEntityRenderer.this.mainHandItem;

                    if (shouldRenderCustom(rightItem)) {
                        yield rightItem;
                    }
                    yield null;
                }
                default -> null;
            };
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, AbstractSpellCastingMob animatable) {
            return switch (bone.getName()) {
                case LEFT_HAND, RIGHT_HAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                default -> ItemDisplayContext.NONE;
            };
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                          AbstractSpellCastingMob animatable, MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
            if (RandomHumanEntityRenderer.this.shieldWasHidden &&
                    !RandomHumanEntityRenderer.this.hiddenShield.isEmpty()) {
                animatable.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                        RandomHumanEntityRenderer.this.hiddenShield);
            }

            if (stack == RandomHumanEntityRenderer.this.mainHandItem) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f));

                if (stack.getItem() instanceof ShieldItem) {
                    poseStack.translate(0, 0.125, -0.25);
                }
            }
            else if (stack == RandomHumanEntityRenderer.this.offhandItem) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f));

                if (stack.getItem() instanceof ShieldItem) {
                    poseStack.translate(0, 0.125, 0.25);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
            }

            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);

            RandomHumanEntityRenderer.this.hiddenShield = ItemStack.EMPTY;
            RandomHumanEntityRenderer.this.shieldWasHidden = false;
        }

        private boolean shouldRenderCustom(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }

            return stack.getItem() instanceof ShieldItem;
        }
    }
}
