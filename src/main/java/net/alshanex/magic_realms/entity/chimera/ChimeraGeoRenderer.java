package net.alshanex.magic_realms.entity.chimera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alshanex.magic_realms.data.ChimeraSlot;
import net.alshanex.magic_realms.util.chimera.ChimeraAssembly;
import net.alshanex.magic_realms.util.chimera.ChimeraModelResolver;
import net.alshanex.magic_realms.util.chimera.ChimeraPartSource;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.EnumMap;
import java.util.Map;

public class ChimeraGeoRenderer extends GeoEntityRenderer<ChimeraEntity> {

    private static final Map<String, ChimeraSlot> BONE_TO_SLOT = Map.of(
            "head", ChimeraSlot.HEAD,
            "torso", ChimeraSlot.TORSO,
            "left_arm", ChimeraSlot.LEFT_ARM,
            "right_arm", ChimeraSlot.RIGHT_ARM,
            "left_leg", ChimeraSlot.LEFT_LEG,
            "right_leg", ChimeraSlot.RIGHT_LEG
    );

    // Per-frame render state
    private final Map<ChimeraSlot, ChimeraPartSource> slotSources = new EnumMap<>(ChimeraSlot.class);
    private int lastAssemblyHash = 0;
    private float currentAlpha = 1.0f;
    private MultiBufferSource currentBufferSource;
    private int currentPackedLight;

