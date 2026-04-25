package net.alshanex.magic_realms.util.humans.mercenaries.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Bridge between the chat-component mixins and {@link ChatFaceRenderer}.
 */
@OnlyIn(Dist.CLIENT)
public final class ChatLineFaceTable {

    private static final WeakHashMap<GuiMessage.Line, UUID> LINE_TO_UUID = new WeakHashMap<>();

    private ChatLineFaceTable() {}

    /**
     * Called from the addMessage mixin once per newly created trimmed line. Stores the face UUID extracted from the source component, if any.
     */
    public static void registerLineForMessage(GuiMessage.Line line, Component sourceComponent) {
        if (line == null || sourceComponent == null) return;
        UUID uuid = MercenaryMessageFormatter.extractFaceUUID(sourceComponent);
        if (uuid != null) {
            LINE_TO_UUID.put(line, uuid);
        }
    }

    /**
     * Look up the face UUID for a line being rendered. Returns null if the line wasn't tagged.
     */
    public static UUID faceFor(GuiMessage.Line line) {
        if (line == null) return null;
        return LINE_TO_UUID.get(line);
    }

    /** Forget all stored bindings (e.g. on disconnect). */
    public static void clear() {
        LINE_TO_UUID.clear();
    }
}
