package net.alshanex.magic_realms.entity.creeper;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRSoundRegistry;
import net.alshanex.magic_realms.registry.ModLootTables;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
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

    protected void initializeSchool() {
        if (this.getWeakSchool() == null) {
            List<AbstractSpell> allTaggedSpells = new ArrayList<>();

            for (var spell : SpellRegistry.getEnabledSpells()) {
                SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                    if (a.is(ModTags.CREEPER_SPELLS)) {
                        allTaggedSpells.add(spell);
                    }
                });
            }

            if (!allTaggedSpells.isEmpty()) {
                AbstractSpell chosenSpell = allTaggedSpells.get(random.nextInt(allTaggedSpells.size()));
                this.explosionSpell = chosenSpell;
                this.setWeakSchool(chosenSpell.getSchoolType());
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
            updateFuseTime();
            equipWizardHat();
        } else {
            this.entityData.set(WEAK_SCHOOL, "");
        }
    }

    private void equipWizardHat() {
        if (!this.level().isClientSide && this.getWeakSchool() != null) {
            ItemStack hat = new ItemStack(ItemRegistry.WIZARD_HAT.get());

            // Set the clothing variant to "hat"
            hat.set(ComponentRegistry.CLOTHING_VARIANT, "hat");

            // Dye it with the school color
            Vector3f color = this.getWeakSchool().getTargetingColor();
            if (color != null) {
                int dyeColor = ((int)(color.x() * 255) << 16) | ((int)(color.y() * 255) << 8) | (int)(color.z() * 255);
                hat.set(DataComponents.DYED_COLOR, new DyedItemColor(dyeColor, false));
            }

            this.setItemSlot(EquipmentSlot.HEAD, hat);
        }
    }

    private void updateFuseTime() {
        if (this.explosionSpell != null) {
            int spellLevel = Math.max(this.explosionSpell.getMaxLevel() / 2, 1);
            int castTime = this.explosionSpell.getEffectiveCastTime(spellLevel, this);
            this.maxSwell = castTime + 10;
        }
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
                forceLookAtTarget(getTarget());

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
                forceLookAtTarget(getTarget());

                if (castingSpell.getSpell().getCastType() == CastType.LONG
                        || castingSpell.getSpell().getCastType() == CastType.INSTANT) {
                    int spellLevel = this.isPowered() ? castingSpell.getSpell().getMaxLevel() : (castingSpell.getSpell().getMaxLevel() / 2);
                    castingSpell.getSpell().onCast(level(), spellLevel, this, CastSource.MOB, playerMagicData);
                }
                castComplete();
            }

            hasInitiatedCast = false;

            if(!this.isInvisible()){
                playSound(MRSoundRegistry.MAGIC_CREEPER_EXPLOSION.get(), 4.0F, (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F);
                spawnLingeringCloud();
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

    private void forceLookAtTarget(net.minecraft.world.entity.LivingEntity target) {
        if (target != null) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dy = target.getEyeY() - this.getEyeY();
            double dist = Math.sqrt(dx * dx + dz * dz);
            float yRot = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180F / (float) Math.PI)) - 90.0F;
            float xRot = (float) (-(net.minecraft.util.Mth.atan2(dy, dist) * (180F / (float) Math.PI)));
            this.setXRot(xRot % 360);
            this.setYRot(yRot % 360);
            this.yHeadRot = this.getYRot();
            this.yBodyRot = this.getYRot();
        }
    }

    private void spawnLingeringCloud() {
        Collection<MobEffectInstance> collection = this.getActiveEffects();
        if (!collection.isEmpty()) {
            AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            areaeffectcloud.setRadius(2.5F);
            areaeffectcloud.setRadiusOnUse(-0.5F);
            areaeffectcloud.setWaitTime(10);
            areaeffectcloud.setDuration(areaeffectcloud.getDuration() / 2);
            areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / (float)areaeffectcloud.getDuration());

            for (MobEffectInstance mobeffectinstance : collection) {
                areaeffectcloud.addEffect(new MobEffectInstance(mobeffectinstance));
            }

            this.level().addFreshEntity(areaeffectcloud);
        }
    }

    @Override
    protected @NotNull ResourceKey<LootTable> getDefaultLootTable() {
        return ModLootTables.MAGIC_CREEPER_LOOT;
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

        if (getTarget() != null) {
            forceLookAtTarget(getTarget());
        }

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
        if(this.explosionSpell != null){
            compound.putString("savedSpellId", this.explosionSpell.getSpellId());
        }
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
        equipWizardHat();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WEAK_SCHOOL.equals(key)) {
            this.weakSchool = null;
        }
    }
}
