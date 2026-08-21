package net.alshanex.magic_realms.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TitleProgressData implements INBTSerializable<CompoundTag> {

    /** Guard against a runaway counter map (e.g. a mod spawning thousands of unique entity types). */
    private static final int MAX_COUNTERS = 512;

    private final Map<String, Long> counters = new HashMap<>();
    private final Set<ResourceLocation> earnedTitles = new LinkedHashSet<>();

    @Nullable
    private ResourceLocation displayedTitle;

    public TitleProgressData() {}

    // Counters

    public long getCounter(String key) {
        if (key == null || key.isEmpty()) return 0L;
        return counters.getOrDefault(key, 0L);
    }

    /** Increments and returns the new value. Negative deltas are allowed but the counter floors at zero. */
    public long addCounter(String key, long delta) {
        if (key == null || key.isEmpty() || delta == 0L) return getCounter(key);

        long current = counters.getOrDefault(key, 0L);
        long updated = Math.max(0L, current + delta);

        if (updated == 0L) {
            counters.remove(key);
            return 0L;
        }
        if (!counters.containsKey(key) && counters.size() >= MAX_COUNTERS) {
            return current; // refuse to grow further rather than bloating saved NBT
        }

        counters.put(key, updated);
        return updated;
    }

    public void setCounter(String key, long value) {
        if (key == null || key.isEmpty()) return;
        if (value <= 0L) {
            counters.remove(key);
        } else if (counters.containsKey(key) || counters.size() < MAX_COUNTERS) {
            counters.put(key, value);
        }
    }

    /** Unmodifiable live view, for GUIs and debug output. */
    public Map<String, Long> getCounters() {
        return Collections.unmodifiableMap(counters);
    }

    // Titles

    public boolean hasTitle(ResourceLocation id) {
        return id != null && earnedTitles.contains(id);
    }

    /** Returns true if the title was newly added. */
    public boolean grantTitle(ResourceLocation id) {
        return id != null && earnedTitles.add(id);
    }

    /** Returns true if the title was actually held. Also clears it from display if it was the shown one. */
    public boolean revokeTitle(ResourceLocation id) {
        if (id == null) return false;
        boolean removed = earnedTitles.remove(id);
        if (removed && id.equals(displayedTitle)) displayedTitle = null;
        return removed;
    }

    /** Earned ids in the order they were awarded. */
    public Set<ResourceLocation> getEarnedTitles() {
        return Collections.unmodifiableSet(earnedTitles);
    }

    public int getTitleCount() {
        return earnedTitles.size();
    }

    @Nullable
    public ResourceLocation getDisplayedTitle() {
        return displayedTitle;
    }

    /**
     * Sets the shown title. Only titles the mercenary has actually earned can be displayed; passing null (or an unearned id) clears the display. Returns true if the stored value changed.
     */
    public boolean setDisplayedTitle(@Nullable ResourceLocation id) {
        ResourceLocation next = (id != null && earnedTitles.contains(id)) ? id : null;
        if (Objects.equals(next, displayedTitle)) return false;
        displayedTitle = next;
        return true;
    }

    // NBT

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        CompoundTag countersTag = new CompoundTag();
        for (Map.Entry<String, Long> e : counters.entrySet()) {
            countersTag.putLong(e.getKey(), e.getValue());
        }
        tag.put("counters", countersTag);

        ListTag titlesTag = new ListTag();
        for (ResourceLocation id : earnedTitles) {
            titlesTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("earned_titles", titlesTag);

        if (displayedTitle != null) {
            tag.putString("displayed_title", displayedTitle.toString());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        counters.clear();
        earnedTitles.clear();
        displayedTitle = null;

        if (tag.contains("counters", Tag.TAG_COMPOUND)) {
            CompoundTag countersTag = tag.getCompound("counters");
            for (String key : countersTag.getAllKeys()) {
                long value = countersTag.getLong(key);
                if (value > 0L && counters.size() < MAX_COUNTERS) counters.put(key, value);
            }
        }

        if (tag.contains("earned_titles", Tag.TAG_LIST)) {
            ListTag titlesTag = tag.getList("earned_titles", Tag.TAG_STRING);
            for (int i = 0; i < titlesTag.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(titlesTag.getString(i));
                if (id != null) earnedTitles.add(id);
            }
        }

        if (tag.contains("displayed_title")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("displayed_title"));
            if (id != null && earnedTitles.contains(id)) displayedTitle = id;
        }
    }

    @Override
    public String toString() {
        return "TitleProgressData{titles=" + earnedTitles.size()
                + ", displayed=" + displayedTitle
                + ", counters=" + counters.size() + "}";
    }

    // Attachment serializer

    public static class Serializer implements IAttachmentSerializer<CompoundTag, TitleProgressData> {
        @Override
        public TitleProgressData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            TitleProgressData data = new TitleProgressData();
            data.deserializeNBT(provider, tag);
            return data;
        }

        @Nullable
        @Override
        public CompoundTag write(TitleProgressData attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }

    // Network stream codec (so the client can render titles without a bespoke packet)

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleProgressData> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public TitleProgressData decode(RegistryFriendlyByteBuf buf) {
                    CompoundTag tag = ByteBufCodecs.COMPOUND_TAG.decode(buf);
                    RegistryAccess access = buf.registryAccess();
                    TitleProgressData data = new TitleProgressData();
                    data.deserializeNBT(access, tag);
                    return data;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, TitleProgressData data) {
                    RegistryAccess access = buf.registryAccess();
                    ByteBufCodecs.COMPOUND_TAG.encode(buf, data.serializeNBT(access));
                }
            };
}
