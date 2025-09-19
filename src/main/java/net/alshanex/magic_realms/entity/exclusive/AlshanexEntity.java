package net.alshanex.magic_realms.entity.exclusive;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ContractUtils;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class AlshanexEntity extends AbstractMercenaryEntity {
    public AlshanexEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public AlshanexEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.ALSHANEX.get(), level);
        setSummoner(owner);
    }

    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        setGender(Gender.MALE);
        setEntityClass(EntityClass.MAGE);
        setEntityName("Alshanex");
    }

    @Override
    protected int getInitialStarLevel(RandomSource randomSource) {
        return 3;
    }

    @Override
    protected void initializeFearedEntity(RandomSource randomSource) {
        setFearedEntity(EntityRegistry.ICE_SPIDER.get());
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return List.of(
                SpellRegistry.SUNBEAM_SPELL.get(),
                SpellRegistry.EVASION_SPELL.get()
        );
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = List.of(
                SchoolRegistry.HOLY.get(),
                SchoolRegistry.ENDER.get()
        );
        setMagicSchools(schools);
    }

    @Override
    protected void handleContractInteraction(Player player, ContractData contractData, ItemStack heldItem) {
        if (heldItem.getItem() instanceof PermanentContractItem) {
            ContractUtils.handlePermanentContractCreation(player, this, contractData, heldItem);
        } else if (heldItem.getItem() instanceof TieredContractItem tieredContract) {
            ContractUtils.handleTieredContractCreation(player, this, contractData, heldItem, tieredContract);
        } else {
            ContractUtils.handleContractInteraction(player, this, contractData);
        }
    }

    @Override
    protected void handlePostSpawnInitialization() {

    }

    @Override
    protected void handleAppearanceSpecificTick() {

    }

    @Override
    protected boolean isExclusiveMercenary() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
    }
}
