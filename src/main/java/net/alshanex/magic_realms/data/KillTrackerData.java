package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;

public class KillTrackerData {
    public static final Codec<KillTrackerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("total_kills").forGetter(data -> data.totalKills),
                    Codec.INT.fieldOf("current_level").forGetter(data -> data.currentLevel),
                    Codec.INT.fieldOf("experience_points").forGetter(data -> data.experiencePoints)
            ).apply(instance, KillTrackerData::new)
    );

    private int totalKills;
    private int currentLevel;
    private int experiencePoints;

    public KillTrackerData() {
        this.totalKills = 0;
        this.currentLevel = 1;
        this.experiencePoints = 0;
    }

    private KillTrackerData(int totalKills, int currentLevel, int experiencePoints) {
        this.totalKills = totalKills;
        this.currentLevel = currentLevel;
        this.experiencePoints = experiencePoints;
    }

    public void addKill(LivingEntity killedEntity) {
        int expGained = calculateExperienceGain(killedEntity);

        if (expGained > 0) {
            totalKills++;
            experiencePoints += expGained;

            checkLevelUp();
        }
    }

    private int calculateExperienceGain(LivingEntity entity) {
        int vanillaExp = getVanillaExperienceReward(entity);

        if (vanillaExp == 0) {
            return 0;
        }

        int baseExp = Math.max(1, vanillaExp * 2);

        double levelPenalty = Math.max(0.1, 1.0 - (currentLevel * 0.05));

        int finalExp = Math.max(1, (int) (baseExp * levelPenalty));

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

            if (entity instanceof net.minecraft.world.entity.AgeableMob ageableMob && ageableMob.isBaby()) {
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

        // Fórmula: exp requerida = 100 * (nivel^1.5)
        return (int) (100 * Math.pow(level, 1.5));
    }


    private int getMaxLevel() {
        return 300;
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

    public int getExperienceToNextLevel() {
        if (currentLevel >= getMaxLevel()) {
            return 0; // Ya está al máximo nivel
        }

        int requiredForNext = getRequiredExperienceForLevel(currentLevel + 1);
        return Math.max(0, requiredForNext - experiencePoints);
    }

    public double getProgressToNextLevel() {
        if (currentLevel >= getMaxLevel()) {
            return 1.0; // 100% si está al máximo nivel
        }

        int currentLevelReq = getRequiredExperienceForLevel(currentLevel);
        int nextLevelReq = getRequiredExperienceForLevel(currentLevel + 1);
        int expInCurrentLevel = experiencePoints - currentLevelReq;
        int expNeededForLevel = nextLevelReq - currentLevelReq;

        return Math.min(1.0, (double) expInCurrentLevel / expNeededForLevel);
    }

    public void setLevel(int level) {
        this.currentLevel = Math.max(1, Math.min(level, getMaxLevel()));
        this.experiencePoints = getRequiredExperienceForLevel(this.currentLevel);
    }

    public void addExperience(int exp) {
        this.experiencePoints += exp;
        checkLevelUp();
    }

    public void reset() {
        this.totalKills = 0;
        this.currentLevel = 1;
        this.experiencePoints = 0;
    }

    @Override
    public String toString() {
        return String.format("KillTrackerData{level=%d, exp=%d, totalKills=%d, expToNext=%d}",
                currentLevel, experiencePoints, totalKills, getExperienceToNextLevel());
    }
}
