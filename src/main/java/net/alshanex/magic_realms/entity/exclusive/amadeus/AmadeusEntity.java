package net.alshanex.magic_realms.entity.exclusive.amadeus;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.IExclusiveMercenary;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Hobby;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.PersonalityArchetype;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Quirk;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class AmadeusEntity extends AbstractMercenaryEntity implements IExclusiveMercenary {
    private final String name = "Amadeus Voidwalker";

    public AmadeusEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public AmadeusEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.AMADEUS.get(), level);
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
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = List.of(
                SchoolRegistry.FIRE.get(),
                SchoolRegistry.ENDER.get()
        );
        setMagicSchools(schools);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return List.of(
                SpellRegistry.BURNING_DASH_SPELL.get(),
                SpellRegistry.FLAMING_STRIKE_SPELL.get(),
                SpellRegistry.FIREBALL_SPELL.get(),
                SpellRegistry.HEAT_SURGE_SPELL.get(),
                SpellRegistry.STARFALL_SPELL.get(),
                SpellRegistry.TELEPORT_SPELL.get(),
                SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                SpellRegistry.MAGIC_ARROW_SPELL.get()
        );
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.AMADEUS_FEARS);
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
        return "ui.magic_realms.introduction.amadeus";
    }

    @Override
    protected void initializeDefaultEquipment() {
        super.initializeDefaultEquipment();
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:amadeus",
                () -> new PersonalityInitializer.FixedPersonality(
                        PersonalityArchetype.LOYAL,
                        "magic_realms:favorite_foods/bread",
                        "magic_realms:disliked_foods/gross",
                        "history",
                        "The Voidwalker Estate",
                        64,
                        EnumSet.of(Quirk.BOOKWORM, Quirk.NIGHT_OWL)
                )
        );
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (!this.level().isClientSide && this.getSummoner() != null && this.random.nextFloat() < 0.2f) {
            if(hasContractorNearby(this.getSummoner(), this.level())) {
                getSummoner().sendSystemMessage(Component.translatable("message.magic_realms.amadeus.combat.entering", getExclusiveMercenaryName()).withStyle(ChatFormatting.GOLD));
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
}
