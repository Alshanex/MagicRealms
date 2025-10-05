package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.screens.ContractHumanInfoScreen;
import net.alshanex.magic_realms.screens.ContractInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientScreenEventHandler {

    private static double storedMouseX = -1;
    private static double storedMouseY = -1;
    private static boolean shouldRestoreMouse = false;

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        Screen screen = event.getScreen();

        // Store mouse position when closing contract screens
        if (screen instanceof ContractHumanInfoScreen || screen instanceof ContractInventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.mouseHandler != null) {
                storedMouseX = mc.mouseHandler.xpos();
                storedMouseY = mc.mouseHandler.ypos();
                shouldRestoreMouse = true;
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        Screen newScreen = event.getNewScreen();

        // Restore mouse position when opening contract screens
        if ((newScreen instanceof ContractHumanInfoScreen || newScreen instanceof ContractInventoryScreen)
                && shouldRestoreMouse && storedMouseX != -1 && storedMouseY != -1) {

            // Schedule mouse position restoration for next frame
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                try {
                    GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), storedMouseX, storedMouseY);
                    shouldRestoreMouse = false; // Reset flag after use
                } catch (Exception e) {
                    // Ignore errors - mouse position restoration is not critical
                }
            });
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        // Additional mouse position restoration after screen is fully initialized
        if ((screen instanceof ContractHumanInfoScreen || screen instanceof ContractInventoryScreen)
                && shouldRestoreMouse && storedMouseX != -1 && storedMouseY != -1) {

            Minecraft mc = Minecraft.getInstance();
            // Delay by a few ticks to ensure screen is fully ready
            mc.execute(() -> mc.execute(() -> mc.execute(() -> {
                try {
                    GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), storedMouseX, storedMouseY);
                } catch (Exception e) {
                    // Ignore errors
                }
            })));
        }
    }
}
