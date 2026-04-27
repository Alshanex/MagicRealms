package net.alshanex.magic_realms.entity.exclusive.jara;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class JaraEntity extends AbstractMercenaryEntity implements IExclusiveMercenary, IChatFaceProvider {
    private final String name = "Jara The Shardling";

    public JaraEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public JaraEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.JARA.get(), level);
        setSummoner(owner);
    }

    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        setGender(Gender.FEMALE);
        setEntityClass(EntityClass.ROGUE);
        setEntityName(name);
    }

    @Override
    protected int getInitialStarLevel(RandomSource randomSource) {
        return 3;
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().filter(
                schoolType -> ModTags.isSchoolInTag(schoolType, ModTags.JARA_SCHOOLS)
        ).toList();
        setMagicSchools(schools);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return SpellListGenerator.getSpellsFromTag(ModTags.JARA_SPELLS);
    }

    @Override
    protected void handlePostSpawnInitialization() {
        if (!this.level().isClientSide) {
            this.setImmortal(true);
            setFearedEntityTag(ModTags.JARA_FEARS);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.AMETHYST_RAPIER));
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

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25)
                .add(Attributes.SCALE, 0.85);
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
        return "ui.magic_realms.introduction.jara";
    }

    @Override
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return PersonalityInitializer.FixedPersonality.fromCatalogOrElse(
                "magic_realms:jara",
                () -> new PersonalityInitializer.FixedPersonality(
                        "superstitious",
                        "stargazing",
                        "Furryland",
                        EnumSet.of(Quirk.HATES_RAIN, Quirk.CANT_SWIM)
                )
        );
    }

    @Override
    public ItemStack getDefaultVisualArmor(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            if (ModList.get().isLoaded("magic_of_color")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("magic_of_color", "bedrock_warlock_helmet");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return ItemStack.EMPTY;
        }
        if (slot == EquipmentSlot.CHEST) {
            if (ModList.get().isLoaded("magic_of_color")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("magic_of_color", "bedrock_warlock_robes");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.PALADIN_CHESTPLATE);
        }
        if (slot == EquipmentSlot.LEGS) {
            if (ModList.get().isLoaded("magic_of_color")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("magic_of_color", "bedrock_warlock_leggings");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.PRIEST_LEGGINGS);
        }
        if (slot == EquipmentSlot.FEET) {
            if (ModList.get().isLoaded("magic_of_color")) {
                ResourceLocation itemLoc = ResourceLocation.fromNamespaceAndPath("magic_of_color", "bedrock_warlock_greaves");
                Optional<Item> compatItem = BuiltInRegistries.ITEM.getOptional(itemLoc);

                if (compatItem.isPresent()) {
                    return new ItemStack(compatItem.get());
                }
            }
            return new ItemStack(ItemRegistry.PRIEST_BOOTS);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getChatFaceTextureCS() {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/jara.png");
    }

    @Override
    public List<String> getExclusiveSpeechTranslationKeys() {
        return List.of(
                "message.magic_realms.jara.special_phrase.2"
        );
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        if(item.is(Items.CARROT)){
            item.shrink(1);
            this.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }
}
