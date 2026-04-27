package net.alshanex.magic_realms.entity.exclusive.alshanex;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.IExclusiveMercenary;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.IChatFaceProvider;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Quirk;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class AlshanexEntity extends AbstractMercenaryEntity implements IExclusiveMercenary, IChatFaceProvider {
    private final String name = "Alshanex";

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
        setEntityName(name);
    }

    @Override
    protected int getInitialStarLevel(RandomSource randomSource) {
        return 3;
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return SpellListGenerator.getSpellsFromTag(ModTags.ALSHANEX_SPELLS);
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().filter(
                schoolType -> ModTags.isSchoolInTag(schoolType, ModTags.ALSHANEX_SCHOOLS)
        ).toList();
        setMagicSchools(schools);
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.ALSHANEX_FEARS);
            // Schedule the name update to happen after all initialization is complete
            this.level().getServer().execute(() -> {
                if (this.isAlive() && !this.isRemoved()) {
                    KillTrackerData killData = this.getData(MRDataAttachments.KILL_TRACKER);
                    int currentLevel = killData.getCurrentLevel();
                    this.updateCustomNameWithLevel(currentLevel);
                }
            });
        }
    }

    @Override
    public boolean isExclusiveMercenary() {
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

    @Override
    public String getExclusiveMercenaryName() {
        return name;
    }

    @Override
    public String getExclusiveMercenaryPresentationMessage() {
        return "ui.magic_realms.introduction.alshanex";
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:alshanex",
                () -> new PersonalityInitializer.FixedPersonality(
                        "jovial",
                        "music",
                        "Somewhere only we know",
                        EnumSet.of(Quirk.AFRAID_OF_THE_DARK)
                )
        );
    }

    @Override
    public ResourceLocation getChatFaceTextureCS() {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/alshanex.png");
    }

    @Override
    public ItemStack getDefaultVisualArmor(EquipmentSlot slot) {
        if (slot == EquipmentSlot.CHEST) {
            if (ModList.get().isLoaded("hazennstuff")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("hazennstuff", "gabriel_ultrakill_chestplate");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.SHADOWWALKER_CHESTPLATE);
        }
        if (slot == EquipmentSlot.LEGS) {
            if (ModList.get().isLoaded("hazennstuff")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("hazennstuff", "gabriel_ultrakill_leggings");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.SHADOWWALKER_LEGGINGS);
        }
        if (slot == EquipmentSlot.FEET) {
            if (ModList.get().isLoaded("hazennstuff")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("hazennstuff", "gabriel_ultrakill_boots");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.SHADOWWALKER_BOOTS);
        }
        return ItemStack.EMPTY;
    }
}
