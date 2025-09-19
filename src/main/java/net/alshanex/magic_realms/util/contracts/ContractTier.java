package net.alshanex.magic_realms.util.contracts;

import net.alshanex.magic_realms.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ContractTier {
    NOVICE(1, "novice", ChatFormatting.WHITE),
    APPRENTICE(2, "apprentice", ChatFormatting.GREEN),
    JOURNEYMAN(3, "journeyman", ChatFormatting.BLUE),
    EXPERT(4, "expert", ChatFormatting.DARK_PURPLE),
    MASTER(5, "master", ChatFormatting.GOLD);

    private final int tier;
    private final String name;
    private final ChatFormatting color;

    ContractTier(int tier, String name, ChatFormatting color) {
        this.tier = tier;
        this.name = name;
        this.color = color;
    }

    public int getAmountOfTiers(){
        return 5;
    }

    public int getMinLevel() {
        return 1 + (Config.maxLevel / getAmountOfTiers()) * (tier - 1);
    }

    public int getMaxLevel() {
        return (Config.maxLevel / getAmountOfTiers()) * tier;
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean canContractLevel(int level) {
        return level >= getMinLevel() && level <= getMaxLevel();
    }

    public static ContractTier getRequiredTierForLevel(int level) {
        for (ContractTier tier : values()) {
            if (tier.canContractLevel(level)) {
                return tier;
            }
        }
        return MASTER;
    }

    public Component getDisplayName() {
        return Component.translatable("item.magic_realms.contract_" + name)
                .withStyle(color);
    }

    public Component getLevelRange() {
        return Component.literal("(" + getMinLevel() + "-" + getMaxLevel() + ")")
                .withStyle(ChatFormatting.GRAY);
    }
}
