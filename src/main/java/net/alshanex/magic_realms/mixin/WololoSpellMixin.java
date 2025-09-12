package net.alshanex.magic_realms.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.spells.evocation.WololoSpell;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WololoSpell.class)
public abstract class WololoSpellMixin extends AbstractSpell {
    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, .35f, true,
                livingEntity -> livingEntity instanceof Sheep
                        || (livingEntity instanceof RandomHumanEntity human && !human.isStunned()
                        && human.getData(MRDataAttachments.CONTRACT_DATA).isContractor(entity.getUUID())
                        && human.getData(MRDataAttachments.CONTRACT_DATA).isPermanent()
                )
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData healTargetingData) {
            var targetEntity = healTargetingData.getTarget((ServerLevel) level);
            if (targetEntity instanceof Sheep sheep) {
                sheep.setColor(DyeColor.values()[Utils.random.nextInt(DyeColor.values().length)]);
                MagicManager.spawnParticles(level, ParticleTypes.CRIT, sheep.getX(), sheep.getY() + .6, sheep.getZ(), 25, .5, .5, .5, 0, false);
            } else if (targetEntity instanceof RandomHumanEntity human) {
                boolean success = human.regenerateTexture();
                if (success) {
                    // Trigger client-side update
                    human.forceTextureRegeneration();
                }
                MagicManager.spawnParticles(level, ParticleTypes.CRIT, human.getX(), human.getY() + .6, human.getZ(), 25, .5, .5, .5, 0, false);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
