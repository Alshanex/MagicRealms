package net.alshanex.magic_realms.story;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRSoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class LoreProgression {

    /**
     * An object representing a specific piece of lore.
     * @param id The unique identifier saved to the player's data attachment.
     * @param message The translatable component to display when unlocked (optional).
     * @param sound The sound to play when unlocked (optional).
     */
    public record LoreNode(ResourceLocation id, @Nullable Component title, @Nullable Component message, @Nullable SoundEvent sound) {}

    // World Lore (Tablets)
    public static final LoreNode WORLD_1 = new LoreNode(
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "world_lore_1"),
            Component.translatable("lore.magic_realms.world_1.title"),
            Component.translatable("lore.magic_realms.world_1.unlock"),
            null
    );
    public static final LoreNode WORLD_2 = new LoreNode(
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "world_lore_2"),
            Component.translatable("lore.magic_realms.world_2.title"),
            Component.translatable("lore.magic_realms.world_2.unlock"),
            null
    );
    public static final LoreNode WORLD_3 = new LoreNode(
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "world_lore_3"),
            Component.translatable("lore.magic_realms.world_3.title"),
            Component.translatable("lore.magic_realms.world_3.unlock"),
            null
    );
    public static final LoreNode WORLD_4 = new LoreNode(
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "world_lore_4"),
            Component.translatable("lore.magic_realms.world_4.title"),
            Component.translatable("lore.magic_realms.world_4.unlock"),
            null
    );

    public static final List<LoreNode> WORLD_LORE_ORDER = List.of(WORLD_1, WORLD_2, WORLD_3, WORLD_4);

    // Order Lore (Resonance Crystals)
    public static final LoreNode ORDER_1 = new LoreNode(
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "order_lore_1"),
            Component.translatable("subtitles.magic_realms.order_lore_1"),
            null,
            MRSoundRegistry.ORDER_LORE_1.get()
    );
    public static final LoreNode ORDER_2 = new LoreNode(
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "order_lore_2"),
            Component.translatable("subtitles.magic_realms.order_lore_2"),
            null,
            MRSoundRegistry.ORDER_LORE_2.get()
    );
    public static final LoreNode ORDER_3 = new LoreNode(
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "order_lore_3"),
            Component.translatable("subtitles.magic_realms.order_lore_3"),
            null,
            MRSoundRegistry.ORDER_LORE_3.get()
    );

    public static final List<LoreNode> ORDER_LORE_ORDER = List.of(ORDER_1, ORDER_2, ORDER_3);

    /**
     * Finds the next missing World Lore node.
     */
    public static Optional<LoreNode> getNextWorldLore(PlayerLoreProgress progress) {
        for (LoreNode node : WORLD_LORE_ORDER) {
            if (!progress.hasLoreNode(node.id())) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the next missing Order Lore node.
     */
    public static Optional<LoreNode> getNextOrderLore(PlayerLoreProgress progress) {
        if (!progress.hasLoreNode(WORLD_4.id())) {
            return Optional.empty();
        }

        for (LoreNode node : ORDER_LORE_ORDER) {
            if (!progress.hasLoreNode(node.id())) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    @Nullable
    public static LoreNode getNodeById(ResourceLocation id) {
        for (LoreNode node : WORLD_LORE_ORDER) {
            if (node.id().equals(id)) return node;
        }
        for (LoreNode node : ORDER_LORE_ORDER) {
            if (node.id().equals(id)) return node;
        }
        return null;
    }

    /**
     * The Chronicle Book can only be opened after world lore is found.
     */
    public static boolean canOpenBook(PlayerLoreProgress progress) {
        return progress.hasLoreNode(WORLD_4.id());
    }
}
