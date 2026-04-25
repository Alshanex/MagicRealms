package net.alshanex.magic_realms.data;

import net.alshanex.magic_realms.util.humans.mercenaries.personality.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-mercenary personality state. Combines rolled-once traits (archetype, food preferences, hobby, hometown, birthday, quirks) with dynamic state
 * (mood, per-player affinity map, per-player memory events).
 *
 * Affinity is an int per player, roughly bounded [-100, 100]:
 *   < 0    : stranger / distrustful - refuses permanent contracts, charges more
 *   0-50   : acquaintance - default behavior
 *   50-100 : friend - warmer dialogue, small perks
 *   100+   : close - unique lines, refuses to work for others while you're active
 *
 * Mood is a short-term int [0, 100], decays toward 50 (neutral) at 1 unit per ~30 seconds of real time. Driven by nearby events (combat, damage, weather,
 * biome, etc.).
 */
public class PersonalityData implements INBTSerializable<CompoundTag> {

    private boolean initialized = false;
    private PersonalityArchetype archetype;
    private String hobbyId;
    private String hometown;
    private Set<Quirk> quirks = EnumSet.noneOf(Quirk.class);

    private int mood = 50;
    private final Map<UUID, Integer> affinityByPlayer = new HashMap<>();

    private static final int MEMORY_LIMIT_PER_PLAYER = 10;
    public static final int MOOD_MIN = 0;
    public static final int MOOD_MAX = 100;
    public static final int MOOD_NEUTRAL = 50;
    public static final int AFFINITY_MIN = -200;
    public static final int AFFINITY_MAX = 200;

    public static final int AFFINITY_STRANGER_MAX = -1;
    public static final int AFFINITY_FRIEND_MIN = 50;
    public static final int AFFINITY_CLOSE_MIN = 100;

    public PersonalityData() {}

    public void initialize(PersonalityArchetype archetype,
                           String hobbyId,
                           String hometown,
                           Set<Quirk> quirks) {
        if (initialized) return;
        this.archetype = archetype;
        this.hobbyId = hobbyId;
        this.hometown = hometown;
        this.quirks = quirks != null ? EnumSet.copyOf(quirks) : EnumSet.noneOf(Quirk.class);
        this.mood = MOOD_NEUTRAL;
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // Trait getters

    @Nullable
    public PersonalityArchetype getArchetype() {
        return archetype;
    }

    /**
     * Look up the current Hobby definition for this mercenary. Returns null f the hobby id no longer exists in the catalog (datapack removed it).
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

        if (archetype != null) tag.putString("archetype", archetype.getId());
        if (hobbyId != null) tag.putString("hobby", hobbyId);
        if (hometown != null) tag.putString("hometown", hometown);

        ListTag quirksTag = new ListTag();
        for (Quirk q : quirks) quirksTag.add(StringTag.valueOf(q.getId()));
        tag.put("quirks", quirksTag);

        tag.putInt("mood", mood);

        CompoundTag affinityTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> e : affinityByPlayer.entrySet()) {
            affinityTag.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("affinity", affinityTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.initialized = tag.getBoolean("initialized");
        this.archetype = PersonalityArchetype.fromId(tag.getString("archetype"));
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

        this.mood = tag.contains("mood")
                ? clamp(tag.getInt("mood"), MOOD_MIN, MOOD_MAX)
                : MOOD_NEUTRAL;

        this.affinityByPlayer.clear();
        if (tag.contains("affinity", Tag.TAG_COMPOUND)) {
            CompoundTag affinityTag = tag.getCompound("affinity");
            for (String key : affinityTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    affinityByPlayer.put(uuid, affinityTag.getInt(key));
                } catch (IllegalArgumentException ignored) {}
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
