package net.alshanex.magic_realms.compat;

import io.redspace.irons_artifice.gun.ArmPoseKind;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.ReloadState;
import net.alshanex.magic_realms.entity.humans.client.HeldItemPoser;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class GunArmPoser implements HeldItemPoser {

    private static final float QUARTER_PI = Mth.PI * 0.25f;

    @Override
    public boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }

    @Override
    public void pose(Context ctx) {
        if (!(ctx.stack().getItem() instanceof GunItem gunItem)) return;

        GeoBone shooting = ctx.shootingArm();
        GeoBone support = ctx.supportArm();
        GeoBone head = ctx.head();
        if (shooting == null || support == null || head == null) return;

        ReloadState reload = ReloadState.get(ctx.stack());
        if (reload != null && !reload.isFinished()) {
            poseReload(shooting, support, ctx.heldInRightArm(), reload.percent(ctx.partialTick()));
            return;
        }

        if (gunItem.getGun().armPoseKind() == ArmPoseKind.PISTOL) {
            posePistol(shooting, head);
        } else {
            poseRifle(shooting, support, head, ctx.heldInRightArm());
        }
    }

    private void poseRifle(GeoBone shooting, GeoBone support, GeoBone head, boolean rightArm) {
        float headYaw = -head.getRotY();
        float headPitch = -head.getRotX();

        shooting.setPosZ(shooting.getPosZ() + 1f);
        shooting.setRotY(-headYaw);
        shooting.setRotX(-((-Mth.HALF_PI) + headPitch + 0.1f));

        support.setRotY(-((rightArm ? 0.8f : -0.8f) + headYaw));
        support.setRotX(-(-1.5f + headPitch));
        support.setPosZ(support.getPosZ() - 3f);

        float f = -Math.min(headYaw, QUARTER_PI) / QUARTER_PI;
        if (headYaw > 0) {
            support.setPosX(support.getPosX() + f * 4f);
            support.setPosZ(support.getPosZ() + f * 2f);
            shooting.setPosZ(shooting.getPosZ() - f * 2f);
        } else {
            support.setPosZ(support.getPosZ() + f * 4f);
        }

        support.setPosY(support.getPosY() - 1f);
        shooting.setPosY(shooting.getPosY() - 2f);
    }

    private void posePistol(GeoBone shooting, GeoBone head) {
        shooting.setRotY(head.getRotY());
        shooting.setRotX(-(-1.5f + (-head.getRotX())));
    }

    private void poseReload(GeoBone shooting, GeoBone support, boolean rightArm, float progress) {
        shooting.setRotY(-(rightArm ? -0.8f : 0.8f));
        shooting.setRotX(0.97079635f);

        float alpha = Mth.sin(Mth.clamp(progress, 0f, 1f) * Mth.TWO_PI) * 0.5f + 0.5f;
        support.setRotY(-(Mth.lerp(alpha, 0.4f, 0.85f) * (rightArm ? 1f : -1f)));
        support.setRotX(-Mth.lerp(alpha, -0.97079635f, -Mth.HALF_PI));
    }
}
