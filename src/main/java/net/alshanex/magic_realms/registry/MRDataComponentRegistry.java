package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ChimeraBlueprint;
import net.alshanex.magic_realms.data.PageIdentifier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MRDataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MagicRealms.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChimeraBlueprint>> CHIMERA_BLUEPRINT =
            DATA_COMPONENTS.register("chimera_blueprint", () ->
                    DataComponentType.<ChimeraBlueprint>builder()
                            .persistent(ChimeraBlueprint.CODEC)
                            .networkSynchronized(ChimeraBlueprint.STREAM_CODEC)
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PageIdentifier>> PAGE_IDENTIFIER =
            DATA_COMPONENTS.register("page_identifier", () ->
                    DataComponentType.<PageIdentifier>builder()
                            .persistent(PageIdentifier.CODEC)
                            .networkSynchronized(PageIdentifier.STREAM_CODEC)
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> LORE_NODE_ID =
            DATA_COMPONENTS.register("lore_node_id", () ->
                    DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
                            .build()
            );

    public static void register(IEventBus bus){
        DATA_COMPONENTS.register(bus);
    }
}
