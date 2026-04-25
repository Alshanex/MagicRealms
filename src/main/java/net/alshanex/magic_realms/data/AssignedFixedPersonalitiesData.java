package net.alshanex.magic_realms.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-Level attachment that tracks which "unique" fixed personality ids have already been assigned to a mercenary in this world. Attached to the
 * overworld and queried whenever a personality roll wants to pick from the random pool.
 *
 * Once an id lands here, it stays - even after the mercenary dies. That preserves the narrative convention that named characters are
 * one-of-a-kind in a given world save. If a user wants a character to be respawnable, they set `unique: false` on that character's JSON.
 */
public class AssignedFixedPersonalitiesData implements INBTSerializable<CompoundTag> {

    private final Set<String> claimedIds = new HashSet<>();

    public AssignedFixedPersonalitiesData() {}

    /**
     * @return true if this id was added (wasn't already claimed), false if it was already in the set (caller should treat as already-claimed).
     */
    public boolean claim(String id) {
        if (id == null || id.isEmpty()) return false;
        return claimedIds.add(id);
    }

    public boolean isClaimed(String id) {
        return id != null && claimedIds.contains(id);
    }

    public Set<String> snapshot() {
        return Collections.unmodifiableSet(new HashSet<>(claimedIds));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String id : claimedIds) list.add(StringTag.valueOf(id));
        tag.put("claimed", list);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.claimedIds.clear();
        if (tag.contains("claimed", Tag.TAG_LIST)) {
            ListTag list = tag.getList("claimed", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String s = list.getString(i);
                if (!s.isEmpty()) claimedIds.add(s);
            }
        }
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, AssignedFixedPersonalitiesData> {
        @Override
        public AssignedFixedPersonalitiesData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            AssignedFixedPersonalitiesData data = new AssignedFixedPersonalitiesData();
            data.deserializeNBT(provider, tag);
            return data;
        }

        @Nullable
        @Override
        public CompoundTag write(AssignedFixedPersonalitiesData attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }
}
