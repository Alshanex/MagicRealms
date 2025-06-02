package net.alshanex.magic_realms.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(value = Dist.CLIENT, modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class KeyMappings {
    public static final String KEY_BIND_TABLET_CATEGORY= "key.magic_realms.tablet_keys";
    public static final KeyMapping TABLET_KEYMAP = new KeyMapping(getResourceName("tablet_key"), KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_X, KEY_BIND_TABLET_CATEGORY);

    private static String getResourceName(String name) {
        return String.format("key.magic_realms.%s", name);
    }

    @SubscribeEvent
    public static void onRegisterKeybinds(RegisterKeyMappingsEvent event) {
        event.register(TABLET_KEYMAP);
    }
}
