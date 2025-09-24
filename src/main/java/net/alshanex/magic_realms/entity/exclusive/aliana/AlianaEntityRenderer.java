package net.alshanex.magic_realms.entity.exclusive.aliana;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.AbstractFixedTextureRenderer;
import net.alshanex.magic_realms.entity.exclusive.alshanex.AlshanexEntityModel;
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
