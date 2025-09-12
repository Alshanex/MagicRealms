package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class TavernKeeperEntityRenderer extends AbstractSpellCastingMobRenderer {
    public TavernKeeperEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new TavernKeeperEntityModel());
    }
}
