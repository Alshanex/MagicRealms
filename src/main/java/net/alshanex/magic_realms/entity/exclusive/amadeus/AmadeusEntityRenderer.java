package net.alshanex.magic_realms.entity.exclusive.amadeus;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.AbstractFixedTextureRenderer;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntityModel;
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
