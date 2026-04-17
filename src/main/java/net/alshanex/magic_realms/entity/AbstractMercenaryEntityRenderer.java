package net.alshanex.magic_realms.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import java.util.List;

public abstract class AbstractMercenaryEntityRenderer extends AbstractSpellCastingMobRenderer {
    private static final String LEFT_HAND = "bipedHandLeft";
    private static final String RIGHT_HAND = "bipedHandRight";

    private static final boolean AZURELIB_ARMOR_LOADED =
            ModList.get() != null && ModList.get().isLoaded("azurelib");

    // Base humanoid models used as pose targets for AzureLib armor.
    private static final HumanoidModel<LivingEntity> INNER_ARMOR_MODEL = new HumanoidModel<>(
            Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
    private static final HumanoidModel<LivingEntity> OUTER_ARMOR_MODEL = new HumanoidModel<>(
            Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));

    protected ItemStack mainHandItem;
    protected ItemStack offhandItem;
    protected ItemStack hiddenShield = ItemStack.EMPTY;
    private boolean shieldWasHidden = false;

    public AbstractMercenaryEntityRenderer(EntityRendererProvider.Context renderManager, AbstractSpellCastingMobModel model) {
        super(renderManager, model);

        removeParentArmorAndItemLayers();

        addRenderLayer(new CustomItemGeoLayer(this));
        addRenderLayer(new CustomArmorGeoLayer(this));
    }

    private void removeParentArmorAndItemLayers() {
        List<GeoRenderLayer<AbstractSpellCastingMob>> layers = this.getRenderLayers();
        try {
            layers.removeIf(layer -> layer instanceof ItemArmorGeoLayer);
        } catch (UnsupportedOperationException ignored) {
            // immutable
        }
    }

    @Override
    public void preRender(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        this.mainHandItem = animatable.getMainHandItem();
        this.offhandItem = animatable.getOffhandItem();

        if (this.offhandItem.getItem() instanceof ShieldItem) {
            this.hiddenShield = this.offhandItem.copy();
            animatable.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            this.shieldWasHidden = true;
        }
    }

    /**
     * Render AzureLib armors after the main GeoModel has been drawn. The poseStack at this point is post-applyRotations
     * (yaw applied, scale applied) — the correct frame for feeding into AzureLib's armor pipeline, which internally flips Y
     * via our scale(-1,-1,1) below and expects vanilla-humanoid coordinate conventions.
     */
    @Override
    public void actuallyRender(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel model,
                               @Nullable RenderType renderType, MultiBufferSource bufferSource,
                               @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        // Let super do all the setup and rendering. This pushes pose, applies yaw/scale/rotations,
        // renders all geo bones, then pops. After return, poseStack is back to pre-actuallyRender state.
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);

