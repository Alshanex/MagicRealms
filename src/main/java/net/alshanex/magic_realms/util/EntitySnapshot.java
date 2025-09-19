package net.alshanex.magic_realms.util;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntitySnapshot {
    public final UUID entityUUID;
    public final String entityName;
    public final Gender gender;
    public final EntityClass entityClass;
    public final int starLevel;
    public final int currentLevel;
    public final int totalKills;
    public final int experiencePoints;
    public final boolean hasShield;
    public final boolean isArcher;
    public final List<String> magicSchools;
    public final CompoundTag attributes;
    public final CompoundTag equipment;
    public final String textureUUID;
    public final String savedTexturePath;
    public final List<String> entitySpells;
    public final EntityType<? extends AbstractMercenaryEntity> entityType; // NEW FIELD

    private EntitySnapshot(UUID entityUUID, String entityName, Gender gender, EntityClass entityClass,
                           int starLevel, int currentLevel, int totalKills, int experiencePoints,
                           boolean hasShield, boolean isArcher, List<String> magicSchools,
                           CompoundTag attributes, CompoundTag equipment, List<String> entitySpells,
                           EntityType<? extends AbstractMercenaryEntity> entityType) {
        this(entityUUID, entityName, gender, entityClass, starLevel, currentLevel, totalKills,
                experiencePoints, hasShield, isArcher, magicSchools, attributes, equipment,
                null, null, entitySpells, entityType);
    }

    public EntitySnapshot(UUID entityUUID, String entityName, Gender gender, EntityClass entityClass,
                          int starLevel, int currentLevel, int totalKills, int experiencePoints,
                          boolean hasShield, boolean isArcher, List<String> magicSchools,
                          CompoundTag attributes, CompoundTag equipment,
                          String textureUUID, String savedTexturePath, List<String> entitySpells,
                          EntityType<? extends AbstractMercenaryEntity> entityType) {
        this.entityUUID = entityUUID;
        this.entityName = entityName;
        this.gender = gender;
        this.entityClass = entityClass;
        this.starLevel = starLevel;
        this.currentLevel = currentLevel;
        this.totalKills = totalKills;
        this.experiencePoints = experiencePoints;
        this.hasShield = hasShield;
        this.isArcher = isArcher;
        this.magicSchools = magicSchools;
        this.attributes = attributes;
        this.equipment = equipment;
        this.textureUUID = textureUUID != null ? textureUUID : entityUUID.toString();
        this.savedTexturePath = savedTexturePath;
        this.entitySpells = entitySpells != null ? new ArrayList<>(entitySpells) : new ArrayList<>();
        this.entityType = entityType;
    }

    public static EntitySnapshot fromEntity(AbstractMercenaryEntity entity) {
        KillTrackerData killData = entity.getData(MRDataAttachments.KILL_TRACKER);

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

        String savedTexturePath = null;

        if(!entity.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) entity.level();
            Path worldDir = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
            Path texturePath = worldDir.resolve("magic_realms_textures")
                    .resolve("entity").resolve("human")
                    .resolve(entity.getUUID() + "_complete.png");
            if (java.nio.file.Files.exists(texturePath)) {
                savedTexturePath = texturePath.toString();
            }
        }

        return new EntitySnapshot(
                entity.getUUID(),
                entity.getEntityName(),
                entity.getGender(),
                entity.getEntityClass(),
                entity.getStarLevel(),
                killData.getCurrentLevel(),
                killData.getTotalKills(),
                killData.getExperiencePoints(),
                entity.hasShield(),
                entity.isArcher(),
                schools,
                attributes,
                equipment,
                entity.getUUID().toString(),
                savedTexturePath,
                spells,
                (EntityType<? extends AbstractMercenaryEntity>) entity.getType() // Capture entity type
        );
    }

    // ... existing captureAttributes and captureEquipment methods remain the same ...

    private static void captureAttributes(AbstractMercenaryEntity entity, CompoundTag attributes) {
        try {
            attributes.putDouble("health", entity.getHealth());
            attributes.putDouble("max_health", entity.getMaxHealth());
            attributes.putDouble("armor", entity.getAttributeValue(Attributes.ARMOR));
            attributes.putDouble("attack_damage", entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
            attributes.putDouble("movement_speed", entity.getAttributeValue(Attributes.MOVEMENT_SPEED));
            attributes.putDouble("knockback_resistance", entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));

            MagicRealms.LOGGER.debug("Captured basic attributes for entity: {}", entity.getEntityName());

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

                MagicRealms.LOGGER.debug("Captured Iron's Spells attributes for entity: {}", entity.getEntityName());

            } catch (Exception e) {
                MagicRealms.LOGGER.debug("Could not capture Iron's Spells attributes: {}", e.getMessage());
            }

            try {
                captureAttributeValue(entity, attributes, ALObjects.Attributes.ARMOR_PIERCE, "armor_pierce", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.ARMOR_SHRED, "armor_shred", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.ARROW_DAMAGE, "arrow_damage", 1.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.ARROW_VELOCITY, "arrow_velocity", 1.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.COLD_DAMAGE, "cold_damage", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.CRIT_CHANCE, "crit_chance", 0.05);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.CRIT_DAMAGE, "crit_damage", 1.5);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.CURRENT_HP_DAMAGE, "current_hp_damage", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.DODGE_CHANCE, "dodge_chance", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.DRAW_SPEED, "draw_speed", 1.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.EXPERIENCE_GAINED, "experience_gained", 1.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.FIRE_DAMAGE, "fire_damage", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.GHOST_HEALTH, "ghost_health", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.HEALING_RECEIVED, "healing_received", 1.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.LIFE_STEAL, "life_steal", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.MINING_SPEED, "mining_speed", 1.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.OVERHEAL, "overheal", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.PROJECTILE_DAMAGE, "projectile_damage", 1.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.PROT_PIERCE, "prot_pierce", 0.0);
                captureAttributeValue(entity, attributes, ALObjects.Attributes.PROT_SHRED, "prot_shred", 0.0);

                MagicRealms.LOGGER.debug("Captured Apothic attributes for entity: {}", entity.getEntityName());

            } catch (Exception e) {
                MagicRealms.LOGGER.debug("Could not capture Apothic attributes: {}", e.getMessage());
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
                MagicRealms.LOGGER.debug("Captured attribute {}: {}", key, value);
            } else {
                attributes.putDouble(key, defaultValue);
                MagicRealms.LOGGER.debug("Used default value for attribute {}: {}", key, defaultValue);
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
            MagicRealms.LOGGER.debug("Captured all attribute modifiers for entity: {}", entity.getEntityName());

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

        if (attributeHolder == null) {
            MagicRealms.LOGGER.debug("Power attribute not found for school {}: {}",
                    school.getId(), powerAttributeId);
        } else {
            MagicRealms.LOGGER.debug("Found power attribute for school {}: {}",
                    school.getId(), powerAttributeId);
        }

        return attributeHolder;
    }

    private static Holder<Attribute> getResistanceAttributeForSchool(SchoolType school) {
        ResourceLocation resistAttributeId = ResourceLocation.fromNamespaceAndPath(
                school.getId().getNamespace(),
                school.getId().getPath() + "_magic_resist"
        );

        var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(resistAttributeId).orElse(null);

        if (attributeHolder == null) {
            MagicRealms.LOGGER.debug("Resistance attribute not found for school {}: {}",
                    school.getId(), resistAttributeId);
        } else {
            MagicRealms.LOGGER.debug("Found resistance attribute for school {}: {}",
                    school.getId(), resistAttributeId);
        }

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
        tag.putInt("current_level", currentLevel);
        tag.putInt("total_kills", totalKills);
        tag.putInt("experience_points", experiencePoints);
        tag.putBoolean("has_shield", hasShield);
        tag.putBoolean("is_archer", isArcher);

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

        tag.put("attributes", attributes);
        tag.put("equipment", equipment);
        tag.putString("texture_uuid", textureUUID);
        if (savedTexturePath != null) {
            tag.putString("saved_texture_path", savedTexturePath);
        }

        // Serialize entity type
        tag.putString("entity_type", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());

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
            int currentLevel = tag.getInt("current_level");
            int totalKills = tag.getInt("total_kills");
            int experiencePoints = tag.getInt("experience_points");
            boolean hasShield = tag.getBoolean("has_shield");
            boolean isArcher = tag.getBoolean("is_archer");

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

            CompoundTag attributes = tag.getCompound("attributes");
            CompoundTag equipment = tag.getCompound("equipment");
            String textureUUID = tag.getString("texture_uuid");
            String savedTexturePath = tag.contains("saved_texture_path") ? tag.getString("saved_texture_path") : null;

            // Deserialize entity type
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
                // Default fallback for old saves
                entityType = (EntityType<? extends AbstractMercenaryEntity>) BuiltInRegistries.ENTITY_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("magic_realms", "human_entity"));
            }

            return new EntitySnapshot(entityUUID, entityName, gender, entityClass, starLevel,
                    currentLevel, totalKills, experiencePoints, hasShield, isArcher,
                    schools, attributes, equipment, textureUUID, savedTexturePath, spells, entityType);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to deserialize EntitySnapshot: {}", e.getMessage());
            return null;
        }
    }
}