package net.alshanex.magic_realms.util.humans.mercenaries.chat;

import net.minecraft.resources.ResourceLocation;

/**
 * Marker interface for any entity whose chat lines should be rendered with an inline face icon in front of the speaker name.
 */
public interface IChatFaceProvider {

    /** Standard player-skin face slice: top-left at (8, 8), 8x8 pixels, in a 64x64 atlas. */
    UVSlice DEFAULT_FACE_SLICE = new UVSlice(8, 8, 8, 8, 64, 64);
    /** Standard player-skin hat slice: top-left at (40, 8), 8x8 pixels, in a 64x64 atlas. Drawn over the face for a complete head icon. */
    UVSlice DEFAULT_HAT_SLICE  = new UVSlice(40, 8, 8, 8, 64, 64);

    /**
     * The texture currently used to render the speaking entity.
     */
    ResourceLocation getChatFaceTextureCS();

    /** Override if your texture's face slice isn't at the standard 8,8 / 8x8 / 64x64 location. */
    default UVSlice getChatFaceUV() { return DEFAULT_FACE_SLICE; }

    /** Override if your texture's hat slice isn't at the standard 40,8 / 8x8 / 64x64 location. Return null to disable hat overlay. */
    default UVSlice getChatFaceHatUV() { return DEFAULT_HAT_SLICE; }

    record UVSlice(int u, int v, int w, int h, int atlasW, int atlasH) {}
}
