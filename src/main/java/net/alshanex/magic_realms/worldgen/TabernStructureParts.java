package net.alshanex.magic_realms.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber()
public class TabernStructureParts {
    private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY = ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.fromNamespaceAndPath("minecraft", "empty"));

    private static void addBuildingToPool(Registry<StructureTemplatePool> templatePoolRegistry,
                                          Registry<StructureProcessorList> processorListRegistry,
                                          ResourceLocation poolRL,
                                          String nbtPieceRL,
                                          int weight) {

        Holder<StructureProcessorList> processorList = processorListRegistry.getHolderOrThrow(ProcessorLists.MOSSIFY_70_PERCENT);

        StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
        if (pool == null) return;

        SinglePoolElement piece = SinglePoolElement.single(nbtPieceRL,
                processorList).apply(StructureTemplatePool.Projection.RIGID);


        for (int i = 0; i < weight; i++) {
            pool.templates.add(piece);
        }

        List<Pair<StructurePoolElement, Integer>> listOfPieceEntries = new ArrayList<>(pool.rawTemplates);
        listOfPieceEntries.add(new Pair<>(piece, weight));
        pool.rawTemplates = listOfPieceEntries;
    }

    private static void removeBuildingFromPool(Registry<StructureTemplatePool> templatePoolRegistry,
                                               ResourceLocation poolRL,
                                               String nbtPieceRL) {

        StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
        if (pool == null) return;

        // Remove from the weighted rawTemplates list
        pool.rawTemplates = new ArrayList<>(pool.rawTemplates);
        pool.rawTemplates.removeIf(pair -> {
            StructurePoolElement element = pair.getFirst();
            return element instanceof SinglePoolElement singleElement
                    && singleElement.template.left().isPresent()
                    && singleElement.template.left().get().toString().equals(nbtPieceRL);
        });

        // Remove from the flat templates list used for random selection
        pool.templates.removeIf(element -> {
            return element instanceof SinglePoolElement singleElement
                    && singleElement.template.left().isPresent()
                    && singleElement.template.left().get().toString().equals(nbtPieceRL);
        });
    }

    @SubscribeEvent
    public static void modifyTavernPieces(final ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> templatePoolRegistry = event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).orElseThrow();
        Registry<StructureProcessorList> processorListRegistry = event.getServer().registryAccess().registry(Registries.PROCESSOR_LIST).orElseThrow();

        if(ModList.get().isLoaded("brewinandchewin") || ModList.get().isLoaded("create")){
            removeBuildingFromPool(templatePoolRegistry,
                    ResourceLocation.parse("magic_realms:taverns/plains/bar"),
                    "magic_realms:taverns/plains/bar/default_1");

            if(ModList.get().isLoaded("brewinandchewin")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/bar"),
                        "magic_realms:taverns/plains/bar/brewinandchewin_1", 1);
            }

            if(ModList.get().isLoaded("create")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/bar"),
                        "magic_realms:taverns/plains/bar/create_1", 1);
            }
        }

        if(ModList.get().isLoaded("farmersdelight") || ModList.get().isLoaded("handcrafted")){
            removeBuildingFromPool(templatePoolRegistry,
                    ResourceLocation.parse("magic_realms:taverns/plains/kitchen"),
                    "magic_realms:taverns/plains/kitchen/default_1");

            if(ModList.get().isLoaded("farmersdelight")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/kitchen"),
                        "magic_realms:taverns/plains/kitchen/farmersdelight_1", 1);
            }

            if(ModList.get().isLoaded("handcrafted")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/kitchen"),
                        "magic_realms:taverns/plains/kitchen/handcrafted_1", 1);
            }
        }

        if(ModList.get().isLoaded("mynethersdelight") || ModList.get().isLoaded("handcrafted")
            || ModList.get().isLoaded("supplementaries") || ModList.get().isLoaded("create")){

            if(ModList.get().isLoaded("handcrafted")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/handcrafted_1", 1);

                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/handcrafted_2", 1);
            }

            if(ModList.get().isLoaded("create")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/create_1", 1);

                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/create_2", 1);
            }

            if(ModList.get().isLoaded("mynethersdelight")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/mynethersdelight_1", 1);

                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/mynethersdelight_2", 1);
            }

            if(ModList.get().isLoaded("supplementaries")){
                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/supplementaries_1", 1);

                addBuildingToPool(templatePoolRegistry, processorListRegistry,
                        ResourceLocation.parse("magic_realms:taverns/plains/tables"),
                        "magic_realms:taverns/plains/tables/supplementaries_2", 1);
            }
        }
    }
}
