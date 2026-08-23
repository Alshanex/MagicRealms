package net.alshanex.magic_realms.story;

import net.alshanex.magic_realms.data.PageIdentifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BossStory {
    private final ResourceLocation bossEntity;
    private final List<Chapter> chapters;

    public record ContentEntry(ContentType type, @Nullable ResourceLocation image, int textIndex) {
        public boolean isText() { return type == ContentType.TEXT; }
        public boolean isImage() { return type == ContentType.IMAGE; }
    }

    public enum ContentType { TEXT, IMAGE }

    public record Chapter(@Nullable ResourceLocation coverImage, List<ContentEntry> contents,
                          boolean unlockedByDefault, boolean isSubstory, List<PageIdentifier> prerequisites) {
        public int contentCount() { return contents.size(); }

        public ContentEntry getContent(int index) { return contents.get(index); }

        public boolean isValidContent(int index) { return index >= 0 && index < contents.size(); }

        public int textPageCount() {
            return (int) contents.stream().filter(ContentEntry::isText).count();
        }

        public int pageCount() { return contents.size(); }
    }

    public BossStory(ResourceLocation bossEntity, List<Chapter> chapters) {
        this.bossEntity = bossEntity;
        this.chapters = List.copyOf(chapters);
    }

    public ResourceLocation bossEntity() { return bossEntity; }
    public List<Chapter> chapters() { return chapters; }
    public int totalChapters() { return chapters.size(); }

    public int totalMainChapters() {
        return (int) chapters.stream().filter(c -> !c.isSubstory()).count();
    }

    public boolean isValidChapter(int index) {
        return index >= 0 && index < chapters.size();
    }

    public Chapter getChapter(int index) {
        return chapters.get(index);
    }

    public int totalPages() { return chapters.size(); }

    public boolean isChapterUnlockedByDefault(int chapterIndex) {
        if (!isValidChapter(chapterIndex)) return false;
        return chapters.get(chapterIndex).unlockedByDefault();
    }

    public static String translationKeyPrefix(ResourceLocation storyRegistryId) {
        return "story." + storyRegistryId.getNamespace() + "." + storyRegistryId.getPath();
    }

    public static MutableComponent titleComponent(ResourceLocation storyRegistryId) {
        return Component.translatable(translationKeyPrefix(storyRegistryId) + ".title");
    }

    public static MutableComponent chapterTitleComponent(ResourceLocation storyRegistryId, int chapterIndex) {
        return Component.translatable(translationKeyPrefix(storyRegistryId) + ".chapter_" + chapterIndex + ".title");
    }

    public static MutableComponent pageTextComponent(ResourceLocation storyRegistryId, int chapterIndex, int pageTextIndex) {
        return Component.translatable(translationKeyPrefix(storyRegistryId) + ".chapter_" + chapterIndex + ".page_" + pageTextIndex + ".text");
    }

    public static Builder builder(ResourceLocation bossEntity) {
        return new Builder(bossEntity);
    }

    public static class Builder {
        private final ResourceLocation bossEntity;
        private final List<Chapter> chapters = new ArrayList<>();

        private Builder(ResourceLocation bossEntity) {
            this.bossEntity = bossEntity;
        }

        public ChapterBuilder chapter(@Nullable ResourceLocation coverImage) {
            return new ChapterBuilder(this, coverImage);
        }

        public ChapterBuilder chapter() {
            return new ChapterBuilder(this, null);
        }

        public ChapterBuilder substory(@Nullable ResourceLocation coverImage) {
            return new ChapterBuilder(this, coverImage).markAsSubstory();
        }

        public ChapterBuilder substory() {
            return new ChapterBuilder(this, null).markAsSubstory();
        }

        private Builder addChapter(Chapter chapter) {
            chapters.add(chapter);
            return this;
        }

        public BossStory build() {
            if (chapters.isEmpty()) throw new IllegalStateException("BossStory must have at least one chapter");
            return new BossStory(bossEntity, chapters);
        }
    }

    public static class ChapterBuilder {
        private final Builder parent;
        private final ResourceLocation coverImage;
        private final List<ContentEntry> contents = new ArrayList<>();
        private final List<PageIdentifier> prerequisites = new ArrayList<>();
        private boolean unlockedByDefault = false;
        private boolean isSubstory = false;
        private int textIndex = 0;

        private ChapterBuilder(Builder parent, @Nullable ResourceLocation coverImage) {
            this.parent = parent;
            this.coverImage = coverImage;
        }

        public ChapterBuilder requires(ResourceLocation storyId, int chapter) {
            this.prerequisites.add(new PageIdentifier(storyId, chapter));
            return this;
        }

        public ChapterBuilder unlockedByDefault() {
            this.unlockedByDefault = true;
            return this;
        }

        public ChapterBuilder markAsSubstory() {
            this.isSubstory = true;
            return this;
        }

        public ChapterBuilder page() {
            contents.add(new ContentEntry(ContentType.TEXT, null, textIndex));
            textIndex++;
            return this;
        }

        public ChapterBuilder image(ResourceLocation imageResource) {
            contents.add(new ContentEntry(ContentType.IMAGE, imageResource, -1));
            return this;
        }

        public Builder endChapter() {
            if (contents.isEmpty()) throw new IllegalStateException("Chapter must have at least one content entry");
            parent.addChapter(new Chapter(coverImage, List.copyOf(contents), unlockedByDefault, isSubstory, List.copyOf(prerequisites)));
            return parent;
        }
    }
}