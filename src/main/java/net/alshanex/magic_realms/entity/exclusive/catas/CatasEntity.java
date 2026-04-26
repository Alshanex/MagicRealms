package net.alshanex.magic_realms.entity.exclusive.catas;

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
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryMessageFormatter;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Quirk;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;

public class CatasEntity extends AbstractMercenaryEntity implements IExclusiveMercenary, IChatFaceProvider {
    private final String name = "Catas";

    public CatasEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public CatasEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.CATAS.get(), level);
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
                SchoolRegistry.HOLY.get(),
                SchoolRegistry.FIRE.get(),
                SchoolRegistry.LIGHTNING.get()
        );
        setMagicSchools(schools);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return SpellListGenerator.getSpellsFromTag(ModTags.CATAS_SPELLS);
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.CATAS_FEARS);
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
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if(heldItem.is(Items.PUMPKIN_PIE)){
            if(!this.level().isClientSide) {
                player.sendSystemMessage(
                        MercenaryMessageFormatter.buildFor(this, "message.magic_realms.catas.pumpkin_pie.thanks"));
                heldItem.shrink(1);
                playSound(SoundEvents.GENERIC_EAT, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            } else {
                spawnParticles();
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    protected void spawnParticles() {
        ParticleOptions particleoptions = ParticleTypes.HEART;

        for (int i = 0; i < 7; i++) {
            double d0 = this.random.nextGaussian() * 0.02;
            double d1 = this.random.nextGaussian() * 0.02;
            double d2 = this.random.nextGaussian() * 0.02;
            this.level().addParticle(particleoptions, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
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
        return "ui.magic_realms.introduction.catas";
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:catas",
                () -> new PersonalityInitializer.FixedPersonality(
                        "scholarly",
                        "history",
                        "Stonewick",
                        EnumSet.of(Quirk.BOOKWORM, Quirk.ANIMAL_FRIEND)
                )
        );
    }

    @Override
    public List<String> getExclusiveSpeechTranslationKeys() {
        return List.of(
                "message.magic_realms.catas.moth.response",
                "message.magic_realms.catas.geology.response"
        );
    }

    @Override
    protected void initializeDefaultEquipment() {
        ItemStack robes = new ItemStack(ItemRegistry.PRIEST_CHESTPLATE.get());
        this.setItemSlot(EquipmentSlot.CHEST, robes);

        ItemStack leggings = new ItemStack(ItemRegistry.PRIEST_LEGGINGS.get());
        this.setItemSlot(EquipmentSlot.LEGS, leggings);

        ItemStack boots = new ItemStack(ItemRegistry.PRIEST_BOOTS.get());
        this.setItemSlot(EquipmentSlot.FEET, boots);
    }

    @Override
    public ResourceLocation getChatFaceTextureCS() {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/catas.png");
    }
}
