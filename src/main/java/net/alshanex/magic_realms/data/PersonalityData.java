package net.alshanex.magic_realms.data;

import net.alshanex.magic_realms.util.FoodTags;
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
    private String favoriteFoodTagId;
    private String dislikedFoodTagId;
    private String hobbyId;
    private String hometown;
    private int birthdayDayOfYear;
    private Set<Quirk> quirks = EnumSet.noneOf(Quirk.class);

    private int mood = 50;
    private final Map<UUID, Integer> affinityByPlayer = new HashMap<>();
    private final Map<UUID, Deque<MemoryEvent>> memoryByPlayer = new HashMap<>();

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
                           String favoriteFoodTagId,
                           String dislikedFoodTagId,
                           String hobbyId,
                           String hometown,
                           int birthdayDayOfYear,
                           Set<Quirk> quirks) {
        if (initialized) return;
        this.archetype = archetype;
        this.favoriteFoodTagId = favoriteFoodTagId;
        this.dislikedFoodTagId = dislikedFoodTagId;
        this.hobbyId = hobbyId;
        this.hometown = hometown;
        this.birthdayDayOfYear = birthdayDayOfYear;
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

    @Nullable
    public TagKey<Item> getFavoriteFoodTag() {
        return FoodTags.parse(favoriteFoodTagId);
    }

    @Nullable
    public String getFavoriteFoodTagId() {
        return favoriteFoodTagId;
    }

    @Nullable
    public TagKey<Item> getDislikedFoodTag() {
        return FoodTags.parse(dislikedFoodTagId);
    }

    @Nullable
    public String getDislikedFoodTagId() {
        return dislikedFoodTagId;
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

    public int getBirthdayDayOfYear() {
        return birthdayDayOfYear;
    }

    public Set<Quirk> getQuirks() {
        return EnumSet.copyOf(quirks);
    }

    public boolean hasQuirk(Quirk q) {
        return quirks.contains(q);
    }

    // Mood

    public int getMood() {
        return mood;
    }

    public void adjustMood(int delta) {
        this.mood = clamp(this.mood + delta, MOOD_MIN, MOOD_MAX);
    }

    public void setMood(int value) {
        this.mood = clamp(value, MOOD_MIN, MOOD_MAX);
    }

    public void decayMood() {
        if (mood > MOOD_NEUTRAL) mood--;
        else if (mood < MOOD_NEUTRAL) mood++;
    }

    public MoodTier getMoodTier() {
        if (mood >= 70) return MoodTier.HAPPY;
        if (mood >= 40) return MoodTier.CONTENT;
        if (mood >= 20) return MoodTier.GRUMPY;
        return MoodTier.MISERABLE;
    }

    public enum MoodTier { HAPPY, CONTENT, GRUMPY, MISERABLE }

    // Affinity

    public int getAffinity(UUID playerId) {
        if (playerId == null) return 0;
        return affinityByPlayer.getOrDefault(playerId, 0);
    }

    public void adjustAffinity(UUID playerId, int delta) {
        if (playerId == null) return;
        int current = affinityByPlayer.getOrDefault(playerId, 0);
        affinityByPlayer.put(playerId, clamp(current + delta, AFFINITY_MIN, AFFINITY_MAX));
    }

    public void setAffinity(UUID playerId, int value) {
        if (playerId == null) return;
        affinityByPlayer.put(playerId, clamp(value, AFFINITY_MIN, AFFINITY_MAX));
    }

    public AffinityTier getAffinityTier(UUID playerId) {
        int a = getAffinity(playerId);
        if (a <= AFFINITY_STRANGER_MAX) return AffinityTier.STRANGER;
        if (a >= AFFINITY_CLOSE_MIN) return AffinityTier.CLOSE;
        if (a >= AFFINITY_FRIEND_MIN) return AffinityTier.FRIEND;
        return AffinityTier.ACQUAINTANCE;
    }

    public enum AffinityTier { STRANGER, ACQUAINTANCE, FRIEND, CLOSE }

    // Memory

    public void addMemory(UUID playerId, MemoryEvent event) {
        if (playerId == null || event == null) return;
        Deque<MemoryEvent> log = memoryByPlayer.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        if (!log.isEmpty() && log.peekLast() == event) return;
        log.offerLast(event);
        while (log.size() > MEMORY_LIMIT_PER_PLAYER) log.pollFirst();
    }

    public boolean remembers(UUID playerId, MemoryEvent event) {
        if (playerId == null || event == null) return false;
        Deque<MemoryEvent> log = memoryByPlayer.get(playerId);
        return log != null && log.contains(event);
    }

    // NBT Serialization

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", initialized);

        if (archetype != null) tag.putString("archetype", archetype.getId());
        if (favoriteFoodTagId != null) tag.putString("favorite_food_tag", favoriteFoodTagId);
        if (dislikedFoodTagId != null) tag.putString("disliked_food_tag", dislikedFoodTagId);
        if (hobbyId != null) tag.putString("hobby", hobbyId);
        if (hometown != null) tag.putString("hometown", hometown);
        tag.putInt("birthday", birthdayDayOfYear);

        ListTag quirksTag = new ListTag();
        for (Quirk q : quirks) quirksTag.add(StringTag.valueOf(q.getId()));
        tag.put("quirks", quirksTag);

        tag.putInt("mood", mood);

        CompoundTag affinityTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> e : affinityByPlayer.entrySet()) {
            affinityTag.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("affinity", affinityTag);

        CompoundTag memoryTag = new CompoundTag();
        for (Map.Entry<UUID, Deque<MemoryEvent>> e : memoryByPlayer.entrySet()) {
            ListTag list = new ListTag();
            for (MemoryEvent ev : e.getValue()) list.add(StringTag.valueOf(ev.name()));
            memoryTag.put(e.getKey().toString(), list);
        }
        tag.put("memory", memoryTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.initialized = tag.getBoolean("initialized");
        this.archetype = PersonalityArchetype.fromId(tag.getString("archetype"));
        this.favoriteFoodTagId = tag.contains("favorite_food_tag") ? tag.getString("favorite_food_tag") : null;
        this.dislikedFoodTagId = tag.contains("disliked_food_tag") ? tag.getString("disliked_food_tag") : null;
        this.hobbyId = tag.contains("hobby") ? tag.getString("hobby") : null;
        this.hometown = tag.contains("hometown") ? tag.getString("hometown") : null;
        this.birthdayDayOfYear = tag.getInt("birthday");

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

        this.memoryByPlayer.clear();
        if (tag.contains("memory", Tag.TAG_COMPOUND)) {
            CompoundTag memoryTag = tag.getCompound("memory");
            for (String key : memoryTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ListTag list = memoryTag.getList(key, Tag.TAG_STRING);
                    Deque<MemoryEvent> log = new ArrayDeque<>();
                    for (int i = 0; i < list.size(); i++) {
                        MemoryEvent ev = MemoryEvent.fromName(list.getString(i));
                        if (ev != null) log.offerLast(ev);
                    }
                    memoryByPlayer.put(uuid, log);
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
