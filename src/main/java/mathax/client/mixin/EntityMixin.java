package mathax.client.mixin;

import mathax.client.MatHax;
import mathax.client.events.entity.LivingEntityMoveEvent;
import mathax.client.events.entity.player.JumpVelocityMultiplierEvent;
import mathax.client.events.entity.player.PlayerMoveEvent;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.entity.fakeplayer.FakePlayerEntity;
import mathax.client.utils.render.EntityShaders;
import mathax.client.systems.modules.combat.Hitboxes;
import mathax.client.systems.modules.movement.Moses;
import mathax.client.systems.modules.movement.NoSlow;
import mathax.client.systems.modules.movement.Velocity;
import mathax.client.systems.modules.render.ESP;
import mathax.client.systems.modules.render.NoRender;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static mathax.client.MatHax.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public World world;

    @Shadow
    public abstract BlockPos getBlockPos();

    @Shadow
    protected abstract BlockPos getVelocityAffectingPos();

    @Shadow
    public abstract void emitGameEvent(GameEvent event);

    @Redirect(method = "updateMovementInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;getVelocity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d updateMovementInFluidFluidStateGetVelocity(FluidState state, BlockView world, BlockPos pos) {
        Vec3d vec = state.getVelocity(world, pos);

        Velocity velocity = Modules.get().get(Velocity.class);
        if ((Object) this == mc.player && velocity.isActive() && velocity.liquids.get()) vec = vec.multiply(velocity.getHorizontal(velocity.liquidsHorizontal), velocity.getVertical(velocity.liquidsVertical), velocity.getHorizontal(velocity.liquidsHorizontal));

        return vec;
    }

    @ModifyArgs(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
    private void onPushAwayFrom(Args args, Entity entity) {
        Velocity velocity = Modules.get().get(Velocity.class);
        if ((Object) this == mc.player && velocity.isActive() && velocity.entityPush.get()) {
            double multiplier = velocity.entityPushAmount.get();
            args.set(0, (double) args.get(0) * multiplier);
            args.set(2, (double) args.get(2) * multiplier);
        } else if (entity instanceof FakePlayerEntity player && player.doNotPush) { // FakePlayerEntity
            args.set(0, 0.0);
            args.set(2, 0.0);
        }
    }

    @Inject(method = "getJumpVelocityMultiplier", at = @At("HEAD"), cancellable = true)
    private void onGetJumpVelocityMultiplier(CallbackInfoReturnable<Float> info) {
        if ((Object) this == mc.player) {
            float f = world.getBlockState(getBlockPos()).getBlock().getJumpVelocityMultiplier();
            float g = world.getBlockState(getVelocityAffectingPos()).getBlock().getJumpVelocityMultiplier();
            float a = f == 1.0D ? g : f;

            JumpVelocityMultiplierEvent event = MatHax.EVENT_BUS.post(JumpVelocityMultiplierEvent.get());
            info.setReturnValue(a * event.multiplier);
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this == mc.player) MatHax.EVENT_BUS.post(PlayerMoveEvent.get(type, movement));
        else if ((Object) this instanceof LivingEntity) MatHax.EVENT_BUS.post(LivingEntityMoveEvent.get((LivingEntity) (Object) this, movement));
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> info) {
        if (EntityShaders.renderingOutlines) info.setReturnValue(Modules.get().get(ESP.class).getColor((Entity) (Object) this).getPacked());
    }

    @Redirect(method = "getVelocityMultiplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"))
    private Block getVelocityMultiplierGetBlockProxy(BlockState blockState) {
        if ((Object) this != mc.player) return blockState.getBlock();
        if (blockState.getBlock() == Blocks.SOUL_SAND && Modules.get().get(NoSlow.class).soulSand()) return Blocks.STONE;
        return blockState.getBlock();
    }

    @Inject(method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void isInvisibleToCanceller(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(NoRender.class).noInvisibility() || Modules.get().get(ESP.class).shouldDrawOutline((Entity) (Object) this)) info.setReturnValue(false);
    }

    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> info) {
        double v = Modules.get().get(Hitboxes.class).getEntityValue((Entity) (Object) this);
        if (v != 0) info.setReturnValue((float) v);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        if (player == null) info.setReturnValue(false);
    }

    @Redirect(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSprinting()Z"))
    private boolean updateSwimmingisSprintingProxy(Entity self) {
        return (!self.isSprinting() || !Modules.get().isActive(Moses.class)) && self.isSprinting();
    }
}
