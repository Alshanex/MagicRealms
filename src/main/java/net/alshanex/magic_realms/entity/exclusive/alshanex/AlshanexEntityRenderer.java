package net.alshanex.magic_realms.entity.exclusive.alshanex;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.AbstractFixedTextureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AlshanexEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation ALSHANEX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/alshanex.png");

    public AlshanexEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AlshanexEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return ALSHANEX_TEXTURE;
    }
}
