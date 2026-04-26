package net.alshanex.magic_realms.entity.exclusive.jara;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.exclusive.AbstractFixedTextureRenderer;
import net.alshanex.magic_realms.entity.exclusive.ace.AceEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class JaraEntityRenderer extends AbstractFixedTextureRenderer {
    private static final ResourceLocation JARA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/jara.png");

    public JaraEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new JaraEntityModel());
    }

    @Override
    protected ResourceLocation getOriginalTexture() {
        return JARA_TEXTURE;
    }
}
