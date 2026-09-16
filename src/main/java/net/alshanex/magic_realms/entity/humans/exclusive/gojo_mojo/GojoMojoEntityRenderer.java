package net.alshanex.magic_realms.entity.humans.exclusive.gojo_mojo;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.exclusive.AbstractFixedTextureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class GojoMojoEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation GOJO_MOJO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/gojo_mojo.png");

    public GojoMojoEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GojoMojoEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return GOJO_MOJO_TEXTURE;
    }
}
