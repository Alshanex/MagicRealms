package net.alshanex.magic_realms.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.ClientHooks;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomArmorGeoLayer extends ItemArmorGeoLayer<AbstractSpellCastingMob> {

    private static final boolean AZURELIB_ARMOR_LOADED =
            ModList.get() != null && ModList.get().isLoaded("azurelib");

    public CustomArmorGeoLayer(GeoRenderer<AbstractSpellCastingMob> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, AbstractSpellCastingMob animatable) {
        return switch (bone.getName()) {
            case DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT -> this.bootsStack;
            case DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT -> this.leggingsStack;
            case DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT -> this.chestplateStack;
            case DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT -> this.helmetStack;
            default -> null;
        };
    }

    @Nonnull
    @Override
    protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, AbstractSpellCastingMob animatable) {
        return switch (bone.getName()) {
            case DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT -> EquipmentSlot.FEET;
            case DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT -> EquipmentSlot.LEGS;
            case DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT ->
                    !animatable.isLeftHanded() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            case DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT ->
                    animatable.isLeftHanded() ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            case DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT -> EquipmentSlot.CHEST;
            case DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT -> EquipmentSlot.HEAD;
            default -> super.getEquipmentSlotForBone(bone, stack, animatable);
        };
    }

    @Nonnull
    @Override
    protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack,
                                            AbstractSpellCastingMob animatable, HumanoidModel<?> baseModel) {
        return switch (bone.getName()) {
            case DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT -> baseModel.leftLeg;
            case DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT -> baseModel.rightLeg;
            case DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT -> baseModel.rightArm;
            case DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT -> baseModel.leftArm;
            case DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT -> baseModel.body;
            case DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT -> baseModel.head;
            default -> super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
        };
    }

    /**
     * Skip AzureLib armors — they're rendered by AbstractMercenaryEntityRenderer.actuallyRender.
     * Vanilla armors and GeckoLib-rendered armors fall through to super normally.
     */
    @Override
    public void renderForBone(com.mojang.blaze3d.vertex.PoseStack poseStack, AbstractSpellCastingMob animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (AZURELIB_ARMOR_LOADED) {
            ItemStack armorStack = getArmorItemForBone(bone, animatable);
            if (armorStack != null && AzureLibArmorCompat.hasRenderer(armorStack)) {
                return; // handled by the renderer, not here
            }
        }
        super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                partialTick, packedLight, packedOverlay);
    }

    /**
     * Route vanilla armor texture lookup through ClientHooks so mod overrides of getArmorTexture (like Ignitium) actually take effect.
     */
    @Override
    protected VertexConsumer getVanillaArmorBuffer(MultiBufferSource bufferSource,
                                                   AbstractSpellCastingMob animatable,
                                                   ItemStack stack, EquipmentSlot slot, GeoBone bone,
                                                   @Nullable ArmorMaterial.Layer layer,
                                                   int packedLight, int packedOverlay, boolean forGlint) {
        if (forGlint || layer == null) {
            return super.getVanillaArmorBuffer(bufferSource, animatable, stack, slot, bone,
                    layer, packedLight, packedOverlay, forGlint);
        }
        boolean innerModel = slot == EquipmentSlot.LEGS;
        ResourceLocation texture = ClientHooks.getArmorTexture(animatable, stack, layer, innerModel, slot);
        return bufferSource.getBuffer(RenderType.armorCutoutNoCull(texture));
    }
}
