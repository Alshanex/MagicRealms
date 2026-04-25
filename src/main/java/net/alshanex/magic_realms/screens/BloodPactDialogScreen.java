package net.alshanex.magic_realms.screens;

import net.alshanex.magic_realms.network.TavernkeepBloodPactChoicePacket;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Shown when the player right-clicks the tavernkeeper while carrying a blood pact in their inventory. Two buttons:
 *   - Ask about blood pacts → triggers the existing tavernkeep tip message.
 *   - I want to buy something → opens the merchant trading screen.
 */
@OnlyIn(Dist.CLIENT)
public class BloodPactDialogScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 110;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 6;

    private final UUID tavernkeepUUID;

    public BloodPactDialogScreen(UUID tavernkeepUUID) {
        super(Component.translatable("ui.magic_realms.tavernkeep_dialog.title"));
        this.tavernkeepUUID = tavernkeepUUID;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int buttonsTopY = centerY - BUTTON_HEIGHT - (BUTTON_SPACING / 2);
        int firstButtonY = buttonsTopY;
        int secondButtonY = buttonsTopY + BUTTON_HEIGHT + BUTTON_SPACING;

        Button askButton = Button.builder(
                        Component.translatable("ui.magic_realms.tavernkeep_dialog.ask_blood_pacts"),
                        b -> sendChoice( MRUtils.Choice.ASK_ABOUT_BLOOD_PACTS))
                .bounds(centerX - BUTTON_WIDTH / 2, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        Button buyButton = Button.builder(
                        Component.translatable("ui.magic_realms.tavernkeep_dialog.buy"),
                        b -> sendChoice( MRUtils.Choice.OPEN_TRADES))
                .bounds(centerX - BUTTON_WIDTH / 2, secondButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        this.addRenderableWidget(askButton);
        this.addRenderableWidget(buyButton);
    }

    private void sendChoice( MRUtils.Choice choice) {
        PacketDistributor.sendToServer(new TavernkeepBloodPactChoicePacket(tavernkeepUUID, choice));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // Dim panel background
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xC0000000);
        // Border
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFFAAAAAA);
        guiGraphics.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFAAAAAA);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, 0xFFAAAAAA);
        guiGraphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFAAAAAA);

        // Question
        Component question = Component.translatable("ui.magic_realms.tavernkeep_dialog.question");
        int titleWidth = this.font.width(question);
        guiGraphics.drawString(this.font, question, centerX - titleWidth / 2, panelY + 10, 0xFFFFFF, true);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
