package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.data.ChimeraBlueprint;
import net.alshanex.magic_realms.entity.chimera.ChimeraEntity;
import net.alshanex.magic_realms.item.ChimeraCatalystItem;
import net.alshanex.magic_realms.registry.ChimeraPartRegistry;
import net.alshanex.magic_realms.registry.MRDataComponentRegistry;
import net.alshanex.magic_realms.screens.ChimeraAssemblyScreen;
import net.alshanex.magic_realms.screens.LoreArtifactScreen;
import net.alshanex.magic_realms.screens.LostPagesBookScreen;
import net.alshanex.magic_realms.util.chimera.ChimeraAssembly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MRClientUtils {

    public static void handleSyncPacket(int entityId, CompoundTag assemblyData) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof ChimeraEntity chimera) {
                ChimeraAssembly assembly = ChimeraAssembly.fromNbt(assemblyData);
                chimera.setAssembly(assembly);
            }
        }
    }

    public static void handlePrtRegistrySyncPacket(CompoundTag registryData){
        ChimeraPartRegistry registry = ChimeraPartRegistry.getOrCreateClientInstance();
        registry.applyFromNetwork(registryData);
    }

    public static void openAssemblyScreen() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack catalystStack = null;
        boolean isMainHand = false;

        if (mainHand.getItem() instanceof ChimeraCatalystItem) {
            catalystStack = mainHand;
            isMainHand = true;
        } else if (offHand.getItem() instanceof ChimeraCatalystItem) {
            catalystStack = offHand;
        }

        if (catalystStack != null) {
            ChimeraBlueprint existing = catalystStack.get(MRDataComponentRegistry.CHIMERA_BLUEPRINT.get());
            mc.setScreen(new ChimeraAssemblyScreen(catalystStack, isMainHand, existing));
        }
    }

    public static void openBookScreen(ItemStack bookStack) {
        Minecraft.getInstance().setScreen(new LostPagesBookScreen(bookStack));
    }

    public static void openLoreArtifactScreen(Component loreText, Component loreTitle) {
        Minecraft.getInstance().setScreen(new LoreArtifactScreen(loreText, loreTitle));
    }

    public static void playLoreSound(SoundEvent soundEvent) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0F, 1.5F));
    }
}
