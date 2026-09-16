package net.alshanex.magic_realms.entity.humans;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.resources.ResourceLocation;

public class RandomHumanEntityModel extends AbstractMercenaryEntityModel {
    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }
}