        // For AzureLib armor we need the poseStack in post-applyRotations frame, which super already popped.
        // Reapply the same transforms here inside our own push/pop so armor sits correctly.
        if (!isReRender && AZURELIB_ARMOR_LOADED && bufferSource != null) {
            renderAzureLibArmor(poseStack, animatable, model, partialTick, packedLight);
        }
    }

    private void renderAzureLibArmor(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel model,
                                     float partialTick, int packedLight) {
        ItemStack helmet = animatable.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest  = animatable.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs   = animatable.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots  = animatable.getItemBySlot(EquipmentSlot.FEET);

        if (!hasAzRenderer(helmet) && !hasAzRenderer(chest)
                && !hasAzRenderer(legs) && !hasAzRenderer(boots)) {
            return;
        }

        poseStack.pushPose();
        try {
            // Replicate the relevant transforms from GeoEntityRenderer.actuallyRender so our
            // coordinate frame matches the one the geo bones were rendered in.
            float lerpBodyRot = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
            float ageInTicks = animatable.tickCount + partialTick;
            float nativeScale = animatable.getScale();

            poseStack.scale(nativeScale, nativeScale, nativeScale);
            applyRotations(animatable, poseStack, ageInTicks, lerpBodyRot, partialTick, nativeScale);
            poseStack.translate(0, 0.01f, 0);

            // Pose base models from geo bones (rotations only).
            poseBaseFromGeo(OUTER_ARMOR_MODEL, model);
            poseBaseFromGeo(INNER_ARMOR_MODEL, model);

            // Armor rendering assumes Y-down convention — matches vanilla LivingEntityRenderer.
            poseStack.scale(-1, -1, 1);
            poseStack.translate(0.0f, -1.501f, 0.0f);

            tryRenderAzArmor(poseStack, animatable, EquipmentSlot.HEAD,  helmet, packedLight);
            tryRenderAzArmor(poseStack, animatable, EquipmentSlot.CHEST, chest,  packedLight);
            tryRenderAzArmor(poseStack, animatable, EquipmentSlot.LEGS,  legs,   packedLight);
            tryRenderAzArmor(poseStack, animatable, EquipmentSlot.FEET,  boots,  packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    private static void tryRenderAzArmor(PoseStack poseStack, AbstractSpellCastingMob animatable,
                                         EquipmentSlot slot, ItemStack stack, int packedLight) {
        if (!hasAzRenderer(stack)) return;
        AzArmorRenderer renderer = AzArmorRendererRegistry.getOrNull(stack);
        if (renderer == null) return;

        HumanoidModel<?> baseModel = (slot == EquipmentSlot.LEGS) ? INNER_ARMOR_MODEL : OUTER_ARMOR_MODEL;
        int color = stack.is(ItemTags.DYEABLE) ? DyedItemColor.getOrDefault(stack, -6265536) : -1;

        poseStack.pushPose();
        try {
            renderer.prepForRender(animatable, stack, slot, baseModel);
            renderer.rendererPipeline().armorModel().renderToBuffer(
                    poseStack, null, packedLight, OverlayTexture.NO_OVERLAY, color);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("AzArmor render failed for " + slot + ": " + e);
        } finally {
            poseStack.popPose();
        }
    }

    private static boolean hasAzRenderer(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof ArmorItem
                && AzArmorRendererRegistry.getOrNull(stack) != null;
    }

    private static void poseBaseFromGeo(HumanoidModel<?> base, BakedGeoModel model) {
        copyRot(model, "head",      base.head);
        copyRot(model, "torso",     base.body);
        copyRot(model, "left_arm",  base.leftArm);
        copyRot(model, "right_arm", base.rightArm);
        copyRot(model, "left_leg",  base.leftLeg);
        copyRot(model, "right_leg", base.rightLeg);
    }

    private static void copyRot(BakedGeoModel model, String boneName, ModelPart part) {
        model.getBone(boneName).ifPresent(b -> {
            part.xRot = -b.getRotX();
            part.yRot = -b.getRotY();
            part.zRot =  b.getRotZ();
        });
    }

    private class CustomItemGeoLayer extends BlockAndItemGeoLayer<AbstractSpellCastingMob> {

        public CustomItemGeoLayer(AbstractMercenaryEntityRenderer renderer) {
            super(renderer);
        }

        @Nullable
        @Override
        protected ItemStack getStackForBone(GeoBone bone, AbstractSpellCastingMob animatable) {
            return switch (bone.getName()) {
                case LEFT_HAND -> {
                    ItemStack leftItem = animatable.isLeftHanded() ?
                            AbstractMercenaryEntityRenderer.this.mainHandItem :
                            AbstractMercenaryEntityRenderer.this.offhandItem;
                    if (shouldRenderCustom(leftItem)) yield leftItem;
                    yield null;
                }
                case RIGHT_HAND -> {
                    ItemStack rightItem = animatable.isLeftHanded() ?
                            AbstractMercenaryEntityRenderer.this.offhandItem :
                            AbstractMercenaryEntityRenderer.this.mainHandItem;
                    if (shouldRenderCustom(rightItem)) yield rightItem;
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
            if (AbstractMercenaryEntityRenderer.this.shieldWasHidden &&
                    !AbstractMercenaryEntityRenderer.this.hiddenShield.isEmpty()) {
                animatable.setItemSlot(EquipmentSlot.OFFHAND,
                        AbstractMercenaryEntityRenderer.this.hiddenShield);
            }

            if (stack == AbstractMercenaryEntityRenderer.this.mainHandItem) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
                if (stack.getItem() instanceof ShieldItem) {
                    poseStack.translate(0, 0.125, -0.25);
                }
            } else if (stack == AbstractMercenaryEntityRenderer.this.offhandItem) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
                if (stack.getItem() instanceof ShieldItem) {
                    poseStack.translate(0, 0.125, 0.25);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
            }

            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);

            AbstractMercenaryEntityRenderer.this.hiddenShield = ItemStack.EMPTY;
            AbstractMercenaryEntityRenderer.this.shieldWasHidden = false;
        }

        private boolean shouldRenderCustom(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return stack.getItem() instanceof ShieldItem;
        }
    }
}