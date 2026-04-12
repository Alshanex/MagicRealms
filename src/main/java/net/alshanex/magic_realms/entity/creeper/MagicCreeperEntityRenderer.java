package net.alshanex.magic_realms.entity.creeper;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

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
        this.addLayer(new MagicCreeperPowerLayer(this, context.getModelSet()));
        this.addLayer(new MagicCreeperHatLayer(this));

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
    protected void scale(MagicCreeperEntity entity, PoseStack poseStack, float partialTickTime) {
        float f = entity.getSwelling(partialTickTime);
        float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        f *= f;
        f *= f;
        float f2 = (1.0F + f * 0.4F) * f1;
        float f3 = (1.0F + f * 0.1F) / f1;
        poseStack.scale(f2, f3, f2);
    }

    @Override
    protected float getWhiteOverlayProgress(MagicCreeperEntity entity, float partialTicks) {
        float f = entity.getSwelling(partialTicks);
        return (int)(f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
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

    public static class MagicCreeperPowerLayer extends EnergySwirlLayer<MagicCreeperEntity, CreeperModel<MagicCreeperEntity>> {
        private static final ResourceLocation POWER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
        private final CreeperModel<MagicCreeperEntity> model;

        public MagicCreeperPowerLayer(RenderLayerParent<MagicCreeperEntity, CreeperModel<MagicCreeperEntity>> renderer, EntityModelSet modelSet) {
            super(renderer);
            this.model = new CreeperModel<>(modelSet.bakeLayer(ModelLayers.CREEPER_ARMOR));
        }

        @Override
        protected float xOffset(float tickCount) {
            return tickCount * 0.01F;
        }

        @Override
        protected ResourceLocation getTextureLocation() {
            return POWER_LOCATION;
        }

        @Override
        protected EntityModel<MagicCreeperEntity> model() {
            return this.model;
        }
    }

    public static class MagicCreeperHatLayer extends RenderLayer<MagicCreeperEntity, CreeperModel<MagicCreeperEntity>> {

        public MagicCreeperHatLayer(RenderLayerParent<MagicCreeperEntity, CreeperModel<MagicCreeperEntity>> renderer) {
            super(renderer);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                           MagicCreeperEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

            if (entity.isInvisible()) return;

            ItemStack hat = entity.getItemBySlot(EquipmentSlot.HEAD);
            if (hat.isEmpty()) return;

            GeoRenderProvider renderProvider = GeoRenderProvider.of(hat);
            if (renderProvider == null) return;

            HumanoidModel<?> armorModel = renderProvider.getGeoArmorRenderer(entity, hat, EquipmentSlot.HEAD, null);
            if (!(armorModel instanceof GeoArmorRenderer geoRenderer)) return;

            geoRenderer.prepForRender(entity, hat, EquipmentSlot.HEAD, armorModel);

            armorModel.setAllVisible(false);
            armorModel.head.visible = true;
            armorModel.hat.visible = true;

            poseStack.pushPose();

            ModelPart head = this.getParentModel().root().getChild("head");
            head.translateAndRotate(poseStack);

            poseStack.translate(0.0F, -0.05F, 0.0F);

            geoRenderer.renderToBuffer(poseStack, null, packedLight,
                    OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }
}
