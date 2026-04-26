package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.SeatEntity;
import net.alshanex.magic_realms.entity.creeper.MagicCreeperEntity;
import net.alshanex.magic_realms.entity.enderman.WizardEndermanEntity;
import net.alshanex.magic_realms.entity.exclusive.ace.AceEntity;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.exclusive.alshanex.AlshanexEntity;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntity;
import net.alshanex.magic_realms.entity.exclusive.catas.CatasEntity;
import net.alshanex.magic_realms.entity.exclusive.lilac.LilacEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.alshanex.magic_realms.entity.slime.MagicSlimeEntity;
import net.alshanex.magic_realms.entity.slime.SummonedMagicSlimeEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.entity.tim.TimEntity;
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
            ENTITIES.register("mercenary", () -> EntityType.Builder.<RandomHumanEntity>of(RandomHumanEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "mercenary").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<HostileRandomHumanEntity>> HOSTILE_HUMAN =
            ENTITIES.register("bandit", () -> EntityType.Builder.<HostileRandomHumanEntity>of(HostileRandomHumanEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "bandit").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<TavernKeeperEntity>> TAVERNKEEP =
            ENTITIES.register("tavernkeep", () -> EntityType.Builder.<TavernKeeperEntity>of(TavernKeeperEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavernkeep").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AlshanexEntity>> ALSHANEX =
            ENTITIES.register("alshanex_mercenary", () -> EntityType.Builder.<AlshanexEntity>of(AlshanexEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "alshanex_mercenary").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AlianaEntity>> ALIANA =
            ENTITIES.register("aliana_mercenary", () -> EntityType.Builder.<AlianaEntity>of(AlianaEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "aliana_mercenary").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<CatasEntity>> CATAS =
            ENTITIES.register("catas_mercenary", () -> EntityType.Builder.<CatasEntity>of(CatasEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "catas_mercenary").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AmadeusEntity>> AMADEUS =
            ENTITIES.register("amadeus_mercenary", () -> EntityType.Builder.<AmadeusEntity>of(AmadeusEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "amadeus_mercenary").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AceEntity>> ACE =
            ENTITIES.register("eden_mercenary", () -> EntityType.Builder.<AceEntity>of(AceEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "eden_mercenary").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<LilacEntity>> LILAC =
            ENTITIES.register("lilac_mercenary", () -> EntityType.Builder.<LilacEntity>of(LilacEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "lilac_mercenary").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<MagicSlimeEntity>> MAGIC_SLIME =
            ENTITIES.register("chuchu", () -> EntityType.Builder.<MagicSlimeEntity>of(MagicSlimeEntity::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "chuchu").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<SummonedMagicSlimeEntity>> SUMMONED_MAGIC_SLIME =
            ENTITIES.register("summoned_chuchu", () -> EntityType.Builder.<SummonedMagicSlimeEntity>of(SummonedMagicSlimeEntity::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "summoned_chuchu").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT =
            ENTITIES.register("seat", () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.001F, 0.001F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "seat").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<MagicCreeperEntity>> MAGIC_CREEPER =
            ENTITIES.register("fizzle", () -> EntityType.Builder.<MagicCreeperEntity>of(MagicCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "fizzle").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<TimEntity>> TIM =
            ENTITIES.register("tim", () -> EntityType.Builder.<TimEntity>of(TimEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tim").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<WizardEndermanEntity>> WIZARD_ENDERMAN =
            ENTITIES.register("endermage", () -> EntityType.Builder.of(WizardEndermanEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.8f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "endermage").toString()));
}
