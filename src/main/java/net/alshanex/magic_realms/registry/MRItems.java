package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.item.*;
import net.alshanex.magic_realms.util.contracts.ContractTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class MRItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_NOVICE =
            ITEMS.register("contract_novice", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.NOVICE
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_APPRENTICE =
            ITEMS.register("contract_apprentice", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.APPRENTICE
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_JOURNEYMAN =
            ITEMS.register("contract_journeyman", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.JOURNEYMAN
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_EXPERT =
            ITEMS.register("contract_expert", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.EXPERT
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_MASTER =
            ITEMS.register("contract_master", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.MASTER
            ));

    public static final DeferredHolder<Item, PermanentContractItem> CONTRACT_PERMANENT =
            ITEMS.register("contract_permanent", () -> new PermanentContractItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .fireResistant()
                            .rarity(net.minecraft.world.item.Rarity.EPIC)
            ));

    public static final DeferredHolder<Item, HellPass> HELL_PASS =
            ITEMS.register("hell_pass", HellPass::new);

    public static final DeferredHolder<Item, Item> BLOOD_PACT =
            ITEMS.register("blood_pact", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, PermanentBloodPact> PERMANENT_BLOOD_PACT =
            ITEMS.register("permanent_blood_pact", PermanentBloodPact::new);

    public static final DeferredHolder<Item, RaidTriggerItem> MIDAS_COIN =
            ITEMS.register("midas_coin", () -> new RaidTriggerItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
            ));

    public static final Supplier<Item> WOODEN_CHAIR = ITEMS.register("wisewood_chair", () ->
            new BlockItem(MRBlocks.WOODEN_CHAIR.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.magic_realms.wisewood_chair")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });

    public static final Supplier<Item> WOODEN_CHAIR_SIMPLE = ITEMS.register("wisewood_chair_inert", () ->
            new BlockItem(MRBlocks.WOODEN_CHAIR_SIMPLE.get(), new Item.Properties()){
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.magic_realms.wisewood_chair_inert")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });

    public static final DeferredHolder<Item, SkinCustomizerItem> SKIN_CUSTOMIZER =
            ITEMS.register("skin_customizer", () -> new SkinCustomizerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<Item, Item> SLEEPING_PASS =
            ITEMS.register("sleeping_pass", () -> new Item(new Item.Properties()
                    .stacksTo(16)
                    .rarity(Rarity.UNCOMMON)
            ));

    public static final DeferredHolder<Item, FloatingArrowItem> FLOATING_ARROW_WEAPON =
            ITEMS.register("floating_arrow_weapon", () -> new FloatingArrowItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
            ));

    public static final DeferredHolder<Item, Item> KNOWLEDGE_CRYSTAL =
            ITEMS.register("knowledge_crystal", KnowledgeCrystalItem::new);

    public static final DeferredHolder<Item, Item> MAGE_ENDERMAN_SPAWN_EGG =
            ITEMS.register(
                    "mage_enderman_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            MREntityRegistry.WIZARD_ENDERMAN,
                            0x5A5A62,
                            0xA8A8B0,
                            new Item.Properties().stacksTo(64)
                    )
            );

    public static final DeferredHolder<Item, Item> MAGIC_CREEPER_SPAWN_EGG =
            ITEMS.register(
                    "magic_creeper_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            MREntityRegistry.MAGIC_CREEPER,
                            0x4F9E5A,
                            0xB84DFF,
                            new Item.Properties().stacksTo(64)
                    )
            );

    public static final DeferredHolder<Item, Item> MAGIC_SLIME_SPAWN_EGG =
            ITEMS.register(
                    "magic_slime_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            MREntityRegistry.MAGIC_SLIME,
                            0x4CBE9B,
                            0x9B5CFF,
                            new Item.Properties().stacksTo(64)
                    )
            );

    public static final DeferredHolder<Item, Item> HUMAN_MERCENARY_SPAWN_EGG =
            ITEMS.register(
                    "human_mercenary_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            MREntityRegistry.HUMAN,
                            0x7A5A42,
                            0xB0B0B0,
                            new Item.Properties().stacksTo(64)
                    )
            );

    public static final DeferredHolder<Item, Item> TAVERNKEEPER_SPAWN_EGG =
            ITEMS.register(
                    "tavernkeeper_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            MREntityRegistry.TAVERNKEEP,
                            0x8B5A2B,
                            0xE6B85C,
                            new Item.Properties().stacksTo(64)
                    )
            );

    public static final DeferredHolder<Item, Item> BANDIT_SPAWN_EGG =
            ITEMS.register(
                    "bandit_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            MREntityRegistry.HOSTILE_HUMAN,
                            0x303035,
                            0x7A3030,
                            new Item.Properties().stacksTo(64)
                    )
            );

    public static final DeferredHolder<Item, Item> ZOMBIE_MAGE_SPAWN_EGG =
            ITEMS.register(
                    "zombie_mage_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            MREntityRegistry.TIM,
                            0x54785A,
                            0x8B4DCC,
                            new Item.Properties().stacksTo(64)
                    )
            );
}
