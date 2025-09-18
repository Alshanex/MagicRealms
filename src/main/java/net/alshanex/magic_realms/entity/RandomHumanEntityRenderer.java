package net.alshanex.magic_realms.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.EntityTextureConfig;
import net.minecraft.client.Minecraft;
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
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob entity) {
        if (entity instanceof RandomHumanEntity human) {
            String entityUUID = human.getUUID().toString();
            Minecraft mc = Minecraft.getInstance();
            human.debugTextureGeneration();

            // Priority 1: Received server texture (multiplayer)
            CombinedTextureManager.TextureResult receivedTexture =
                    CombinedTextureManager.getReceivedTexture(entityUUID);
            if (receivedTexture != null) {
                return receivedTexture.getTextureLocation();
            }

            // Priority 2: Entity's own texture config (should be deterministic)
            EntityTextureConfig config = human.getTextureConfig();

            // If config is null, try to create one using the entity (deterministic)
            if (config == null && human.getGender() != null && human.getEntityClass() != null) {
                try {
                    // Create deterministic texture config using entity - this ensures consistency
                    config = new EntityTextureConfig(human);
                    MagicRealms.LOGGER.debug("Created deterministic texture config for entity in renderer: {}", entityUUID);
                } catch (Exception e) {
                    MagicRealms.LOGGER.error("Failed to create texture config in renderer for entity: {}", entityUUID, e);
                }
            }

            if (config != null && config.hasValidTexture()) {
                MagicRealms.LOGGER.debug("Using entity texture config for entity: {}", entityUUID);
                return config.getSkinTexture();
            }

            // Priority 3: Cached texture
            ResourceLocation cachedTexture = CombinedTextureManager.getCachedTexture(entityUUID);
            if (cachedTexture != null) {
                MagicRealms.LOGGER.debug("Using cached texture for entity: {}", entityUUID);
                return cachedTexture;
            }

            MagicRealms.LOGGER.warn("No texture available for entity: {}, using fallback", entityUUID);
        }

        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
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
