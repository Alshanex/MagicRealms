package net.alshanex.magic_realms.entity.humans.exclusive.aliana;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.exclusive.AbstractFixedTextureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AlianaEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation ALIANA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/aliana.png");

    public AlianaEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AlianaEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return ALIANA_TEXTURE;
    }
}
