package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.screens.HumanInfoMenu;
import net.alshanex.magic_realms.screens.HumanTeamMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MRMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    public static final DeferredHolder<MenuType<?>, MenuType<HumanInfoMenu>> HUMAN_INFO_MENU =
            MENUS.register("human_info_menu", () -> IMenuTypeExtension.create((containerId, inventory, data) -> {
                return new HumanInfoMenu(containerId, inventory, data);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<HumanTeamMenu>> HUMAN_TEAM_MENU =
            MENUS.register("human_team_menu", () -> IMenuTypeExtension.create((containerId, inventory, data) -> {
                return new HumanTeamMenu(containerId, inventory, data);
            }));
}
