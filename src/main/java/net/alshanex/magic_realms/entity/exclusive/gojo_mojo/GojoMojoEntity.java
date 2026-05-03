package net.alshanex.magic_realms.entity.exclusive.gojo_mojo;

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
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Quirk;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class GojoMojoEntity extends AbstractMercenaryEntity implements IExclusiveMercenary, IChatFaceProvider {
    private final String name = "GojoMojo";

    public GojoMojoEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public GojoMojoEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.GOJO_MOJO.get(), level);
        setSummoner(owner);
    }

    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        setGender(Gender.MALE);
        setEntityClass(EntityClass.WARRIOR);
        setEntityName(name);
        this.setHasShield(true);
    }

    @Override
    protected int getInitialStarLevel(RandomSource randomSource) {
        return 3;
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().filter(
                schoolType -> ModTags.isSchoolInTag(schoolType, ModTags.GOJO_MOJO_SCHOOLS)
        ).toList();
        setMagicSchools(schools);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(this, randomSource);
        for(AbstractSpell spell : SpellRegistry.getEnabledSpells()){
            if(ModTags.isSpellInTag(spell, ModTags.GOJO_MOJO_SPELLS) && !spells.contains(spell)){
                spells.add(spell);
            }
        }
        return spells;
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.GOJO_MOJO_FEARS);
            ItemStack maceStack = new ItemStack(Items.MACE);
            maceStack.set(DataComponents.MAX_DAMAGE, 1);
            this.setItemSlot(EquipmentSlot.MAINHAND, maceStack);
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
        return "ui.magic_realms.introduction.gojo_mojo";
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:gojo_mojo",
                () -> new PersonalityInitializer.FixedPersonality(
                        "pious",
                        "gambling",
                        "My House",
                        EnumSet.of(Quirk.CLAUSTROPHOBIC, Quirk.HEAT_INTOLERANT)
                )
        );
    }

    @Override
    public ItemStack getDefaultVisualArmor(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            ItemStack item = new ItemStack(ItemRegistry.NETHERITE_MAGE_HELMET);
            item.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3B7BC2, false));

            return item;
        }
        if (slot == EquipmentSlot.CHEST) {
            ItemStack item = new ItemStack(ItemRegistry.NETHERITE_MAGE_CHESTPLATE);
            item.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3B7BC2, false));

            return item;
        }
        if (slot == EquipmentSlot.LEGS) {
            ItemStack item = new ItemStack(ItemRegistry.NETHERITE_MAGE_LEGGINGS);
            item.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3B7BC2, false));

            return item;
        }
        if (slot == EquipmentSlot.FEET) {
            ItemStack item = new ItemStack(ItemRegistry.NETHERITE_MAGE_BOOTS);
            item.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3B7BC2, false));

            return item;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getChatFaceTextureCS() {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/gojo_mojo.png");
    }

    @Override
    public List<String> getExclusiveSpeechTranslationKeys() {
        return List.of(
                "message.magic_realms.gojo_mojo.special_phrase.1"
        );
    }
}
