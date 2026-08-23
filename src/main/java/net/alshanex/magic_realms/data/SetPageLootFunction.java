package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.registry.MRDataComponentRegistry;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A custom loot function that sets the PageIdentifier on Lost Page items.
 * <p>
 * Can be used in two modes:
 * - Specific: Set a fixed story_id and chapter
 * - Random: Pick a random story and chapter from the registry
 * <p>
 * Example loot table usage (specific page):
 * <pre>
 * {
 *   "function": "lost_pages:set_page",
 *   "story_id": "lost_pages:the_frozen_king",
 *   "chapter": 0
 * }
 * </pre>
 * <p>
 * Example loot table usage (random page):
 * <pre>
 * {
 *   "function": "lost_pages:set_page"
 * }
 * </pre>
 */
public class SetPageLootFunction extends LootItemConditionalFunction {

    public static final MapCodec<SetPageLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).and(
                    instance.group(
                            ResourceLocation.CODEC.optionalFieldOf("story_id").forGetter(f -> f.storyId),
                            Codec.INT.optionalFieldOf("chapter").forGetter(f -> f.chapter)
                    )
            ).apply(instance, SetPageLootFunction::new)
    );

    public static LootItemFunctionType<SetPageLootFunction> TYPE;

    private final Optional<ResourceLocation> storyId;
    private final Optional<Integer> chapter;

    protected SetPageLootFunction(List<LootItemCondition> conditions,
                                  Optional<ResourceLocation> storyId,
                                  Optional<Integer> chapter) {
        super(conditions);
        this.storyId = storyId;
        this.chapter = chapter;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        ResourceLocation selectedStory;
        int selectedChapter;

        if (storyId.isPresent() && chapter.isPresent()) {
            // Specific page mode
            selectedStory = storyId.get();
            selectedChapter = chapter.get();
        } else if (storyId.isPresent()) {
            // Specific story, random chapter
            selectedStory = storyId.get();
            BossStory story = MRRegistries.BOSS_STORIES.get(selectedStory);
            if (story == null || story.totalPages() == 0) return stack;
            selectedChapter = context.getRandom().nextInt(story.totalPages());
        } else {
            // Fully random
            var entries = new ArrayList<>(MRRegistries.BOSS_STORIES.entrySet().stream().toList());
            if (entries.isEmpty()) return stack;

            var randomEntry = entries.get(context.getRandom().nextInt(entries.size()));
            selectedStory = randomEntry.getKey().location();
            BossStory story = randomEntry.getValue();
            if (story.totalPages() == 0) return stack;
            selectedChapter = context.getRandom().nextInt(story.totalPages());
        }

        // Validate
        BossStory story = MRRegistries.BOSS_STORIES.get(selectedStory);
        if (story != null && story.isValidChapter(selectedChapter)) {
            stack.set(MRDataComponentRegistry.PAGE_IDENTIFIER.get(),
                    new PageIdentifier(selectedStory, selectedChapter));
        }

        return stack;
    }

    @Override
    public LootItemFunctionType<SetPageLootFunction> getType() {
        return TYPE;
    }
}
