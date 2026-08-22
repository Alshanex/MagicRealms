package net.alshanex.magic_realms.util.humans.mercenaries;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.TextureComponents;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Hobby;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Quirk;
import net.alshanex.magic_realms.util.humans.titles.Title;
import net.alshanex.magic_realms.util.humans.titles.TitleCatalogHolder;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EntitySnapshot {
    public final UUID entityUUID;
    public final String entityName;
    public final Gender gender;
    public final EntityClass entityClass;
    public final int starLevel;
    /** Ids of every title the mercenary has earned, in catalog (priority) order. */
    public final List<String> earnedTitles;
    /** Id of the title currently shown on the nameplate, or null when none is shown. */
    public final String displayedTitle;
    public final boolean hasShield;
    public final boolean isArcher;
    public final List<String> magicSchools;
    public final CompoundTag attributes;
    public final CompoundTag equipment;
    public final List<String> entitySpells;
    public final EntityType<? extends AbstractMercenaryEntity> entityType;
    public final CompoundTag textureComponents;
    public final String archetypeId;
    public final String hobbyId;
    public final List<String> quirkIds;

    public EntitySnapshot(UUID entityUUID, String entityName, Gender gender, EntityClass entityClass,
                          int starLevel, List<String> earnedTitles, String displayedTitle,
                          boolean hasShield, boolean isArcher, List<String> magicSchools,
                          CompoundTag attributes, CompoundTag equipment, List<String> entitySpells,
                          String archetypeId, String hobbyId, List<String> quirkIds,
                          EntityType<? extends AbstractMercenaryEntity> entityType,
                          CompoundTag textureComponents) {
        this.entityUUID = entityUUID;
        this.entityName = entityName;
        this.gender = gender;
        this.entityClass = entityClass;
        this.starLevel = starLevel;
        this.earnedTitles = earnedTitles != null ? new ArrayList<>(earnedTitles) : new ArrayList<>();
        this.displayedTitle = displayedTitle;
        this.hasShield = hasShield;
        this.isArcher = isArcher;
        this.magicSchools = magicSchools;
        this.attributes = attributes;
        this.equipment = equipment;
        this.entitySpells = entitySpells != null ? new ArrayList<>(entitySpells) : new ArrayList<>();
        this.archetypeId = archetypeId;
        this.hobbyId = hobbyId;
        this.quirkIds = quirkIds != null ? new ArrayList<>(quirkIds) : new ArrayList<>();
        this.entityType = entityType;
        this.textureComponents = textureComponents;
    }

    public List<Title> resolveTitles(boolean clientSide) {
        if (earnedTitles.isEmpty()) return List.of();

        List<ResourceLocation> ids = new ArrayList<>(earnedTitles.size());
        for (String raw : earnedTitles) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id != null) ids.add(id);
        }
        return TitleCatalogHolder.get(clientSide).resolve(ids);
    }

    public Title resolveDisplayedTitle(boolean clientSide) {
        if (displayedTitle == null || displayedTitle.isEmpty()) return null;
        ResourceLocation id = ResourceLocation.tryParse(displayedTitle);
        return id == null ? null : TitleCatalogHolder.get(clientSide).byId(id);
    }

    public boolean hasTitles() {
        return !earnedTitles.isEmpty();
    }

    public static EntitySnapshot fromEntity(AbstractMercenaryEntity entity) {
        List<String> schools = entity.getMagicSchools().stream()
                .map(school -> school.getId().toString())
                .toList();

        List<String> spells = entity.getPersistedSpells().stream()
                .map(spell -> spell.getSpellName())
                .toList();

        CompoundTag attributes = new CompoundTag();
        captureAttributes(entity, attributes);

        CompoundTag equipment = new CompoundTag();
        captureEquipment(entity, equipment);

        CompoundTag textureComponents = null;
        if (entity instanceof RandomHumanEntity randomHuman) {
            // Get texture metadata instead of full components
            CompoundTag metadata = randomHuman.getTextureMetadata();
            if (metadata != null && !metadata.isEmpty()) {
                textureComponents = metadata.copy();
            } else {
                // Fallback: try to get actual texture components if they exist
                TextureComponents components = randomHuman.getTextureComponents();
                if (components != null) {
                    textureComponents = components.toNBT();
                }
            }
        }

        // Capture titles (catalog may be empty on a fresh world — snapshot whatever is there)
        List<String> earnedTitles = new ArrayList<>();
        String displayedTitle = null;
        try {
            for (Title title : TitleManager.earnedTitles(entity)) {
                earnedTitles.add(title.id().toString());
            }
            Title shown = TitleManager.displayedTitle(entity);
            if (shown != null) displayedTitle = shown.id().toString();
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not capture title data for snapshot: {}", e.getMessage());
        }

        // Capture personality (may be null/uninitialized — we still snapshot what's there)
        String archetypeId = null;
        String hobbyId = null;
        List<String> quirkIds = new ArrayList<>();
        try {
            PersonalityData personality = entity.getData(MRDataAttachments.PERSONALITY);
            if (personality != null && personality.isInitialized()) {
                archetypeId = personality.getArchetypeId();
                Hobby hobby = personality.getHobby(false); // server-side lookup
                hobbyId = hobby != null ? hobby.id() : null;
                Set<Quirk> quirks = personality.getQuirks();
                if (quirks != null) {
                    for (Quirk q : quirks) quirkIds.add(q.getId());
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not capture personality data for snapshot: {}", e.getMessage());
        }

        return new EntitySnapshot(
                entity.getUUID(),
                entity.getEntityName(),
                entity.getGender(),
                entity.getEntityClass(),
                entity.getStarLevel(),
                earnedTitles,
                displayedTitle,
                entity.hasShield(),
                entity.isArcher(),
                schools,
                attributes,
                equipment,
                spells,
                archetypeId, hobbyId, quirkIds,
                (EntityType<? extends AbstractMercenaryEntity>) entity.getType(),
                textureComponents);
    }

    private static void captureAttributes(AbstractMercenaryEntity entity, CompoundTag attributes) {
        try {
            attributes.putDouble("health", entity.getHealth());
            attributes.putDouble("max_health", entity.getMaxHealth());
            attributes.putDouble("armor", entity.getAttributeValue(Attributes.ARMOR));
            attributes.putDouble("attack_damage", entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
            attributes.putDouble("movement_speed", entity.getAttributeValue(Attributes.MOVEMENT_SPEED));
            attributes.putDouble("knockback_resistance", entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            captureAttributeValue(entity, attributes, Attributes.ARMOR_TOUGHNESS, "armor_toughness", 0.0);
            captureAttributeValue(entity, attributes, Attributes.ATTACK_SPEED, "attack_speed", 4.0);
            captureAttributeValue(entity, attributes, Attributes.ATTACK_KNOCKBACK, "attack_knockback", 0.0);
            captureAttributeValue(entity, attributes, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, "explosion_knockback_resistance", 0.0);
            captureAttributeValue(entity, attributes, Attributes.MAX_ABSORPTION, "max_absorption", 0.0);
            captureAttributeValue(entity, attributes, Attributes.SAFE_FALL_DISTANCE, "safe_fall_distance", 3.0);
            captureAttributeValue(entity, attributes, Attributes.FALL_DAMAGE_MULTIPLIER, "fall_damage_multiplier", 1.0);
            captureAttributeValue(entity, attributes, Attributes.BURNING_TIME, "burning_time", 1.0);
            captureAttributeValue(entity, attributes, Attributes.OXYGEN_BONUS, "oxygen_bonus", 0.0);
            captureAttributeValue(entity, attributes, Attributes.WATER_MOVEMENT_EFFICIENCY, "water_movement_efficiency", 0.0);
            captureAttributeValue(entity, attributes, Attributes.MOVEMENT_EFFICIENCY, "movement_efficiency", 0.0);
            captureAttributeValue(entity, attributes, Attributes.STEP_HEIGHT, "step_height", 0.6);
            captureAttributeValue(entity, attributes, Attributes.SCALE, "scale", 1.0);
            captureAttributeValue(entity, attributes, Attributes.FOLLOW_RANGE, "follow_range", 32.0);

            try {
                captureAttributeValue(entity, attributes, AttributeRegistry.MAX_MANA, "max_mana", 100.0);
                captureAttributeValue(entity, attributes, AttributeRegistry.MANA_REGEN, "mana_regen", 1.0);
                captureAttributeValue(entity, attributes, AttributeRegistry.SPELL_POWER, "spell_power", 1.0);
                captureAttributeValue(entity, attributes, AttributeRegistry.SPELL_RESIST, "spell_resist", 1.0);
                captureAttributeValue(entity, attributes, AttributeRegistry.COOLDOWN_REDUCTION, "cooldown_reduction", 1.0);
                captureAttributeValue(entity, attributes, AttributeRegistry.CAST_TIME_REDUCTION, "cast_time_reduction", 1.0);
                captureAttributeValue(entity, attributes, AttributeRegistry.CASTING_MOVESPEED, "casting_movespeed", 1.0);
                captureAttributeValue(entity, attributes, AttributeRegistry.SUMMON_DAMAGE, "summon_damage", 1.0);

                List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
                for (SchoolType school : schools) {
                    Holder<Attribute> resistanceAttribute = getResistanceAttributeForSchool(school);
                    if (resistanceAttribute != null) {
                        String resistKey = school.getId().getPath() + "_magic_resist";
                        captureAttributeValue(entity, attributes, resistanceAttribute, resistKey, 1.0);
                    }

                    Holder<Attribute> powerAttribute = getPowerAttributeForSchool(school);
                    if (powerAttribute != null) {
                        String powerKey = school.getId().getPath() + "_spell_power";
                        captureAttributeValue(entity, attributes, powerAttribute, powerKey, 1.0);
                    }
                }

            } catch (Exception e) {
                MagicRealms.LOGGER.debug("Could not capture Iron's Spells attributes: {}", e.getMessage());
            }

            captureAllAttributeModifiers(entity, attributes);

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error capturing attributes for entity {}: {}", entity.getEntityName(), e.getMessage());
        }
    }

    private static void captureAttributeValue(AbstractMercenaryEntity entity, CompoundTag attributes, Holder<Attribute> attributeHolder, String key, double defaultValue) {
        try {
            AttributeInstance instance = entity.getAttribute(attributeHolder);
            if (instance != null) {
                double value = instance.getValue();
                attributes.putDouble(key, value);
            } else {
                attributes.putDouble(key, defaultValue);
            }
        } catch (Exception e) {
            attributes.putDouble(key, defaultValue);
            MagicRealms.LOGGER.debug("Error capturing attribute {}, using default: {}", key, defaultValue);
        }
    }

    private static void captureAllAttributeModifiers(AbstractMercenaryEntity entity, CompoundTag attributes) {
        try {
            CompoundTag modifiersTag = new CompoundTag();

            for (AttributeInstance instance : entity.getAttributes().getSyncableAttributes()) {
                Attribute attribute = instance.getAttribute().value();
                String attributeName = BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString();

                CompoundTag attributeModifiers = new CompoundTag();
                attributeModifiers.putDouble("base_value", instance.getBaseValue());
                attributeModifiers.putDouble("current_value", instance.getValue());

                ListTag modifiersList = new ListTag();
                for (AttributeModifier modifier : instance.getModifiers()) {
                    CompoundTag modifierTag = new CompoundTag();
                    modifierTag.putString("id", modifier.id().toString());
                    modifierTag.putDouble("amount", modifier.amount());
                    modifierTag.putString("operation", modifier.operation().toString());
                    modifiersList.add(modifierTag);
                }
                attributeModifiers.put("modifiers", modifiersList);

                modifiersTag.put(attributeName, attributeModifiers);
            }

            attributes.put("all_attribute_modifiers", modifiersTag);

        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not capture attribute modifiers: {}", e.getMessage());
        }
    }

    private static Holder<Attribute> getPowerAttributeForSchool(SchoolType school) {
        ResourceLocation powerAttributeId = ResourceLocation.fromNamespaceAndPath(
                school.getId().getNamespace(),
                school.getId().getPath() + "_spell_power"
        );

        var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(powerAttributeId).orElse(null);

        return attributeHolder;
    }

    private static Holder<Attribute> getResistanceAttributeForSchool(SchoolType school) {
        ResourceLocation resistAttributeId = ResourceLocation.fromNamespaceAndPath(
                school.getId().getNamespace(),
                school.getId().getPath() + "_magic_resist"
        );

        var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(resistAttributeId).orElse(null);

        return attributeHolder;
    }

    private static void captureEquipment(AbstractMercenaryEntity entity, CompoundTag equipment) {
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!mainHandItem.isEmpty()) {
            CompoundTag mainHand = new CompoundTag();
            mainHandItem.save(entity.level().registryAccess(), mainHand);
            equipment.put("main_hand", mainHand);
        }

        ItemStack offHandItem = entity.getOffhandItem();
        if (!offHandItem.isEmpty()) {
            CompoundTag offHand = new CompoundTag();
            offHandItem.save(entity.level().registryAccess(), offHand);
            equipment.put("off_hand", offHand);
        }

        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!headItem.isEmpty()) {
            CompoundTag head = new CompoundTag();
            headItem.save(entity.level().registryAccess(), head);
            equipment.put("head", head);
        }

        ItemStack chestItem = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestItem.isEmpty()) {
            CompoundTag chest = new CompoundTag();
            chestItem.save(entity.level().registryAccess(), chest);
            equipment.put("chest", chest);
        }

        ItemStack legsItem = entity.getItemBySlot(EquipmentSlot.LEGS);
        if (!legsItem.isEmpty()) {
            CompoundTag legs = new CompoundTag();
            legsItem.save(entity.level().registryAccess(), legs);
            equipment.put("legs", legs);
        }

        ItemStack bootsItem = entity.getItemBySlot(EquipmentSlot.FEET);
        if (!bootsItem.isEmpty()) {
            CompoundTag boots = new CompoundTag();
            bootsItem.save(entity.level().registryAccess(), boots);
            equipment.put("boots", boots);
        }
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("entity_uuid", entityUUID.toString());
        tag.putString("entity_name", entityName);
        tag.putString("gender", gender.getName());
        tag.putString("entity_class", entityClass.getName());
        tag.putInt("star_level", starLevel);
        tag.putBoolean("has_shield", hasShield);
        tag.putBoolean("is_archer", isArcher);

        if (!earnedTitles.isEmpty()) {
            ListTag titlesTag = new ListTag();
            for (String title : earnedTitles) {
                titlesTag.add(StringTag.valueOf(title));
            }
            tag.put("earned_titles", titlesTag);
        }
        if (displayedTitle != null && !displayedTitle.isEmpty()) {
            tag.putString("displayed_title", displayedTitle);
        }

        ListTag schoolsTag = new ListTag();
        for (String school : magicSchools) {
            schoolsTag.add(StringTag.valueOf(school));
        }
        tag.put("magic_schools", schoolsTag);

        ListTag spellsTag = new ListTag();
        for (String spell : entitySpells) {
            spellsTag.add(StringTag.valueOf(spell));
        }
        tag.put("entity_spells", spellsTag);

        if (archetypeId != null && !archetypeId.isEmpty()) {
            tag.putString("archetype_id", archetypeId);
        }
        if (hobbyId != null && !hobbyId.isEmpty()) {
            tag.putString("hobby_id", hobbyId);
        }
        if (quirkIds != null && !quirkIds.isEmpty()) {
            ListTag quirksTag = new ListTag();
            for (String q : quirkIds) quirksTag.add(StringTag.valueOf(q));
            tag.put("quirk_ids", quirksTag);
        }

        tag.put("attributes", attributes);
        tag.put("equipment", equipment);

        tag.putString("entity_type", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());

        if (textureComponents != null) {
            tag.put("texture_components", textureComponents);
        }

        return tag;
    }

    @SuppressWarnings("unchecked")
    public static EntitySnapshot deserialize(CompoundTag tag) {
        try {
            UUID entityUUID = UUID.fromString(tag.getString("entity_uuid"));
            String entityName = tag.getString("entity_name");
            Gender gender = Gender.valueOf(tag.getString("gender").toUpperCase());
            EntityClass entityClass = EntityClass.valueOf(tag.getString("entity_class").toUpperCase());
            int starLevel = tag.getInt("star_level");
            boolean hasShield = tag.getBoolean("has_shield");
            boolean isArcher = tag.getBoolean("is_archer");

            List<String> earnedTitles = new java.util.ArrayList<>();
            if (tag.contains("earned_titles")) {
                ListTag titlesTag = tag.getList("earned_titles", 8);
                for (int i = 0; i < titlesTag.size(); i++) {
                    String t = titlesTag.getString(i);
                    if (!t.isEmpty()) earnedTitles.add(t);
                }
            }
            String displayedTitle = tag.contains("displayed_title") ? tag.getString("displayed_title") : null;
            if (displayedTitle != null && displayedTitle.isEmpty()) displayedTitle = null;

            List<String> schools = new java.util.ArrayList<>();
            ListTag schoolsTag = tag.getList("magic_schools", 8);
            for (int i = 0; i < schoolsTag.size(); i++) {
                schools.add(schoolsTag.getString(i));
            }

            List<String> spells = new java.util.ArrayList<>();
            if (tag.contains("entity_spells")) {
                ListTag spellsTag = tag.getList("entity_spells", 8);
                for (int i = 0; i < spellsTag.size(); i++) {
                    spells.add(spellsTag.getString(i));
                }
            }

            String archetypeId = tag.contains("archetype_id") ? tag.getString("archetype_id") : null;
            if (archetypeId != null && archetypeId.isEmpty()) archetypeId = null;
            String hobbyId = tag.contains("hobby_id") ? tag.getString("hobby_id") : null;
            if (hobbyId != null && hobbyId.isEmpty()) hobbyId = null;
            List<String> quirkIds = new java.util.ArrayList<>();
            if (tag.contains("quirk_ids")) {
                ListTag quirksTag = tag.getList("quirk_ids", 8);
                for (int i = 0; i < quirksTag.size(); i++) {
                    String q = quirksTag.getString(i);
                    if (!q.isEmpty()) quirkIds.add(q);
                }
            }

            CompoundTag attributes = tag.getCompound("attributes");
            CompoundTag equipment = tag.getCompound("equipment");

            EntityType<? extends AbstractMercenaryEntity> entityType;
            if (tag.contains("entity_type")) {
                try {
                    ResourceLocation entityTypeId = ResourceLocation.parse(tag.getString("entity_type"));
                    entityType = (EntityType<? extends AbstractMercenaryEntity>) BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Failed to deserialize entity type, using default: {}", e.getMessage());
                    entityType = (EntityType<? extends AbstractMercenaryEntity>) BuiltInRegistries.ENTITY_TYPE.get(
                            ResourceLocation.fromNamespaceAndPath("magic_realms", "human_entity"));
                }
            } else {
                // Default fallback
                entityType = (EntityType<? extends AbstractMercenaryEntity>) BuiltInRegistries.ENTITY_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("magic_realms", "human_entity"));
            }

            CompoundTag textureComponents = tag.contains("texture_components") ?
                    tag.getCompound("texture_components") : null;

            return new EntitySnapshot(entityUUID, entityName, gender, entityClass, starLevel,
                    earnedTitles, displayedTitle, hasShield, isArcher,
                    schools, attributes, equipment, spells,
                    archetypeId, hobbyId, quirkIds,
                    entityType, textureComponents);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to deserialize EntitySnapshot: {}", e.getMessage());
            return null;
        }
    }
}