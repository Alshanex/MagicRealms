package net.alshanex.magic_realms.entity.exclusive.aliana;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.alshanex.AlshanexEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AlianaEntityRenderer extends AbstractMercenaryEntityRenderer {

    public AlianaEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AlianaEntityModel());
    }
}
