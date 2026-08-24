package net.alshanex.magic_realms.story;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.PageIdentifier;
import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Unlocks story chapters when a player stands inside a structure bound to that chapter via {@code .unlockedByVisiting(...)},
 * optionally narrowed to specific biomes with {@code .inBiome(...)} or to specific jigsaw pieces with {@code .inPiece(...)}.
 * <p>
 * The unlock itself is delegated to {@link ChapterUnlocker}, so prerequisites, the book seal and duplicate protection behave exactly as they do when inserting a Lost Page.
 * If the player does not meet the requirements, nothing happens and no feedback is given - the place simply stays silent until they are ready for it.
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class StructureLoreUnlockHandler {

    private StructureLoreUnlockHandler() {}

    /** How often a player's position is tested against structures. 20 ticks = once per second. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final boolean REQUIRE_INSIDE_PIECE = true;

    /** One chapter paired with one of the visit conditions that can grant it. */
    private record Binding(PageIdentifier page, BossStory.VisitCondition condition) {}

    /** Structure id -> bindings, for conditions that name a structure directly. */
    private static Map<ResourceLocation, List<Binding>> byStructure;
    /** Bindings whose condition names a structure tag instead of a single structure. */
    private static List<Binding> byStructureTag;
    /** Every chapter reachable through a structure visit, for the cheap early-out. */
    private static List<PageIdentifier> allVisitChapters;

    // Index

    /**
     * Builds the structure -> chapter lookup once. The boss story registry is static after mod registration,
     * so this never needs invalidating. Tag membership is resolved per-check against the level's registries, since tags come from datapacks.
     */
    private static void buildIndexIfNeeded() {
        if (byStructure != null) return;

        Map<ResourceLocation, List<Binding>> direct = new HashMap<>();
        List<Binding> tagged = new ArrayList<>();
        List<PageIdentifier> all = new ArrayList<>();

        for (var entry : MRRegistries.BOSS_STORIES.entrySet()) {
            ResourceLocation storyId = entry.getKey().location();
            BossStory story = entry.getValue();

            for (int i = 0; i < story.totalChapters(); i++) {
                BossStory.Chapter chapter = story.getChapter(i);
                if (!chapter.hasStructureUnlocks()) continue;

                PageIdentifier page = new PageIdentifier(storyId, i);
                all.add(page);

                for (BossStory.VisitCondition condition : chapter.visitConditions()) {
                    Binding binding = new Binding(page, condition);

                    if (condition.structureId() != null) {
                        direct.computeIfAbsent(condition.structureId(), k -> new ArrayList<>()).add(binding);
                    } else if (condition.structureTag() != null) {
                        tagged.add(binding);
                    }
                }
            }
        }

        byStructure = direct;
        byStructureTag = tagged;
        allVisitChapters = all;

        MagicRealms.LOGGER.debug("Indexed {} structure-unlockable chapters ({} direct bindings, {} tag bindings)",
                all.size(), direct.size(), tagged.size());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator() || !player.isAlive()) return;
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        buildIndexIfNeeded();
        if (allVisitChapters.isEmpty()) return;

        PlayerLoreProgress progress = player.getData(MRDataAttachments.PLAYER_LORE);

        // The book is still sealed - no chapter can be granted yet, so skip the world lookup entirely.
        if (!LoreProgression.canOpenBook(progress)) return;

        // Everything reachable this way is already collected. Nothing left to find; skip the lookup.
        if (allVisitChapters.stream().allMatch(p -> progress.hasPage(p.storyId(), p.chapter()))) return;

        Set<PageIdentifier> candidates = findCandidatesAt(level, player.blockPosition());
        if (candidates.isEmpty()) return;

        // Lower chapters first, so a structure bound to several chapters can cascade in one pass
        // (unlocking chapter 2 satisfies chapter 3's prerequisite immediately).
        List<PageIdentifier> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator
                .comparing((PageIdentifier p) -> p.storyId().toString())
                .thenComparingInt(PageIdentifier::chapter));

        boolean unlockedAny = false;
        for (PageIdentifier page : ordered) {
            // Sound is passed as null here: the effect below covers audio for the whole visit, so a location
            // bound to two chapters does not fire two overlapping chimes.
            ChapterUnlocker.Result result = ChapterUnlocker.tryUnlock(player,
                    page.storyId(), page.chapter(), buildUnlockMessage(page), null);

            if (result.isUnlocked()) unlockedAny = true;
            // Any other result is intentionally silent: the player either already has the chapter,
            // or is not far enough into the story for this place to mean anything yet.
        }

        if (unlockedAny) {
            playUnlockEffect(level, player);
        }
    }

    // Structure lookup

    /**
     * Returns every chapter whose visit condition is satisfied at this position: the right structure, and - where the condition asks for it - the right biome.
     */
    private static Set<PageIdentifier> findCandidatesAt(ServerLevel level, BlockPos pos) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        StructureManager manager = level.structureManager();

        Set<PageIdentifier> found = new LinkedHashSet<>();

        try {
            Map<Structure, LongSet> nearby = manager.getAllStructuresAt(pos);
            if (nearby.isEmpty()) return found;

            for (Structure structure : nearby.keySet()) {
                StructureStart start = structureStartAt(manager, pos, structure);
                if (start == null || !start.isValid()) continue;

                ResourceLocation id = registry.getKey(structure);
                if (id == null) continue;

                List<Binding> matching = new ArrayList<>();

                List<Binding> direct = byStructure.get(id);
                if (direct != null) matching.addAll(direct);

                if (!byStructureTag.isEmpty()) {
                    Holder<Structure> holder = registry.wrapAsHolder(structure);
                    for (Binding binding : byStructureTag) {
                        if (holder.is(binding.condition().structureTag())) matching.add(binding);
                    }
                }

                if (matching.isEmpty()) continue;

                // All resolved lazily and at most once per structure: most conditions constrain none of
                // these, and none of the lookups is worth doing until one does.
                Holder<Biome> biome = null;
                Set<ResourceLocation> occupiedPieces = null;
                Set<ResourceLocation> allPieces = null;

                for (Binding binding : matching) {
                    if (binding.condition().hasBiomeConstraint()) {
                        if (biome == null) biome = resolveBiome(level, start, pos);
                        if (!binding.condition().matchesBiome(biome)) continue;
                    }

                    if (binding.condition().hasPieceConstraint()) {
                        if (occupiedPieces == null) occupiedPieces = collectPieces(start, pos);
                        if (occupiedPieces.stream().noneMatch(binding.condition()::matchesPiece)) continue;
                    }

                    if (binding.condition().hasRequiredPieceConstraint()) {
                        if (allPieces == null) allPieces = collectPieces(start, null);
                        if (allPieces.stream().noneMatch(binding.condition()::matchesRequiredPiece)) continue;
                    }

                    found.add(binding.page());
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Structure lookup failed at {}: {}", pos, e.getMessage());
            return Set.of();
        }

        return found;
    }

    @Nullable
    private static StructureStart structureStartAt(StructureManager manager, BlockPos pos, Structure structure) {
        StructureStart start = REQUIRE_INSIDE_PIECE
                ? manager.getStructureWithPieceAt(pos, structure)
                : manager.getStructureAt(pos, structure);
        return start == StructureStart.INVALID_START ? null : start;
    }

    /**
     * Returns the template pool ids of this structure's jigsaw pieces.
     */
    private static Set<ResourceLocation> collectPieces(StructureStart start, @Nullable BlockPos pos) {
        Set<ResourceLocation> ids = new HashSet<>();

        for (StructurePiece piece : start.getPieces()) {
            if (pos != null && !piece.getBoundingBox().isInside(pos)) continue;
            if (!(piece instanceof PoolElementStructurePiece poolPiece)) continue;
            if (!(poolPiece.getElement() instanceof SinglePoolElement single)) continue;

            single.template.left().ifPresent(ids::add);
        }

        return ids;
    }

    /**
     * Samples the biome at the horizontal middle of the structure rather than at the player's feet.
     */
    private static Holder<Biome> resolveBiome(ServerLevel level, StructureStart start, BlockPos playerPos) {
        Vec3i centre = start.getBoundingBox().getCenter();
        return level.getBiome(new BlockPos(centre.getX(), playerPos.getY(), centre.getZ()));
    }

    // Feedback

    /**
     * Plays the "the place remembers you" effect: glyphs converging on the player, a soft column of light at their feet, and a two-layer chime.
     */
    private static void playUnlockEffect(ServerLevel level, ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        final int glyphs = 40;
        for (int i = 0; i < glyphs; i++) {
            double angle = (Math.PI * 2.0 / glyphs) * i;
            double spread = 1.5 + level.random.nextDouble();
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 0,
                    Math.cos(angle) * spread,
                    (level.random.nextDouble() - 0.25) * spread,
                    Math.sin(angle) * spread,
                    1.0);
        }

        // A quiet rising column, so the effect still reads at a glance if the glyphs are missed.
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.1, z, 14, 0.2, 0.4, 0.2, 0.02);
        level.sendParticles(ParticleTypes.SOUL, x, y + 0.1, z, 6, 0.3, 0.05, 0.3, 0.01);

        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.2F);
        level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.3F, 1.7F);
    }

    private static Component buildUnlockMessage(PageIdentifier page) {
        return Component.translatable("message.magic_realms.structure_memory_unlocked",
                        BossStory.chapterTitleComponent(page.storyId(), page.chapter())
                                .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
    }
}