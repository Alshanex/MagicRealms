package net.alshanex.magic_realms.compat;

import io.redspace.irons_artifice.entity.ai.RangedGunAttackGoal;
import io.redspace.irons_artifice.registry.ItemRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.combat.AbstractCombatClass;
import net.alshanex.magic_realms.util.humans.combat.ClassLoadout;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GunslingerClass extends AbstractCombatClass {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "gunslinger");

    private static final float ENGAGE_RANGE = 24f;

    public GunslingerClass() {
        super("gunslinger");
    }

    @Override
    public String skinCategory() {
        return "gunslinger";
    }

    @Override
    public int spawnWeight() {
        return 4;
    }

    @Override
    public boolean inRandomPool() {
        return !gunPool().isEmpty();
    }

    @Override
    public ItemStack symbolItem(AbstractMercenaryEntity e) {
        return new ItemStack(ItemRegistry.FLINTLOCK_PISTOL.get());
    }

    @Override public boolean prefersRangedWeapons(ClassLoadout l) { return true; }
    @Override public boolean prefersMelee(ClassLoadout l) { return false; }
    @Override public boolean usesSpellbooks() { return false; }
    @Override public boolean usesMagicSchools() { return false; }
    @Override public boolean canGainSpellPower() { return false; }
    @Override public boolean canGainSpellResist() { return false; }

    @Override
    public boolean acceptsMainHand(ItemStack stack, ClassLoadout l) {
        return stack.is(ModTags.GUNS);
    }

    @Override
    public boolean acceptsOffHand(ItemStack stack, ClassLoadout l) {
        return false;
    }

    @Override
    public void applyStartingEquipment(AbstractMercenaryEntity e) {
        e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.FLINTLOCK_PISTOL.get()));

        e.getInventory().addItem(new ItemStack(ItemRegistry.BULLET.get(), 16));
    }

    @Override
    public void initializeSpecifics(AbstractMercenaryEntity e, RandomSource rng) {
        e.setIsArcher(true);
    }

    @Override
    public void applyAttributes(AbstractMercenaryEntity e, int stars, RandomSource rng) {
        addModifier(e, Attributes.MAX_HEALTH, "health", 10.0, AttributeModifier.Operation.ADD_VALUE);
        e.heal(e.getMaxHealth());

        addModifier(e, Attributes.FOLLOW_RANGE, "sightlines",
                starValue(stars, 4, 10, 8, 16, 12, 24, rng), AttributeModifier.Operation.ADD_VALUE);

        addModifier(e, Attributes.MOVEMENT_SPEED, "mobility",
                starValue(stars, 2, 6, 5, 10, 8, 15, rng) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @Override
    public List<AbstractSpell> generateSpells(AbstractMercenaryEntity e, RandomSource rng) {
        return List.of();
    }

    @Override
    protected void clearOwnGoals(AbstractMercenaryEntity e) {
        super.clearOwnGoals(e);
        e.goalSelector.removeAllGoals(g -> g instanceof RangedGunAttackGoal);
    }

    @Override
    public void installCombatGoals(AbstractMercenaryEntity e, List<AbstractSpell> spells) {
        clearOwnGoals(e);

        e.goalSelector.addGoal(2, new RangedGunAttackGoal<>(e, ENGAGE_RANGE, 15, 45, 40, 80));
    }

    @Override
    public void onEquipmentChanged(AbstractMercenaryEntity e) {
        if (!hasGunGoal(e)) {
            installCombatGoals(e, e.getPersistedSpells());
        }
    }

    private static boolean hasGunGoal(AbstractMercenaryEntity e) {
        for (WrappedGoal wrapped : e.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof RangedGunAttackGoal) return true;
        }
        return false;
    }

    private static List<Holder<Item>> gunPool() {
        return BuiltInRegistries.ITEM.getTag(ModTags.GUNS)
                .map(set -> set.stream().toList())
                .orElse(List.of());
    }
}
