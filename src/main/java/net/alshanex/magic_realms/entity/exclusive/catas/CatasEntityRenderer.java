package net.alshanex.magic_realms.entity.exclusive.catas;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CatasEntityRenderer extends AbstractMercenaryEntityRenderer {

    public CatasEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CatasEntityModel());
    }
}
