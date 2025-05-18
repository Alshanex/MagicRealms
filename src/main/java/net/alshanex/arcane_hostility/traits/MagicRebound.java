package net.alshanex.arcane_hostility.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class MagicRebound extends LegendaryTrait {
    public MagicRebound(ChatFormatting format) {
        super(format);
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        if(event.getSource().getDirectEntity() instanceof AbstractMagicProjectile projectile){
            LivingEntity target = event.getTarget();

            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 projectilePos = projectile.position();

            Vec3 direction = projectilePos.subtract(targetPos).normalize();

            Vec3 currentMotion = projectile.getDeltaMovement();
            float speed = (float) currentMotion.length();

            float bounceFactor = 0.8f;

            projectile.setDeltaMovement(direction.scale(speed * bounceFactor));

            projectile.setPos(
                    projectile.getX() + direction.x * 0.5,
                    projectile.getY() + direction.y * 0.5,
                    projectile.getZ() + direction.z * 0.5
            );
            return true;
        }
        return false;
    }
}
