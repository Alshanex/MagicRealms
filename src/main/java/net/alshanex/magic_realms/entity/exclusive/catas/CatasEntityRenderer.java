package net.alshanex.magic_realms.entity.exclusive.catas;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.AbstractFixedTextureRenderer;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class CatasEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation CATAS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/catas.png");

    public CatasEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CatasEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return CATAS_TEXTURE;
    }
}
