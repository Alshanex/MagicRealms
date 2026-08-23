package net.alshanex.magic_realms.util.chimera;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.data.ChimeraSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a specific chimera configuration - which mob provides each body part.
 * This is what gets saved on the ChimeraEntity as NBT data.
 * <p>
 * Attributes are computed at runtime by reading the actual entity types' registered AttributeSuppliers - no hardcoded values.
 */
public class ChimeraAssembly {
    public static final Codec<ChimeraAssembly> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("head").forGetter(a -> a.getEntityForSlot(ChimeraSlot.HEAD)),
                    ResourceLocation.CODEC.optionalFieldOf("left_arm").forGetter(a -> a.getEntityForSlot(ChimeraSlot.LEFT_ARM)),
                    ResourceLocation.CODEC.optionalFieldOf("right_arm").forGetter(a -> a.getEntityForSlot(ChimeraSlot.RIGHT_ARM)),
                    ResourceLocation.CODEC.optionalFieldOf("torso").forGetter(a -> a.getEntityForSlot(ChimeraSlot.TORSO)),
                    ResourceLocation.CODEC.optionalFieldOf("left_leg").forGetter(a -> a.getEntityForSlot(ChimeraSlot.LEFT_LEG)),
                    ResourceLocation.CODEC.optionalFieldOf("right_leg").forGetter(a -> a.getEntityForSlot(ChimeraSlot.RIGHT_LEG))
            ).apply(instance, ChimeraAssembly::new)
    );

    private final EnumMap<ChimeraSlot, ResourceLocation> slotAssignments = new EnumMap<>(ChimeraSlot.class);

    public ChimeraAssembly() {}

    private ChimeraAssembly(Optional<ResourceLocation> head,
                            Optional<ResourceLocation> leftArm,
                            Optional<ResourceLocation> rightArm,
                            Optional<ResourceLocation> torso,
                            Optional<ResourceLocation> leftLeg,
                            Optional<ResourceLocation> rightLeg) {
        head.ifPresent(rl -> slotAssignments.put(ChimeraSlot.HEAD, rl));
        leftArm.ifPresent(rl -> slotAssignments.put(ChimeraSlot.LEFT_ARM, rl));
        rightArm.ifPresent(rl -> slotAssignments.put(ChimeraSlot.RIGHT_ARM, rl));
        torso.ifPresent(rl -> slotAssignments.put(ChimeraSlot.TORSO, rl));
        leftLeg.ifPresent(rl -> slotAssignments.put(ChimeraSlot.LEFT_LEG, rl));
        rightLeg.ifPresent(rl -> slotAssignments.put(ChimeraSlot.RIGHT_LEG, rl));
    }

    public void setSlot(ChimeraSlot slot, ResourceLocation entityId) {
        slotAssignments.put(slot, entityId);
    }

    public void clearSlot(ChimeraSlot slot) {
        slotAssignments.remove(slot);
    }

    public Optional<ResourceLocation> getEntityForSlot(ChimeraSlot slot) {
        return Optional.ofNullable(slotAssignments.get(slot));
    }

    public boolean isComplete() {
        for (ChimeraSlot slot : ChimeraSlot.values()) {
            if (!slotAssignments.containsKey(slot)) return false;
        }
        return true;
    }

    public Map<ChimeraSlot, ResourceLocation> getAllAssignments() {
        return Map.copyOf(slotAssignments);
    }

    // Attribute Computation

    public ComputedAttributes computeAttributes() {
        ResourceLocation mainArm = slotAssignments.get(ChimeraSlot.RIGHT_ARM);
        double attackDamage = mainArm != null ? EntityAttributeHelper.getAttackDamage(mainArm) : 3.0;
        double attackSpeed = mainArm != null ? EntityAttributeHelper.getAttackSpeed(mainArm) : 1.0;
        double attackRange = mainArm != null ? EntityAttributeHelper.getAttackRange(mainArm) : 2.0;

        ResourceLocation torso = slotAssignments.get(ChimeraSlot.TORSO);
        double maxHealth = torso != null ? Math.min(EntityAttributeHelper.getMaxHealth(torso), 100.0) : 20.0;
        double armor = torso != null ? EntityAttributeHelper.getArmor(torso) : 0.0;

        ResourceLocation leftLeg = slotAssignments.get(ChimeraSlot.LEFT_LEG);
        ResourceLocation rightLeg = slotAssignments.get(ChimeraSlot.RIGHT_LEG);
        double leftSpeed = leftLeg != null ? EntityAttributeHelper.getMovementSpeed(leftLeg) : 0.25;
        double rightSpeed = rightLeg != null ? EntityAttributeHelper.getMovementSpeed(rightLeg) : 0.25;
        double movementSpeed = (leftSpeed + rightSpeed) / 2.0;

        return new ComputedAttributes(maxHealth, armor, attackDamage, attackSpeed, attackRange, movementSpeed);
    }

    public record ComputedAttributes(
            double maxHealth, double armor,
            double attackDamage, double attackSpeed, double attackRange,
            double movementSpeed
    ) {}

    // NBT

    public CompoundTag toNbt() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode ChimeraAssembly: " + msg));
    }

    public static ChimeraAssembly fromNbt(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .getOrThrow(msg -> new IllegalStateException("Failed to decode ChimeraAssembly: " + msg));
    }
}
