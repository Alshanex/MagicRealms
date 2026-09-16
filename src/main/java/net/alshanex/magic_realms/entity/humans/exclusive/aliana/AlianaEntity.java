package net.alshanex.magic_realms.entity.humans.exclusive.aliana;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.humans.IExclusiveMercenary;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.combat.MRCombatClasses;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.IChatFaceProvider;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Quirk;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.rpgdialogue.murmur.MurmurManager;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class AlianaEntity extends AbstractMercenaryEntity implements IExclusiveMercenary, IChatFaceProvider {
    private final String name = "Aliana";

    private static final ResourceLocation NATURE_LOVER =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "nature_lover");

    public AlianaEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public AlianaEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.ALIANA.get(), level);
        setSummoner(owner);
    }

    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        setGender(Gender.FEMALE);
        setCombatClass(MRCombatClasses.MAGE);
        setEntityName(name);
    }

    @Override
    protected int getInitialStarLevel(RandomSource randomSource) {
        return 3;
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().filter(
                schoolType -> ModTags.isSchoolInTag(schoolType, ModTags.ALIANA_SCHOOLS)
        ).toList();
        setMagicSchools(schools);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(this, randomSource);
        for(AbstractSpell spell : SpellRegistry.getEnabledSpells()){
            if(ModTags.isSpellInTag(spell, ModTags.ALIANA_SPELLS) && !spells.contains(spell)){
                spells.add(spell);
            }
        }
        return spells;
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (!this.level().isClientSide && spell == SpellRegistry.ROOT_SPELL.get()) {
            Component line = Component.translatable("message.magic_realms.aliana.combat.root");
            MurmurManager.speak(this, line, MurmurManager.EARSHOT);
        }
        super.initiateCastSpell(spell, spellLevel);
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.ALIANA_FEARS);
            // Schedule the name update to happen after all initialization is complete
            this.level().getServer().execute(() -> {
                if (this.isAlive() && !this.isRemoved()) {
                    TitleManager.grantSilently(this, List.of(NATURE_LOVER));
                    TitleManager.setDisplayedTitle(this, NATURE_LOVER);
                    this.refreshDisplayName();
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
        return "ui.magic_realms.introduction.aliana";
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:aliana",
                () -> new PersonalityInitializer.FixedPersonality(
                        "cheerful",
                        "gardening",
                        EnumSet.of(Quirk.ANIMAL_FRIEND, Quirk.GLUTTON)
                )
        );
    }

    @Override
    public ItemStack getDefaultVisualArmor(EquipmentSlot slot) {
        if (slot == EquipmentSlot.CHEST) {
            ItemStack robes = new ItemStack(ItemRegistry.WIZARD_CHESTPLATE.get());
            robes.set(DataComponents.DYED_COLOR, new DyedItemColor(0x80C71F, false));
            return robes;
        }
        if (slot == EquipmentSlot.LEGS) {
            ItemStack leggings = new ItemStack(ItemRegistry.WIZARD_LEGGINGS.get());
            leggings.set(DataComponents.DYED_COLOR, new DyedItemColor(0x80C71F, false));
            return leggings;
        }
        return ItemStack.EMPTY;
    }

    private final Map<EquipmentSlot, Boolean> visualArmorStateCache = new EnumMap<>(EquipmentSlot.class);

    @Override
    public Map<EquipmentSlot, Boolean> getVisualArmorStateCache() {
        return this.visualArmorStateCache;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickVisualArmor();
    }

    @Override
    public ResourceLocation getChatFaceTextureCS() {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/aliana.png");
    }
}
