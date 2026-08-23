package net.alshanex.magic_realms.setup;

import com.mojang.blaze3d.platform.InputConstants;
import io.redspace.ironsspellbooks.player.ExtendedKeyMapping;
import net.alshanex.magic_realms.MagicRealms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(value = Dist.CLIENT, modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class KeyMappings {
    public static final String KEY_BIND_CATEGORY= "key.magic_realms.keys";
    public static final ExtendedKeyMapping OPEN_ASSEMBLY = new ExtendedKeyMapping(getResourceName("open_assembly"), KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_N, KEY_BIND_CATEGORY);

    private static String getResourceName(String name) {
        return String.format("key.magic_realms.%s", name);
    }

    @SubscribeEvent
    public static void onRegisterKeybinds(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ASSEMBLY);
    }
}
