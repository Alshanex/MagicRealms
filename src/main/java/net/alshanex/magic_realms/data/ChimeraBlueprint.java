package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.util.chimera.ChimeraAssembly;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Data component that stores a chimera's assembly configuration on a ChimeraCatalystItem.
 * When the component is present and contains a complete assembly, the item can spawn a chimera.
 * When absent or incomplete, the player can open the assembly GUI to configure it.
 */
public record ChimeraBlueprint(Optional<ChimeraAssembly> assembly, Optional<String> customName) {

    public static final ChimeraBlueprint EMPTY = new ChimeraBlueprint(Optional.empty(), Optional.empty());

    public static final Codec<ChimeraBlueprint> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ChimeraAssembly.CODEC.optionalFieldOf("assembly").forGetter(ChimeraBlueprint::assembly),
                    Codec.STRING.optionalFieldOf("custom_name").forGetter(ChimeraBlueprint::customName)
            ).apply(instance, ChimeraBlueprint::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChimeraBlueprint> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /**
     * Whether this blueprint has a complete, ready-to-spawn assembly.
     */
    public boolean isComplete() {
        return assembly.isPresent() && assembly.get().isComplete();
    }

    /**
     * Create a blueprint with the given assembly and no custom name.
     */
    public static ChimeraBlueprint of(ChimeraAssembly assembly) {
        return new ChimeraBlueprint(Optional.of(assembly), Optional.empty());
    }

    /**
     * Create a blueprint with the given assembly and custom name.
     */
    public static ChimeraBlueprint of(ChimeraAssembly assembly, String customName) {
        Optional<String> name = (customName != null && !customName.isBlank())
                ? Optional.of(customName) : Optional.empty();
        return new ChimeraBlueprint(Optional.of(assembly), name);
    }
}

