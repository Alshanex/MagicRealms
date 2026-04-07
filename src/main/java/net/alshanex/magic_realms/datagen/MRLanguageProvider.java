package net.alshanex.magic_realms.datagen;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.registry.MRBlocks;
import net.alshanex.magic_realms.registry.MREffects;
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
        add(MRItems.BLOOD_PACT.get(), "Blood Pact");
        add(MRItems.PERMANENT_BLOOD_PACT.get(), "Binding Blood Pact");
        add(MRItems.MIDAS_COIN.get(), "Midas Coin");

        add(MREntityRegistry.HUMAN.get(), "Human");
        add(MREntityRegistry.TAVERNKEEP.get(), "Tavernkeep");
        add(MREntityRegistry.ALSHANEX.get(), "Alshanex");
        add(MREntityRegistry.ALIANA.get(), "Aliana");
        add(MREntityRegistry.CATAS.get(), "Catas");
        add(MREntityRegistry.AMADEUS.get(), "Amadeus Voidwalker");

        add(MREffects.STUN.get(), "Stunned");

        add(MRBlocks.WOODEN_CHAIR.get(), "Wooden Chair");
        add(MRBlocks.WOODEN_CHAIR_SIMPLE.get(), "Wooden Chair");

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
        add("tooltip.magic_realms.blood_pact", "Has the ability to bind two souls forever");
        add("tooltip.magic_realms.raid_item", "Legends say that this coin can grant immense fortune to the user, but be careful since other people might want to snatch it if you try to use it.");

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

        // Contract messages
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
        add("ui.magic_realms.contract_reject_permanent", "%s: Sorry but i don't want to be tied to anyone forever");

        //Human introductions
        add("ui.magic_realms.introduction.warrior", "%s: Hello, i'm a mercenary specialized in close range combat and my position is usually tank. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.assassin", "%s: Hello, i'm a mercenary specialized in close range combat and my specialty is dodging attacks and dealing high damage. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.archer", "%s: Hello, i'm a mercenary specialized in long range combat with my bow. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.mage", "%s: Hello, i'm a mercenary specialized in fighting with magic. I only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.default", "%s: Hello, i'm a mercenary ready to serve. I only accept a %s, which will last for %d minutes");

        // Exclusive mercenaries introductions
        add("ui.magic_realms.introduction.alshanex", "%s: Hey there, i'm Alshanex, not the mod creator but just a random roaming mage, trust. Anyways, i'm a mage who likes to use Sunbeam and a couple movement spells, because i saw the tavernkeep fighting like this and thought he looked very cool. If you want to contract me, i'll only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.aliana","%s: MtheNinja? No no no that's not me..... But I am available for hire though!!! If you want to contract me, i'll only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.catas","%s: I don’t know why I’m here, I don’t drink and I don’t really know anyone. Anyways, if you want to contract me, i'll only accept a %s, which will last for %d minutes");
        add("ui.magic_realms.introduction.amadeus","%s: Salutations! I am Amadeus, the  Starfire Sorcerer! If you wish to contract me, i'll only accept a %s, which will last for %d minutes");

        // Catas keywords
        add("message.magic_realms.catas.moth.keyword", "moth");
        add("message.magic_realms.catas.moth.response", "%s: I love moths! Very adorable creatures, did you know that most of them don’t even have a mouth? it’s really interesti- you don’t care? Oh…");
        add("message.magic_realms.catas.geology.keyword_1", "volcano");
        add("message.magic_realms.catas.geology.keyword_2", "stone");
        add("message.magic_realms.catas.geology.keyword_3", "basalt");
        add("message.magic_realms.catas.geology.keyword_4", "andesite");
        add("message.magic_realms.catas.geology.keyword_5", "blackstone");
        add("message.magic_realms.catas.geology.response", "%s: Isn’t geology just super cool? I mean like, it’s incredible that you can tell the history of rock just by looking at it!");
        add("message.magic_realms.catas.pumpkin_pie.response", "%s: You have pumpkin pie?! Can I have some?! Please!?");
        add("message.magic_realms.catas.pumpkin_pie.thanks", "%s: YIPPEE! Thank you! I love pumpkin pie!");

        // Amadeus phrases
        add("message.magic_realms.amadeus.combat.entering", "%s: This is the Spell passed down in the Voidwalker Family for generations!");
        add("message.magic_realms.amadeus.combat.contractor_low_health", "%s: Hang in there, %s! You can do it!");
        add("message.magic_realms.amadeus.enderman.scared", "%s: If only Astias were still here, she was an Archmage familiar you know?");
        add("message.magic_realms.amadeus.enderman.killed", "%s: You know, you remind me of my Archmage Familiar Astias, she’s in a better place now.");
        add("message.magic_realms.amadeus.boss.killed", "%s: By the Arbiter, that was a difficult fight!");
        add("message.magic_realms.amadeus.ally.killed", "%s: Rest well comrade.");

        // Aliana phrases
        add("message.magic_realms.aliana.combat.root", "%s: I love rooting people! It's my favorite spell!");
        add("message.magic_realms.aliana.eat.poison_potatoes", "%s: Despite common belief poisonous potatoes can't kill you! They taste bad though.......");
        add("message.magic_realms.aliana.travel.nether", "%s: PLEASE don't take me to a fortress......");

        // New permanent contract requirement messages
        add("ui.magic_realms.permanent_contract_insufficient_time", "%s: I don't completely trust you, sorry, maybe ask me in %d minutes?");

        add("message.magic_realms.already_immortal", "%s: Thanks but i don't need that, i already have my own pass");
        add("message.magic_realms.granted_immortality", "%s: Oh, i've been searching for this pass for a long time, thank you so much!");
        add("ui.magic_realms.no_items_to_trade", "%s: Sorry, i have nothing i can give you for that emerald");
        add("ui.magic_realms.trade_success", "%s: Thanks for the emerald! Take this in return");

        add("message.magic_realms.tavernkeep_tip", "Tavernkeep: I don't know the exact method, but i've heard rumours about a book called Necronomicon that has information regarding blood magic");
        add("message.magic_realms.tavernkeep_tip.question_key", "how");
        add("message.magic_realms.tavernkeep_tip.action_key_1", "get");
        add("message.magic_realms.tavernkeep_tip.action_key_2", "obtain");
        add("message.magic_realms.tavernkeep_tip.subject_key_1", "tavernkeep");
        add("message.magic_realms.tavernkeep_tip.subject_key_2", "tavernkeeper");

        add("message.magic_realms.custom_raid.start", "People saw your coin and will try to snatch it. Survive and get your fortune!");

        add("ui.magic_realms.blood_experiment_title", "Blood Infusing Experiment #1");
        add("ui.magic_realms.blood_experiment_text", "I've noticed that some blood spells have secondary effects on the caster, so i started an experiment to transform my offhand item into a different item by imbuing blood magic into it.");
        add("ui.magic_realms.blood_experiment_title_2", "Blood Infusing Experiment #2");
        add("ui.magic_realms.blood_experiment_result", "The result of the experiment was quite surprising, since it seemed like hurting a test subject with the devour spell had a reaction with contract items, but i couldn't transform the test contracts.");
        add("ui.magic_realms.blood_experiment_title_3", "Blood Infusing Experiment #3");
        add("ui.magic_realms.blood_experiment_result_2", "Maybe the experiment subject was too weak to get enough blood from it? Or maybe the test contracts were too low to get infused with blood magic?");
    }
}
