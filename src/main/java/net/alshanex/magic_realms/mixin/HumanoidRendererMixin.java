package net.alshanex.magic_realms.mixin;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.alshanex.magic_realms.entity.exclusive.lilac.LilacEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.mobs.HumanoidRenderer$2")
public class HumanoidRendererMixin {
    @Redirect(
            method = "getStackForBone",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/mobs/abstract_spell_casting_mob/AbstractSpellCastingMobRenderer;makePotion(Lio/redspace/ironsspellbooks/entity/mobs/abstract_spell_casting_mob/AbstractSpellCastingMob;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack redirectMakePotion(AbstractSpellCastingMob mob) {
        if (mob instanceof LilacEntity) {
            ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
            SuspiciousStewEffects.Entry saturationEntry = new SuspiciousStewEffects.Entry(MobEffects.SATURATION, 160);
            SuspiciousStewEffects effects = SuspiciousStewEffects.EMPTY.withEffectAdded(saturationEntry);
            stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
            return stew;
        }
        return AbstractSpellCastingMobRenderer.makePotion(mob);
    }
}
