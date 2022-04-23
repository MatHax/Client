package mathax.client.systems.modules.movement;

import baritone.api.utils.Rotation;
import com.google.common.collect.Lists;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.movement.elytrafly.ElytraFly;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.Arrays;
import java.util.List;

/*/------------------/*/
/*/ Made by Piotrek4 /*/
/*/------------------/*/

public class AutoMLG extends Module {
    private static final List<ItemStack> BUCKETS = Arrays.asList(
        new ItemStack(Items.WATER_BUCKET),
        new ItemStack(Items.POWDER_SNOW_BUCKET)
    );

    private BlockPos waterPlaceBlock = null;

    private Vec2f lastRot = null;

    private Vec3d lastPos = null;

    private boolean isWaterBucket = false;
    private boolean placedWater = false;
    private boolean isOffHand = false;

    private int pickupRetry = 0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgIgnore = settings.createGroup("Ignore");

    // General

    private final Setting<PriorityItem> priority = sgGeneral.add(new EnumSetting.Builder<PriorityItem>()
        .name("priority")
        .description("Determines which item to prioritize for mlg.")
        .defaultValue(PriorityItem.Water_Bucket)
        .build()
    );

    private final Setting<Double> minFall = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-fall")
        .description("Determines at which minimum height the client should perform the mlg bucket.")
        .defaultValue(4.0)
        .min(1)
        .sliderRange(1, 20.0)
        .build()
    );

	/*private final Setting<Boolean> allowInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-inventory")
        .description("Takes buckets from your inventory.")
        .defaultValue(false)
        .build()
    );*/

    private final Setting<Boolean> posSnap = sgGeneral.add(new BoolSetting.Builder()
        .name("snap")
        .description("Clamps your X and Z positions.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> snowInWarm = sgGeneral.add(new BoolSetting.Builder()
        .name("use-powder-snow-in-nether")
        .description("Uses powder snow bucket instead of water bucket in the nether even if water bucket is set as priority.")
        .defaultValue(true)
        .build()
    );

    // Ignore

    private final Setting<Boolean> inCreative = sgIgnore.add(new BoolSetting.Builder()
        .name("in-creative")
        .description("Tries to mlg bucket while in creative mode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFly = sgIgnore.add(new BoolSetting.Builder()
        .name("ignore-flight")
        .description("Tries to mlg bucket even if Flight module is enabled.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreElytraFly = sgIgnore.add(new BoolSetting.Builder()
        .name("ignore-elytra-fly")
        .description("Tries to mlg bucket even if Elytra Fly module is enabled.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreSlowFall = sgIgnore.add(new BoolSetting.Builder()
        .name("ignore-slow-fall")
        .description("Tries to mlg bucket even if player has slow falling effect.")
        .defaultValue(false)
        .build()
    );

    public AutoMLG() {
        super(Categories.Movement, Items.WATER_BUCKET, "auto-mlg", "Tries to mlg bucket for you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.getAbilities().creativeMode && !inCreative.get()) return;
        if (Modules.get().isActive(Flight.class) && !ignoreFly.get()) return;
        if (Modules.get().isActive(ElytraFly.class) && !ignoreElytraFly.get()) return;
        if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) && !ignoreSlowFall.get()) return;

        if (!placedWater) {
            if (mc.player.fallDistance > minFall.get() - 2.0f && !(mc.player.isFallFlying())) {
                if (lastPos == null) lastPos = mc.player.getPos();

                if (mc.player.getPos().y - lastPos.y < 0.0D) {
                    BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos(), mc.player.getPos().subtract(0, 5, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
                    BlockPos blockPos = result.getBlockPos();
                    if (result != null && result.getType() == BlockHitResult.Type.BLOCK && Math.max(0.0, (float) mc.player.getPos().y - (float) (blockPos.getY())) - 1.3f + mc.player.fallDistance > minFall.get().floatValue() && causeFallDamage(BlockUtils.getBlockState(result.getBlockPos())) && causeFallDamage(BlockUtils.getBlockState(result.getBlockPos().up()))) {
                        for (ItemStack bucket : (priority.get() == PriorityItem.Powder_Snow ? Lists.reverse(BUCKETS) : BUCKETS)) {
                            int location = switchToStack(bucket);
                            if (location == 0) continue;

                            isOffHand = location < 0;
                            isWaterBucket = bucket.getItem().equals(Items.WATER_BUCKET);

                            if (mc.world.getDimension().isUltrawarm() && snowInWarm.get()) isWaterBucket = false;

                            if (posSnap.get()) {
                                double x = MathHelper.floor(mc.player.getX()) + 0.5;
                                double z = MathHelper.floor(mc.player.getZ()) + 0.5;
                                if ((Math.abs(mc.player.getX() - x) > 1e-5) || (Math.abs(mc.player.getZ() - z) > 1e-5)) {
                                    mc.player.setPosition(x, mc.player.getY(), z);
                                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                                }

                                mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
                            }

                            if (lastRot == null) lastRot = mc.player.getRotationClient();

                            if (rightClickBlock(blockPos)) {
                                placedWater = true;
                                waterPlaceBlock = blockPos;
                                pickupRetry = 0;
                            }

                            break;
                        }
                    }
                }

                lastPos = mc.player.getPos();
            } else lastRot = null;
        } else {
            if (mc.player.isOnGround() || mc.player.isTouchingWater() && mc.player.getItemCooldownManager().getCooldownProgress(mc.player.getInventory().getMainHandStack().getItem(), 0) < 1) {
                int location = switchToStack(new ItemStack(Items.BUCKET));
                if (location != 0) {
                    isOffHand = location < 0;
                    rightClickBlock(waterPlaceBlock);
                    if ((isOffHand ? mc.player.getOffHandStack() : mc.player.getInventory().getMainHandStack()).getItem().equals(Items.BUCKET)) {
                        pickupRetry++;

                        if (pickupRetry <= 10) return;
                    }
                }

                restoreRotation();
                lastPos = null;
                placedWater = false;
            }
        }
    }

    private void restoreRotation() {
        if (lastRot == null) return;

        mc.player.setPitch(lastRot.x);
        mc.player.setYaw(lastRot.y);
        lastRot = null;
    }

    private boolean causeFallDamage(BlockState blockState) {
        if (blockState.isAir()) return true;

        Block block = blockState.getBlock();
        return !(block instanceof FluidBlock) && !(block instanceof SeaPickleBlock) && !(block instanceof SeagrassBlock);
    }

    private int switchToStack(ItemStack stack) {
        int slot = mc.player.getInventory().getSlotWithStack(stack);
        if (slot >= 0 && slot <= 8) {
            mc.player.getInventory().selectedSlot = slot;
            return 1;
        } /*else if (slot >= 9 && slot <= 35 && allowInventory.get()) {
            PlayerUtils.windowClickSwap(slot, mc.player.getInventory().selectedSlot);
            return 1;
        }*/ else if (mc.player.getOffHandStack().isItemEqual(stack)) return -1;

        return 0;
    }

    private boolean rightClickBlock(BlockPos pos) {
        Vec3d hitVec = Vec3d.ofCenter(pos).add(Vec3d.of(Direction.UP.getVector()).multiply(0.5));
        Rotation rotation = PlayerUtils.getNeededRotations(hitVec);

        mc.player.setYaw(rotation.getYaw());
        mc.player.setPitch(rotation.getPitch());
        PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(rotation.getYaw(), rotation.getPitch(), mc.player.isOnGround());
        mc.player.networkHandler.sendPacket(packet);

        if (!placedWater && !isWaterBucket) mc.interactionManager.interactBlock(mc.player, mc.world, isOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false));
        else {
            Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

            if (eyesPos.squaredDistanceTo(hitVec) > 18.0625) return false;

            mc.interactionManager.interactItem(mc.player, mc.world, isOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        }

        return true;
    }

    public enum PriorityItem {
        Water_Bucket("Water Bucket"),
        Powder_Snow("Powder Snow");

        private final String title;

        PriorityItem(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
