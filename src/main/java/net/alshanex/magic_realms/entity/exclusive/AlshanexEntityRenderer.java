package net.alshanex.magic_realms.entity.exclusive;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AlshanexEntityRenderer extends AbstractMercenaryEntityRenderer {

    public AlshanexEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AlshanexEntityModel());
    }
}
