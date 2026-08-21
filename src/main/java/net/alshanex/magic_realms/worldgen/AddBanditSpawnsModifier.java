package net.alshanex.magic_realms.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.registry.MRBiomeModifiers;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import java.util.List;

/**
 * Adds bandit natural spawns to a set of biomes, optionally pinning the spawned entities to one or more BanditProfiles.
 *
 * <p>The entity type is implicit - this modifier only ever spawns bandits.
 *
 * <pre>{@code
 * {
 *   "type": "magic_realms:add_bandit_spawns",
 *   "biomes": "#magic_realms:bandit_spawns_in",
 *   "weight": 20,
 *   "minCount": 2,
 *   "maxCount": 4,
 *   "profiles": [
 *     { "id": "magic_realms:highwayman", "weight": 5 },
 *     { "id": "magic_realms:bandit_captain", "weight": 1 }
 *   ]
 * }
 * }</pre>
 *
 * <p>{@code profiles} is optional; omit it and spawned bandits fall back to the normal random pool.
 */
public record AddBanditSpawnsModifier(
        HolderSet<Biome> biomes,
        int weight,
        int minCount,
        int maxCount,
        List<ProfileEntry> profiles
) implements BiomeModifier {

    /** One weighted candidate in the {@code profiles} pool. */
    public record ProfileEntry(ResourceLocation id, int weight) {
        public static final Codec<ProfileEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(ProfileEntry::id),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", 1).forGetter(ProfileEntry::weight)
        ).apply(inst, ProfileEntry::new));
    }

    public static final MapCodec<AddBanditSpawnsModifier> CODEC = RecordCodecBuilder
            .<AddBanditSpawnsModifier>mapCodec(inst -> inst.group(
                    Biome.LIST_CODEC.fieldOf("biomes")
                            .forGetter(AddBanditSpawnsModifier::biomes),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("weight")
                            .forGetter(AddBanditSpawnsModifier::weight),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("minCount", 1)
                            .forGetter(AddBanditSpawnsModifier::minCount),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("maxCount", 1)
                            .forGetter(AddBanditSpawnsModifier::maxCount),
                    ProfileEntry.CODEC.listOf().optionalFieldOf("profiles", List.of())
                            .forGetter(AddBanditSpawnsModifier::profiles)
            ).apply(inst, AddBanditSpawnsModifier::new))
            .validate(m -> m.maxCount() >= m.minCount()
                    ? DataResult.success(m)
                    : DataResult.error(() -> "maxCount (" + m.maxCount()
                    + ") must be >= minCount (" + m.minCount() + ")"));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) return;
        if (!this.biomes.contains(biome)) return;

        EntityType<?> type = MREntityRegistry.HOSTILE_HUMAN.get();
        builder.getMobSpawnSettings()
                .getSpawner(type.getCategory())
                .add(new MobSpawnSettings.SpawnerData(type, weight, minCount, maxCount));
    }

    /** Total weight of the profile pool. Zero when the pool is empty. */
    public int totalProfileWeight() {
        int total = 0;
        for (ProfileEntry e : profiles) total += e.weight();
        return total;
    }

    /** Weighted pick from this modifier's profile pool, or null if the pool is empty. */
    public ResourceLocation pickProfile(RandomSource random) {
        int total = totalProfileWeight();
        if (total <= 0) return null;

        int roll = random.nextInt(total);
        int acc = 0;
        for (ProfileEntry e : profiles) {
            acc += e.weight();
            if (roll < acc) return e.id();
        }
        return profiles.getLast().id(); // unreachable, safety net
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return MRBiomeModifiers.ADD_BANDIT_SPAWNS.get();
    }
}
