package net.alshanex.magic_realms.mixin;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At(value = "HEAD"), cancellable = true)
    public void isAlliedTo(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Entity self = ((Entity) (Object) this);
        if (entity instanceof AbstractMercenaryEntity summon && summon.getSummoner() != null)
            cir.setReturnValue(self.isAlliedTo(summon.getSummoner()) || self.equals(summon.getSummoner()));

    }
}
