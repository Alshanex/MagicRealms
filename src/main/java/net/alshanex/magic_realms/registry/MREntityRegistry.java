package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.exclusive.alshanex.AlshanexEntity;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntity;
import net.alshanex.magic_realms.entity.exclusive.catas.CatasEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.alshanex.magic_realms.entity.slime.MagicSlimeEntity;
import net.alshanex.magic_realms.entity.slime.SummonedMagicSlimeEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MREntityRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    public static final DeferredHolder<EntityType<?>, EntityType<RandomHumanEntity>> HUMAN =
            ENTITIES.register("human_entity", () -> EntityType.Builder.<RandomHumanEntity>of(RandomHumanEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "human_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<HostileRandomHumanEntity>> HOSTILE_HUMAN =
            ENTITIES.register("hostile_human_entity", () -> EntityType.Builder.<HostileRandomHumanEntity>of(HostileRandomHumanEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "hostile_human_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<TavernKeeperEntity>> TAVERNKEEP =
            ENTITIES.register("tavernkeep", () -> EntityType.Builder.<TavernKeeperEntity>of(TavernKeeperEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavernkeep").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AlshanexEntity>> ALSHANEX =
            ENTITIES.register("alshanex_entity", () -> EntityType.Builder.<AlshanexEntity>of(AlshanexEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "alshanex_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AlianaEntity>> ALIANA =
            ENTITIES.register("aliana_entity", () -> EntityType.Builder.<AlianaEntity>of(AlianaEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "aliana_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<CatasEntity>> CATAS =
            ENTITIES.register("catas_entity", () -> EntityType.Builder.<CatasEntity>of(CatasEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "catas_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AmadeusEntity>> AMADEUS =
            ENTITIES.register("amadeus_entity", () -> EntityType.Builder.<AmadeusEntity>of(AmadeusEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "amadeus_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<MagicSlimeEntity>> MAGIC_SLIME =
            ENTITIES.register("magic_slime", () -> EntityType.Builder.<MagicSlimeEntity>of(MagicSlimeEntity::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "magic_slime").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<SummonedMagicSlimeEntity>> SUMMONED_MAGIC_SLIME =
            ENTITIES.register("summoned_magic_slime", () -> EntityType.Builder.<SummonedMagicSlimeEntity>of(SummonedMagicSlimeEntity::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "summoned_magic_slime").toString()));
}
