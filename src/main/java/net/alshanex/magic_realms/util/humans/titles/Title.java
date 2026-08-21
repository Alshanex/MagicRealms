package net.alshanex.magic_realms.util.humans.titles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.TitleProgressData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A datapack-loaded mercenary title.
 *
 * <p>Each file at {@code data/<namespace>/mercenaries/titles/<name>.json} becomes one of these.
 *
 * <p>A full example ({@code data/magic_realms/mercenaries/titles/dragonslayer.json}):
 * <pre>{@code
 * {
 *   "display_key": "title.magic_realms.dragonslayer",
 *   "description_key": "title.magic_realms.dragonslayer.desc",
 *   "color": "#D45CFF",
 *   "priority": 100,
 *   "requirement_mode": "all",
 *   "requirements": [
 *     { "type": "kill_entity", "target": "minecraft:ender_dragon", "amount": 1 }
 *   ],
 *   "rewards": {
 *     "attribute_modifiers": [
 *       { "attribute": "minecraft:generic.attack_damage", "id": "magic_realms:dragonslayer", "amount": 3.0, "operation": "add_value" }
 *     ]
 *   }
 * }
 * }</pre>
 */
public record Title(
        ResourceLocation id,
        String displayKey,
        String descriptionKey,
        String color,
        int priority,
        List<TitleRequirement> requirements,
        boolean requireAll,
        TitleRewards rewards,
        boolean announce,
        boolean hidden
) {

    /** Placeholder id used between JSON parsing and {@link #withId(ResourceLocation)}. */
    private static final ResourceLocation UNIDENTIFIED =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "unidentified");

    /** Fallback colour when the JSON omits one or specifies something unparseable. */
    public static final int DEFAULT_COLOR = 0xFFD700;

    /**
     * {@code "all"} (default) requires every listed requirement; {@code "any"} requires at least one. Written as a
     * string in JSON because it reads better than a boolean named {@code require_all}.
     */
    private static final Codec<Boolean> MODE_CODEC = Codec.STRING.xmap(
            s -> !"any".equalsIgnoreCase(s),
            b -> b ? "all" : "any"
    );

    public static final Codec<Title> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("display_key").forGetter(Title::displayKey),
            Codec.STRING.optionalFieldOf("description_key", "").forGetter(Title::descriptionKey),
            Codec.STRING.optionalFieldOf("color", "").forGetter(Title::color),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Title::priority),
            TitleRequirement.CODEC.listOf().optionalFieldOf("requirements", List.of()).forGetter(Title::requirements),
            MODE_CODEC.optionalFieldOf("requirement_mode", true).forGetter(Title::requireAll),
            TitleRewards.CODEC.optionalFieldOf("rewards", TitleRewards.NONE).forGetter(Title::rewards),
            Codec.BOOL.optionalFieldOf("announce", true).forGetter(Title::announce),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Title::hidden)
    ).apply(instance, (displayKey, descKey, color, priority, reqs, requireAll, rewards, announce, hidden) ->
            new Title(UNIDENTIFIED, displayKey, descKey, color, priority, reqs, requireAll, rewards, announce, hidden)));

    /** Returns a copy with the id populated. Called by the reload listener with the file's resource location. */
    public Title withId(ResourceLocation newId) {
        return new Title(newId, displayKey, descriptionKey, color, priority,
                requirements, requireAll, rewards, announce, hidden);
    }

    /**
     * True when this mercenary currently meets the requirements. A title with no requirements at all is never
     * auto-earned - it can only be granted explicitly (by command, or by another system), which is what you want for story/reward titles.
     */
    public boolean isEarnedBy(AbstractMercenaryEntity entity, TitleProgressData data) {
        if (requirements.isEmpty()) return false;

        if (requireAll) {
            for (TitleRequirement req : requirements) {
                if (!req.isSatisfied(entity, data)) return false;
            }
            return true;
        }

        for (TitleRequirement req : requirements) {
            if (req.isSatisfied(entity, data)) return true;
        }
        return false;
    }

    /** Overall completion, averaged across requirements. Handy for a "closest title" progress readout. */
    public float progressFor(AbstractMercenaryEntity entity, TitleProgressData data) {
        if (requirements.isEmpty()) return 0.0f;

        if (requireAll) {
            float sum = 0.0f;
            for (TitleRequirement req : requirements) sum += req.progressFraction(entity, data);
            return sum / requirements.size();
        }

        float best = 0.0f;
        for (TitleRequirement req : requirements) best = Math.max(best, req.progressFraction(entity, data));
        return best;
    }

    /** Resolved display colour. Accepts {@code "#RRGGBB"}, {@code "RRGGBB"} or a vanilla {@link ChatFormatting} name. */
    public int resolvedColor() {
        if (color == null || color.isEmpty()) return DEFAULT_COLOR;

        String raw = color.trim();
        if (raw.startsWith("#")) raw = raw.substring(1);

        try {
            return Integer.parseInt(raw, 16);
        } catch (NumberFormatException ignored) {
            // fall through to the named-colour lookup
        }

        ChatFormatting named = ChatFormatting.getByName(color.trim().toLowerCase(Locale.ROOT));
        if (named != null && named.getColor() != null) return named.getColor();

        return DEFAULT_COLOR;
    }

    /** The title text, coloured, ready to drop into a nameplate or chat line. */
    public MutableComponent displayComponent() {
        return Component.translatable(displayKey)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(resolvedColor())));
    }

    public MutableComponent descriptionComponent() {
        return descriptionKey == null || descriptionKey.isEmpty()
                ? Component.empty()
                : Component.translatable(descriptionKey);
    }

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, Title t) {
        buf.writeResourceLocation(t.id);
        buf.writeUtf(t.displayKey);
        buf.writeUtf(t.descriptionKey == null ? "" : t.descriptionKey);
        buf.writeUtf(t.color == null ? "" : t.color);
        buf.writeVarInt(t.priority);

        buf.writeVarInt(t.requirements.size());
        for (TitleRequirement req : t.requirements) TitleRequirement.writeToBuf(buf, req);

        buf.writeBoolean(t.requireAll);
        TitleRewards.writeToBuf(buf, t.rewards);
        buf.writeBoolean(t.announce);
        buf.writeBoolean(t.hidden);
    }

    public static Title readFromBuf(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        String displayKey = buf.readUtf();
        String descriptionKey = buf.readUtf();
        String color = buf.readUtf();
        int priority = buf.readVarInt();

        int reqCount = buf.readVarInt();
        List<TitleRequirement> reqs = new ArrayList<>(reqCount);
        for (int i = 0; i < reqCount; i++) reqs.add(TitleRequirement.readFromBuf(buf));

        boolean requireAll = buf.readBoolean();
        TitleRewards rewards = TitleRewards.readFromBuf(buf);
        boolean announce = buf.readBoolean();
        boolean hidden = buf.readBoolean();

        return new Title(id, displayKey, descriptionKey, color, priority, reqs, requireAll, rewards, announce, hidden);
    }
}
