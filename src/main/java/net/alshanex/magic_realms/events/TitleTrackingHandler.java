package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.titles.Title;
import net.alshanex.magic_realms.util.humans.titles.TitleKeys;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.alshanex.magic_realms.util.humans.titles.TitleRewards;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.UUID;

/**
 * Replaces the old {@code KillTrackingHandler}. Feeds combat feats into
 * {@link net.alshanex.magic_realms.data.TitleProgressData} counters and applies the combat-side title rewards
 * (bonus damage, damage scaling against mob families, on-hit effects, incoming damage reduction).
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TitleTrackingHandler {

    /** How close a contracted mercenary must be to share credit for their contractor's kill. */
    private static final double ASSIST_RADIUS = 32.0;

    /** Health fraction below which a surviving hit counts as a "close call". */
    private static final float CLOSE_CALL_THRESHOLD = 0.15f;

    private TitleTrackingHandler() {}

    // Kills

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        // Never let mercenaries farm each other for titles.
        if (victim instanceof AbstractMercenaryEntity) return;

        var killer = event.getSource().getEntity();

        if (killer instanceof AbstractMercenaryEntity mercenary) {
            creditKill(mercenary, victim);
            return;
        }

        if (killer instanceof Player player) {
            creditNearbyContractedMercenaries(player, victim);
        }
    }

    private static void creditKill(AbstractMercenaryEntity mercenary, LivingEntity victim) {
        if (!countsAsKill(victim)) return;

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        boolean boss = victim.getType().is(ModTags.BOSSES_TAG);

        mercenary.mutateTitleProgress(d -> {
            d.addCounter(TitleKeys.KILLS_TOTAL, 1L);
            if (typeId != null) d.addCounter(TitleKeys.killEntity(typeId), 1L);
            if (boss) d.addCounter(TitleKeys.KILLS_BOSS, 1L);
        });

        TitleManager.evaluate(mercenary);
    }

    private static void creditNearbyContractedMercenaries(Player player, LivingEntity victim) {
        if (!countsAsKill(victim)) return;

        AABB box = victim.getBoundingBox().inflate(ASSIST_RADIUS);
        List<AbstractMercenaryEntity> nearby = victim.level().getEntitiesOfClass(
                AbstractMercenaryEntity.class, box,
                merc -> merc.isAlive() && isContractedBy(merc, player.getUUID()));

        for (AbstractMercenaryEntity mercenary : nearby) {
            creditKill(mercenary, victim);
        }
    }

    /** Only hostile mobs count, matching the old XP rules. Passive animals shouldn't build a war record. */
    private static boolean countsAsKill(LivingEntity victim) {
        return victim instanceof Monster || victim.getType().is(ModTags.BOSSES_TAG);
    }

    private static boolean isContractedBy(AbstractMercenaryEntity mercenary, UUID playerUUID) {
        ContractData contract = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
        if (contract == null || !contract.hasActiveContract(mercenary.level())) return false;
        UUID contractor = contract.getContractorUUID();
        return contractor != null && contractor.equals(playerUUID);
    }

    // Damage: outgoing rewards + incoming reduction + counters

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        var attacker = event.getSource().getEntity();
        if (attacker instanceof AbstractMercenaryEntity mercenary && mercenary != victim) {
            handleMercenaryAttack(mercenary, victim, event);
        }

        if (victim instanceof AbstractMercenaryEntity mercenary) {
            handleMercenaryDefense(mercenary, event);
        }
    }

    private static void handleMercenaryAttack(AbstractMercenaryEntity mercenary, LivingEntity victim,
                                              LivingIncomingDamageEvent event) {
        List<Title> titles = TitleManager.earnedTitles(mercenary);

        float amount = event.getAmount();
        double flatBonus = 0.0;
        double multiplier = 1.0;

        // Resolved once rather than per title — weapon bonuses key off what's in hand, not what's being hit.
        ItemStack weapon = mercenary.getMainHandItem();

        for (Title title : titles) {
            TitleRewards rewards = title.rewards();
            flatBonus += rewards.bonusDamage();

            for (TitleRewards.DamageBonus bonus : rewards.damageBonusVs()) {
                if (matchesTag(victim, bonus.normalizedTag())) {
                    multiplier *= bonus.multiplier();
                }
            }

            for (TitleRewards.WeaponBonus bonus : rewards.weaponBonuses()) {
                if (bonus.matches(weapon)) {
                    flatBonus += bonus.bonus();
                    multiplier *= bonus.multiplier();
                }
            }

            for (TitleRewards.OnHitEffect onHit : rewards.onHitEffects()) {
                if (onHit.chance() >= 1.0f || mercenary.getRandom().nextFloat() < onHit.chance()) {
                    victim.addEffect(new MobEffectInstance(
                            onHit.effect(), onHit.duration(), onHit.amplifier(), false, true));
                }
            }
        }

        float finalAmount = (float) ((amount + flatBonus) * multiplier);
        if (finalAmount != amount) {
            event.setAmount(Math.max(0.0f, finalAmount));
        }

        long dealt = (long) Math.floor(Math.max(0.0f, finalAmount));
        if (dealt > 0) {
            mercenary.mutateTitleProgress(d -> d.addCounter(TitleKeys.DAMAGE_DEALT, dealt));
        }
    }

    private static void handleMercenaryDefense(AbstractMercenaryEntity mercenary, LivingIncomingDamageEvent event) {
        List<Title> titles = TitleManager.earnedTitles(mercenary);

        double multiplier = 1.0;
        for (Title title : titles) {
            multiplier *= title.rewards().incomingDamageMultiplier();
        }

        float amount = event.getAmount();
        if (multiplier != 1.0) {
            amount = (float) Math.max(0.0, amount * multiplier);
            event.setAmount(amount);
        }

        long taken = (long) Math.floor(amount);
        float healthBefore = mercenary.getHealth();
        float threshold = mercenary.getMaxHealth() * CLOSE_CALL_THRESHOLD;
        boolean closeCall = healthBefore > threshold && (healthBefore - amount) <= threshold && (healthBefore - amount) > 0.0f;

        if (taken > 0 || closeCall) {
            final long takenFinal = taken;
            mercenary.mutateTitleProgress(d -> {
                if (takenFinal > 0) d.addCounter(TitleKeys.DAMAGE_TAKEN, takenFinal);
                if (closeCall) d.addCounter(TitleKeys.CLOSE_CALLS, 1L);
            });
        }
    }

    private static boolean matchesTag(LivingEntity entity, String tagId) {
        if (tagId == null || tagId.isEmpty()) return false;
        ResourceLocation id = ResourceLocation.tryParse(tagId);
        if (id == null) return false;

        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, id);
        return entity.getType().is(tag);
    }
}