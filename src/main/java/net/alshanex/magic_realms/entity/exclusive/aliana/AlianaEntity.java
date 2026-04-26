package net.alshanex.magic_realms.entity.exclusive.aliana;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class AlianaEntity extends AbstractMercenaryEntity implements IExclusiveMercenary, IChatFaceProvider {
    private final String name = "Aliana";

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
        setEntityClass(EntityClass.MAGE);
        setEntityName(name);
    }

    @Override
    protected int getInitialStarLevel(RandomSource randomSource) {
        return 3;
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = List.of(
                SchoolRegistry.NATURE.get()
        );
        setMagicSchools(schools);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(this, randomSource);
        if (!spells.contains(SpellRegistry.ROOT_SPELL.get())) {
            spells.add(SpellRegistry.ROOT_SPELL.get());
        }
        return spells;
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (!this.level().isClientSide && this.getSummoner() != null && spell == SpellRegistry.ROOT_SPELL.get()) {
            if(hasContractorNearby(this.getSummoner(), this.level())) {
                getSummoner().sendSystemMessage(
                        MercenaryMessageFormatter.buildFor(this, "message.magic_realms.aliana.combat.root"));
            }
        }
        super.initiateCastSpell(spell, spellLevel);
    }

    private boolean hasContractorNearby(LivingEntity entity, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                this.getX() - SEARCH_RADIUS,
                this.getY() - SEARCH_RADIUS,
                this.getZ() - SEARCH_RADIUS,
                this.getX() + SEARCH_RADIUS,
                this.getY() + SEARCH_RADIUS,
                this.getZ() + SEARCH_RADIUS
        );

        List<Player> nearbyContractor = level.getEntitiesOfClass(
                Player.class,
                searchArea,
                player1 -> player1.is(entity)
        );

        return !nearbyContractor.isEmpty();
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.ALIANA_FEARS);
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
        return "ui.magic_realms.introduction.aliana";
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:aliana",
                () -> new PersonalityInitializer.FixedPersonality(
                        "cheerful",
                        "gardening",
                        "Brackenfield",
                        EnumSet.of(Quirk.ANIMAL_FRIEND, Quirk.GLUTTON)
                )
        );
    }

    @Override
    protected void initializeDefaultEquipment() {
        ItemStack robes = new ItemStack(ItemRegistry.WIZARD_CHESTPLATE.get());
        robes.set(DataComponents.DYED_COLOR, new DyedItemColor(0x80C71F, false));
        this.setItemSlot(EquipmentSlot.CHEST, robes);

        ItemStack leggings = new ItemStack(ItemRegistry.WIZARD_LEGGINGS.get());
        leggings.set(DataComponents.DYED_COLOR, new DyedItemColor(0x80C71F, false));
        this.setItemSlot(EquipmentSlot.LEGS, leggings);
    }

    @Override
    public ResourceLocation getChatFaceTextureCS() {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/aliana.png");
    }
}
