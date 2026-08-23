package net.alshanex.magic_realms.story;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.PageIdentifier;
import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.alshanex.magic_realms.story.ChapterUnlocker;
import net.alshanex.magic_realms.story.LoreProgression;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

/**
 * Unlocks story chapters when a player stands inside a structure bound to that chapter via {@code .unlockedByVisiting(...)} in the boss story definition.
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

    /** Direct structure id -> chapters it can unlock. */
    private static Map<ResourceLocation, List<PageIdentifier>> byStructure;
    /** Structure tag -> chapter it can unlock. */
    private static List<TagBinding> byTag;
    /** Every chapter reachable through a structure visit, for the cheap early-out. */
    private static List<PageIdentifier> allVisitChapters;

    private record TagBinding(TagKey<Structure> tag, PageIdentifier page) {}

    // Index

    /**
     * Builds the structure -> chapter lookup once. The boss story registry is static after mod registration, so this never needs invalidating.
     * Tag membership is resolved per-check against the level's registry, since tags come from datapacks.
     */
    private static void buildIndexIfNeeded() {
        if (byStructure != null) return;

        Map<ResourceLocation, List<PageIdentifier>> direct = new HashMap<>();
        List<TagBinding> tags = new ArrayList<>();
        List<PageIdentifier> all = new ArrayList<>();

        for (var entry : MRRegistries.BOSS_STORIES.entrySet()) {
            ResourceLocation storyId = entry.getKey().location();
            BossStory story = entry.getValue();

            for (int i = 0; i < story.totalChapters(); i++) {
                BossStory.Chapter chapter = story.getChapter(i);
                if (!chapter.hasStructureUnlocks()) continue;

                PageIdentifier page = new PageIdentifier(storyId, i);
                all.add(page);

                for (ResourceLocation structureId : chapter.unlockStructures()) {
                    direct.computeIfAbsent(structureId, k -> new ArrayList<>()).add(page);
                }
                for (TagKey<Structure> tag : chapter.unlockStructureTags()) {
                    tags.add(new TagBinding(tag, page));
                }
            }
        }

        byStructure = direct;
        byTag = tags;
        allVisitChapters = all;

        MagicRealms.LOGGER.debug("Indexed {} structure-unlockable chapters ({} direct bindings, {} tag bindings)",
                all.size(), direct.size(), tags.size());
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

        // Lower chapters first, so a structure bound to several chapters can cascade in one pass (unlocking chapter 2 satisfies chapter 3's prerequisite immediately).
        List<PageIdentifier> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator
                .comparing((PageIdentifier p) -> p.storyId().toString())
                .thenComparingInt(PageIdentifier::chapter));

        boolean unlockedAny = false;
        for (PageIdentifier page : ordered) {
            // Sound is passed as null here: the effect below covers audio for the whole visit, so a location bound to two chapters does not fire two overlapping chimes.
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
     * Returns every chapter bound to a structure the player is currently standing in.
     */
    private static Set<PageIdentifier> findCandidatesAt(ServerLevel level, BlockPos pos) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        StructureManager manager = level.structureManager();

        Set<PageIdentifier> found = new LinkedHashSet<>();

        try {
            Map<Structure, LongSet> nearby = manager.getAllStructuresAt(pos);
            if (nearby.isEmpty()) return found;

            for (Structure structure : nearby.keySet()) {
                if (!isActuallyInside(manager, pos, structure)) continue;

                ResourceLocation id = registry.getKey(structure);
                if (id == null) continue;

                List<PageIdentifier> direct = byStructure.get(id);
                if (direct != null) found.addAll(direct);

                if (!byTag.isEmpty()) {
                    Holder<Structure> holder = registry.wrapAsHolder(structure);
                    for (TagBinding binding : byTag) {
                        if (holder.is(binding.tag())) found.add(binding.page());
                    }
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Structure lookup failed at {}: {}", pos, e.getMessage());
            return Set.of();
        }

        return found;
    }

    private static boolean isActuallyInside(StructureManager manager, BlockPos pos, Structure structure) {
        return REQUIRE_INSIDE_PIECE
                ? manager.getStructureWithPieceAt(pos, structure).isValid()
                : manager.getStructureAt(pos, structure).isValid();
    }

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
