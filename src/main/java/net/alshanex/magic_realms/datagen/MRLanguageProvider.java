package net.alshanex.magic_realms.datagen;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.registry.MRItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class MRLanguageProvider extends LanguageProvider {

    public MRLanguageProvider(PackOutput output) {
        super(output, MagicRealms.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Items
        add(MRItems.HUMAN_INFO_ITEM.get(), "Contract");

        // GUI translations
        add("gui.magic_realms.human_info.title", "Entity Information");
        add("gui.magic_realms.human_info.tab.iron_spells", "Iron Spells");
        add("gui.magic_realms.human_info.tab.apothic", "Apothic");
        add("gui.magic_realms.human_info.level", "Level");
        add("gui.magic_realms.human_info.class", "Class");
        add("gui.magic_realms.human_info.stars", "Stars");
        add("gui.magic_realms.human_info.health", "Health");
        add("gui.magic_realms.human_info.armor", "Armor");
        add("gui.magic_realms.human_info.damage", "Damage");
        add("gui.magic_realms.human_info.no_attributes", "No attributes");

        // Contract messages (Action Bar)
        add("ui.magic_realms.already_have_contract", "This entity is already under contract with another player!");
        add("ui.magic_realms.contract_established", "Contract established with %s for %d minutes!");
        add("ui.magic_realms.contract_extended", "Contract with %s extended by %d minutes! Time remaining: %d:%02d");
        add("ui.magic_realms.contract_other_player", "This entity is under contract with another player!");
        add("ui.magic_realms.need_contract_item", "You need a Contract to hire this entity for %d minutes!");
        add("ui.magic_realms.contract_time_remaining", "Contract time remaining: %d:%02d");
        add("ui.magic_realms.contract_time_remaining_with_extension", "Time remaining: %d:%02d (Use contract item to add %d more minutes)");
        add("ui.magic_realms.contract_expired", "Contract with %s has expired!");
    }
}
