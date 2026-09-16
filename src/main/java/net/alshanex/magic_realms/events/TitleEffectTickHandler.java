package net.alshanex.magic_realms.events;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.titles.Title;
import net.alshanex.magic_realms.util.humans.titles.TitleKeys;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.alshanex.magic_realms.util.humans.titles.TitleRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.*;

/**
 * Applies the persistent half of the title rewards, and records the non-combat feats.
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TitleEffectTickHandler {

    /** How often the handler runs. Everything below is a multiple of this. */
    public static final int CHECK_INTERVAL_TICKS = 20;

    /** Passive effects are refreshed every cycle with a duration comfortably longer than the cycle. */
    private static final int PASSIVE_EFFECT_DURATION = 100;

    /** One in-game minute, in ticks. */
    private static final int TICKS_PER_MINUTE = 1200;

    /** Structure occupancy is polled every N cycles (5 * 20 ticks = 5 seconds). */
    private static final int STRUCTURE_CHECK_CYCLES = 5;

    /** Full catalog re-evaluation cadence, as a safety net for counters bumped outside {@code TitleManager}. */
    private static final int EVALUATE_CYCLES = 10;

    /** Per-entity map of applied modifier id -> the attribute it lives on. */
    private static final Map<UUID, Map<ResourceLocation, Holder<Attribute>>> appliedModifiers = new HashMap<>();

    /** Per-entity set of structures the mercenary is currently standing inside, for entry-edge detection. */
    private static final Map<UUID, Set<ResourceLocation>> occupiedStructures = new HashMap<>();

    private TitleEffectTickHandler() {}

    public static void tick(AbstractMercenaryEntity entity) {
        if (entity.level().isClientSide) return;

        applyAttributeModifiers(entity);
        stripImmuneEffects(entity);
        applyPassiveEffects(entity);

        int cycle = entity.tickCount / CHECK_INTERVAL_TICKS;

        if (cycle % STRUCTURE_CHECK_CYCLES == 0) {
            trackStructures(entity);
        }

        if (entity.tickCount % TICKS_PER_MINUTE == 0) {
            trackContractTime(entity);
        }

        if (cycle % EVALUATE_CYCLES == 0) {
            TitleManager.evaluate(entity);
        }
    }

    // Attribute modifiers

    private static void applyAttributeModifiers(AbstractMercenaryEntity entity) {
        List<Title> titles = TitleManager.earnedTitles(entity);

        // Build the desired set, keyed by a stable id derived from the title and entry index.
        Map<ResourceLocation, DesiredModifier> desired = new HashMap<>();
        for (Title title : titles) {
            List<TitleRewards.AttributeEntry> entries = title.rewards().attributeModifiers();
            for (int i = 0; i < entries.size(); i++) {
                TitleRewards.AttributeEntry entry = entries.get(i);
                ResourceLocation modId = modifierIdFor(title, i);
                desired.putIfAbsent(modId, new DesiredModifier(
                        entry.attribute(),
                        new AttributeModifier(modId, entry.modifier().amount(), entry.modifier().operation())
                ));
            }
        }

        Map<ResourceLocation, Holder<Attribute>> current =
                appliedModifiers.computeIfAbsent(entity.getUUID(), k -> new HashMap<>());

        // Remove anything no longer desired.
        if (!current.isEmpty()) {
            Iterator<Map.Entry<ResourceLocation, Holder<Attribute>>> it = current.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ResourceLocation, Holder<Attribute>> entry = it.next();
                if (desired.containsKey(entry.getKey())) continue;

                AttributeInstance instance = entity.getAttribute(entry.getValue());
                if (instance != null) {
                    AttributeModifier existing = instance.getModifier(entry.getKey());
                    if (existing != null) instance.removeModifier(existing);
                }
                it.remove();
            }
        }

        // Add anything new, or re-add anything the AttributeInstance has lost.
        for (Map.Entry<ResourceLocation, DesiredModifier> entry : desired.entrySet()) {
            DesiredModifier desiredMod = entry.getValue();
            AttributeInstance instance = entity.getAttribute(desiredMod.attribute);
            if (instance == null) {
                current.remove(entry.getKey());
                continue;
            }

            // The AttributeInstance is authoritative, not our tracking map.
            if (instance.getModifier(entry.getKey()) != null) {
                current.put(entry.getKey(), desiredMod.attribute);
                continue;
            }

            instance.addTransientModifier(desiredMod.modifier);
            current.put(entry.getKey(), desiredMod.attribute);
        }
    }

    /** Removes every title modifier this handler applied and forgets the entity. */
    public static void clearAllAppliedModifiers(AbstractMercenaryEntity entity) {
        Map<ResourceLocation, Holder<Attribute>> map = appliedModifiers.remove(entity.getUUID());
        if (map == null || map.isEmpty()) return;

        for (Map.Entry<ResourceLocation, Holder<Attribute>> entry : map.entrySet()) {
            AttributeInstance instance = entity.getAttribute(entry.getValue());
            if (instance == null) continue;
            AttributeModifier existing = instance.getModifier(entry.getKey());
            if (existing != null) instance.removeModifier(existing);
        }
    }

    /**
     * Stable modifier id for the i-th attribute entry of a title, e.g.
     * {@code magic_realms:title.magic_realms.dragonslayer.0}.
     */
    private static ResourceLocation modifierIdFor(Title title, int entryIndex) {
        String safe = title.id() == null
                ? "unknown"
                : title.id().toString().replace(':', '.').replace('/', '.');
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "title." + safe + "." + entryIndex);
    }

    // Passive effects

    private static void applyPassiveEffects(AbstractMercenaryEntity entity) {
        for (Title title : TitleManager.earnedTitles(entity)) {
            for (TitleRewards.EffectSpec spec : title.rewards().passiveEffects()) {
                Holder<MobEffect> effect = spec.effect();

                // Immunity wins over a passive grant, so a pack that both grants and immunises doesn't flicker.
                if (TitleManager.isImmuneTo(entity, effect)) continue;

                MobEffectInstance existing = entity.getEffect(effect);

                // Only refresh when it's about to lapse, or when a weaker instance is present.
                if (existing != null
                        && existing.getAmplifier() >= spec.amplifier()
                        && existing.getDuration() > CHECK_INTERVAL_TICKS * 2) {
                    continue;
                }

                entity.addEffect(new MobEffectInstance(
                        effect, PASSIVE_EFFECT_DURATION, spec.amplifier(), true, spec.showParticles()));
            }
        }
    }

    /**
     * Clears effects the mercenary is now immune to.
     */
    private static void stripImmuneEffects(AbstractMercenaryEntity entity) {
        List<Title> titles = TitleManager.earnedTitles(entity);
        if (titles.isEmpty()) return;

        List<Holder<MobEffect>> toRemove = null;
        for (Title title : titles) {
            for (Holder<MobEffect> immune : title.rewards().immuneEffects()) {
                if (entity.getEffect(immune) == null) continue;
                if (toRemove == null) toRemove = new ArrayList<>(2);
                toRemove.add(immune);
            }
        }

        if (toRemove == null) return;
        // Collected first so we're not mutating the active-effects map while iterating it.
        for (Holder<MobEffect> effect : toRemove) {
            entity.removeEffect(effect);
        }
    }

    // Structure visits

    private static void trackStructures(AbstractMercenaryEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = entity.blockPosition();
        Registry<Structure> registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);

        Set<ResourceLocation> nowInside = new HashSet<>();
        try {
            Map<Structure, LongSet> candidates = serverLevel.structureManager().getAllStructuresAt(pos);
            for (Structure structure : candidates.keySet()) {
                if (!serverLevel.structureManager().getStructureAt(pos, structure).isValid()) continue;
                ResourceLocation id = registry.getKey(structure);
                if (id != null) nowInside.add(id);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Structure lookup failed for {}: {}", entity.getEntityName(), e.getMessage());
            return;
        }

        Set<ResourceLocation> previously = occupiedStructures.computeIfAbsent(entity.getUUID(), k -> new HashSet<>());

        // Count only the entry edge, so standing in a fortress doesn't inflate the counter.
        List<ResourceLocation> entered = new ArrayList<>();
        for (ResourceLocation id : nowInside) {
            if (!previously.contains(id)) entered.add(id);
        }

        previously.clear();
        previously.addAll(nowInside);

        if (entered.isEmpty()) return;

        entity.mutateTitleProgress(d -> {
            for (ResourceLocation id : entered) d.addCounter(TitleKeys.structure(id), 1L);
        });
        TitleManager.evaluate(entity);
    }

    // Contract time

    private static void trackContractTime(AbstractMercenaryEntity entity) {
        ContractData contract = entity.getData(MRDataAttachments.CONTRACT_DATA);
        if (contract == null || !contract.hasActiveContract(entity.level())) return;

        entity.mutateTitleProgress(d -> d.addCounter(TitleKeys.CONTRACT_MINUTES, 1L));
        TitleManager.evaluate(entity);
    }

    // Cleanup

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary)) return;

        appliedModifiers.remove(mercenary.getUUID());
        occupiedStructures.remove(mercenary.getUUID());
    }

    private record DesiredModifier(Holder<Attribute> attribute, AttributeModifier modifier) {}
}