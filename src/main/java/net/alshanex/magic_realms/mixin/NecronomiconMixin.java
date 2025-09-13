package net.alshanex.magic_realms.mixin;

import io.redspace.ironsspellbooks.item.NecronomiconSpellBook;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

@Mixin(NecronomiconSpellBook.class)
public abstract class NecronomiconMixin extends SpellBook {
    @Override
    public List<Component> getPages(ItemStack stack) {
        // Get the original pages from the parent class
        List<Component> originalPages = new ArrayList<>(super.getPages(stack));

        // Create the custom pages
        Component customPage = createBloodInfusingExperimentPage();
        originalPages.add(customPage);
        Component customPage2 = createBloodInfusingExperimentResultPage();
        originalPages.add(customPage2);
        Component customPage3 = createBloodInfusingExperimentResultPage2();
        originalPages.add(customPage3);

        return originalPages;
    }

    private Component createBloodInfusingExperimentPage() {
        var titleStyle = Style.EMPTY
                .withColor(0x8B0000)
                .withUnderlined(true)
                .withBold(true);

        var title = Component.translatable("ui.magic_realms.blood_experiment_title").withStyle(titleStyle);

        var experimentText = Component.translatable("ui.magic_realms.blood_experiment_text").withStyle(ChatFormatting.BLACK);

        return Component.literal("")
                .append(title)
                .append("\n\n")
                .append(experimentText);
    }

    private Component createBloodInfusingExperimentResultPage() {
        var titleStyle = Style.EMPTY
                .withColor(0x8B0000)
                .withUnderlined(true)
                .withBold(true);

        var title = Component.translatable("ui.magic_realms.blood_experiment_title_2").withStyle(titleStyle);

        var experimentText = Component.translatable("ui.magic_realms.blood_experiment_result").withStyle(ChatFormatting.BLACK);

        return Component.literal("")
                .append(title)
                .append("\n\n")
                .append(experimentText);
    }

    private Component createBloodInfusingExperimentResultPage2() {
        var titleStyle = Style.EMPTY
                .withColor(0x8B0000)
                .withUnderlined(true)
                .withBold(true);

        var title = Component.translatable("ui.magic_realms.blood_experiment_title_3").withStyle(titleStyle);

        var experimentText = Component.translatable("ui.magic_realms.blood_experiment_result_2").withStyle(ChatFormatting.BLACK);

        return Component.literal("")
                .append(title)
                .append("\n\n")
                .append(experimentText);
    }
}
