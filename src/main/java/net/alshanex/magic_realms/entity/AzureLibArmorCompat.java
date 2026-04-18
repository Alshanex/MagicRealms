package net.alshanex.magic_realms.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public final class AzureLibArmorCompat {
    private AzureLibArmorCompat() {}

    /** @return true if AzureLib has a custom renderer registered for this armor stack. */
    public static boolean hasRenderer(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof ArmorItem
                && AzArmorRendererRegistry.getOrNull(stack) != null;
    }

    /**
     * Renders a single AzureLib armor piece for the given slot.
     * Caller is responsible for setting up the poseStack frame (scale, translate, Y-flip).
     * No-op if this stack has no AzureLib renderer.
     */
    public static void tryRender(PoseStack poseStack, AbstractSpellCastingMob animatable,
                                 EquipmentSlot slot, ItemStack stack,
                                 HumanoidModel<?> innerModel, HumanoidModel<?> outerModel,
                                 int packedLight) {
        if (!hasRenderer(stack)) return;
        AzArmorRenderer renderer = AzArmorRendererRegistry.getOrNull(stack);
        if (renderer == null) return;

        HumanoidModel<?> baseModel = (slot == EquipmentSlot.LEGS) ? innerModel : outerModel;
        int color = stack.is(ItemTags.DYEABLE) ? DyedItemColor.getOrDefault(stack, -6265536) : -1;

        poseStack.pushPose();
        try {
            renderer.prepForRender(animatable, stack, slot, baseModel);
            renderer.rendererPipeline().armorModel().renderToBuffer(
                    poseStack, null, packedLight, OverlayTexture.NO_OVERLAY, color);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("AzArmor render failed for " + slot + ": " + e);
        } finally {
            poseStack.popPose();
        }
    }
}
