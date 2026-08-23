package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record PlayerLoreProgress(Map<ResourceLocation, List<Integer>> collectedPages, Set<ResourceLocation> unlockedLoreNodes) {

    public static final PlayerLoreProgress EMPTY = new PlayerLoreProgress(Map.of(), Set.of());

    public static final Codec<PlayerLoreProgress> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT.listOf())
                            .fieldOf("collected_pages").forGetter(PlayerLoreProgress::collectedPages),
                    ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
                            .fieldOf("unlocked_lore_nodes").forGetter(PlayerLoreProgress::unlockedLoreNodes)
            ).apply(instance, PlayerLoreProgress::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLoreProgress> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public boolean hasPage(ResourceLocation storyId, int chapter) {
        List<Integer> pages = collectedPages.get(storyId);
        return pages != null && pages.contains(chapter);
    }

    public boolean hasLoreNode(ResourceLocation nodeId) {
        return unlockedLoreNodes.contains(nodeId);
    }

    public int getCollectedCount(ResourceLocation storyId) {
        List<Integer> pages = collectedPages.get(storyId);
        return pages != null ? pages.size() : 0;
    }

    public int getCollectedMainCount(ResourceLocation storyId) {
        BossStory story = MRRegistries.BOSS_STORIES.get(storyId);
        if (story == null) return 0;
        List<Integer> pages = collectedPages.get(storyId);
        if (pages == null) return 0;
        int count = 0;
        for (int p : pages) {
            if (story.isValidChapter(p) && !story.getChapter(p).isSubstory()) {
                count++;
            }
        }
        return count;
    }

    public boolean isStoryComplete(ResourceLocation storyId) {
        BossStory story = MRRegistries.BOSS_STORIES.get(storyId);
        if (story == null) return false;
        return getCollectedMainCount(storyId) >= story.totalMainChapters();
    }

    public PlayerLoreProgress withPage(ResourceLocation storyId, int chapter) {
        if (hasPage(storyId, chapter)) return this;
        Map<ResourceLocation, List<Integer>> newMap = new HashMap<>(collectedPages);
        List<Integer> pages = new ArrayList<>(newMap.getOrDefault(storyId, List.of()));
        pages.add(chapter);
        Collections.sort(pages);
        newMap.put(storyId, List.copyOf(pages));
        return new PlayerLoreProgress(Map.copyOf(newMap), this.unlockedLoreNodes);
    }

    public PlayerLoreProgress withLoreNode(ResourceLocation nodeId) {
        if (hasLoreNode(nodeId)) return this;
        Set<ResourceLocation> newNodes = new HashSet<>(unlockedLoreNodes);
        newNodes.add(nodeId);
        return new PlayerLoreProgress(this.collectedPages, Set.copyOf(newNodes));
    }

    public PlayerLoreProgress withDefaultUnlocks() {
        PlayerLoreProgress result = this;
        for (var entry : MRRegistries.BOSS_STORIES.entrySet()) {
            ResourceLocation storyId = entry.getKey().location();
            BossStory story = entry.getValue();
            for (int i = 0; i < story.totalChapters(); i++) {
                if (story.isChapterUnlockedByDefault(i) && !result.hasPage(storyId, i)) {
                    result = result.withPage(storyId, i);
                }
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return collectedPages.isEmpty() && unlockedLoreNodes.isEmpty();
    }
}