package net.alshanex.magic_realms.util.humans.titles;

import com.mojang.blaze3d.vertex.PoseStack;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Draws a mercenary's displayed title one text line above their nameplate.
 */
@OnlyIn(Dist.CLIENT)
public final class TitleNameplateRenderer {

    /** Vertical gap in local (pre-scale) units. One vanilla line is 9; 10 leaves a 1px gap. */
    private static final float LINE_OFFSET = 10.0f;

    /** Same cutoff vanilla uses for name tags - 64 blocks, squared. */
    private static final double MAX_DISTANCE_SQR = 4096.0;

    private TitleNameplateRenderer() {}

    public static void renderTitleAbove(EntityRenderDispatcher dispatcher, Font font,
                                        AbstractMercenaryEntity entity, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, float partialTick) {
        if (dispatcher.distanceToSqr(entity) > MAX_DISTANCE_SQR) return;

        Title title = TitleManager.displayedTitle(entity);
        if (title == null) return;

        Vec3 attachment = entity.getAttachments()
                .getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (attachment == null) return;

        Component text = title.displayComponent();
        boolean seeThrough = !entity.isDiscrete();

        poseStack.pushPose();
        poseStack.translate(attachment.x, attachment.y + 0.5, attachment.z);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;
        float x = -font.width(text) / 2.0f;

        font.drawInBatch(text, x, -LINE_OFFSET, 553648127, false, matrix, bufferSource,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                backgroundColor, packedLight);

        if (seeThrough) {
            font.drawInBatch(text, x, -LINE_OFFSET, -1, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }

        poseStack.popPose();
    }
}
