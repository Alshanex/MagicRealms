package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;

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
    public final CompoundTag traits;
    public final CompoundTag equipment;
    public final String textureUUID;
    public final String savedTexturePath;

    private EntitySnapshot(UUID entityUUID, String entityName, Gender gender, EntityClass entityClass,
                           int starLevel, int currentLevel, int totalKills, int experiencePoints,
                           boolean hasShield, boolean isArcher, List<String> magicSchools,
                           CompoundTag attributes, CompoundTag traits, CompoundTag equipment) {
        this(entityUUID, entityName, gender, entityClass, starLevel, currentLevel, totalKills,
                experiencePoints, hasShield, isArcher, magicSchools, attributes, traits, equipment, null, null);
    }

    public EntitySnapshot(UUID entityUUID, String entityName, Gender gender, EntityClass entityClass,
                          int starLevel, int currentLevel, int totalKills, int experiencePoints,
                          boolean hasShield, boolean isArcher, List<String> magicSchools,
                          CompoundTag attributes, CompoundTag traits, CompoundTag equipment,
                          String textureUUID, String savedTexturePath) {
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
        this.traits = traits;
        this.equipment = equipment;
        this.textureUUID = textureUUID != null ? textureUUID : entityUUID.toString();
        this.savedTexturePath = savedTexturePath;
    }

    public static EntitySnapshot fromEntity(RandomHumanEntity entity) {
        KillTrackerData killData = entity.getData(MRDataAttachments.KILL_TRACKER);

        List<String> schools = entity.getMagicSchools().stream()
                .map(school -> school.getId().toString())
                .toList();

        // Capturar atributos actuales
        CompoundTag attributes = new CompoundTag();
        captureAttributes(entity, attributes);

        // Capturar traits actuales
        CompoundTag traits = new CompoundTag();
        captureTraits(entity, traits);

        // Capturar equipamiento
        CompoundTag equipment = new CompoundTag();
        captureEquipment(entity, equipment);

        // Intentar capturar la ruta de textura guardada
        String savedTexturePath = null;
        try {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                // Verificar si existe una textura guardada para esta entidad
                java.nio.file.Path gameDir = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath();
                java.nio.file.Path textureDir = gameDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
                java.nio.file.Path texturePath = textureDir.resolve(entity.getUUID().toString() + "_complete.png");
                if (java.nio.file.Files.exists(texturePath)) {
                    savedTexturePath = texturePath.toString();
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not check for saved texture: {}", e.getMessage());
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
                traits,
                equipment,
                entity.getUUID().toString(),
                savedTexturePath
        );
    }

    private static void captureAttributes(RandomHumanEntity entity, CompoundTag attributes) {
        // Capturar atributos básicos
        attributes.putDouble("health", entity.getHealth());
        attributes.putDouble("max_health", entity.getMaxHealth());
        attributes.putDouble("armor", entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR));
        attributes.putDouble("attack_damage", entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));

        try {
            // Iron's Spells attributes
            if (entity.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA) != null) {
                attributes.putDouble("max_mana", entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA));
            }
            if (entity.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN) != null) {
                attributes.putDouble("mana_regen", entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN));
            }
            if (entity.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER) != null) {
                attributes.putDouble("spell_power", entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER));
            }
            if (entity.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_RESIST) != null) {
                attributes.putDouble("spell_resist", entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_RESIST));
            }
            if (entity.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION) != null) {
                attributes.putDouble("cooldown_reduction", entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION));
            }
            if (entity.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED) != null) {
                attributes.putDouble("casting_movespeed", entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED));
            }
            if (entity.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE) != null) {
                attributes.putDouble("summon_damage", entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE));
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not capture Iron's Spells attributes: {}", e.getMessage());
        }

        try {
            // Apothic attributes
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.CRIT_CHANCE) != null) {
                attributes.putDouble("crit_chance", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.CRIT_CHANCE));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.CRIT_DAMAGE) != null) {
                attributes.putDouble("crit_damage", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.CRIT_DAMAGE));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.DODGE_CHANCE) != null) {
                attributes.putDouble("dodge_chance", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.DODGE_CHANCE));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARMOR_SHRED) != null) {
                attributes.putDouble("armor_shred", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARMOR_SHRED));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARMOR_PIERCE) != null) {
                attributes.putDouble("armor_pierce", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARMOR_PIERCE));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.LIFE_STEAL) != null) {
                attributes.putDouble("life_steal", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.LIFE_STEAL));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARROW_DAMAGE) != null) {
                attributes.putDouble("arrow_damage", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARROW_DAMAGE));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARROW_VELOCITY) != null) {
                attributes.putDouble("arrow_velocity", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.ARROW_VELOCITY));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.DRAW_SPEED) != null) {
                attributes.putDouble("draw_speed", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.DRAW_SPEED));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.GHOST_HEALTH) != null) {
                attributes.putDouble("ghost_health", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.GHOST_HEALTH));
            }
            if (entity.getAttribute(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.OVERHEAL) != null) {
                attributes.putDouble("overheal", entity.getAttributeValue(dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.OVERHEAL));
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not capture Apothic attributes: {}", e.getMessage());
        }
    }

    private static void captureTraits(RandomHumanEntity entity, CompoundTag traits) {
        try {
            dev.xkmc.l2hostility.content.capability.mob.MobTraitCap cap =
                    dev.xkmc.l2hostility.init.registrate.LHMiscs.MOB.type().getOrCreate(entity);
            if (cap != null) {
                ListTag traitList = new ListTag();
                cap.traitEvent((trait, level) -> {
                    CompoundTag traitTag = new CompoundTag();
                    traitTag.putString("name", trait.getClass().getSimpleName());
                    traitTag.putInt("level", level);
                    traitList.add(traitTag);
                });
                traits.put("trait_list", traitList);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not capture traits: {}", e.getMessage());
        }
    }

    private static void captureEquipment(RandomHumanEntity entity, CompoundTag equipment) {
        // Main hand
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!mainHandItem.isEmpty()) {
            CompoundTag mainHand = new CompoundTag();
            mainHandItem.save(entity.level().registryAccess(), mainHand);
            equipment.put("main_hand", mainHand);
        }

        // Off hand
        ItemStack offHandItem = entity.getOffhandItem();
        if (!offHandItem.isEmpty()) {
            CompoundTag offHand = new CompoundTag();
            offHandItem.save(entity.level().registryAccess(), offHand);
            equipment.put("off_hand", offHand);
        }

        // Chest
        ItemStack chestItem = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!chestItem.isEmpty()) {
            CompoundTag chest = new CompoundTag();
            chestItem.save(entity.level().registryAccess(), chest);
            equipment.put("chest", chest);
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

        tag.put("attributes", attributes);
        tag.put("traits", traits);
        tag.put("equipment", equipment);
        tag.putString("texture_uuid", textureUUID);
        if (savedTexturePath != null) {
            tag.putString("saved_texture_path", savedTexturePath);
        }

        return tag;
    }

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

            CompoundTag attributes = tag.getCompound("attributes");
            CompoundTag traits = tag.getCompound("traits");
            CompoundTag equipment = tag.getCompound("equipment");
            String textureUUID = tag.getString("texture_uuid");
            String savedTexturePath = tag.contains("saved_texture_path") ? tag.getString("saved_texture_path") : null;

            return new EntitySnapshot(entityUUID, entityName, gender, entityClass, starLevel,
                    currentLevel, totalKills, experiencePoints, hasShield, isArcher,
                    schools, attributes, traits, equipment, textureUUID, savedTexturePath);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to deserialize EntitySnapshot: {}", e.getMessage());
            return null;
        }
    }
}
