package net.alshanex.magic_realms.util.humans.titles;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.TitleProgressData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Central entry point for the titles system. Everything else (events, commands, items, GUI) goes through here.
 */
public final class TitleManager {

    private TitleManager() {}

    // Data access

    public static TitleProgressData progress(AbstractMercenaryEntity entity) {
        return entity.getData(MRDataAttachments.TITLE_PROGRESS);
    }

    /** Bumps a counter and re-evaluates titles. Server-side only; no-ops on the client. */
    public static void bump(AbstractMercenaryEntity entity, String counterKey, long delta) {
        if (entity == null || entity.level().isClientSide) return;
        if (counterKey == null || counterKey.isEmpty() || delta == 0L) return;

        entity.mutateTitleProgress(d -> d.addCounter(counterKey, delta));
        evaluate(entity);
    }

    /** Bumps several counters, then evaluates once. Cheaper than repeated {@link #bump} calls. */
    public static void bumpAll(AbstractMercenaryEntity entity, String... counterKeys) {
        if (entity == null || entity.level().isClientSide || counterKeys == null) return;

        entity.mutateTitleProgress(d -> {
            for (String key : counterKeys) {
                if (key != null && !key.isEmpty()) d.addCounter(key, 1L);
            }
        });
        evaluate(entity);
    }

    // Evaluation

    /**
     * Maximum cascade depth for {@code has_title} chains resolving inside a single evaluation. A chain deeper than
     * this still completes, just one evaluation tick later per extra link.
     */
    private static final int MAX_CASCADE_PASSES = 4;

    /**
     * Grants every catalog title whose requirements are now met and that the mercenary doesn't already hold.
     * Safe to call often - it is a cheap pass over the catalog and does nothing when nothing changed.
     *
     * <p>Runs repeatedly until a pass grants nothing, so a title gated on {@code has_title} unlocks in the same
     * evaluation as its prerequisite rather than waiting for the next tick.
     */
    public static void evaluate(AbstractMercenaryEntity entity) {
        if (entity == null || entity.level().isClientSide) return;

        TitleCatalog catalog = TitleCatalogHolder.server();
        if (catalog.isEmpty()) return;

        TitleProgressData data = progress(entity);
        List<Title> allNewlyEarned = new ArrayList<>();

        for (int pass = 0; pass < MAX_CASCADE_PASSES; pass++) {
            List<Title> earnedThisPass = new ArrayList<>();

            for (Title title : catalog.all()) {
                if (data.hasTitle(title.id())) continue;
                if (!title.isEarnedBy(entity, data)) continue;
                earnedThisPass.add(title);
            }

            if (earnedThisPass.isEmpty()) break;

            entity.mutateTitleProgress(d -> {
                for (Title t : earnedThisPass) d.grantTitle(t.id());
            });
            allNewlyEarned.addAll(earnedThisPass);
        }

        if (allNewlyEarned.isEmpty()) return;

        for (Title t : allNewlyEarned) {
            if (t.announce()) announce(entity, t);
        }
        spawnTitleEarnedEffects(entity);
        refreshDisplayName(entity);
    }

    // Granting / revoking

    /** Explicit grant, bypassing requirements. Used by commands and reward items. */
    public static boolean grant(AbstractMercenaryEntity entity, ResourceLocation titleId, boolean announce) {
        if (entity == null || entity.level().isClientSide || titleId == null) return false;

        TitleProgressData data = progress(entity);
        if (data.hasTitle(titleId)) return false;

        entity.mutateTitleProgress(d -> d.grantTitle(titleId));

        Title title = TitleCatalogHolder.server().byId(titleId);
        if (announce && title != null && title.announce()) announce(entity, title);
        spawnTitleEarnedEffects(entity);
        refreshDisplayName(entity);
        return true;
    }

    /**
     * Grants several titles at once with no announcement, particles or sound.
     *
     * <p>Used when titles are part of an entity's definition rather than something it achieved - a bandit profile
     * spawning a pre-titled miniboss, for instance. Firing the "title earned" fanfare every time such a mob spawns
     * would be noise, and there's no contractor to announce to anyway.
     *
     * @return how many titles were newly added
     */
    public static int grantSilently(AbstractMercenaryEntity entity, Collection<ResourceLocation> titleIds) {
        if (entity == null || entity.level().isClientSide || titleIds == null || titleIds.isEmpty()) return 0;

        int[] added = {0};
        entity.mutateTitleProgress(d -> {
            for (ResourceLocation id : titleIds) {
                if (id != null && d.grantTitle(id)) added[0]++;
            }
        });

        if (added[0] > 0) refreshDisplayName(entity);
        return added[0];
    }

    public static boolean revoke(AbstractMercenaryEntity entity, ResourceLocation titleId) {
        if (entity == null || entity.level().isClientSide || titleId == null) return false;

        TitleProgressData data = progress(entity);
        if (!data.hasTitle(titleId)) return false;

        entity.mutateTitleProgress(d -> d.revokeTitle(titleId));
        refreshDisplayName(entity);
        return true;
    }

