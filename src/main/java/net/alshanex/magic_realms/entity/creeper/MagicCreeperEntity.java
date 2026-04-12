package net.alshanex.magic_realms.entity.creeper;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MagicCreeperEntity extends Creeper implements IMagicEntity {
    private final MagicData playerMagicData = new MagicData(true);
    private @Nullable SpellData castingSpell;
    private SchoolType weakSchool;
    private AbstractSpell explosionSpell;

    private boolean hasInitiatedCast = false;
    private int previousSwellDir = -1;

    private static final EntityDataAccessor<String> WEAK_SCHOOL = SynchedEntityData.defineId(MagicCreeperEntity.class, EntityDataSerializers.STRING);

    public MagicCreeperEntity(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        playerMagicData.setSyncedData(new SyncedSpellData(this));
        this.initializeSchool();
    }

    public MagicCreeperEntity(Level level, SchoolType parentSchool) {
        super(MREntityRegistry.MAGIC_CREEPER.get(), level);
        playerMagicData.setSyncedData(new SyncedSpellData(this));
        if (parentSchool != null) {
            this.setWeakSchool(parentSchool);
        } else {
            this.initializeSchool();
        }
    }

    protected void initializeSchool() {
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
            if (weakSchool.getId().equals(SchoolRegistry.FIRE.get().getId())) {
                extinguishFire();
            }
            if (weakSchool.getId().equals(SchoolRegistry.ICE.get().getId())) {
                setTicksFrozen(0);
            }
            this.explosionSpell = getSpellForExplosion();
            updateFuseTime();
        } else {
            this.entityData.set(WEAK_SCHOOL, "");
        }
    }

    private void updateFuseTime() {
        if (this.explosionSpell != null) {
            int spellLevel = Math.max(this.explosionSpell.getMaxLevel() / 2, 1);
            int castTime = this.explosionSpell.getEffectiveCastTime(spellLevel, this);
            this.maxSwell = castTime + 10;
        }
    }

    private AbstractSpell getSpellForExplosion() {
        var list = new ArrayList<AbstractSpell>();

        for (var spell : SpellRegistry.getEnabledSpells()) {
            SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                if (a.is(ModTags.CREEPER_SPELLS)) {
                    list.add(spell);
                }
            });
        }

        if (!list.isEmpty()) {
            List<AbstractSpell> finalList = list.stream()
                    .filter(spell -> spell.getSchoolType().getId()
                            .equals(getWeakSchool().getId()))
                    .toList();

            if (finalList.isEmpty()) {
                return null;
            }

            return finalList.get(this.random.nextInt(finalList.size()));
        }

        return null;
    }

    @Override
    public boolean isOnFire() {
        return this.isInvulnerable();
    }

    private int deathCountdown = -1;

    @Override
    public void tick() {
        if (!this.level().isClientSide && deathCountdown >= 0) {
            if (deathCountdown-- <= 0) {
                this.triggerOnDeathMobEffects(Entity.RemovalReason.KILLED);
                this.discard();
                return;
            }
        }

        if (this.isAlive() && !this.level().isClientSide && deathCountdown < 0) {
            int currentSwellDir = this.getSwellDir();

            if (currentSwellDir > 0 && previousSwellDir <= 0 && !hasInitiatedCast) {
                if (this.explosionSpell != null) {
                    int spellLevel = Math.max(this.explosionSpell.getMaxLevel() / 2, 1);
                    this.initiateCastSpell(this.explosionSpell, spellLevel);
                    hasInitiatedCast = true;
                }
            }

            if (currentSwellDir < 0 && previousSwellDir > 0 && hasInitiatedCast) {
                this.cancelCast();
                hasInitiatedCast = false;
            }

            if (hasInitiatedCast && castingSpell != null) {
                playerMagicData.handleCastDuration();

                if (playerMagicData.isCasting()) {
                    castingSpell.getSpell().onServerCastTick(level(), castingSpell.getLevel(), this, playerMagicData);
                }

                if (castingSpell != null && castingSpell.getSpell().getCastType() == CastType.CONTINUOUS) {
                    if ((playerMagicData.getCastDurationRemaining() + 1) % 10 == 0) {
                        castingSpell.getSpell().onCast(level(), castingSpell.getLevel(), this, CastSource.MOB, playerMagicData);
                    }
                }
            }

            previousSwellDir = currentSwellDir;
        }

        super.tick();
    }

    @Override
    public void explodeCreeper() {
        if (!this.level().isClientSide && deathCountdown < 0) {
            if (hasInitiatedCast && castingSpell != null) {
                if (castingSpell.getSpell().getCastType() == CastType.LONG
                        || castingSpell.getSpell().getCastType() == CastType.INSTANT) {
                    castingSpell.getSpell().onCast(level(), castingSpell.getLevel(), this, CastSource.MOB, playerMagicData);
                }
                castComplete();
            }

            hasInitiatedCast = false;

            if(!this.isInvisible()){
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F);
            }

            // Hide but keep entity ticking
            this.setInvisible(true);
            this.setNoAi(false);
            this.setInvulnerable(true);
            this.setNoGravity(true);
            this.setSilent(true);

            deathCountdown = 10;
        }
    }

    @Override
    public boolean fireImmune() {
        SchoolType school = getWeakSchool();
        if (school != null && school.getId().equals(SchoolRegistry.FIRE.get().getId())) {
            return true;
        }
        return super.fireImmune();
    }

    @Override
    public int getTicksFrozen() {
        SchoolType school = getWeakSchool();
        if (school != null && school.getId().equals(SchoolRegistry.ICE.get().getId())) {
            return 0;
        }
        return super.getTicksFrozen();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    @Override
    public MagicData getMagicData() {
        return playerMagicData;
    }

    @Override
    public void setSyncedSpellData(SyncedSpellData syncedSpellData) {
        if (!level().isClientSide) {
            return;
        }

        var isCasting = playerMagicData.isCasting();
        playerMagicData.setSyncedData(syncedSpellData);
        castingSpell = playerMagicData.getCastingSpell();

        if (castingSpell == null) {
            return;
        }

        if (!playerMagicData.isCasting() && isCasting) {
            castComplete();
        } else if (playerMagicData.isCasting() && !isCasting) {
            var spell = playerMagicData.getCastingSpell().getSpell();
            initiateCastSpell(spell, playerMagicData.getCastingSpellLevel());

            if (castingSpell.getSpell().getCastType() == CastType.INSTANT) {
                castingSpell.getSpell().onClientPreCast(level(), castingSpell.getLevel(), this, InteractionHand.MAIN_HAND, playerMagicData);
                castComplete();
            }
        }
    }

    @Override
    public boolean isCasting() {
        return playerMagicData.isCasting();
    }

    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (spell == SpellRegistry.none()) {
            castingSpell = null;
            return;
        }

        castingSpell = new SpellData(spell, spellLevel);

        if (!level().isClientSide && !castingSpell.getSpell().checkPreCastConditions(level(), spellLevel, this, playerMagicData)) {
            castingSpell = null;
            return;
        }

        playerMagicData.initiateCast(
                castingSpell.getSpell(),
                castingSpell.getLevel(),
                castingSpell.getSpell().getEffectiveCastTime(castingSpell.getLevel(), this),
                CastSource.MOB,
                SpellSelectionManager.MAINHAND
        );

        if (!level().isClientSide) {
            castingSpell.getSpell().onServerPreCast(level(), castingSpell.getLevel(), this, playerMagicData);
        }
    }

    @Override
    public void cancelCast() {
        if (isCasting()) {
            castComplete();
        }
    }

    @Override
    public void castComplete() {
        if (!level().isClientSide) {
            if (castingSpell != null) {
                castingSpell.getSpell().onServerCastComplete(level(), castingSpell.getLevel(), this, playerMagicData, false);
            }
        } else {
            playerMagicData.resetCastingState();
        }

        castingSpell = null;
    }

    @Override
    public void notifyDangerousProjectile(Projectile projectile) {
    }

    @Override
    public boolean setTeleportLocationBehindTarget(int distance) {
        return false;
    }

    @Override
    public void setBurningDashDirectionData() {
    }

    @Override
    public boolean isDrinkingPotion() {
        return false;
    }

    @Override
    public boolean getHasUsedSingleAttack() {
        return false;
    }

    @Override
    public void setHasUsedSingleAttack(boolean bool) {
    }

    @Override
    public void startDrinkingPotion() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WEAK_SCHOOL, "");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        playerMagicData.getSyncedData().saveNBTData(compound, level().registryAccess());
        compound.putString("weakSchool", this.entityData.get(WEAK_SCHOOL));
        compound.putString("savedSpellId", this.explosionSpell.getSpellId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        var syncedSpellData = new SyncedSpellData(this);
        syncedSpellData.loadNBTData(compound, level().registryAccess());
        playerMagicData.setSyncedData(syncedSpellData);

        if (compound.contains("weakSchool")) {
            String schoolIdString = compound.getString("weakSchool");
            this.entityData.set(WEAK_SCHOOL, schoolIdString);
            this.weakSchool = null;
        }

        if(compound.contains("savedSpellId")){
            this.explosionSpell = SpellRegistry.getSpell(compound.getString("savedSpellId"));
            updateFuseTime();
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
