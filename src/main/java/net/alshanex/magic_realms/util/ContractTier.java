package net.alshanex.magic_realms.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ContractTier {
    NOVICE(1, 20, "novice", ChatFormatting.WHITE),
    APPRENTICE(21, 40, "apprentice", ChatFormatting.GREEN),
    JOURNEYMAN(41, 60, "journeyman", ChatFormatting.BLUE),
    EXPERT(61, 80, "expert", ChatFormatting.DARK_PURPLE),
    MASTER(81, 100, "master", ChatFormatting.GOLD);

    private final int minLevel;
    private final int maxLevel;
    private final String name;
    private final ChatFormatting color;

    ContractTier(int minLevel, int maxLevel, String name, ChatFormatting color) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.name = name;
        this.color = color;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean canContractLevel(int level) {
        return level >= minLevel && level <= maxLevel;
    }

    public static ContractTier getRequiredTierForLevel(int level) {
        for (ContractTier tier : values()) {
            if (tier.canContractLevel(level)) {
                return tier;
            }
        }
        return MASTER; // Fallback para niveles superiores a 100
    }

    public Component getDisplayName() {
        return Component.translatable("item.magic_realms.contract_" + name)
                .withStyle(color);
    }

    public Component getLevelRange() {
        return Component.literal("(" + minLevel + "-" + maxLevel + ")")
                .withStyle(ChatFormatting.GRAY);
    }
}
