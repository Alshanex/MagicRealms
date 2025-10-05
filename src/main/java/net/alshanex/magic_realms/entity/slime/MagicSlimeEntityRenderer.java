package net.alshanex.magic_realms.entity.slime;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MagicSlimeEntityRenderer extends MobRenderer<MagicSlimeEntity, SlimeModel<MagicSlimeEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/slime/base_magic_slime.png");
    private static final Map<SchoolType, ResourceLocation> SCHOOL_TEXTURES = new HashMap<>();

    public MagicSlimeEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25f);
        this.addLayer(new SlimeOuterLayer<>(this, context.getModelSet()));

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
    public void render(MagicSlimeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = 0.25F * (float)entity.getSize();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(MagicSlimeEntity livingEntity, PoseStack poseStack, float partialTickTime) {
        float f = 0.999F;
        poseStack.scale(0.999F, 0.999F, 0.999F);
        poseStack.translate(0.0F, 0.001F, 0.0F);
        float f1 = (float)livingEntity.getSize();
        float f2 = Mth.lerp(partialTickTime, livingEntity.oSquish, livingEntity.squish) / (f1 * 0.5F + 1.0F);
        float f3 = 1.0F / (f2 + 1.0F);
        poseStack.scale(f3 * f1, 1.0F / f3 * f1, f3 * f1);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicSlimeEntity entity) {
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
            ResourceLocation out = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "magic_slime/" + nameSuffix);

            // register with texture manager (this will call load and eventually upload)
            mc.getTextureManager().register(out, dyn);

            return out;
        } catch (IOException e) {
            e.printStackTrace();
            return original; // fallback if something fails
        }
    }
}