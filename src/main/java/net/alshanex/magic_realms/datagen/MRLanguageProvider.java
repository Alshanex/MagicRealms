package net.alshanex.magic_realms.datagen;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.registry.MRCreativeTab;
import net.alshanex.magic_realms.registry.MREntityRegistry;
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
        add(MRItems.HELL_PASS.get(), "Hell's Pass");
        add(MRItems.TIME_ESSENCE.get(), "Time Essence");

        add(MREntityRegistry.HUMAN.get(), "Human");
        add(MREntityRegistry.TAVERNKEEP.get(), "Tavernkeep");

        add("effect.magic_realms.stun", "Stunned");

        add("itemGroup.magic_realms.main_tab", "Magic Realms");

        // Contract tooltips
        add("tooltip.magic_realms.contract.level_range", "Entity Level Range:");
        add("tooltip.magic_realms.contract.usage", "Usage:");
        add("tooltip.magic_realms.contract.usage_desc", "Right-click on entities to hire them");
        add("tooltip.magic_realms.permanent_contract.level_range", "Entity Level Range:");
        add("tooltip.magic_realms.permanent_contract.duration", "Duration:");
        add("tooltip.magic_realms.permanent_contract.duration_desc", "Permanent - Never expires!");
        add("tooltip.magic_realms.permanent_contract.warning", "This contract cannot be undone!");
        add("tooltip.magic_realms.permanent_contract.requirement", "Requires 200+ minutes of previous contracts with entity");
        add("tooltip.magic_realms.hell_pass", "Give to any human to allow him survive fatal hits");

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
        add("ui.magic_realms.already_have_contract", "%s: Sorry, i'm already contracted by someone else");
        add("ui.magic_realms.contract_established", "%s: Okay, i'll accept the contract for %d minutes");
        add("ui.magic_realms.contract_extended", "%s: Fine, i'll extend our contract %d minutes");
        add("ui.magic_realms.contract_time_remaining_with_extension", "%s: In case you forgot, the contract will last for %d:%02d");
        add("ui.magic_realms.contract_expired", "%s: I've completed our contract, see you");
        add("ui.magic_realms.entity_level_too_low", "%s: Sorry but i can't accept that tier of contracts");
        add("ui.magic_realms.contract_established_permanent", "%s: Fine, i'll accept the permanent contract");
        add("ui.magic_realms.contract_already_permanent", "%s: I don't need anymore contracts");
        add("ui.magic_realms.contract_time_permanent", "%s: In case you forgot, our contract never expires");
        add("ui.magic_realms.contract_failed", "Failed to establish contract - please try again!");
        add("ui.magic_realms.patrol_active", "PATROL MODE");
        add("ui.magic_realms.patrol_following", "FOLLOWING");

        //Human introductions
        add("ui.magic_realms.introduction.warrior", "%s: Hello, i'm a mercenary specialized in close range combat and my position is usually tank. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.assassin", "%s: Hello, i'm a mercenary specialized in close range combat and my specialty is dodging attacks and dealing high damage. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.archer", "%s: Hello, i'm a mercenary specialized in long range combat with my bow. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.mage", "%s: Hello, i'm a mercenary specialized in fighting with magic. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.default", "%s: Hello, i'm a mercenary ready to serve. I only accept a %s, which will last for %d minutes");

        // New permanent contract requirement messages
        add("ui.magic_realms.permanent_contract_insufficient_time", "%s: I don't completely trust you, sorry, maybe ask me in %d minutes?");

        add("message.magic_realms.already_immortal", "%s: Thanks but i don't need that, i already have my own pass");
        add("message.magic_realms.granted_immortality", "%s: Oh, i've been searching for this pass for a long time, thank you so much!");
        add("ui.magic_realms.no_items_to_trade", "%s: Sorry, i have nothing i can give you for that emerald");
        add("ui.magic_realms.trade_success", "%s: Thanks for the emerald! Take this in return");
    }
}
