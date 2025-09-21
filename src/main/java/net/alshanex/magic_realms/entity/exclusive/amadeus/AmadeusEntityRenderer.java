package net.alshanex.magic_realms.entity.exclusive.amadeus;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AmadeusEntityRenderer extends AbstractMercenaryEntityRenderer {

    public AmadeusEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AmadeusEntityModel());
    }
}
