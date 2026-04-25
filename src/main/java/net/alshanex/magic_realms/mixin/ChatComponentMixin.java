package net.alshanex.magic_realms.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.ChatFaceRenderer;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.ChatLineFaceTable;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

/**
 * Adds inline face icons to chat lines from mercenaries and tavernkeeper.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Final
    @Shadow
    private List<GuiMessage.Line> trimmedMessages;

    /**
     * After {@code addMessageToDisplayQueue} finishes its loop and 100-line cap-trim, the most recent N lines are at indices 0..N-1 of
     * {@code trimmedMessages} (where N is the wrap-result list size). The visually-top line is at index {@code N - 1}; that's the only one
     * we want to tag with a face.
     */
    @Inject(
            method = "addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V",
            at = @At("TAIL"),
            require = 0
    )
    private void magic_realms$tagTopLineOfMessage(GuiMessage message, CallbackInfo ci,
                                                  @Local List<FormattedCharSequence> wrappedLines) {
        if (this.trimmedMessages == null || this.trimmedMessages.isEmpty()) return;
        if (wrappedLines == null || wrappedLines.isEmpty()) return;

        // Visually-top line is at index (wrap-count - 1). Clamp in case the cap-trim removed any of our new lines (vanishingly rare).
        int topIndex = Math.min(wrappedLines.size() - 1, this.trimmedMessages.size() - 1);
        if (topIndex < 0) return;

        GuiMessage.Line topLine = this.trimmedMessages.get(topIndex);
        ChatLineFaceTable.registerLineForMessage(topLine, message.content());
    }

    /**
     * Wrap the per-line text draw in {@code render}. If the line is one of ours, draw the face on the left and shift text right.
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"
            ),
            require = 0
    )
    private int magic_realms$wrapDrawString(GuiGraphics graphics, Font font, FormattedCharSequence text,
                                            int x, int y, int color,
                                            Operation<Integer> original,
                                            @Local GuiMessage.Line currentLine) {
        UUID faceUUID = ChatLineFaceTable.faceFor(currentLine);
        if (faceUUID == null) {
            return original.call(graphics, font, text, x, y, color);
        }

        boolean drew = ChatFaceRenderer.draw(graphics, faceUUID, x, y);
        int shiftedX = drew ? (x + ChatFaceRenderer.totalWidth()) : x;
        return original.call(graphics, font, text, shiftedX, y, color);
    }
}
