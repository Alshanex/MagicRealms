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
        add(MRItems.HUMAN_INFO_ITEM.get(), "Human Information Scroll");

        // GUI translations
        add("gui.magic_realms.human_info.title", "Entity Information");
        add("gui.magic_realms.human_info.tab.iron_spells", "Iron Spells");
        add("gui.magic_realms.human_info.tab.apothic", "Apothic");
        add("gui.magic_realms.human_info.tab.traits", "Traits");
        add("gui.magic_realms.human_info.level", "Level");
        add("gui.magic_realms.human_info.class", "Class");
        add("gui.magic_realms.human_info.stars", "Stars");
        add("gui.magic_realms.human_info.health", "Health");
        add("gui.magic_realms.human_info.armor", "Armor");
        add("gui.magic_realms.human_info.damage", "Damage");
        add("gui.magic_realms.human_info.no_traits", "No traits");
        add("gui.magic_realms.human_info.no_attributes", "No attributes");
        add("gui.magic_realms.human_info.entity_not_found", "Entity not found");
        add("gui.magic_realms.human_info.not_linked", "Not linked to entity");

        //Entities
        add("entity.magic_realms.warden_clone", "Sculk Guardian");
    }
}
