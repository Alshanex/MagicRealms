package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanTeamScreen extends AbstractContainerScreen<HumanTeamMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_team.png");

    public HumanTeamScreen(HumanTeamMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // Establecer el tamaño de la GUI para que coincida con tu textura
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Ocultar etiquetas por defecto si es necesario
        this.inventoryLabelY = 10000; // Mover fuera de la pantalla
        this.titleLabelY = 10000; // Mover fuera de la pantalla
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Renderizar el fondo primero
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Renderizar el contenedor (esto maneja los slots automáticamente)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Renderizar títulos personalizados si es necesario
        renderCustomLabels(guiGraphics);

        // Renderizar tooltips al final
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderCustomLabels(GuiGraphics guiGraphics) {
        // Renderizar título personalizado
        Component title = Component.translatable("gui.magic_realms.human_team.title");
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, leftPos + titleX, topPos + 6, 0x404040, false);

        // Renderizar etiqueta del inventario
        Component inventoryLabel = Component.translatable("container.inventory");
        guiGraphics.drawString(font, inventoryLabel, leftPos + 8, topPos + 72, 0x404040, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
