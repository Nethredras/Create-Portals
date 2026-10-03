package net.nethredras.create_portals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.nethredras.create_portals.item.custom.portal_gun.PortalGunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRenderer.class)
public abstract class PortalGunArmPositionMixin {

    @ModifyReturnValue(
            method = "getArmPose(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("TAIL")
    )
    private static HumanoidModel.ArmPose portalGunTwoHanded(HumanoidModel.ArmPose original,
                                                                       AbstractClientPlayer player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof PortalGunItem) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return original;
    }
}