package com.BEVL.maid_damage_fix.mixin;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidHurtEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.swarfare.event.GunHurtMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.level.ExplosionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunHurtMaidEvent.class, remap = false)
public abstract class sbwGunHurtMaidEvent {

    @Shadow
    private boolean isBulletDamage(DamageSource source) {
        return false;
    }

    /**
     * 新增：专属的友军伤害深度判定逻辑
     */
    @Unique
    private boolean maid_damage_fix$isFriendlyFire(EntityMaid maid, Entity attacker) {
        if (attacker == null) return false;
        if (attacker == maid) return true;
        if (maid.isAlliedTo(attacker)) return true;
        // 检查是否为同一主人的其他女仆
        if (attacker instanceof EntityMaid otherMaid) {
            return maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(otherMaid.getOwnerUUID());
        }
        return false;
    }

    @Inject(method = "onMaidHurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void onMaidHurtFix(MaidHurtEvent event, CallbackInfo ci) {
        DamageSource source = event.getSource();
        EntityMaid maid = event.getMaid();

        if (maid.getOwnerUUID() == null) {
            ci.cancel();
            return;
        }

        if (isBulletDamage(source)) {
            Entity attacker = source.getEntity();
            // 替换为更严谨的友军判定
            if (maid_damage_fix$isFriendlyFire(maid, attacker)) {
                event.setCanceled(true);
            }
            ci.cancel();
        }
    }

    @Inject(method = "onExplosionDetonateEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExplosionDetonateEventFix(ExplosionEvent.Detonate event, CallbackInfo ci) {
        Entity directEntity = event.getExplosion().getDirectSourceEntity();

        if (directEntity instanceof ProjectileEntity || directEntity instanceof FastThrowableProjectile) {

            // 核心：更精准地获取抛射物的射手
            Entity shooter = null;
            if (directEntity instanceof Projectile projectile) {
                shooter = projectile.getOwner();
            } else {
                shooter = event.getExplosion().getIndirectSourceEntity();
            }

            Entity finalShooter = shooter;
            event.getAffectedEntities().removeIf(e -> {
                if (e instanceof EntityMaid maid && maid.getOwnerUUID() != null) {
                    // 引入友军判定，防止女仆互炸
                    return maid_damage_fix$isFriendlyFire(maid, finalShooter);
                }
                return false;
            });
            ci.cancel();
        }
    }
}