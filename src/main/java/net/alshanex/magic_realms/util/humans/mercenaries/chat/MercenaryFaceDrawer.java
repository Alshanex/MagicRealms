package net.alshanex.magic_realms.util.humans.mercenaries.chat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alshanex.magic_realms.data.ContractedMercenaryEntry.Face;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Draws a mercenary face from a stored {@link Face} instead of a live entity, so it works for mercenaries that
 * aren't loaded on the client. Uses the same texture sources and UV layout as {@link ChatFaceRenderer}.
 */
@OnlyIn(Dist.CLIENT)
public final class MercenaryFaceDrawer {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    /** Layout of the 16x8 mini atlas built by {@link ChatFaceCompositeCache}. */
    private static final IChatFaceProvider.UVSlice ATLAS_FACE = new IChatFaceProvider.UVSlice(0, 0, 8, 8, 16, 8);
    private static final IChatFaceProvider.UVSlice ATLAS_HAT  = new IChatFaceProvider.UVSlice(8, 0, 8, 8, 16, 8);

    private MercenaryFaceDrawer() {}

    public static void draw(GuiGraphics g, @Nullable Face face, int x, int y, int size) {
        ResourceLocation tex = null;
        IChatFaceProvider.UVSlice faceUv = IChatFaceProvider.DEFAULT_FACE_SLICE;
        IChatFaceProvider.UVSlice hatUv = IChatFaceProvider.DEFAULT_HAT_SLICE;

        if (face != null && face.layered()) {
            tex = ChatFaceCompositeCache.getOrBuild(face.toTextureComponents());
            faceUv = ATLAS_FACE;
            hatUv = ATLAS_HAT;
        } else if (face != null && !face.texture().isEmpty()) {
            tex = ResourceLocation.tryParse(face.texture());
        }

        if (tex == null) {
            tex = FALLBACK;
            faceUv = IChatFaceProvider.DEFAULT_FACE_SLICE;
            hatUv = IChatFaceProvider.DEFAULT_HAT_SLICE;
        }

        blit(g, tex, x, y, size, faceUv);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        blit(g, tex, x, y, size, hatUv);
        RenderSystem.disableBlend();
    }

    private static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, int size, IChatFaceProvider.UVSlice s) {
        g.blit(tex, x, y, size, size, s.u(), s.v(), s.w(), s.h(), s.atlasW(), s.atlasH());
    }
}