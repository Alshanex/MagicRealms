package net.alshanex.magic_realms;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_LEVEL = BUILDER
            .comment("Max level entities can level up")
            .defineInRange("maxLevel", 100, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue HEALTH_AMOUNT = BUILDER
            .comment("Amount of health humans gain on level up")
            .defineInRange("healthAmount", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue HEALTH_AMOUNT_TIMES = BUILDER
            .comment("Max times humans will get the health bonus upon leveling up")
            .defineInRange("healthAmountTimes", 100, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue HEALTH_AMOUNT_BOSS_KILLS = BUILDER
            .comment("Amount of health bonus humans get upon killing bosses")
            .defineInRange("healthAmountBossKills", 2, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue HEALTH_AMOUNT_BOSS_KILLS_TIMES = BUILDER
            .comment("Max times humans will get the health bonus upon killing bosses")
            .defineInRange("healthAmountBossKillsTimes", 20, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue DAMAGE_AMOUNT_BOSS_KILLS = BUILDER
            .comment("Amount of damage bonus humans get upon killing bosses")
            .defineInRange("damageAmountBossKills", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue DAMAGE_AMOUNT_BOSS_KILLS_TIMES = BUILDER
            .comment("Max times humans will get the damage bonus upon killing bosses")
            .defineInRange("damageAmountBossKillsTimes", 20, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ARMOR_AMOUNT_BOSS_KILLS = BUILDER
            .comment("Amount of armor bonus humans get upon killing bosses")
            .defineInRange("armorAmountBossKills", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ARMOR_AMOUNT_BOSS_KILLS_TIMES = BUILDER
            .comment("Max times humans will get the armor bonus upon killing bosses")
            .defineInRange("armorAmountBossKillsTimes", 20, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue MAX_SPELL_POWER_PERCENTAGE = BUILDER
            .comment("Max percentage of bonus spell power mages can get upon leveling up (50.0 = 50%)")
            .defineInRange("maxSpellPowerPercentage", 50.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue MAX_SPELL_RESISTANCE_PERCENTAGE = BUILDER
            .comment("Max percentage of bonus spell resistance mages can get upon leveling up (50.0 = 50%)")
            .defineInRange("maxSpellResistancePercentage", 50.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.IntValue DAMAGE_AMOUNT_WARRIORS = BUILDER
            .comment("Amount of damage bonus warriors get upon leveling up")
            .defineInRange("damageAmountWarriors", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue DAMAGE_AMOUNT_WARRIORS_TIMES = BUILDER
            .comment("Max times warriors will get the damage bonus upon leveling up")
            .defineInRange("damageAmountWarriorsTimes", 20, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ARMOR_AMOUNT_WARRIORS = BUILDER
            .comment("Amount of armor bonus warriors get upon leveling up")
            .defineInRange("armorAmountWarriors", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ARMOR_AMOUNT_WARRIORS_TIMES = BUILDER
            .comment("Max times warriors will get the armor bonus upon leveling up")
            .defineInRange("armorAmountWarriorsTimes", 30, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue DAMAGE_AMOUNT_ROGUES = BUILDER
            .comment("Amount of damage bonus rogues get upon leveling up")
            .defineInRange("damageAmountRogues", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue DAMAGE_AMOUNT_ROGUES_TIMES = BUILDER
            .comment("Max times rogues will get the damage bonus upon leveling up")
            .defineInRange("damageAmountRoguesTimes", 20, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue MAX_SPEED_PERCENTAGE = BUILDER
            .comment("Max percentage of bonus speed rogues can get upon leveling up (50.0 = 50%)")
            .defineInRange("maxSpeedPercentage", 50.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue MAX_ARROW_DAMAGE_PERCENTAGE = BUILDER
            .comment("Max percentage of bonus arrow damage archers can get upon leveling up (50.0 = 50%)")
            .defineInRange("maxArrowDamagePercentage", 50.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue MAX_ARROW_VELOCITY_PERCENTAGE = BUILDER
            .comment("Max percentage of bonus arrow velocity archers can get upon leveling up (50.0 = 50%)")
            .defineInRange("maxArrowVelocityPercentage", 50.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue MAX_DRAW_SPEED_PERCENTAGE = BUILDER
            .comment("Max percentage of bonus draw speed archers can get upon leveling up (50.0 = 50%)")
            .defineInRange("maxDrawSpeedPercentage", 50.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue XP_GAIN_MULTIPLIER = BUILDER
            .comment("XP gain multiplier (50.0 = 50% = xp_gained * 0.5)")
            .defineInRange("xpGainedMultiplier", 100.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue XP_LEVEL_UP_MULTIPLIER = BUILDER
            .comment("XP needed to level up multiplier (50.0 = 50% = xp_needed * 0.5)")
            .defineInRange("xpNeededMultiplier", 100.0, 0, Double.MAX_VALUE);

    private static final ModConfigSpec.IntValue EMERALD_OVERFLOW_THRESHOLD = BUILDER
            .comment("Number of emeralds above which humans will consider spending overflow (default: 64 = 1 stack)")
            .defineInRange("emeraldOverflowThreshold", 64, 1, 999);

    private static final ModConfigSpec.IntValue CLERIC_EMERALD_COST = BUILDER
            .comment("Number of emeralds required to get a beneficial effect from a cleric")
            .defineInRange("clericEmeraldCost", 10, 1, 64);

    private static final ModConfigSpec.IntValue CLERIC_BUFF_DURATION_MINUTES = BUILDER
            .comment("Duration of cleric buffs in minutes")
            .defineInRange("clericBuffDurationMinutes", 10, 1, 60);

    private static final ModConfigSpec.IntValue IMMORTAL_STUN_DURATION = BUILDER
            .comment("Duration in seconds that immortal entities are stunned after being knocked out")
            .defineInRange("immortalStunDuration", 10, 1, 60);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int maxLevel;
    public static int healthAmount;
    public static int healthAmountBossKills;
    public static int damageAmountBossKills;
    public static int armorAmountBossKills;
    public static int healthAmountBossKillsTimes;
    public static int healthAmountTimes;
    public static int damageAmountBossKillsTimes;
    public static int armorAmountBossKillsTimes;
    public static double maxSpellPowerPercentage;
    public static double maxSpellResistancePercentage;
    public static int damageAmountWarriors;
    public static int damageAmountWarriorsTimes;
    public static int armorAmountWarriors;
    public static int armorAmountWarriorsTimes;
    public static int damageAmountRogues;
    public static int damageAmountRoguesTimes;
    public static double maxSpeedPercentage;
    public static double xpGainedMultiplier;
    public static double xpNeededMultiplier;
    public static double maxArrowDamagePercentage;
    public static double maxArrowVelocityPercentage;
    public static double maxDrawSpeedPercentage;
    public static int emeraldOverflowThreshold;
    public static int clericEmeraldCost;
    public static int clericBuffDurationMinutes;
    public static int immortalStunDuration;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        maxLevel = MAX_LEVEL.get();
        healthAmount = HEALTH_AMOUNT.get();
        healthAmountBossKills = HEALTH_AMOUNT_BOSS_KILLS.get();
        damageAmountBossKills = DAMAGE_AMOUNT_BOSS_KILLS.get();
        armorAmountBossKills = ARMOR_AMOUNT_BOSS_KILLS.get();
        healthAmountBossKillsTimes = HEALTH_AMOUNT_BOSS_KILLS_TIMES.get();
        healthAmountTimes = HEALTH_AMOUNT_TIMES.get();
        damageAmountBossKillsTimes = DAMAGE_AMOUNT_BOSS_KILLS_TIMES.get();
        armorAmountBossKillsTimes = ARMOR_AMOUNT_BOSS_KILLS_TIMES.get();
        maxSpellPowerPercentage = MAX_SPELL_POWER_PERCENTAGE.get();
        maxSpellResistancePercentage = MAX_SPELL_RESISTANCE_PERCENTAGE.get();
        damageAmountWarriors = DAMAGE_AMOUNT_WARRIORS.get();
        damageAmountWarriorsTimes = DAMAGE_AMOUNT_WARRIORS_TIMES.get();
        armorAmountWarriors = ARMOR_AMOUNT_WARRIORS.get();
        armorAmountWarriorsTimes = ARMOR_AMOUNT_WARRIORS_TIMES.get();
        damageAmountRogues = DAMAGE_AMOUNT_ROGUES.get();
        damageAmountRoguesTimes = DAMAGE_AMOUNT_ROGUES_TIMES.get();
        maxSpeedPercentage = MAX_SPEED_PERCENTAGE.get();
        xpGainedMultiplier = XP_GAIN_MULTIPLIER.get();
        xpNeededMultiplier = XP_LEVEL_UP_MULTIPLIER.get();
        maxArrowDamagePercentage = MAX_ARROW_DAMAGE_PERCENTAGE.get();
        maxArrowVelocityPercentage = MAX_ARROW_VELOCITY_PERCENTAGE.get();
        maxDrawSpeedPercentage = MAX_DRAW_SPEED_PERCENTAGE.get();
        emeraldOverflowThreshold = EMERALD_OVERFLOW_THRESHOLD.get();
        clericEmeraldCost = CLERIC_EMERALD_COST.get();
        clericBuffDurationMinutes = CLERIC_BUFF_DURATION_MINUTES.get();
        immortalStunDuration = IMMORTAL_STUN_DURATION.get();
    }
}
