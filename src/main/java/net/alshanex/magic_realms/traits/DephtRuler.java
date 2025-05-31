package net.alshanex.magic_realms.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;

public class DephtRuler extends MobTrait {
    public DephtRuler(ChatFormatting format) {
        super(format);
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        return !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
                !event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS) &&
                event.getSource().is(SchoolRegistry.ELDRITCH.get().getDamageType());
    }
}
