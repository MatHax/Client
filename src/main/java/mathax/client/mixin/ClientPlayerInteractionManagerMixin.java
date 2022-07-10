package mathax.client.mixin;

import mathax.client.events.entity.DropItemsEvent;
import mathax.client.events.entity.player.*;
import mathax.client.mixininterface.IClientPlayerInteractionManager;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.world.BlockUtils;
import mathax.client.MatHax;
import mathax.client.systems.modules.misc.InventoryTweaks;
import mathax.client.systems.modules.player.NoBreakDelay;
import mathax.client.systems.modules.player.Reach;
import mathax.client.systems.modules.world.Nuker;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManager {
    @Shadow
    private int blockBreakingCooldown;

    @Shadow
    protected abstract void syncSelectedSlot();
    
    @Shadow
    public abstract void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player);

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void onClickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
        if (actionType == SlotActionType.THROW && slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
            if (MatHax.EVENT_BUS.post(DropItemsEvent.get(player.currentScreenHandler.slots.get(slotId).getStack())).isCancelled()) info.cancel();
        } else if (slotId == -999) {
            // Clicking outside of inventory
            if (MatHax.EVENT_BUS.post(DropItemsEvent.get(player.currentScreenHandler.getCursorStack())).isCancelled()) info.cancel();
        }
    }
    
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void onClickArmorSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!Modules.get().get(InventoryTweaks.class).armorStorage()) return;

        ScreenHandler screenHandler = player.currentScreenHandler;

        if (screenHandler instanceof PlayerScreenHandler) {
            if (slotId >= 5 && slotId <= 8) {
                int armorSlot = (8 - slotId) + 36;
                if (actionType == SlotActionType.PICKUP && !screenHandler.getCursorStack().isEmpty()) {
                    clickSlot(syncId, 17, armorSlot, SlotActionType.SWAP, player); //armor slot <-> inv slot
                    clickSlot(syncId, 17, button, SlotActionType.PICKUP, player); //inv slot <-> cursor slot
                    clickSlot(syncId, 17, armorSlot, SlotActionType.SWAP, player); //armor slot <-> inv slot
                    ci.cancel();
                } else if (actionType == SlotActionType.SWAP) {
                    clickSlot(syncId, 36 + button, armorSlot, SlotActionType.SWAP, player); //invert swap
                    ci.cancel();
                }
            }
        }
    }
    
    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        if (MatHax.EVENT_BUS.post(StartBreakingBlockEvent.get(blockPos, direction)).isCancelled()) info.cancel();
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    public void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
        if (MatHax.EVENT_BUS.post(InteractBlockEvent.get(player.getMainHandStack().isEmpty() ? Hand.OFF_HAND : hand, hitResult)).isCancelled()) callbackInfoReturnable.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        if (MatHax.EVENT_BUS.post(AttackEntityEvent.get(target)).isCancelled()) info.cancel();
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        if (MatHax.EVENT_BUS.post(InteractEntityEvent.get(entity, hand)).isCancelled()) info.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "dropCreativeStack", at = @At("HEAD"), cancellable = true)
    private void onDropCreativeStack(ItemStack stack, CallbackInfo info) {
        if (MatHax.EVENT_BUS.post(DropItemsEvent.get(stack)).isCancelled()) info.cancel();
    }

    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void onGetReachDistance(CallbackInfoReturnable<Float> info) {
        info.setReturnValue(Modules.get().get(Reach.class).getReach());
    }

    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD))
    private void onMethod_2902SetField_3716Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (Modules.get().isActive(NoBreakDelay.class) || Modules.get().isActive(Nuker.class)) value = 0;
        blockBreakingCooldown = value;
    }

    @Redirect(method = "attackBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD))
    private void onAttackBlockSetField_3719Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (Modules.get().isActive(NoBreakDelay.class) || Modules.get().isActive(Nuker.class)) value = 0;
        blockBreakingCooldown = value;
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> info) {
        MatHax.EVENT_BUS.post(BreakBlockEvent.get(blockPos));
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        InteractItemEvent event = MatHax.EVENT_BUS.post(InteractItemEvent.get(hand));
        if (event.toReturn != null) info.setReturnValue(event.toReturn);
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onCancelBlockBreaking(CallbackInfo info) {
        if (BlockUtils.breaking) info.cancel();
    }

    @Override
    public void syncSelected() {
        syncSelectedSlot();
    }
}
