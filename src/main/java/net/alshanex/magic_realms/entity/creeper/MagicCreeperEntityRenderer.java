package net.alshanex.magic_realms.entity.creeper;

import com.mojang.blaze3d.platform.NativeImage;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MagicCreeperEntityRenderer extends MobRenderer<MagicCreeperEntity, CreeperModel<MagicCreeperEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/creeper/creeper_base.png");
    private static final Map<SchoolType, ResourceLocation> SCHOOL_TEXTURES = new HashMap<>();

    public MagicCreeperEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.25f);

        if (SCHOOL_TEXTURES.isEmpty()) {
            List<SchoolType> availableSchools = SchoolRegistry.REGISTRY.stream().toList();
            for (SchoolType school : availableSchools) {
                // guard in case getTargetingColor() can be null
                Vector3f color = school.getTargetingColor();
                if (color == null) {
                    SCHOOL_TEXTURES.put(school, TEXTURE);
                } else {
                    // produce a stable unique name for the texture entry
                    String key = "school_" + Integer.toHexString(Objects.hash(school));
                    ResourceLocation recolored = createRecoloredTexture(TEXTURE, color, key);
                    SCHOOL_TEXTURES.put(school, recolored);
                }
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(MagicCreeperEntity entity) {
        SchoolType school = entity.getWeakSchool();
        if (school != null && SCHOOL_TEXTURES.containsKey(school)) {
            return SCHOOL_TEXTURES.get(school);
        }
        return TEXTURE;
    }

    private static ResourceLocation createRecoloredTexture(ResourceLocation original, Vector3f targetColor, String nameSuffix) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Resource resource = mc.getResourceManager().getResource(original).orElseThrow();
            // read the image (NativeImage owns the native pixels after read)
            NativeImage image = NativeImage.read(resource.open()); // do NOT close image here: DynamicTexture will use it
            int w = image.getWidth();
            int h = image.getHeight();

            // targetColor components are expected in [0..1]
            float tR = targetColor.x();
            float tG = targetColor.y();
            float tB = targetColor.z();

            // iterate pixels and replace color while preserving brightness/shading
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int px = image.getPixelRGBA(x, y); // ABGR packed int
                    int a = FastColor.ABGR32.alpha(px);
                    int b = FastColor.ABGR32.blue(px);
                    int g = FastColor.ABGR32.green(px);
                    int r = FastColor.ABGR32.red(px);

                    // If fully transparent, keep it as-is
                    if (a == 0) {
                        continue;
                    }

                    // compute brightness (0..1) from original RGB
                    float brightness = (r + g + b) / (3f * 255f);

                    int newR = Mth.clamp((int) (tR * 255f * brightness), 0, 255);
                    int newG = Mth.clamp((int) (tG * 255f * brightness), 0, 255);
                    int newB = Mth.clamp((int) (tB * 255f * brightness), 0, 255);

                    // FastColor.ABGR32.color expects (alpha, blue, green, red)
                    int newPixel = FastColor.ABGR32.color(a, newB, newG, newR);
                    image.setPixelRGBA(x, y, newPixel);
                }
            }

            // create a DynamicTexture from the modified NativeImage
            DynamicTexture dyn = new DynamicTexture(image);

            // choose a ResourceLocation for the new texture
            ResourceLocation out = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "magic_creeper/" + nameSuffix);

            // register with texture manager (this will call load and eventually upload)
            mc.getTextureManager().register(out, dyn);

            return out;
        } catch (IOException e) {
            e.printStackTrace();
            return original; // fallback if something fails
        }
    }
}
