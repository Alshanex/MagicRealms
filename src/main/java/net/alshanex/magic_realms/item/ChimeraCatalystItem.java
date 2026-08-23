package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.data.ChimeraBlueprint;
import net.alshanex.magic_realms.entity.chimera.ChimeraEntity;
import net.alshanex.magic_realms.registry.MRDataComponentRegistry;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.chimera.ChimeraAssembly;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static net.alshanex.magic_realms.setup.KeyMappings.OPEN_ASSEMBLY;

/**
 * The Chimera Catalyst item.
 * <p>
 * - Press a keybind while holding it to open the assembly GUI
 * - Right-click to spawn a chimera with the saved configuration
 * - The assembly is stored as a {@link ChimeraBlueprint} data component
 */
public class ChimeraCatalystItem extends Item {

    public ChimeraCatalystItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        // Check if the item has a complete blueprint
        ChimeraBlueprint blueprint = stack.get(MRDataComponentRegistry.CHIMERA_BLUEPRINT.get());
        if (blueprint == null || !blueprint.isComplete()) {
            player.displayClientMessage(
                    Component.translatable("item.magic_realms.catalyst.no_blueprint", OPEN_ASSEMBLY.getKey().getDisplayName())
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // Spawn the chimera
        ChimeraAssembly assembly = blueprint.assembly().orElseThrow();
        ChimeraEntity chimera = new ChimeraEntity(MREntityRegistry.CHIMERA_ENTITY.get(), level);

        // Position: where the player is looking, offset forward
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(lookVec.x * 2, 0, lookVec.z * 2);
        chimera.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        chimera.setAssembly(assembly);

        // Apply custom name if present
        blueprint.customName().ifPresent(name -> {
            if (!name.isBlank()) {
                chimera.setCustomName(Component.literal(name));
            }
        });

        // Trigger spawn animation before adding to world
        chimera.triggerSpawnAnimation();

        chimera.setHealth(chimera.getMaxHealth());

        level.addFreshEntity(chimera);

        // Play spawn sound
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 0.8f);

        player.displayClientMessage(
                Component.translatable("item.magic_realms.catalyst.spawned")
                        .withStyle(ChatFormatting.GREEN),
                true
        );

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);

        ChimeraBlueprint blueprint = stack.get(MRDataComponentRegistry.CHIMERA_BLUEPRINT.get());
        if (blueprint != null && blueprint.isComplete()) {
            tooltipComponents.add(
                    Component.translatable("item.magic_realms.catalyst.tooltip.configured")
                            .withStyle(ChatFormatting.GREEN)
            );

            blueprint.customName().ifPresent(name -> {
                if (!name.isBlank()) {
                    tooltipComponents.add(
                            Component.literal("  Name: ")
                                    .withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(name)
                                            .withStyle(ChatFormatting.AQUA))
                    );
                }
            });

            blueprint.assembly().ifPresent(assembly -> {
                assembly.getAllAssignments().forEach((slot, entityId) -> {
                    tooltipComponents.add(
                            Component.literal("  " + slot.getSerializedName() + ": ")
                                    .withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(entityId.toString())
                                            .withStyle(ChatFormatting.WHITE))
                    );
                });
            });
        } else {
            tooltipComponents.add(
                    Component.translatable("item.magic_realms.catalyst.tooltip.empty")
                            .withStyle(ChatFormatting.GRAY)
            );
            tooltipComponents.add(
                    Component.translatable("item.magic_realms.catalyst.tooltip.keybind_hint", OPEN_ASSEMBLY.getKey().getDisplayName())
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ChimeraBlueprint blueprint = stack.get(MRDataComponentRegistry.CHIMERA_BLUEPRINT.get());
        return blueprint != null && blueprint.isComplete();
    }
}
