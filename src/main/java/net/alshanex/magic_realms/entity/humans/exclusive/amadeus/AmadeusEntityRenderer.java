package net.alshanex.magic_realms.entity.humans.exclusive.amadeus;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.exclusive.AbstractFixedTextureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AmadeusEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation AMADEUS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/amadeus.png");

    public AmadeusEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AmadeusEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return AMADEUS_TEXTURE;
    }
}
