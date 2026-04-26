package net.alshanex.magic_realms.entity.exclusive.lilac;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.exclusive.AbstractFixedTextureRenderer;
import net.alshanex.magic_realms.entity.exclusive.ace.AceEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class LilacEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation LILAC_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/lilac.png");

    public LilacEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LilacEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return LILAC_TEXTURE;
    }
}
