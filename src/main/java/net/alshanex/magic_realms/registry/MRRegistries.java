package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.story.BossStory;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class MRRegistries {
    public static final ResourceKey<Registry<BossStory>> BOSS_STORIES_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "boss_stories"));

    public static final Registry<BossStory> BOSS_STORIES = new RegistryBuilder<>(BOSS_STORIES_KEY)
            .sync(true)
            .create();
}
