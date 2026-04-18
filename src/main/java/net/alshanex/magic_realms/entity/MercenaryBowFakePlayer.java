package net.alshanex.magic_realms.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

public class MercenaryBowFakePlayer extends FakePlayer {
    private static final GameProfile PROFILE = new GameProfile(
            UUID.fromString("41C82C87-7AFB-4024-BA57-13D2C99CAE77"),
            "[MercenaryBowUser]"
    );

    public MercenaryBowFakePlayer(ServerLevel level) {
        super(level, PROFILE);
    }

    public void syncFrom(AbstractMercenaryEntity merc, ItemStack bow, ItemStack arrow) {
        this.setPos(merc.getX(), merc.getY(), merc.getZ());
        this.setXRot(merc.getXRot());
        this.setYRot(merc.getYRot());
        this.yHeadRot = merc.yHeadRot;
        this.yBodyRot = merc.yBodyRot;
        this.setDeltaMovement(merc.getDeltaMovement());

        // Aim at target — the bow reads xRot/yRot via shootFromRotation
        // (caller sets these before calling releaseUsing)

        // CRITICAL: pass the real bow stack so durability damage lands on the merc's actual item
        this.setItemInHand(InteractionHand.MAIN_HAND, bow);
        this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        // Ensure getProjectile(bow) returns our arrow
        this.getInventory().clearContent();
        this.getInventory().setItem(0, arrow);
        this.getInventory().selected = 0;

        this.getAbilities().instabuild = false;
        this.getAbilities().invulnerable = true; // prevent weird feedback loops
    }
}
