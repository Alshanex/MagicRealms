package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.flying_arrow.FloatingArrowEntity;
import net.alshanex.magic_realms.item.FloatingArrowItem;
import net.alshanex.magic_realms.network.FloatingArrowModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class FloatingArrowClientHandler {

    private FloatingArrowClientHandler() {}

    /** Last mode we sent to the server, so we only re-send on changes. */
    private static byte lastSentMode = FloatingArrowEntity.MODE_IDLE;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (!isHoldingFloatingArrow(player)) {
            if (lastSentMode != FloatingArrowEntity.MODE_IDLE) {
                PacketDistributor.sendToServer(new FloatingArrowModePacket(FloatingArrowEntity.MODE_IDLE));
                lastSentMode = FloatingArrowEntity.MODE_IDLE;
            }
            return;
        }

        if (mc.screen != null || !mc.isWindowActive()) {
            if (lastSentMode != FloatingArrowEntity.MODE_HOLD) {
                PacketDistributor.sendToServer(new FloatingArrowModePacket(FloatingArrowEntity.MODE_HOLD));
                lastSentMode = FloatingArrowEntity.MODE_HOLD;
            }
            return;
        }

        long window = mc.getWindow().getWindow();
        boolean leftHeld = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightHeld = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean shiftHeld = player.isShiftKeyDown();

        byte desired;
        if (shiftHeld) {
            // Shift recalls the arrow, regardless of mouse buttons.
            desired = FloatingArrowEntity.MODE_IDLE;
        } else if (leftHeld && !rightHeld) {
            desired = FloatingArrowEntity.MODE_FORWARD;
        } else if (rightHeld && !leftHeld) {
            desired = FloatingArrowEntity.MODE_BACKWARD;
        } else {
            // No input: hover in place.
            desired = FloatingArrowEntity.MODE_HOLD;
        }

        if (desired != lastSentMode) {
            PacketDistributor.sendToServer(new FloatingArrowModePacket(desired));
            lastSentMode = desired;
        }
    }

    /**
     * Cancel default left/right click behaviour while the arrow is held so the player doesn't swing their arm, mine blocks, or place/use items.
     */
    @SubscribeEvent
    public static void onMouseInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!isHoldingFloatingArrow(mc.player)) return;
        // Suppress the vanilla attack / use behaviour entirely.
        if (event.isAttack() || event.isUseItem()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private static boolean isHoldingFloatingArrow(Player player) {
        return player.getMainHandItem().getItem() instanceof FloatingArrowItem;
    }
}
