package com.BEVL.maid_damage_fix.mixins;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidHurtEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.event.GunHurtMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunHurtMaidEvent.class, remap = false)
public abstract class TaczGunHurtMaidEventMixin {

    @Unique
    private boolean maidDamageFix$isGunDamage(DamageSource source) {
        if (source.is(ModDamageTypes.BULLETS_TAG)) {
            return true;
        }
        return source.is(DamageTypeTags.IS_EXPLOSION) && source.getDirectEntity() instanceof EntityKineticBullet;
    }

    @Unique
    private boolean maidDamageFix$isFriendlyFire(EntityMaid maid, Entity attacker) {
        if (attacker == null) {
            return false;
        }
        if (attacker == maid) {
            return true;
        }
        if (maid.isAlliedTo(attacker)) {
            return true;
        }
        if (attacker instanceof EntityMaid otherMaid) {
            return maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(otherMaid.getOwnerUUID());
        }
        return false;
    }

    @Unique
    private Entity maidDamageFix$getAttacker(EntityHurtByGunEvent.Pre event) {
        Entity attacker = event.getAttacker();
        if (attacker != null) {
            return attacker;
        }
        if (event.getBullet() instanceof Projectile projectile) {
            return projectile.getOwner();
        }
        return null;
    }

    @Inject(method = "onMaidHurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void maidDamageFix$onMaidHurtFix(MaidHurtEvent event, CallbackInfo ci) {
        DamageSource source = event.getSource();
        EntityMaid maid = event.getMaid();
        if (maid.getOwnerUUID() == null) {
            ci.cancel();
            return;
        }
        if (maidDamageFix$isGunDamage(source)) {
            Entity attacker = source.getEntity();
            if (attacker instanceof Projectile projectile) {
                attacker = projectile.getOwner();
            }
            if (maidDamageFix$isFriendlyFire(maid, attacker)) {
                event.setCanceled(true);
            }
            ci.cancel();
        }
    }

    @Inject(method = "onGunHurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void maidDamageFix$onGunHurtFix(EntityHurtByGunEvent.Pre event, CallbackInfo ci) {
        if (!(event.getHurtEntity() instanceof EntityMaid maid)) {
            return;
        }
        if (maid.getOwnerUUID() == null || maidDamageFix$isFriendlyFire(maid, maidDamageFix$getAttacker(event))) {
            event.setCanceled(true);
        }
        ci.cancel();
    }

    @Inject(method = "onExplosionDetonateEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private void maidDamageFix$onExplosionDetonateEventFix(ExplosionEvent.Detonate event, CallbackInfo ci) {
        Entity directEntity = event.getExplosion().getDirectSourceEntity();
        if (directEntity instanceof EntityKineticBullet) {
            Entity shooter = null;
            if (directEntity instanceof Projectile projectile) {
                shooter = projectile.getOwner();
            } else {
                shooter = event.getExplosion().getIndirectSourceEntity();
            }
            Entity finalShooter = shooter;
            event.getAffectedEntities().removeIf(e -> e instanceof EntityMaid maid && maid.getOwnerUUID() != null && maidDamageFix$isFriendlyFire(maid, finalShooter));
            ci.cancel();
        }
    }
}
