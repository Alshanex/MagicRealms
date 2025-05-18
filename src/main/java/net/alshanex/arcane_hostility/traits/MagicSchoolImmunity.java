package net.alshanex.arcane_hostility.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;

public class MagicSchoolImmunity extends MobTrait {
    private SchoolType school;

    public MagicSchoolImmunity(ChatFormatting format, SchoolType school) {
        super(format);
        this.school = school;
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        return !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
                !event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS) &&
                event.getSource().is(this.school.getDamageType());
    }
}
