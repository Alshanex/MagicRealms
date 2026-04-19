package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.network.OpenSkinCustomizerPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class SkinCustomizerItem extends Item {
    private static final float REACH = 20.0f;

    public SkinCustomizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (player.level().isClientSide) {
            return InteractionResult.FAIL;
        }

        if (!canUse(player)) {
            return InteractionResult.FAIL;
        }

        if(!(interactionTarget instanceof RandomHumanEntity human)){
            return InteractionResult.FAIL;
        }

        CompoundTag metadata = human.getTextureMetadata();
        if (metadata.getBoolean("usePreset")) {
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp,
                    new OpenSkinCustomizerPacket(
                            human.getUUID(),
                            human.getGender().getName(),
                            human.getEntityClass().getName(),
                            human.getEntityName() != null ? human.getEntityName() : "",
                            metadata.contains("skinTexture") ? metadata.getString("skinTexture") : "",
                            metadata.contains("clothesTexture") ? metadata.getString("clothesTexture") : "",
                            metadata.contains("eyesTexture") ? metadata.getString("eyesTexture") : "",
                            metadata.contains("hairTexture") ? metadata.getString("hairTexture") : ""
                    ));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.magic_realms.skin_customizer.creative").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipComponents.add(Component.translatable("item.magic_realms.skin_customizer.preset_not_editable").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private boolean canUse(Player player) {
        if (player.getAbilities().instabuild) return true;
        if (player instanceof ServerPlayer sp) {
            return sp.hasPermissions(2);
        }
        return false;
    }

    private Entity rayTraceEntity(Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eyePos.add(look.scale(REACH));

        AABB box = player.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(), player, eyePos, end, box,
                e -> !e.isSpectator() && e.isPickable() && e instanceof RandomHumanEntity,
                REACH * REACH);
        return hit != null ? hit.getEntity() : null;
    }
}
