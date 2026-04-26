package net.alshanex.magic_realms.entity.exclusive.ace;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
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
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryMessageFormatter;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Quirk;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class AceEntity extends AbstractMercenaryEntity implements IExclusiveMercenary, IChatFaceProvider {
    private final String name = "Eden";

    public AceEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public AceEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.ACE.get(), level);
        setSummoner(owner);
    }

    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        setGender(Gender.FEMALE);
        setEntityClass(EntityClass.MAGE);
        setEntityName(name);
    }

    @Override
    protected int getInitialStarLevel(RandomSource randomSource) {
        return 3;
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().filter(
                schoolType -> ModTags.isSchoolInTag(schoolType, ModTags.ACE_SCHOOLS)
        ).toList();
        setMagicSchools(schools);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return SpellListGenerator.getSpellsFromTag(ModTags.ACE_SPELLS);
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.ACE_FEARS);
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
        return "ui.magic_realms.introduction.eden";
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:eden",
                () -> new PersonalityInitializer.FixedPersonality(
                        "stoic",
                        "stargazing",
                        "Eldritch Dome",
                        EnumSet.of(Quirk.ANIMAL_FRIEND, Quirk.NIGHT_OWL)
                )
        );
    }

    @Override
    public ItemStack getDefaultVisualArmor(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            if (ModList.get().isLoaded("discerning_the_eldritch")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("discerning_the_eldritch", "eldritch_warlock_helmet");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            if (ModList.get().isLoaded("hazennstuff")) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(ItemRegistry.NETHERITE_MAGE_HELMET);
        }
        if (slot == EquipmentSlot.CHEST) {
            if (ModList.get().isLoaded("discerning_the_eldritch")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("discerning_the_eldritch", "eldritch_warlock_robes");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            if (ModList.get().isLoaded("hazennstuff")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("hazennstuff", "dark_ritual_templar_chestplate");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.NETHERITE_MAGE_CHESTPLATE);
        }
        if (slot == EquipmentSlot.LEGS) {
            if (ModList.get().isLoaded("discerning_the_eldritch")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("discerning_the_eldritch", "eldritch_warlock_leggings");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            if (ModList.get().isLoaded("hazennstuff")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("hazennstuff", "dark_ritual_templar_leggings");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.NETHERITE_MAGE_LEGGINGS);
        }
        if (slot == EquipmentSlot.FEET) {
            if (ModList.get().isLoaded("discerning_the_eldritch")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("discerning_the_eldritch", "eldritch_warlock_greaves");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            if (ModList.get().isLoaded("hazennstuff")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("hazennstuff", "dark_ritual_templar_boots");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.NETHERITE_MAGE_BOOTS);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getChatFaceTextureCS() {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/eden.png");
    }

    @Override
    public List<String> getExclusiveSpeechTranslationKeys() {
        return List.of(
                "message.magic_realms.eden.special_phrase"
        );
    }
}
