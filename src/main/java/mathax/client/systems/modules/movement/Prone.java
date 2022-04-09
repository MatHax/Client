package mathax.client.systems.modules.movement;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.CollisionShapeEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.player.FindItemResult;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.Rotations;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Arrays;
import java.util.List;

public class Prone extends Module {
    private final List<BlockPos> waterModeTargets = Arrays.asList(
        new BlockPos(0, 0, 1),
        new BlockPos(0, 0, -1),
        new BlockPos(1, 0, 0),
        new BlockPos(-1, 0, 0)
    );

    private int waterModeStage = 0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode used.")
        .defaultValue(Mode.Water_Bucket)
        .build()
    );

    private final Setting<Boolean> autoMaintain = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-maintain")
        .description("Switch to maintain mode when prone.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .build()
    );

    public Prone() {
        super(Categories.Movement, Items.WATER_BUCKET, "prone", "Become prone on demand.");
    }

    @Override
    public void onDeactivate() {
        waterModeStage = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (autoMaintain.get() && mc.player.isInSwimmingPose() && !mc.player.isSubmergedInWater()) BlockUtils.place(mc.player.getBlockPos().up(), InvUtils.find((itemstack) -> (itemstack.getItem() instanceof BlockItem blockitem && blocks.get().contains(blockitem.getBlock()))), true, 1);

        if (mode.get() == Mode.Water_Bucket && mc.player.isInSwimmingPose() && waterModeStage > 0) {
            mc.options.keyForward.setPressed(false);
            waterModeStage = 0;
        }

        if (mode.get() == Mode.Water_Bucket && !mc.player.isInSwimmingPose()) {
            if (mc.player.isSubmergedInWater()) {
                mc.options.keySprint.setPressed(true);
                waterModeStage += 1;
                if (waterModeStage > 2) mc.options.keyForward.setPressed(true);
            } else {
                FindItemResult result = InvUtils.findInHotbar(Items.WATER_BUCKET);
                if (!result.found()) waterModeStage = 0;
                for (BlockPos offset : waterModeTargets) {
                    BlockPos target = mc.player.getBlockPos().add(offset);
                    if (mc.world.getBlockState(target).isFullCube(mc.world, target) && mc.world.getBlockState(target.up()).isAir()) {
                        useBucket(result, target);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mode.get() != Mode.Collision) return;
        if (mc.world == null || mc.player == null || event.pos == null) return;
        if (event.state == null) return;
        if (event.pos.getY() != mc.player.getY() + 1) return;

        event.shape = VoxelShapes.fullCube();
    }

    private void useBucket(FindItemResult bucket, BlockPos target) {
        if (!bucket.found()) return;

        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 10, true, () -> {
            if (bucket.isOffhand()) mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
            else {
                InvUtils.swap(bucket.slot(), true);
                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                InvUtils.swapBack();
            }
        });
    }

    public enum Mode {
        Water_Bucket,
        Just_Maintain,
        Collision
    }
}
