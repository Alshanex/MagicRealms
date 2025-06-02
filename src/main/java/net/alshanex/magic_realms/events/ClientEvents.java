package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.player.KeyState;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.network.SummonAlliesPackage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;

import static net.alshanex.magic_realms.events.KeyMappings.TABLET_KEYMAP;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    private static final ArrayList<KeyState> KEY_STATES = new ArrayList<>();
    private static final KeyState TABLET_SUMMON_STATE = register(TABLET_KEYMAP);

    private static KeyState register(KeyMapping key) {
        var k = new KeyState(key);
        KEY_STATES.add(k);
        return k;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        handleInputEvent(event.getKey(), event.getAction());
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        handleInputEvent(event.getButton(), event.getAction());
    }

    private static void handleInputEvent(int button, int action) {
        var minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        if (TABLET_SUMMON_STATE.wasPressed() && minecraft.screen == null) {
            PacketDistributor.sendToServer(new SummonAlliesPackage());
        }
        update();
    }

    private static void update() {
        for (KeyState k : KEY_STATES) {
            k.update();
        }
    }
}
