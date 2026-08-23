package net.alshanex.magic_realms.registry;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.story.BossStory;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All boss story definitions for The Lost Pages.
 * Each story corresponds to a boss and contains multiple chapters that the player must collect as Lost Pages.
 */
public class MRBossStoryRegistry {
    public static final DeferredRegister<BossStory> BOSS_STORIES =
            DeferredRegister.create(MRRegistries.BOSS_STORIES_KEY, MagicRealms.MODID);

    // Example Boss Stories
    // Replace these with your actual boss stories

    public static final DeferredHolder<BossStory, BossStory> MARCUS_THORPAN =
            BOSS_STORIES.register("marcus_thorpan", () ->
                    BossStory.builder(
                                    ResourceLocation.fromNamespaceAndPath("minecraft", "zombie")
                            )
                            // Chapter 0: "Angakkuq"
                            .chapter(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_0.png"))
                            .unlockedByDefault()
                            .image(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_0/image_1.png"))
                            .page()
                            .page()
                            .endChapter()
                            // Chapter 1: "The Ruined Village"
                            .chapter(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_1.png"))
                            .page()
                            .page()
                            .image(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_1/image_1.png"))
                            .page()
                            .page()
                            .image(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_1/image_2.png"))
                            .endChapter()
                            // Chapter 2: "Frozen Graves"
                            .chapter(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_2.png"))
                            .requires(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "marcus_thorpan"), 1) // Requires Chapter 1 to be unlocked
                            .page()
                            .page()
                            .page()
                            .page()
                            .image(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_2/image_1.png"))
                            .page()
                            .endChapter()
                            // Chapter 3: "The Throne of Death"
                            .chapter(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_3.png"))
                            .requires(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "marcus_thorpan"), 2) // Requires Chapter 2 to be unlocked
                            .page()
                            .page()
                            .image(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_3/image_1.png"))
                            .page()
                            .page()
                            .page()
                            .endChapter()
                            // Chapter 4: "Ilisiitsoq"
                            .chapter(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_4.png"))
                            .requires(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "marcus_thorpan"), 3) // Requires Chapter 3 to be unlocked
                            .page()
                            .page()
                            .image(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/chapter_4/page_1.png"))
                            .page()
                            .page()
                            .endChapter()
                            // Substory 1: Lila's secret diary
                            .substory(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/substory_2.png"))
                            .page()
                            .page()
                            .page()
                            .page()
                            .page()
                            .page()
                            .page()
                            .page()
                            .endChapter()
                            // Substory 2: Note from marcus at the tombs
                            .substory(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/substory_1.png"))
                            .page()
                            .page()
                            .page()
                            .endChapter()
                            //Substory 3: The hunter's note
                            .substory(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/substory_3.png"))
                            .page()
                            .page()
                            .endChapter()
                            // Substory 4: The priest note
                            .substory(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stories/marcus_thorpan/substory_4.png"))
                            .page()
                            .page()
                            .endChapter()

                            .build()
            );


    public static void register(IEventBus bus) {
        BOSS_STORIES.register(bus);
    }
}
