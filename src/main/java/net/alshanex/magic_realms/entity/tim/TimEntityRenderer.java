package net.alshanex.magic_realms.entity.tim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class TimEntityRenderer extends AbstractSpellCastingMobRenderer {

    public TimEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TimEntityModel());
        this.addRenderLayer(new TimGlowingEyesLayer(this));
    }

    public static class TimGlowingEyesLayer extends GeoRenderLayer<AbstractSpellCastingMob> {
        private static final RenderType EYES = RenderType.eyes(
                ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/tim/tim_eyes.png")
        );

        public TimGlowingEyesLayer(GeoEntityRenderer<AbstractSpellCastingMob> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            VertexConsumer eyesBuffer = bufferSource.getBuffer(EYES);
            this.getRenderer().actuallyRender(
                    poseStack, animatable, bakedModel, EYES,
                    bufferSource, eyesBuffer, true, partialTick,
                    15728640, packedOverlay, 0xFFFFFFFF
            );
        }
    }
}
