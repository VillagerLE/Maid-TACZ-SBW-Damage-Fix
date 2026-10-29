package com.BEVL.maid_damage_fix.mixins;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidHurtEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.swarfare.event.GunHurtMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = GunHurtMaidEvent.class, remap = false)
public abstract class SbwGunHurtMaidEventMixin {

    @Shadow
    private boolean isBulletDamage(DamageSource source) {
        throw new AssertionError("shadow method, replaced by mixin");
    }

    @Unique
    private boolean pillagersGun$isFriendlyFire(EntityMaid maid, Entity attacker) {
        if (attacker == null) {
            return false;
        }
        if (attacker == maid) {
            return true;
        }
        if (maid.isAlliedTo(attacker)) {
            return true;
        }
        // Maids with the same owner are friendly to each other
        if (attacker instanceof EntityMaid otherMaid) {
            return maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(otherMaid.getOwnerUUID());
        }
        return false;
    }

    @Inject(method = "onMaidHurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void pillagersGun$onMaidHurtFix(MaidHurtEvent event, CallbackInfo ci) {
        DamageSource source = event.getSource();
        EntityMaid maid = event.getMaid();

        if (maid.getOwnerUUID() == null) {
            ci.cancel();
            return;
        }

        if (isBulletDamage(source)) {
            Entity attacker = source.getEntity();
            if (attacker instanceof Projectile projectile) {
                attacker = projectile.getOwner();
            }
            // Replace with a strict friendly-fire check so enemy gunners can hurt maids
            if (pillagersGun$isFriendlyFire(maid, attacker)) {
                event.setCanceled(true);
            }
            ci.cancel();
        }
    }

    @Inject(method = "onExplosionDetonateEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private void pillagersGun$onExplosionDetonateEventFix(ExplosionEvent.Detonate event, CallbackInfo ci) {
        Entity directEntity = event.getExplosion().getDirectSourceEntity();

        if (directEntity instanceof ProjectileEntity || directEntity instanceof FastThrowableProjectile) {
            Entity shooter = null;
            if (directEntity instanceof Projectile projectile) {
                shooter = projectile.getOwner();
            } else {
                shooter = event.getExplosion().getIndirectSourceEntity();
            }

            Entity finalShooter = shooter;
            event.getAffectedEntities().removeIf(e -> {
                if (e instanceof EntityMaid maid && maid.getOwnerUUID() != null) {
                    // Only protect maids from friendly explosions
                    return pillagersGun$isFriendlyFire(maid, finalShooter);
                }
                return false;
            });
            ci.cancel();
        }
    }
}
