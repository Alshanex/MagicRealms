package net.alshanex.magic_realms.util.humans.mercenaries.chat;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Builds chat messages where the speaker's name is colored (deterministic per-entity) and the rest of the message is plain white.
 */
public final class MercenaryMessageFormatter {

    /** Prefix written into {@link Style#getInsertion()} so we can recognize our tagged messages on the client. */
    public static final String FACE_INSERTION_PREFIX = "magic_realms_face:";

    /**
     * Palette of "looks-fine on a dark chat background" {@link ChatFormatting} colors. Excludes white, black, dark gray, and gray.
     * Order doesn't matter; we pick by hash.
     */
    private static final ChatFormatting[] NAME_PALETTE = new ChatFormatting[] {
            ChatFormatting.DARK_RED,
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.DARK_GREEN,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.DARK_PURPLE
    };

    private MercenaryMessageFormatter() {}

    /**
     * Pick a stable, "looks fine" name color for the given UUID. Same UUID always returns the same color.
     */
    public static ChatFormatting colorFor(UUID uuid) {
        if (uuid == null) return ChatFormatting.GOLD;
        // mix the high and low bits so similar consecutive-spawn UUIDs don't all collide on one color
        long mixed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        int idx = (int) (Math.floorMod(mixed, NAME_PALETTE.length));
        return NAME_PALETTE[idx];
    }

    /**
     * Build a translatable message component where {@code %1$s} (the first arg) is the entity's name in its assigned color, and the
     * rest of the message body is plain white. The wrapper carries a face-insertion tag so the client mixin can render the entity's face
     * before the line.
     */
    public static MutableComponent build(Entity entity, String entityName, String translationKey, Object... extraArgs) {
        return buildWithBodyColor(entity, entityName, translationKey, ChatFormatting.WHITE, extraArgs);
    }

    public static MutableComponent buildWithBodyColor(Entity entity, String entityName, String translationKey,
                                                      ChatFormatting bodyColor, Object... extraArgs) {
        UUID uuid = entity == null ? null : entity.getUUID();
        ChatFormatting nameColor = colorFor(uuid);

        MutableComponent nameComponent = Component.literal(entityName == null ? "" : entityName)
                .withStyle(nameColor);

        Object[] args = new Object[1 + (extraArgs == null ? 0 : extraArgs.length)];
        args[0] = nameComponent;
        if (extraArgs != null) {
            System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        }

        MutableComponent message = Component.translatable(translationKey, args)
                .withStyle(Style.EMPTY.withColor(bodyColor == null ? ChatFormatting.WHITE : bodyColor));

        if (uuid != null) {
            message = message.withStyle(s -> s.withInsertion(FACE_INSERTION_PREFIX + uuid));
        }

        return message;
    }

    /** Convenience: pulls the name from {@code merc.getEntityName()} and forwards to {@link #build}. */
    public static MutableComponent buildFor(AbstractMercenaryEntity merc, String translationKey, Object... extraArgs) {
        return build(merc, merc == null ? "" : merc.getEntityName(), translationKey, extraArgs);
    }

    /** Convenience: pulls the name from {@code tavernkeep.getName().getString()} and forwards to {@link #build}. */
    public static MutableComponent buildFor(TavernKeeperEntity tavernkeep, String translationKey, Object... extraArgs) {
        String name = tavernkeep == null ? "" : tavernkeep.getName().getString();
        return build(tavernkeep, name, translationKey, extraArgs);
    }

    /** Convenience with body color, for tavernkeeper denial-style lines that need to stay non-white. */
    public static MutableComponent buildForWithBodyColor(TavernKeeperEntity tavernkeep, String translationKey,
                                                         ChatFormatting bodyColor, Object... extraArgs) {
        String name = tavernkeep == null ? "" : tavernkeep.getName().getString();
        return buildWithBodyColor(tavernkeep, name, translationKey, bodyColor, extraArgs);
    }

    /** Convenience with body color, for mercenary lines that need a non-white body (rare, but available for symmetry). */
    public static MutableComponent buildForWithBodyColor(AbstractMercenaryEntity merc, String translationKey,
                                                         ChatFormatting bodyColor, Object... extraArgs) {
        return buildWithBodyColor(merc, merc == null ? "" : merc.getEntityName(), translationKey, bodyColor, extraArgs);
    }

    /**
     * Variant for messages that take TWO names (e.g. Amadeus combat lines that say "X says Y is in trouble"). Both names are colored.
     * The face on the chat line belongs to the speaker (first arg), not the second name.
     */
    public static MutableComponent buildTwoNamed(Entity speaker, String speakerName,
                                                 Component secondName,
                                                 String translationKey, Object... extraArgs) {
        UUID uuid = speaker == null ? null : speaker.getUUID();
        ChatFormatting nameColor = colorFor(uuid);

        MutableComponent firstNameComponent = Component.literal(speakerName == null ? "" : speakerName)
                .withStyle(nameColor);

        Object[] args = new Object[2 + (extraArgs == null ? 0 : extraArgs.length)];
        args[0] = firstNameComponent;
        args[1] = secondName;
        if (extraArgs != null) {
            System.arraycopy(extraArgs, 0, args, 2, extraArgs.length);
        }

        MutableComponent message = Component.translatable(translationKey, args)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));

        if (uuid != null) {
            message = message.withStyle(s -> s.withInsertion(FACE_INSERTION_PREFIX + uuid));
        }

        return message;
    }

    /**
     * Extract the UUID embedded in a message's insertion tag, if any.
     * Returns null if the component isn't one of ours.
     */
    public static UUID extractFaceUUID(Component component) {
        if (component == null) return null;
        Style style = component.getStyle();
        if (style == null) return null;
        String insertion = style.getInsertion();
        if (insertion == null || !insertion.startsWith(FACE_INSERTION_PREFIX)) return null;
        try {
            return UUID.fromString(insertion.substring(FACE_INSERTION_PREFIX.length()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
