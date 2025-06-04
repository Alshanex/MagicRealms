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
        add(MRItems.CONTRACT_NOVICE.get(), "Novice Contract");
        add(MRItems.CONTRACT_APPRENTICE.get(), "Apprentice Contract");
        add(MRItems.CONTRACT_JOURNEYMAN.get(), "Journeyman Contract");
        add(MRItems.CONTRACT_EXPERT.get(), "Expert Contract");
        add(MRItems.CONTRACT_MASTER.get(), "Master Contract");
        add(MRItems.CONTRACT_PERMANENT.get(), "Permanent Contract");

        // Contract tooltips
        add("tooltip.magic_realms.contract.level_range", "Entity Level Range:");
        add("tooltip.magic_realms.contract.usage", "Usage:");
        add("tooltip.magic_realms.contract.usage_desc", "Right-click on entities to hire them");
        add("tooltip.magic_realms.permanent_contract.level_range", "Entity Level Range:");
        add("tooltip.magic_realms.permanent_contract.duration", "Duration:");
        add("tooltip.magic_realms.permanent_contract.duration_desc", "Permanent - Never expires!");
        add("tooltip.magic_realms.permanent_contract.warning", "This contract cannot be undone!");
        add("tooltip.magic_realms.permanent_contract.requirement", "Requires 200+ minutes of previous contracts with entity");

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
        add("ui.magic_realms.wrong_contract_tier", "This entity (Level %d) requires a %s contract!");
        add("ui.magic_realms.entity_level_too_high", "This entity (Level %d) is too powerful for this contract!");
        add("ui.magic_realms.entity_level_too_low", "This entity (Level %d) doesn't require such a powerful contract!");
        add("ui.magic_realms.contract_tier_mismatch", "Level %d entities need %s (Level %d-%d)");
        add("ui.magic_realms.contract_established_permanent", "Permanent contract established with %s!");
        add("ui.magic_realms.contract_upgraded_permanent", "Contract with %s upgraded to permanent!");
        add("ui.magic_realms.contract_already_permanent", "%s is already under a permanent contract with you!");
        add("ui.magic_realms.contract_time_permanent", "Contract: Permanent (Never expires)");
        add("ui.magic_realms.contract_permanent_no_upgrade", "%s already has a permanent contract - no upgrade needed!");
        add("ui.magic_realms.contract_permanent_other_player", "%s has a permanent contract with another player!");
        add("ui.magic_realms.contract_failed", "Failed to establish contract - please try again!");

        // New permanent contract requirement messages
        add("ui.magic_realms.permanent_contract_insufficient_time", "Need %d more contract minutes with this entity!");
        add("ui.magic_realms.permanent_contract_progress", "Contract progress: %d/%d minutes towards permanent contract eligibility");
        add("ui.magic_realms.permanent_contract_available", "Eligible for permanent contract!");
        add("ui.magic_realms.permanent_contract_progress_remaining", "Progress: %d/%d minutes (%d more needed for permanent contract)");
    }
}
