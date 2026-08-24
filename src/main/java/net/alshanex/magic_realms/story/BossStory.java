package net.alshanex.magic_realms.story;

import net.alshanex.magic_realms.data.PageIdentifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

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
                          boolean unlockedByDefault, boolean isSubstory, List<PageIdentifier> prerequisites,
                          List<VisitCondition> visitConditions) {

        public boolean hasStructureUnlocks() {
            return !visitConditions.isEmpty();
        }

        public int contentCount() { return contents.size(); }

        public ContentEntry getContent(int index) { return contents.get(index); }

        public boolean isValidContent(int index) { return index >= 0 && index < contents.size(); }

        public int textPageCount() {
            return (int) contents.stream().filter(ContentEntry::isText).count();
        }

        public int pageCount() { return contents.size(); }
    }

    /**
     * One way to reach a chapter by travelling: a structure, optionally narrowed to certain biomes.
     * <p>
     * Exactly one of {@code structureId} / {@code structureTag} is set. Biome lists are OR-ed, and an empty pair means the structure counts wherever it generates.
     */
    public record VisitCondition(@Nullable ResourceLocation structureId,
                                 @Nullable TagKey<Structure> structureTag,
                                 List<ResourceLocation> biomes,
                                 List<TagKey<Biome>> biomeTags,
                                 List<ResourceLocation> pieces,
                                 List<ResourceLocation> requiredPieces) {

        public boolean hasBiomeConstraint() {
            return !biomes.isEmpty() || !biomeTags.isEmpty();
        }

        /** The player must be standing inside one of these pieces. */
        public boolean hasPieceConstraint() {
            return !pieces.isEmpty();
        }

        /** The structure must contain one of these pieces, wherever the player happens to be in it. */
        public boolean hasRequiredPieceConstraint() {
            return !requiredPieces.isEmpty();
        }

        public boolean matchesBiome(Holder<Biome> holder) {
            if (!hasBiomeConstraint()) return true;

            ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id != null && biomes.contains(id)) return true;

            for (TagKey<Biome> tag : biomeTags) {
                if (holder.is(tag)) return true;
            }
            return false;
        }

        public boolean matchesPiece(ResourceLocation templateId) {
            return pieces.contains(templateId);
        }

        public boolean matchesRequiredPiece(ResourceLocation templateId) {
            return requiredPieces.contains(templateId);
        }
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
        private final List<VisitCondition> visitConditions = new ArrayList<>();

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

        /** Visiting this structure unlocks the chapter, if its prerequisites are already met. */
        public ChapterBuilder unlockedByVisiting(ResourceLocation structureId) {
            this.visitConditions.add(new VisitCondition(structureId, null,
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            return this;
        }

        /** Visiting any structure in this tag unlocks the chapter. */
        public ChapterBuilder unlockedByVisiting(TagKey<Structure> structureTag) {
            this.visitConditions.add(new VisitCondition(null, structureTag,
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            return this;
        }

        /**
         * Narrows the structure declared immediately above to this biome. Call more than once to accept
         * any of several biomes.
         */
        public ChapterBuilder inBiome(ResourceLocation biomeId) {
            VisitCondition c = lastVisitCondition();
            List<ResourceLocation> biomes = new ArrayList<>(c.biomes());
            biomes.add(biomeId);
            replaceLastVisitCondition(new VisitCondition(c.structureId(), c.structureTag(),
                    biomes, c.biomeTags(), c.pieces(), c.requiredPieces()));
            return this;
        }

        /** Narrows the structure declared immediately above to any biome in this tag. */
        public ChapterBuilder inBiome(TagKey<Biome> biomeTag) {
            VisitCondition c = lastVisitCondition();
            List<TagKey<Biome>> tags = new ArrayList<>(c.biomeTags());
            tags.add(biomeTag);
            replaceLastVisitCondition(new VisitCondition(c.structureId(), c.structureTag(),
                    c.biomes(), tags, c.pieces(), c.requiredPieces()));
            return this;
        }

        /**
         * Narrows the structure declared immediately above to a specific jigsaw piece. The id is the template path as it appears in the pool,
         * e.g. "magic_realms:taverns/plains/bar/default_1". Call more than once to accept any of several pieces.
         */
        public ChapterBuilder inPiece(ResourceLocation templateId) {
            VisitCondition c = lastVisitCondition();
            List<ResourceLocation> pieces = new ArrayList<>(c.pieces());
            pieces.add(templateId);
            replaceLastVisitCondition(new VisitCondition(c.structureId(), c.structureTag(),
                    c.biomes(), c.biomeTags(), pieces, c.requiredPieces()));
            return this;
        }

        /** Convenience overload, since pool piece ids are usually written as plain strings. */
        public ChapterBuilder inPiece(String templateId) {
            return inPiece(ResourceLocation.parse(templateId));
        }

        /**
         * Requires the structure to contain this jigsaw piece somewhere, without the player having to reach it.
         * Call more than once to accept any of several pieces.
         */
        public ChapterBuilder containingPiece(ResourceLocation templateId) {
            VisitCondition c = lastVisitCondition();
            List<ResourceLocation> required = new ArrayList<>(c.requiredPieces());
            required.add(templateId);
            replaceLastVisitCondition(new VisitCondition(c.structureId(), c.structureTag(),
                    c.biomes(), c.biomeTags(), c.pieces(), required));
            return this;
        }

        public ChapterBuilder containingPiece(String templateId) {
            return containingPiece(ResourceLocation.parse(templateId));
        }

        private VisitCondition lastVisitCondition() {
            if (visitConditions.isEmpty()) {
                throw new IllegalStateException("inBiome() must follow an unlockedByVisiting() call");
            }
            return visitConditions.get(visitConditions.size() - 1);
        }

        private void replaceLastVisitCondition(VisitCondition replacement) {
            visitConditions.set(visitConditions.size() - 1, replacement);
        }

        public Builder endChapter() {
            if (contents.isEmpty()) throw new IllegalStateException("Chapter must have at least one content entry");
            parent.addChapter(new Chapter(coverImage, List.copyOf(contents), unlockedByDefault, isSubstory,
                    List.copyOf(prerequisites), List.copyOf(visitConditions)));
            return parent;
        }
    }
}