    // Queries

    /** Every earned title that the current datapack still defines, in priority order. */
    public static List<Title> earnedTitles(AbstractMercenaryEntity entity) {
        TitleCatalog catalog = TitleCatalogHolder.get(entity.level().isClientSide);
        return catalog.resolve(progress(entity).getEarnedTitles());
    }

    /** Earned titles the contractor is allowed to pick from for display (hidden titles are excluded). */
    public static List<Title> selectableTitles(AbstractMercenaryEntity entity) {
        List<Title> out = new ArrayList<>();
        for (Title t : earnedTitles(entity)) {
            if (!t.hidden()) out.add(t);
        }
        return out;
    }

    /**
     * The title currently shown on the nameplate: the contractor's explicit pick if there is one, otherwise the
     * highest-priority non-hidden earned title, otherwise null.
     */
    @Nullable
    public static Title displayedTitle(AbstractMercenaryEntity entity) {
        TitleProgressData data = progress(entity);
        TitleCatalog catalog = TitleCatalogHolder.get(entity.level().isClientSide);

        ResourceLocation chosen = data.getDisplayedTitle();
        if (chosen != null) {
            Title t = catalog.byId(chosen);
            if (t != null && data.hasTitle(chosen)) return t;
        }

        // Catalog order is already priority-descending.
        for (Title t : catalog.all()) {
            if (t.hidden()) continue;
            if (data.hasTitle(t.id())) return t;
        }
        return null;
    }

    /** Sets which earned title shows on the nameplate. Passing null clears it back to the automatic pick. */
    public static boolean setDisplayedTitle(AbstractMercenaryEntity entity, @Nullable ResourceLocation titleId) {
        if (entity == null || entity.level().isClientSide) return false;

        boolean[] changed = {false};
        entity.mutateTitleProgress(d -> changed[0] = d.setDisplayedTitle(titleId));
        if (changed[0]) refreshDisplayName(entity);
        return changed[0];
    }

    /**
     * True when any held title grants immunity to this effect.
     *
     * <p>Consumed by {@code AbstractMercenaryEntity.canBeAffected}, which is vanilla's own gate on
     * {@link net.minecraft.world.entity.LivingEntity#addEffect} - so this blocks every source of the effect
     * (potions, tipped arrows, mob attacks, commands) without needing to intercept each one.
     */
    public static boolean isImmuneTo(AbstractMercenaryEntity entity, Holder<MobEffect> effect) {
        if (entity == null || effect == null) return false;

        for (Title t : earnedTitles(entity)) {
            for (Holder<MobEffect> immune : t.rewards().immuneEffects()) {
                if (immune.value() == effect.value()) return true;
            }
        }
        return false;
    }

    /** True when any held title grants out-of-combat regeneration. Replaces the old boss-kill regen unlock. */
    public static boolean hasNaturalRegen(AbstractMercenaryEntity entity) {
        for (Title t : earnedTitles(entity)) {
            if (t.rewards().naturalRegen()) return true;
        }
        return false;
    }

    // Nameplate

    /**
     * Rebuilds the visible nameplate. The title is <em>not</em> appended here - it is drawn one line above the name
     * by {@code TitleNameplateRenderer}, which reads the synced {@code TITLE_PROGRESS} attachment on the client.
     * This method exists so the plain name stays correct when it's renamed or first initialised.
     */
    public static void refreshDisplayName(AbstractMercenaryEntity entity) {
        if (entity == null) return;

        String name = entity.getEntityName();
        if (name == null || name.isEmpty()) return;

        entity.setCustomName(Component.literal(name));
        entity.setCustomNameVisible(true);
    }

    // Feedback

    private static void announce(AbstractMercenaryEntity entity, Title title) {
        ContractData contract = entity.getData(MRDataAttachments.CONTRACT_DATA);
        if (contract == null) return;

        UUID contractorUUID = contract.getContractorUUID();
        if (contractorUUID == null) return;

        Player contractor = entity.level().getPlayerByUUID(contractorUUID);
        if (!(contractor instanceof ServerPlayer serverPlayer)) return;

        serverPlayer.sendSystemMessage(Component.translatable(
                "ui.magic_realms.title_earned",
                Component.literal(entity.getEntityName()).withStyle(ChatFormatting.YELLOW),
                title.displayComponent()
        ));
    }

    private static void spawnTitleEarnedEffects(AbstractMercenaryEntity entity) {
        if (entity.level().isClientSide) return;

        try {
            MagicManager.spawnParticles(entity.level(),
                    new BlastwaveParticleOptions(SchoolRegistry.HOLY.get().getTargetingColor(), 4),
                    entity.getX(), entity.getY() + .165f, entity.getZ(), 1, 0, 0, 0, 0, true);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
                    new ShockwaveParticlesPacket(new Vec3(entity.getX(), entity.getY() + .165f, entity.getZ()),
                            4, ParticleRegistry.CLEANSE_PARTICLE.get()));

            entity.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.4F);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to spawn title effects: {}", e.getMessage());
        }
    }
}