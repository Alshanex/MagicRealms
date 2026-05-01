package net.alshanex.magic_realms.item;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.stats.LevelingStatsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class KnowledgeCrystalItem extends Item {
    public KnowledgeCrystalItem() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if(interactionTarget instanceof AbstractMercenaryEntity mercenary && !player.level().isClientSide){
            KillTrackerData killData = mercenary.getData(MRDataAttachments.KILL_TRACKER);
            int level = killData.getCurrentLevel();

            if(level < Config.maxLevel){
                int newLevel = level + 1;
                mercenary.mutateKillTracker(d -> d.setLevel(newLevel));

                spawnLevelUpEffects(mercenary);
                mercenary.updateCustomNameWithStars();
                LevelingStatsManager.applyLevelBasedAttributes(mercenary, newLevel);

                stack.shrink(1);

                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
    }

    private static void spawnLevelUpEffects(AbstractMercenaryEntity entity) {
        if (entity.level().isClientSide) return;

        try {
            MagicManager.spawnParticles(entity.level(), new BlastwaveParticleOptions(SchoolRegistry.HOLY.get().getTargetingColor(), 4), entity.getX(), entity.getY() + .165f, entity.getZ(), 1, 0, 0, 0, 0, true);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new ShockwaveParticlesPacket(new Vec3(entity.getX(), entity.getY() + .165f, entity.getZ()), 4, ParticleRegistry.CLEANSE_PARTICLE.get()));

            entity.playSound(
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    0.8F,
                    1.2F
            );
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to spawn level up effects: {}", e.getMessage());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.magic_realms.knowledge_crystal.tooltip"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
