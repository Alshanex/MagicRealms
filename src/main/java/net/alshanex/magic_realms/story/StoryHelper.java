package net.alshanex.magic_realms.story;

import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Utility class for story-related checks.
 */
public class StoryHelper {

    public static boolean canAwakeBoss(Player player, ResourceLocation storyId) {
        return getPlayerLoreProgress(player).isStoryComplete(storyId);
    }

    public static PlayerLoreProgress getPlayerLoreProgress(Player player) {
        return player.getData(MRDataAttachments.PLAYER_LORE);
    }

    public static Optional<BossStory> getStoryForBoss(ResourceLocation bossEntityId) {
        for (var entry : MRRegistries.BOSS_STORIES.entrySet()) {
            if (entry.getValue().bossEntity().equals(bossEntityId)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public static Optional<ResourceLocation> getStoryIdForBoss(ResourceLocation bossEntityId) {
        for (var entry : MRRegistries.BOSS_STORIES.entrySet()) {
            if (entry.getValue().bossEntity().equals(bossEntityId)) {
                return Optional.of(entry.getKey().location());
            }
        }
        return Optional.empty();
    }
}
