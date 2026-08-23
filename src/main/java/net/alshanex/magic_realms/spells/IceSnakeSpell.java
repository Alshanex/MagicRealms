package net.alshanex.magic_realms.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.spells.HomingIceSpikeRunnerEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class IceSnakeSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "ice_snake");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2))
        );
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(15)
            .build();

    public IceSnakeSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 2;
        this.castTime = 40;
        this.baseManaCost = 30;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 20, .15f, false);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 forward = entity.getForward().multiply(1, 0, 1).normalize();
        Vec3 start = entity.getEyePosition().add(forward.scale(1.5));

        float damage = getDamage(spellLevel, entity);
        int count = getCount(spellLevel, entity);
        start = Utils.moveToRelativeGroundLevel(level, start, 1, 3).add(0,0.1,0);
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
            var target = castTargetingData.getTarget((ServerLevel) level);
            if (target != null) {
                HomingIceSpikeRunnerEntity runner = new HomingIceSpikeRunnerEntity(level, entity, target);
                runner.setup(entity, target, damage, count * 200, 1.0f, 200);
                runner.setPos(start);
                level.addFreshEntity(runner);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private int getCount(int spellLevel, LivingEntity entity) {
        return 7 + 3 * spellLevel / 2;
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity);
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }
}
