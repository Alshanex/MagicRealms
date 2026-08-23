package net.alshanex.magic_realms.item.lore;

import net.alshanex.magic_realms.data.PageIdentifier;
import net.alshanex.magic_realms.registry.MRDataComponentRegistry;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Lost Page item that can be inserted into the Lost Pages Book.
 * Each page has a PageIdentifier component that tells which story/chapter it belongs to.
 */
public class LostPageItem extends Item {

    public LostPageItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        PageIdentifier identifier = stack.get(MRDataComponentRegistry.PAGE_IDENTIFIER.get());
        if (identifier != null) {
            BossStory story = MRRegistries.BOSS_STORIES.get(identifier.storyId());
            if (story != null) {
                tooltipComponents.add(Component.translatable("item.magic_realms.lost_page.story",
                                BossStory.titleComponent(identifier.storyId()).withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.GRAY));

                if (story.isValidChapter(identifier.chapter())) {
                    tooltipComponents.add(Component.translatable("item.magic_realms.lost_page.chapter",
                                    BossStory.chapterTitleComponent(identifier.storyId(), identifier.chapter())
                                            .withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltipComponents.add(Component.translatable("item.magic_realms.lost_page.unidentified")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        PageIdentifier identifier = stack.get(MRDataComponentRegistry.PAGE_IDENTIFIER.get());
        if (identifier != null) {
            BossStory story = MRRegistries.BOSS_STORIES.get(identifier.storyId());
            if (story != null && story.isValidChapter(identifier.chapter())) {
                return Component.translatable("item.magic_realms.lost_page.named",
                        BossStory.chapterTitleComponent(identifier.storyId(), identifier.chapter()));
            }
        }
        return super.getName(stack);
    }

    /**
     * Helper to create a Lost Page ItemStack for a specific story and chapter.
     */
    public static ItemStack createPage(Item pageItem, ResourceLocation storyId, int chapter) {
        ItemStack stack = new ItemStack(pageItem);
        stack.set(MRDataComponentRegistry.PAGE_IDENTIFIER.get(), new PageIdentifier(storyId, chapter));
        return stack;
    }
}
