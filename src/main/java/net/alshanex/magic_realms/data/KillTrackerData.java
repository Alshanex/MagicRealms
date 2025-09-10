package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.world.entity.LivingEntity;

public class KillTrackerData {
    public static final Codec<KillTrackerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("total_kills").forGetter(data -> data.totalKills),
                    Codec.INT.fieldOf("current_level").forGetter(data -> data.currentLevel),
                    Codec.INT.fieldOf("experience_points").forGetter(data -> data.experiencePoints),
                    Codec.INT.fieldOf("boss_kills").forGetter(data -> data.bossKills),
                    Codec.BOOL.fieldOf("has_natural_regen").forGetter(data -> data.hasNaturalRegen)
            ).apply(instance, KillTrackerData::new)
    );

    private int totalKills;
    private int currentLevel;
    private int experiencePoints;
    private int bossKills;
    private boolean hasNaturalRegen;

    public KillTrackerData() {
        this.totalKills = 0;
        this.currentLevel = 1;
        this.experiencePoints = 0;
        this.bossKills = 0;
        this.hasNaturalRegen = false;
    }

    private KillTrackerData(int totalKills, int currentLevel, int experiencePoints, int bossKills, boolean hasNaturalRegen) {
        this.totalKills = totalKills;
        this.currentLevel = currentLevel;
        this.experiencePoints = experiencePoints;
        this.bossKills = bossKills;
        this.hasNaturalRegen = hasNaturalRegen;
    }

    public void addKill(LivingEntity killedEntity) {
        int expGained = calculateExperienceGain(killedEntity);

        if (expGained > 0) {
            totalKills++;

            // Check if it's a boss kill
            boolean isBoss = isBossEntity(killedEntity);
            if (isBoss) {
                bossKills++;
                expGained *= 10; // Multiply experience by 10 for boss kills

                // Unlock natural regeneration if not already unlocked
                if (!hasNaturalRegen) {
                    hasNaturalRegen = true;
                }
            }

            experiencePoints += expGained;
            checkLevelUp();
        }
    }

    private boolean isBossEntity(LivingEntity entity) {
        return entity.getType().is(ModTags.BOSSES_TAG);
    }

    private int calculateExperienceGain(LivingEntity entity) {
        int vanillaExp = getVanillaExperienceReward(entity);

        if (vanillaExp == 0) {
            return 0;
        }

        int baseExp = Math.max(1, vanillaExp * 2);

        double levelPenalty = Math.max(0.1, 1.0 - (currentLevel * 0.05));

        int finalExp = Math.max(1, (int) (baseExp * levelPenalty * (Config.xpGainedMultiplier / 100)));

        return vanillaExp == 0 ? 0 : Math.max(finalExp, 1);
    }

    private int getVanillaExperienceReward(LivingEntity entity) {
        try {
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                return 42;
            }

            if (!(entity instanceof net.minecraft.world.entity.monster.Monster)) {
                return 0;
            }

            float maxHealth = entity.getMaxHealth();

            double baseExp = Math.sqrt(maxHealth) * 3.0;

            int finalExp = (int) Math.round(baseExp);

            return Math.max(1, Math.min(finalExp, 100));

        } catch (Exception e) {
            if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                return Math.max(1, Math.min((int) (entity.getMaxHealth() / 5), 20));
            }
            return 0;
        }
    }

    private void checkLevelUp() {
        int requiredExp = getRequiredExperienceForLevel(currentLevel + 1);

        while (experiencePoints >= requiredExp && currentLevel < getMaxLevel()) {
            currentLevel++;
            requiredExp = getRequiredExperienceForLevel(currentLevel + 1);
        }
    }

    private int getRequiredExperienceForLevel(int level) {
        if (level <= 1) return 0;

        return (int) (200 * level * (Config.xpNeededMultiplier / 100));
    }

    public int getMaxLevel() {
        return Config.maxLevel;
    }

    // Getters
    public int getTotalKills() {
        return totalKills;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getExperiencePoints() {
        return experiencePoints;
    }

    public int getBossKills() {
        return bossKills;
    }

    public boolean hasNaturalRegen() {
        return hasNaturalRegen;
    }

    public int getExperienceToNextLevel() {
        if (currentLevel >= getMaxLevel()) {
            return 0;
        }

        int requiredForNext = getRequiredExperienceForLevel(currentLevel + 1);
        return Math.max(0, requiredForNext - experiencePoints);
    }

    public void setLevel(int level) {
        this.currentLevel = Math.max(1, Math.min(level, getMaxLevel()));
        this.experiencePoints = getRequiredExperienceForLevel(this.currentLevel);
    }

    public void addExperience(int exp) {
        this.experiencePoints += exp;
        checkLevelUp();
    }

    @Override
    public String toString() {
        return String.format("KillTrackerData{level=%d, exp=%d, totalKills=%d, bossKills=%d, expToNext=%d, hasRegen=%s}",
                currentLevel, experiencePoints, totalKills, bossKills, getExperienceToNextLevel(), hasNaturalRegen);
    }
}