    public ChimeraGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new ChimeraGeoModel());
        this.shadowRadius = 0.7f;
        ChimeraModelResolver.setEntityModelSet(context.getModelSet());
    }

    //  Pre-render

    @Override
    public void preRender(PoseStack poseStack, ChimeraEntity entity, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {

        float entityScale = entity.getEntityScale();
        if (Math.abs(entityScale - 1.0f) > 0.001f) {
            poseStack.scale(entityScale, entityScale, entityScale);
        }

        // Only apply leg height offset when in normal state
        byte animState = entity.getAnimState();
        if (animState == ChimeraEntity.STATE_NORMAL) {
            float avgLegRatio = entity.getTallestLegHeightRatio();
            if (Math.abs(avgLegRatio - 1.0f) > 0.001f) {
                float legDiffBlocks = 12.0f * (avgLegRatio - 1.0f) / 16.0f;
                poseStack.translate(0.0f, legDiffBlocks, 0.0f);
            }
        }

        // Compute alpha based on animation state
        currentAlpha = 1.0f;
        if (entity.isSpawning()) {
            currentAlpha = entity.getSpawnProgress();
        }
        // Downed, scattering, and reassembling all render at full alpha (the animation handles positioning, not fading)

        currentBufferSource = bufferSource;
        currentPackedLight = packedLight;

        ChimeraAssembly assembly = entity.getAssembly();
        int hash = assembly.getAllAssignments().hashCode();
        if (hash != lastAssemblyHash) {
            rebuildSlotSources(assembly);
            lastAssemblyHash = hash;
        }

        for (GeoBone bone : model.topLevelBones()) {
            hideBoneRecursive(bone);
        }

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ChimeraEntity animatable,
                                  GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {

        poseStack.pushPose();
        applyBoneTransform(poseStack, bone);

        if (!isReRender) {
            ChimeraSlot slot = BONE_TO_SLOT.get(bone.getName());
            if (slot != null) {
                ChimeraPartSource source = slotSources.get(slot);
                if (source != null) {
                    if (source.isGeckoLib()) {
                        renderGeckoLibDonor(poseStack, bone, source);
                    } else {
                        renderVanillaDonor(poseStack, bone, source, slot);
                    }
                }
            }
        }

        for (GeoBone child : bone.getChildBones()) {
            renderRecursively(poseStack, animatable, child, renderType,
                    bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour);
        }

        poseStack.popPose();
    }

    private void applyBoneTransform(PoseStack poseStack, GeoBone bone) {
        float posX = bone.getPosX() / 16.0f;
        float posY = bone.getPosY() / 16.0f;
        float posZ = bone.getPosZ() / 16.0f;

        float pivotX = bone.getPivotX() / 16.0f;
        float pivotY = bone.getPivotY() / 16.0f;
        float pivotZ = bone.getPivotZ() / 16.0f;

        poseStack.translate(pivotX + posX, pivotY + posY, pivotZ + posZ);

        float rotX = bone.getRotX();
        float rotY = bone.getRotY();
        float rotZ = bone.getRotZ();
        if (rotX != 0 || rotY != 0 || rotZ != 0) {
            poseStack.mulPose(new Quaternionf().rotationZYX(rotZ, rotY, rotX));
        }

        float sx = bone.getScaleX();
        float sy = bone.getScaleY();
        float sz = bone.getScaleZ();
        if (sx != 1 || sy != 1 || sz != 1) {
            poseStack.scale(sx, sy, sz);
        }

        poseStack.translate(-pivotX, -pivotY, -pivotZ);
    }

    //  GeckoLib donor rendering

    private void renderGeckoLibDonor(PoseStack poseStack, GeoBone chimeraBone,
                                     ChimeraPartSource source) {

        VertexConsumer vc = currentBufferSource.getBuffer(
                RenderType.entityCutoutNoCull(source.texture()));

        float scale = source.scale();
        float cpx = chimeraBone.getPivotX() / 16.0f;
        float cpy = chimeraBone.getPivotY() / 16.0f;
        float cpz = chimeraBone.getPivotZ() / 16.0f;

        for (GeoBone donorBone : source.geoParts()) {
            poseStack.pushPose();

            float dpx = donorBone.getPivotX() / 16.0f;
            float dpy = donorBone.getPivotY() / 16.0f;
            float dpz = donorBone.getPivotZ() / 16.0f;

            if (Math.abs(scale - 1.0f) > 0.001f) {
                float ox = cpx - dpx * scale;
                float oy = cpy - dpy * scale;
                float oz = cpz - dpz * scale;
                poseStack.translate(ox, oy, oz);
                poseStack.scale(scale, scale, scale);
            } else {
                float dx = cpx - dpx;
                float dy = cpy - dpy;
                float dz = cpz - dpz;
                if (dx != 0 || dy != 0 || dz != 0) {
                    poseStack.translate(dx, dy, dz);
                }
            }

            if (source.offsetX() != 0 || source.offsetY() != 0 || source.offsetZ() != 0) {
                poseStack.translate(
                        source.offsetX() / 16.0f,
                        source.offsetY() / 16.0f,
                        source.offsetZ() / 16.0f
                );
            }

            renderDonorGeoBone(poseStack, vc, donorBone);

            poseStack.popPose();
        }
    }

    private void renderDonorGeoBone(PoseStack poseStack, VertexConsumer vc, GeoBone bone) {
        if (!bone.getCubes().isEmpty()) {
            PoseStack.Pose pose = poseStack.last();
            for (GeoCube cube : bone.getCubes()) {
                renderGeoCube(vc, pose, cube);
            }
        }

        for (GeoBone child : bone.getChildBones()) {
            poseStack.pushPose();

            float pivotX = child.getPivotX() / 16.0f;
            float pivotY = child.getPivotY() / 16.0f;
            float pivotZ = child.getPivotZ() / 16.0f;

            poseStack.translate(pivotX, pivotY, pivotZ);

            if (child.getRotX() != 0 || child.getRotY() != 0 || child.getRotZ() != 0) {
                poseStack.mulPose(new Quaternionf().rotationZYX(
                        child.getRotZ(), child.getRotY(), child.getRotX()));
            }

            float sx = child.getScaleX(), sy = child.getScaleY(), sz = child.getScaleZ();
            if (sx != 1 || sy != 1 || sz != 1) {
                poseStack.scale(sx, sy, sz);
            }

            poseStack.translate(-pivotX, -pivotY, -pivotZ);

            renderDonorGeoBone(poseStack, vc, child);

            poseStack.popPose();
        }
    }

    //  Vanilla donor rendering

    private void renderVanillaDonor(PoseStack poseStack, GeoBone chimeraBone,
                                    ChimeraPartSource source, ChimeraSlot slot) {

        VertexConsumer vc = currentBufferSource.getBuffer(
                RenderType.entityCutoutNoCull(source.texture()));

        float scale = source.scale();

        for (ModelPart part : source.vanillaParts()) {
            poseStack.pushPose();

            poseStack.scale(-1.0f, -1.0f, 1.0f);
            poseStack.translate(0.0f, -24.0f / 16.0f, 0.0f);

            if (Math.abs(scale - 1.0f) > 0.001f) {
                poseStack.scale(scale, scale, scale);
            }

            if (source.offsetX() != 0 || source.offsetY() != 0 || source.offsetZ() != 0) {
                poseStack.translate(
                        source.offsetX() / 16.0f,
                        source.offsetY() / 16.0f,
                        source.offsetZ() / 16.0f
                );
            }

            float origXRot = part.xRot, origYRot = part.yRot, origZRot = part.zRot;
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;

            int color = ((int) (currentAlpha * 255) << 24) | 0xFFFFFF;
            part.render(poseStack, vc, currentPackedLight, OverlayTexture.NO_OVERLAY, color);

            part.xRot = origXRot;
            part.yRot = origYRot;
            part.zRot = origZRot;

            poseStack.popPose();
        }
    }

    //  GeoCube rendering

    private void renderGeoCube(VertexConsumer vc, PoseStack.Pose pose, GeoCube cube) {
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;

            org.joml.Vector3f normal = new org.joml.Vector3f(
                    (float) quad.normal().x(),
                    (float) quad.normal().y(),
                    (float) quad.normal().z()
            );
            pose.normal().transform(normal);

            int a = (int) (currentAlpha * 255);

            for (GeoVertex vertex : quad.vertices()) {
                org.joml.Vector3f position = new org.joml.Vector3f(
                        vertex.position().x(),
                        vertex.position().y(),
                        vertex.position().z()
                );
                pose.pose().transformPosition(position);

                vc.addVertex(position.x, position.y, position.z)
                        .setColor(255, 255, 255, a)
                        .setUv(vertex.texU(), vertex.texV())
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(currentPackedLight)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }

    @Override
    public float getShadowRadius(ChimeraEntity entity) {
        // No shadow when downed (parts are scattered on ground)
        if (entity.isDowned()) return 0.0f;
        return this.shadowRadius;
    }

    //  Utilities

    private void hideBoneRecursive(GeoBone bone) {
        bone.setHidden(true);
        for (GeoBone child : bone.getChildBones()) {
            hideBoneRecursive(child);
        }
    }

    private void rebuildSlotSources(ChimeraAssembly assembly) {
        slotSources.clear();
        for (ChimeraSlot slot : ChimeraSlot.values()) {
            assembly.getEntityForSlot(slot).ifPresent(entityId ->
                    ChimeraModelResolver.buildPartSource(entityId, slot)
                            .ifPresent(source -> slotSources.put(slot, source))
            );
        }
    }

    @Override
    public RenderType getRenderType(ChimeraEntity entity, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}