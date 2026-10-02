package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.humans.RandomHumanEntity;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.IChatFaceProvider;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.TextureComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot of one contracted mercenary, as shown in the contracted mercenaries screen.
 */
public record ContractedMercenaryEntry(UUID uuid,
                                       String name,
                                       Component combatClass,
                                       float health,
                                       float maxHealth,
                                       Face face,
                                       boolean permanent,
                                       long expiresAt,
                                       BlockPos lastPos) {

    public static final Codec<ContractedMercenaryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(ContractedMercenaryEntry::uuid),
            Codec.STRING.fieldOf("name").forGetter(ContractedMercenaryEntry::name),
            ComponentSerialization.CODEC.fieldOf("combat_class").forGetter(ContractedMercenaryEntry::combatClass),
            Codec.FLOAT.fieldOf("health").forGetter(ContractedMercenaryEntry::health),
            Codec.FLOAT.fieldOf("max_health").forGetter(ContractedMercenaryEntry::maxHealth),
            Face.CODEC.optionalFieldOf("face", Face.NONE).forGetter(ContractedMercenaryEntry::face),
            Codec.BOOL.fieldOf("permanent").forGetter(ContractedMercenaryEntry::permanent),
            Codec.LONG.fieldOf("expires_at").forGetter(ContractedMercenaryEntry::expiresAt),
            BlockPos.CODEC.fieldOf("last_pos").forGetter(ContractedMercenaryEntry::lastPos)
    ).apply(i, ContractedMercenaryEntry::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ContractedMercenaryEntry> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static ContractedMercenaryEntry of(AbstractMercenaryEntity merc, ContractData contract) {
        String name = merc.getEntityName();
        return new ContractedMercenaryEntry(
                merc.getUUID(),
                name == null ? "" : name,
                merc.getCombatClass().displayName(merc),
                round1(merc.getHealth()),
                round1(merc.getMaxHealth()),
                Face.of(merc),
                contract.isPermanent(),
                contract.isPermanent() ? -1L : contract.getContractEndTime(),
                merc.blockPosition()
        );
    }

    /** Mirrors {@link ContractData#hasActiveContract}: active while gameTime < end. */
    public boolean isExpired(long gameTime) {
        return !permanent && gameTime >= expiresAt;
    }

    /** True when everything the screen shows is unchanged (position ignored). */
    public boolean sameDisplayAs(@Nullable ContractedMercenaryEntry o) {
        return o != null
                && name.equals(o.name)
                && Objects.equals(combatClass, o.combatClass)
                && Float.compare(health, o.health) == 0
                && Float.compare(maxHealth, o.maxHealth) == 0
                && face.equals(o.face)
                && permanent == o.permanent
                && expiresAt == o.expiresAt;
    }

    /** One decimal is plenty for display and avoids a resync on every tiny regen tick. */
    private static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    // Face

    /**
     * Enough to draw the face client-side without the entity loaded.
     */
    public record Face(boolean layered, String texture, String clothes, String eyes, String hair) {

        public static final Face NONE = new Face(false, "", "", "", "");

        public static final Codec<Face> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("layered", false).forGetter(Face::layered),
                Codec.STRING.optionalFieldOf("texture", "").forGetter(Face::texture),
                Codec.STRING.optionalFieldOf("clothes", "").forGetter(Face::clothes),
                Codec.STRING.optionalFieldOf("eyes", "").forGetter(Face::eyes),
                Codec.STRING.optionalFieldOf("hair", "").forGetter(Face::hair)
        ).apply(i, Face::new));

        /** Server-safe: never calls client-only methods. */
        public static Face of(AbstractMercenaryEntity merc) {
            // RandomHumanEntity#getChatFaceTextureCS is @OnlyIn(CLIENT); read the synced metadata instead.
            if (merc instanceof RandomHumanEntity human) {
                CompoundTag metadata = human.getTextureMetadata();
                if (metadata == null || metadata.isEmpty()) return NONE;

                TextureComponents tc = TextureComponents.fromMetadata(metadata);
                if (tc.isPresetTexture()) {
                    ResourceLocation asset = toAssetPath(tc.getSkinTexture());
                    return asset == null ? NONE : new Face(false, asset.toString(), "", "", "");
                }
                return new Face(true,
                        nz(tc.getSkinTexture()), nz(tc.getClothesTexture()),
                        nz(tc.getEyesTexture()), nz(tc.getHairTexture()));
            }

            // Exclusive mercenaries return a constant asset path and are safe to call server-side.
            if (merc instanceof IChatFaceProvider provider) {
                ResourceLocation tex = provider.getChatFaceTextureCS();
                return tex == null ? NONE : new Face(false, tex.toString(), "", "", "");
            }
            return NONE;
        }

        public TextureComponents toTextureComponents() {
            return new TextureComponents(blankToNull(texture), blankToNull(clothes),
                    blankToNull(eyes), blankToNull(hair), null, false);
        }

        @Nullable
        private static ResourceLocation toAssetPath(@Nullable String textureId) {
            if (textureId == null || textureId.isEmpty()) return null;
            ResourceLocation parsed = ResourceLocation.tryParse(textureId);
            if (parsed == null) return null;
            return ResourceLocation.fromNamespaceAndPath(parsed.getNamespace(), "textures/" + parsed.getPath() + ".png");
        }

        private static String nz(@Nullable String s) { return s == null ? "" : s; }
        @Nullable private static String blankToNull(String s) { return s == null || s.isEmpty() ? null : s; }
    }
}