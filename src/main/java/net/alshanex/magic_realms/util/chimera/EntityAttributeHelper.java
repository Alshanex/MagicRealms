package net.alshanex.magic_realms.util.chimera;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.Optional;

/**
 * Utility class that resolves attributes and dimensions directly from registered EntityTypes at runtime, so we don't need to duplicate them in JSON files.
 */
public final class EntityAttributeHelper {

    private EntityAttributeHelper() {}

    @SuppressWarnings("unchecked")
    public static Optional<EntityType<? extends LivingEntity>> resolveEntityType(ResourceLocation entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        try {
            if (DefaultAttributes.hasSupplier(type)) {
                return Optional.of((EntityType<? extends LivingEntity>) type);
            }
        } catch (Exception e) {
            // Not a living entity
        }
        return Optional.empty();
    }

    public static Optional<AttributeSupplier> getAttributes(ResourceLocation entityId) {
        return resolveEntityType(entityId).map(DefaultAttributes::getSupplier);
    }

    public static double getAttributeBaseValue(ResourceLocation entityId,
                                               Holder<Attribute> attribute,
                                               double fallback) {
        return getAttributes(entityId)
                .map(supplier -> supplier.hasAttribute(attribute) ? supplier.getBaseValue(attribute) : fallback)
                .orElse(fallback);
    }

    public static double getMaxHealth(ResourceLocation entityId) {
        return getAttributeBaseValue(entityId, Attributes.MAX_HEALTH, 20.0);
    }

    public static double getArmor(ResourceLocation entityId) {
        return getAttributeBaseValue(entityId, Attributes.ARMOR, 0.0);
    }

    public static double getAttackDamage(ResourceLocation entityId) {
        return getAttributeBaseValue(entityId, Attributes.ATTACK_DAMAGE, 1.0);
    }

    public static double getAttackSpeed(ResourceLocation entityId) {
        return getAttributeBaseValue(entityId, Attributes.ATTACK_SPEED, 1.0);
    }

    public static double getMovementSpeed(ResourceLocation entityId) {
        return getAttributeBaseValue(entityId, Attributes.MOVEMENT_SPEED, 0.25);
    }

    public static double getAttackRange(ResourceLocation entityId) {
        return getAttributeBaseValue(entityId, Attributes.ENTITY_INTERACTION_RANGE, 2.0);
    }
}
