package net.alshanex.magic_realms.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class RandomHumanEntityRenderer extends AbstractSpellCastingMobRenderer {
    public RandomHumanEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RandomHumanEntityModel());

        addRenderLayer(new ClothesLayer(this));
        addRenderLayer(new EyesLayer(this));
        addRenderLayer(new HairLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob animatable) {
        return ((RandomHumanEntity)animatable).getTextureConfig().getSkinTexture();
    }

    public static class EyesLayer extends GeoRenderLayer<AbstractSpellCastingMob> {

        public EyesLayer(GeoEntityRenderer<AbstractSpellCastingMob> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            ResourceLocation eyesTexture = ((RandomHumanEntity)animatable).getTextureConfig().getEyesTexture();
            if (eyesTexture != null) {
                poseStack.scale(1.002F, 1.002F, 1.002F);
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(eyesTexture));
                this.getRenderer().actuallyRender(poseStack, animatable, bakedModel, renderType, bufferSource, vertexConsumer, true, partialTick, packedLight,
                        OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        }
    }

    public static class ClothesLayer extends GeoRenderLayer<AbstractSpellCastingMob> {

        public ClothesLayer(GeoEntityRenderer<AbstractSpellCastingMob> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

            ResourceLocation clothesTexture = ((RandomHumanEntity)animatable).getTextureConfig().getClothesTexture();
            if (clothesTexture != null) {
                poseStack.scale(1.01F, 1.01F, 1.01F);
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(clothesTexture));
                this.getRenderer().actuallyRender(poseStack, animatable, bakedModel, renderType, bufferSource, vertexConsumer, true, partialTick, packedLight,
                        OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        }
    }

    public static class HairLayer extends GeoRenderLayer<AbstractSpellCastingMob> {

        public HairLayer(GeoEntityRenderer<AbstractSpellCastingMob> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

            ResourceLocation hairTexture = ((RandomHumanEntity)animatable).getTextureConfig().getHairTexture();
            if (hairTexture != null) {
                poseStack.scale(1.02F, 1.02F, 1.02F);
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(hairTexture));
                this.getRenderer().actuallyRender(poseStack, animatable, bakedModel, renderType, bufferSource, vertexConsumer, true, partialTick, packedLight,
                        OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        }
    }
}
