package net.alshanex.magic_realms.entity.exclusive.ace;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.exclusive.AbstractFixedTextureRenderer;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AceEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation ACE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/eden.png");

    public AceEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AceEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return ACE_TEXTURE;
    }
}
