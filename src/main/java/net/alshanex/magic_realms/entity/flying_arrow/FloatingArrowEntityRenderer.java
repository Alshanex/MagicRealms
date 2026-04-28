package net.alshanex.magic_realms.entity.flying_arrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class FloatingArrowEntityRenderer extends EntityRenderer<FloatingArrowEntity> {

    private static final ResourceLocation TEXTURE = IronsSpellbooks.id("textures/entity/magic_arrow.png");

    public FloatingArrowEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingArrowEntity entity) {
        return TEXTURE;
    }

    public static ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public void render(FloatingArrowEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();

        float entityYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float entityPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        double yawRad = Math.toRadians(entityYaw);
        double pitchRad = Math.toRadians(entityPitch);
        double cosPitch = Math.cos(pitchRad);
        double mx = -Math.sin(yawRad) * cosPitch;
        double my = -Math.sin(pitchRad);
        double mz =  Math.cos(yawRad) * cosPitch;
        double horiz = Math.sqrt(mx * mx + mz * mz);

        float xRot = -((float) (Mth.atan2(horiz, my) * (180.0 / Math.PI)) - 90.0F);
        float yRot = -((float) (Mth.atan2(mz, mx) * (180.0 / Math.PI)) + 90.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

        renderModel(poseStack, bufferSource);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    public static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.scale(0.13f, 0.13f, 0.13f);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderHelper.CustomerRenderType.magic(getTextureLocation()));

        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(-2, 0, 0);

        for (int j = 0; j < 4; ++j) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            vertex(poseMatrix, normalMatrix, consumer, -8, -2, 0, 0.0F,     0.0F,     0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer,  8, -2, 0, 0.5F,     0.0F,     0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer,  8,  2, 0, 0.5F,     0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer, -8,  2, 0, 0.0F,     0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT);
        }
    }

    private static void vertex(Matrix4f matrix, Matrix3f normals, VertexConsumer buf,
                               int x, int y, int z,
                               float u, float v,
                               int normalX, int normalY, int normalZ,
                               int packedLight) {
        buf.addVertex(matrix, (float) x, (float) y, (float) z)
                .setColor(200, 200, 200, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal((float) normalX, (float) normalZ, (float) normalY);
    }
}
