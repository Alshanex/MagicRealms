package net.alshanex.arcane_hostility.traits;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2hostility.init.L2Hostility;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.network.TraitEffectToClient;
import dev.xkmc.l2hostility.init.network.TraitEffects;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ManaSyphon extends LegendaryTrait {
    int range = 10;
    public ManaSyphon(ChatFormatting format) {
        super(format);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        int itv = LHConfig.SERVER.killerAuraInterval.get() / level;
        int mana = 50;
        if (!mob.level().isClientSide() && mob.tickCount % itv == 0) {
            MobTraitCap cap = LHMiscs.MOB.type().getOrCreate(mob);
            AABB box = mob.getBoundingBox().inflate(range);
            for (var e : mob.level().getEntitiesOfClass(ServerPlayer.class, box)) {
                if (e.distanceTo(mob) > range) continue;
                TraitEffectCache cache = new TraitEffectCache(e);
                cap.traitEvent((k, v) -> k.postHurtPlayer(v, mob, cache));
                MagicData magicData = MagicData.getPlayerMagicData(e);
                if(magicData.getMana() > mana){
                    var newMana = Math.max(magicData.getMana() - mana, 0);
                    magicData.setMana(newMana);
                    PacketDistributor.sendToPlayer(e, new SyncManaPacket(magicData));
                }
            }
            L2Hostility.HANDLER.toTrackingPlayers(TraitEffectToClient.of(mob, this, TraitEffects.AURA), mob);
        }
        if (mob.level().isClientSide()) {
            Vec3 center = mob.position();
            float tpi = (float) (Math.PI * 2);
            Vec3 v0 = new Vec3(0, range, 0);
            v0 = v0.xRot(tpi / 4).yRot(mob.getRandom().nextFloat() * tpi);
            mob.level().addAlwaysVisibleParticle(ParticleTypes.WAX_OFF,
                    center.x + v0.x,
                    center.y + v0.y + 0.5f,
                    center.z + v0.z, 0, 0, 0);
        }
    }

    @Override
    public void addDetail(List<Component> list) {
        list.add(Component.translatable(getDescriptionId() + ".desc",
                Component.literal("" + range)
                        .withStyle(ChatFormatting.AQUA),
                mapLevel(i -> Component.literal(Math.round(LHConfig.SERVER.killerAuraInterval.get() * 5d / i) * 0.01 + "")
                        .withStyle(ChatFormatting.AQUA))
        ).withStyle(ChatFormatting.GRAY));
    }
}
