package net.alshanex.magic_realms.entity.tim;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.ModLootTables;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TimEntity extends AbstractSpellCastingMob implements Enemy {
    public TimEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(MREntityRegistry.TIM.get(), pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WizardAttackGoal(this, 1.25f, 25, 50)
                .setSpells(
                        List.of(SpellRegistry.ELDRITCH_BLAST_SPELL.get(), SpellRegistry.MAGIC_MISSILE_SPELL.get(), SpellRegistry.BALL_LIGHTNING_SPELL.get()),
                        List.of(),
                        List.of(),
                        List.of(SpellRegistry.BLIGHT_SPELL.get())
                )
        );
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(ZombifiedPiglin.class));
        this.targetSelector.addGoal(2, new PrioritizeArmoredTargetGoal<>(this, LivingEntity.class, true, ModTags.GEM_ARMOR));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        ItemStack hat = new ItemStack(ItemRegistry.WIZARD_HAT.get());
        hat.set(ComponentRegistry.CLOTHING_VARIANT, "hat");
        hat.set(DataComponents.DYED_COLOR, new DyedItemColor(DyedItemColor.LEATHER_COLOR, false));

        ItemStack chestplate = new ItemStack(ItemRegistry.WIZARD_CHESTPLATE.get());
        chestplate.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3454BE, false));

        ItemStack leggings = new ItemStack(ItemRegistry.WIZARD_LEGGINGS.get());
        leggings.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3454BE, false));

        ItemStack boots = new ItemStack(ItemRegistry.WIZARD_BOOTS.get());
        boots.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3454BE, false));

        this.setItemSlot(EquipmentSlot.HEAD, hat);
        this.setItemSlot(EquipmentSlot.CHEST, chestplate);
        this.setItemSlot(EquipmentSlot.LEGS, leggings);
        this.setItemSlot(EquipmentSlot.FEET, boots);
        this.setDropChance(EquipmentSlot.HEAD, 1.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);
    }

    @Override
    protected @NotNull ResourceKey<LootTable> getDefaultLootTable() {
        return ModLootTables.TIM_LOOT;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 35.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23F)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.HOSTILE_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.HOSTILE_SPLASH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.HOSTILE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOSTILE_DEATH;
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.HOSTILE_SMALL_FALL, SoundEvents.HOSTILE_BIG_FALL);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return -level.getPathfindingCostFromLightLevels(pos);
    }

    /**
     * Static predicate for determining if the current light level and environmental conditions allow for a monster to spawn.
     */
    public static boolean isDarkEnoughToSpawn(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) {
            return false;
        } else {
            DimensionType dimensiontype = level.dimensionType();
            int i = dimensiontype.monsterSpawnBlockLightLimit();
            if (i < 15 && level.getBrightness(LightLayer.BLOCK, pos) > i) {
                return false;
            } else {
                int j = level.getLevel().isThundering() ? level.getMaxLocalRawBrightness(pos, 10) : level.getMaxLocalRawBrightness(pos);
                return j <= dimensiontype.monsterSpawnLightTest().sample(random);
            }
        }
    }

    @Override
    public boolean shouldDropExperience() {
        return true;
    }

    @Override
    protected boolean shouldDropLoot() {
        return true;
    }

    public boolean isPreventingPlayerRest(Player player) {
        return true;
    }

    /**
     * Gets an item stack available to this entity to be loaded into the provided weapon, or an empty item stack if no such item stack is available.
     */
    @Override
    public ItemStack getProjectile(ItemStack shootable) {
        if (shootable.getItem() instanceof ProjectileWeaponItem) {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem)shootable.getItem()).getSupportedHeldProjectiles(shootable);
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack);
        } else {
            return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, ItemStack.EMPTY);
        }
    }

    public static class PrioritizeArmoredTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

        private final TagKey<Item> armorTag;

        public PrioritizeArmoredTargetGoal(Mob mob, Class<T> targetType, boolean mustSee, TagKey<Item> armorTag) {
            super(mob, targetType, mustSee);
            this.armorTag = armorTag;
        }

        @Override
        protected void findTarget() {
            double dist = this.getFollowDistance();
            List<T> candidates = this.mob.level()
                    .getEntitiesOfClass(this.targetType, this.getTargetSearchArea(dist),
                            this::isWearingTaggedArmor);

            this.target = this.mob.level().getNearestEntity(
                    candidates, this.targetConditions, this.mob,
                    this.mob.getX(), this.mob.getEyeY(), this.mob.getZ()
            );
        }

        private boolean isWearingTaggedArmor(LivingEntity entity) {
            for (ItemStack stack : entity.getArmorSlots()) {
                if (!stack.isEmpty() && stack.is(this.armorTag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
