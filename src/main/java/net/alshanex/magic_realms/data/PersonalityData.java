package net.alshanex.magic_realms.data;

import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Per-mercenary personality state. Combines rolled-once traits (archetype, hobby, hometown, quirks) with dynamic state.
 */
public class PersonalityData implements INBTSerializable<CompoundTag> {

    private boolean initialized = false;
    private String archetypeId;
    private String hobbyId;
    private String hometown;
    private Set<Quirk> quirks = EnumSet.noneOf(Quirk.class);

    public PersonalityData() {}

    public void initialize(String archetypeId,
                           String hobbyId,
                           String hometown,
                           Set<Quirk> quirks) {
        if (initialized) return;
        this.archetypeId = archetypeId;
        this.hobbyId = hobbyId;
        this.hometown = hometown;
        this.quirks = quirks != null ? EnumSet.copyOf(quirks) : EnumSet.noneOf(Quirk.class);
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // Trait getters

    /** The archetype's short id (e.g. {@code "stoic"}). May be null if uninitialized or if the original datapack entry was removed. */
    @Nullable
    public String getArchetypeId() {
        return archetypeId;
    }

    /**
     * Look up the current {@link Archetype} definition for this mercenary. Returns null if the archetype id is null
     * or no longer exists in the catalog.
     */
    @Nullable
    public Archetype getArchetype(boolean isClientSide) {
        if (archetypeId == null) return null;
        return ArchetypeCatalogHolder.get(isClientSide).byId(archetypeId);
    }

    /**
     * Look up the current Hobby definition for this mercenary. Returns null if the hobby id no longer exists in the
     * catalog (datapack removed it).
     */
    @Nullable
    public Hobby getHobby(boolean isClientSide) {
        if (hobbyId == null) return null;
        HobbyCatalog catalog = HobbyCatalogHolder.get(isClientSide);
        return catalog.byId(hobbyId);
    }

    @Nullable
    public String getHobbyId() {
        return hobbyId;
    }

    @Nullable
    public String getHometown() {
        return hometown;
    }

    public Set<Quirk> getQuirks() {
        return EnumSet.copyOf(quirks);
    }

    public boolean hasQuirk(Quirk q) {
        return quirks.contains(q);
    }

    // NBT Serialization

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", initialized);

        if (archetypeId != null) tag.putString("archetype", archetypeId);
        if (hobbyId != null) tag.putString("hobby", hobbyId);
        if (hometown != null) tag.putString("hometown", hometown);

        ListTag quirksTag = new ListTag();
        for (Quirk q : quirks) quirksTag.add(StringTag.valueOf(q.getId()));
        tag.put("quirks", quirksTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.initialized = tag.getBoolean("initialized");
        // Archetype is a free-form string now; we keep whatever's saved even if the catalog no longer has it (graceful degradation).
        this.archetypeId = tag.contains("archetype") ? tag.getString("archetype") : null;
        if (archetypeId != null && archetypeId.isEmpty()) archetypeId = null;
        this.hobbyId = tag.contains("hobby") ? tag.getString("hobby") : null;
        this.hometown = tag.contains("hometown") ? tag.getString("hometown") : null;

        this.quirks = EnumSet.noneOf(Quirk.class);
        if (tag.contains("quirks", Tag.TAG_LIST)) {
            ListTag quirksTag = tag.getList("quirks", Tag.TAG_STRING);
            for (int i = 0; i < quirksTag.size(); i++) {
                Quirk q = Quirk.fromId(quirksTag.getString(i));
                if (q != null) quirks.add(q);
            }
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // Attachment serializer

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PersonalityData> {
        @Override
        public PersonalityData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PersonalityData data = new PersonalityData();
            data.deserializeNBT(provider, tag);
            return data;
        }

        @Override
        public @Nullable CompoundTag write(PersonalityData attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }

    // Network stream codec

    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalityData> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PersonalityData decode(RegistryFriendlyByteBuf buf) {
                    CompoundTag tag = ByteBufCodecs.COMPOUND_TAG.decode(buf);
                    RegistryAccess access = buf.registryAccess();
                    PersonalityData data = new PersonalityData();
                    data.deserializeNBT(access, tag);
                    return data;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, PersonalityData data) {
                    RegistryAccess access = buf.registryAccess();
                    CompoundTag tag = data.serializeNBT(access);
                    ByteBufCodecs.COMPOUND_TAG.encode(buf, tag);
                }
            };
}
