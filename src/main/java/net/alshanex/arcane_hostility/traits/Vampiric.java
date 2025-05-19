package net.alshanex.arcane_hostility.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;

public class Vampiric extends MobTrait {
    public Vampiric(ChatFormatting format) {
        super(format);
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        if(event.getSource().is(SchoolRegistry.BLOOD.get().getDamageType())){
            event.getTarget().heal(event.getDamageOriginal());
            return !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
                    !event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS);
        }
        return false;
    }
}
