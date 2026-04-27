package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.ArchetypeInteraction;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.ArchetypeInteractionCatalog;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.ArchetypeInteractionCatalogHolder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.*;

/**
 * Tick handler that applies attribute modifiers from the {@link ArchetypeInteractionCatalog} to contracted
 * mercenaries who are near another mercenary contracted by the same player.
 *
 * <p>Behavior each cycle (the caller throttles by checking {@code tickCount % CHECK_INTERVAL_TICKS == 0}):
 * <ol>
 *   <li>If the mercenary has no active contract or no archetype, clear any modifiers we previously applied and stop.</li>
 *   <li>Find every other contracted mercenary within the largest interaction's radius that shares the same contractor.</li>
 *   <li>For each (self-archetype, neighbor-archetype) pair, look up matching interactions in the catalog and gather their modifiers.</li>
 *   <li>Diff the resulting "desired" modifier set against what we applied last tick: add new ones, remove stale ones, leave matched ones untouched.</li>
 * </ol>
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArchetypeInteractionTickHandler {

    /** How often (in ticks) to rescan and reapply. 20 ticks = 1 second is plenty - these effects are atmospheric. */
    public static final int CHECK_INTERVAL_TICKS = 20;

    /** Hard upper bound for the AABB sweep radius. Prevents a misconfigured datapack from spawning a giant search box. */
    private static final double MAX_SEARCH_RADIUS = 64.0;

    /**
     * Per-entity record of which interaction-derived modifier locations are currently active. Keyed by mercenary UUID.
     * Inner map: modifier id -> attribute holder (so we know which AttributeInstance to remove from when the modifier expires).
     */
    private static final Map<UUID, Map<ResourceLocation, Holder<Attribute>>> appliedModifiers = new HashMap<>();

    private ArchetypeInteractionTickHandler() {}

    /**
     * Main entry point. Should be called from {@code MercenaryTickHandler.tick} on a throttled cadence
     * (e.g. {@code if (entity.tickCount % CHECK_INTERVAL_TICKS == 0)}).
     */
    public static void tick(AbstractMercenaryEntity entity) {
        if (entity.level().isClientSide) return;

        ContractData contract = entity.getData(MRDataAttachments.CONTRACT_DATA);
        UUID contractorUUID = contract != null ? contract.getContractorUUID() : null;

        // No active contract -> clear any leftover modifiers and bail.
        if (contractorUUID == null || !contract.hasActiveContract(entity.level())) {
            clearAllAppliedModifiers(entity);
            return;
        }

        PersonalityData personality = entity.getData(MRDataAttachments.PERSONALITY);
        if (personality == null || !personality.isInitialized()) {
            clearAllAppliedModifiers(entity);
            return;
        }

        String selfArchetypeId = personality.getArchetypeId();
        if (selfArchetypeId == null) {
            clearAllAppliedModifiers(entity);
            return;
        }

        ArchetypeInteractionCatalog catalog = ArchetypeInteractionCatalogHolder.server();
        if (catalog.isEmpty()) {
            clearAllAppliedModifiers(entity);
            return;
        }

        // Find the maximum radius of any interaction so we sweep once with the widest box, then filter per-pair.
        double maxRadius = 0.0;
        for (ArchetypeInteraction i : catalog.all()) {
            maxRadius = Math.max(maxRadius, i.radius());
        }
        if (maxRadius <= 0.0) {
            clearAllAppliedModifiers(entity);
            return;
        }
        maxRadius = Math.min(maxRadius, MAX_SEARCH_RADIUS);

        // Sweep nearby teammates contracted by the same player.
        AABB box = entity.getBoundingBox().inflate(maxRadius);
        List<AbstractMercenaryEntity> teammates = entity.level().getEntitiesOfClass(
                AbstractMercenaryEntity.class,
                box,
                other -> other != entity
                        && other.isAlive()
                        && sharesContractor(other, contractorUUID, entity.level())
        );

        // Build desired modifier set by walking each teammate and collecting matching interactions.
        // Use a map keyed by modifier id so duplicate interactions across multiple teammates de-duplicate cleanly.
        Map<ResourceLocation, AppliedModifier> desired = new HashMap<>();
        for (AbstractMercenaryEntity teammate : teammates) {
            PersonalityData mateData = teammate.getData(MRDataAttachments.PERSONALITY);
            if (mateData == null || !mateData.isInitialized()) continue;
            String mateArchetypeId = mateData.getArchetypeId();
            if (mateArchetypeId == null) continue;

            List<ArchetypeInteraction> matched = catalog.forPair(selfArchetypeId, mateArchetypeId);
            if (matched.isEmpty()) continue;

            double distSqr = entity.distanceToSqr(teammate);

            for (ArchetypeInteraction interaction : matched) {
                double r = Math.min(interaction.radius(), MAX_SEARCH_RADIUS);
                if (distSqr > r * r) continue;

                List<ArchetypeInteraction.Entry> entries = interaction.modifiers();
                for (int idx = 0; idx < entries.size(); idx++) {
                    ArchetypeInteraction.Entry e = entries.get(idx);
                    ResourceLocation modId = modifierIdFor(interaction, idx);
                    desired.putIfAbsent(modId, new AppliedModifier(
                            e.attribute(),
                            new AttributeModifier(modId, e.modifier().amount(), e.modifier().operation())
                    ));
                }
            }
        }

        // Diff against currently applied set for this entity.
        Map<ResourceLocation, Holder<Attribute>> currentlyApplied = appliedModifiers.computeIfAbsent(entity.getUUID(), k -> new HashMap<>());

        // Remove anything no longer desired.
        if (!currentlyApplied.isEmpty()) {
            Iterator<Map.Entry<ResourceLocation, Holder<Attribute>>> it = currentlyApplied.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ResourceLocation, Holder<Attribute>> entry = it.next();
                if (!desired.containsKey(entry.getKey())) {
                    AttributeInstance instance = entity.getAttribute(entry.getValue());
                    if (instance != null) {
                        AttributeModifier existing = instance.getModifier(entry.getKey());
                        if (existing != null) instance.removeModifier(existing);
                    }
                    it.remove();
                }
            }
        }

        // Add anything newly desired - or re-add anything that the tracking map thinks is applied
        // but the AttributeInstance has actually lost (e.g. transient modifiers stripped on save/load).
        for (Map.Entry<ResourceLocation, AppliedModifier> entry : desired.entrySet()) {
            AppliedModifier am = entry.getValue();
            AttributeInstance instance = entity.getAttribute(am.attribute);
            if (instance == null) {
                MagicRealms.LOGGER.debug("Skipping archetype interaction modifier {} - attribute instance not present on {}",
                        entry.getKey(), entity.getEntityName());
                currentlyApplied.remove(entry.getKey()); // keep the map honest
                continue;
            }

            // The authoritative state is what's on the AttributeInstance, not what's in our tracking map.
            // After a save/load cycle, transient modifiers are gone but the tracking map (if it survived)
            // would say "still applied". Re-check and re-add when needed.
            boolean alreadyAttached = instance.getModifier(entry.getKey()) != null;
            if (alreadyAttached) {
                // Make sure the tracking map reflects reality.
                currentlyApplied.put(entry.getKey(), am.attribute);
                continue;
            }

            instance.addTransientModifier(am.modifier);
            currentlyApplied.put(entry.getKey(), am.attribute);
        }
    }

    /** Removes all interaction modifiers this handler previously applied to the entity and forgets about them. */
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

    /** Forgets tracking state for an entity (called on death/unload so the static map doesn't leak and reloads start clean). */
    public static void forget(UUID entityUUID) {
        appliedModifiers.remove(entityUUID);
    }


    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary)) return;
        forget(mercenary.getUUID());
    }

    /**
     * Build a stable ResourceLocation for the i-th modifier of the given interaction. The interaction's id is the
     * datapack location (e.g. {@code "magic_realms:stoic_jovial"}); we encode it into the path so it doesn't collide
     * with any other mod's modifiers and remains identifiable in /attribute output.
     */
    private static ResourceLocation modifierIdFor(ArchetypeInteraction interaction, int entryIndex) {
        String src = interaction.id();
        // Strip namespace prefix to keep the path readable; we fold both pieces into a single mod-namespaced location.
        String safe = src == null ? "unknown" : src.replace(':', '.').replace('/', '.');
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID,
                "archetype_interaction." + safe + "." + entryIndex);
    }

    private static boolean sharesContractor(AbstractMercenaryEntity other, UUID contractorUUID, net.minecraft.world.level.Level level) {
        ContractData otherContract = other.getData(MRDataAttachments.CONTRACT_DATA);
        if (otherContract == null) return false;
        if (!otherContract.hasActiveContract(level)) return false;
        UUID otherContractor = otherContract.getContractorUUID();
        return otherContractor != null && otherContractor.equals(contractorUUID);
    }

    private record AppliedModifier(Holder<Attribute> attribute, AttributeModifier modifier) {}
}
