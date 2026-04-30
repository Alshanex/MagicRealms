package net.alshanex.magic_realms.entity.random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.TextureComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders the clothes, eyes, and hair texture layers on top of a {@link RandomHumanEntity}'s base skin texture. The base skin is drawn by the parent renderer's main pass via
 * {@link RandomHumanEntityRenderer#getTextureLocation}; this layer adds three extra passes over the same model geometry, each with a different translucent texture.
 *
 * <p>For preset textures (mercenaries with a single complete skin file), this layer is a no-op.
 */
@OnlyIn(Dist.CLIENT)
public class MercenarySkinLayer extends GeoRenderLayer<AbstractSpellCastingMob> {

    public MercenarySkinLayer(GeoEntityRenderer<AbstractSpellCastingMob> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        // Only random/layered humans need overlay passes. Other AbstractSpellCastingMob types (which this layer might be attached to via the renderer hierarchy) get nothing.
        if (!(animatable instanceof RandomHumanEntity human)) return;

        TextureComponents tc = human.getTextureComponents();
        if (tc == null) return;

        // Preset textures are a single complete file — no overlays.
        if (tc.isPresetTexture()) return;

        // Skin is the base layer drawn by the main renderer; here we just stack overlays on top.
        renderOverlayIfPresent(poseStack, animatable, bakedModel, bufferSource,
                partialTick, packedLight, packedOverlay, tc.getClothesTexture());
        renderOverlayIfPresent(poseStack, animatable, bakedModel, bufferSource,
                partialTick, packedLight, packedOverlay, tc.getEyesTexture());
        renderOverlayIfPresent(poseStack, animatable, bakedModel, bufferSource,
                partialTick, packedLight, packedOverlay, tc.getHairTexture());
    }

    private void renderOverlayIfPresent(PoseStack poseStack, AbstractSpellCastingMob animatable,
                                        BakedGeoModel bakedModel, MultiBufferSource bufferSource,
                                        float partialTick, int packedLight, int packedOverlay,
                                        String textureId) {
        if (textureId == null || textureId.isEmpty()) return;

        ResourceLocation texture;
        try {
            texture = toAssetPath(textureId);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Invalid overlay texture id '{}': {}", textureId, e.getMessage());
            return;
        }

        RenderType overlayType = RenderType.entityTranslucent(texture);
        VertexConsumer overlayBuffer = bufferSource.getBuffer(overlayType);

        getRenderer().actuallyRender(
                poseStack, animatable, bakedModel, overlayType,
                bufferSource, overlayBuffer,
                true,
                partialTick,
                packedLight, packedOverlay,
                0xFFFFFFFF
        );
    }

    /**
     * Same texture-id-to-asset-path conversion the renderer uses. Duplicated here to keep this
     * class self-contained; the RandomHumanEntityRenderer version stays for its own use.
     */
    private static ResourceLocation toAssetPath(String textureId) {
        ResourceLocation parsed = ResourceLocation.parse(textureId);
        return ResourceLocation.fromNamespaceAndPath(
                parsed.getNamespace(),
                "textures/" + parsed.getPath() + ".png"
        );
    }
}
