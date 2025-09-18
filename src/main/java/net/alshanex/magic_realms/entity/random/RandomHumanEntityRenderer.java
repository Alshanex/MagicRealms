package net.alshanex.magic_realms.entity.random;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.EntityTextureConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RandomHumanEntityRenderer extends AbstractMercenaryEntityRenderer {

    public RandomHumanEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RandomHumanEntityModel());
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob entity) {
        if (entity instanceof RandomHumanEntity human) {
            String entityUUID = human.getUUID().toString();
            Minecraft mc = Minecraft.getInstance();
            human.debugTextureGeneration();

            // Priority 1: Received server texture (multiplayer)
            CombinedTextureManager.TextureResult receivedTexture =
                    CombinedTextureManager.getReceivedTexture(entityUUID);
            if (receivedTexture != null) {
                return receivedTexture.getTextureLocation();
            }

            // Priority 2: Entity's own texture config (should be deterministic)
            EntityTextureConfig config = human.getTextureConfig();

            // If config is null, try to create one using the entity (deterministic)
            if (config == null && human.getGender() != null && human.getEntityClass() != null) {
                try {
                    // Create deterministic texture config using entity - this ensures consistency
                    config = new EntityTextureConfig(human);
                } catch (Exception e) {
                    MagicRealms.LOGGER.error("Failed to create texture config in renderer for entity: {}", entityUUID, e);
                }
            }

            if (config != null && config.hasValidTexture()) {
                return config.getSkinTexture();
            }

            // Priority 3: Cached texture
            ResourceLocation cachedTexture = CombinedTextureManager.getCachedTexture(entityUUID);
            if (cachedTexture != null) {
                return cachedTexture;
            }
        }

        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }
}
