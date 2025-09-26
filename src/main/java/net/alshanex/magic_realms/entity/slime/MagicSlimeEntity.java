package net.alshanex.magic_realms.entity.slime;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.List;

public class MagicSlimeEntity extends Slime {
    private static final EntityDataAccessor<String> WEAK_SCHOOL = SynchedEntityData.defineId(MagicSlimeEntity.class, EntityDataSerializers.STRING);

    private SchoolType weakSchool;

    public MagicSlimeEntity(EntityType<? extends Slime> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    private void initializeSchool() {
        if (this.getWeakSchool() == null) {
            List<SchoolType> availableSchools = SchoolRegistry.REGISTRY.stream().toList();
            if (!availableSchools.isEmpty()) {
                int index = random.nextInt(availableSchools.size());
                this.setWeakSchool(availableSchools.get(index));
            }
        }
    }

    public SchoolType getWeakSchool() {
        if (this.weakSchool == null) {
            String schoolId = this.entityData.get(WEAK_SCHOOL);
            if (!schoolId.isEmpty()) {
                ResourceLocation schoolLocation = ResourceLocation.parse(schoolId);
                this.weakSchool = SchoolRegistry.REGISTRY.get(schoolLocation);
            }
        }
        return this.weakSchool;
    }

    public void setWeakSchool(SchoolType weakSchool) {
        this.weakSchool = weakSchool;
        if (weakSchool != null) {
            this.entityData.set(WEAK_SCHOOL, weakSchool.getId().toString());
            MagicRealms.LOGGER.debug("Set weak school to: " + weakSchool.getId());
        } else {
            this.entityData.set(WEAK_SCHOOL, "");
            MagicRealms.LOGGER.debug("Set weak school to null/empty");
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        // Call parent finalizeSpawn first to set up size and other slime properties
        SpawnGroupData spawnData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);

        RandomSource randomsource = pLevel.getRandom();
        int i = randomsource.nextInt(3);
        if (i < 2 && randomsource.nextFloat() < 0.5F * pDifficulty.getSpecialMultiplier()) {
            i++;
        }

        int j = 1 << i;
        this.setSize(j, true);

        this.initializeSchool();

        return spawnData;
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, .2);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("weakSchool", this.entityData.get(WEAK_SCHOOL));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WEAK_SCHOOL, "");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("weakSchool")) {
            String schoolIdString = compound.getString("weakSchool");
            this.entityData.set(WEAK_SCHOOL, schoolIdString);
            // Clear the cached value so it gets refreshed from synced data
            this.weakSchool = null;
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WEAK_SCHOOL.equals(key)) {
            this.weakSchool = null;
        }
    }
}