package mathax.client.systems.modules.movement;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.renderer.ShapeMode;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.misc.Pool;
import mathax.client.utils.player.FindItemResult;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.render.color.Color;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scaffold extends Module {
    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private final BlockPos.Mutable prevBp = new BlockPos.Mutable();
    private final BlockPos.Mutable bp = new BlockPos.Mutable();

    private boolean lastWasSneaking;

    private double lastSneakingY;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> fastTower = sgGeneral.add(new BoolSetting.Builder()
        .name("fast-tower")
        .description("Whether or not to scaffold upwards faster.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically swaps to a block before placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the blocks being placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allows placing blocks in air.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("closest-block-range")
        .description("How far can scaffold place blocks.")
        .defaultValue(4)
        .min(0)
        .sliderRange(0, 8)
        .visible(() -> !airPlace.get())
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings your hand client-side when placing or interacting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders currently placed blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Determines how the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the placed block.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the placed block.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    public Scaffold() {
        super(Categories.Movement, Items.COBBLESTONE, "scaffold", "Automatically places blocks under you.");
    }

    @Override
    public void onActivate() {
        lastWasSneaking = mc.options.keySneak.isPressed();
        if (lastWasSneaking) lastSneakingY = mc.player.getY();

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        if (airPlace.get()) {
            Vec3d vec = mc.player.getPos().add(mc.player.getVelocity()).add(0, -0.5f, 0);
            bp.set(vec.getX(), vec.getY(), vec.getZ());
        } else {
            if (BlockUtils.getPlaceSide(mc.player.getBlockPos().down()) != null) bp.set(mc.player.getBlockPos().down());
            else {
                Vec3d pos = mc.player.getPos();
                pos = pos.add(0, -0.98f, 0);
                pos.add(mc.player.getVelocity());

                if (PlayerUtils.distanceTo(prevBp) > placeRange.get()) {
                    List<BlockPos> blockPosArray = new ArrayList<>();

                    for (int x = (int) (mc.player.getX() - placeRange.get()); x < mc.player.getX() + placeRange.get(); x++) {
                        for (int z = (int) (mc.player.getZ() - placeRange.get()); z < mc.player.getZ() + placeRange.get(); z++) {
                            for (int y = (int) Math.max(mc.world.getBottomY(), mc.player.getY() - placeRange.get()); y < Math.min(mc.world.getTopY(), mc.player.getY() + placeRange.get()); y++) {
                                bp.set(x, y, z);
                                if (!mc.world.getBlockState(bp).isAir()) blockPosArray.add(new BlockPos(bp));
                            }
                        }
                    }

                    if (blockPosArray.size() == 0) return;

                    blockPosArray.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));

                    prevBp.set(blockPosArray.get(0));
                }

                Vec3d vecPrevBP = new Vec3d((double) prevBp.getX() + 0.5f, (double) prevBp.getY() + 0.5f, (double) prevBp.getZ() + 0.5f);
                Vec3d sub = pos.subtract(vecPrevBP);
                Direction facing;

                if (sub.getY() < -0.5f) facing = Direction.DOWN;
                else if (sub.getY() > 0.5f) facing = Direction.UP;
                else facing = Direction.getFacing(sub.getX(), 0, sub.getZ());

                bp.set(prevBp.offset(facing));
            }
        }

        FindItemResult item = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        if (!item.found()) return;


        if (item.getHand() == null && !autoSwitch.get()) return;

        if (mc.options.keySneak.isPressed() && !mc.options.keyJump.isPressed()) {
            if (lastSneakingY - mc.player.getY() < 0.1) {
                lastWasSneaking = false;
                return;
            }
        } else lastWasSneaking = false;
        if (!lastWasSneaking) lastSneakingY = mc.player.getY();

        if (mc.options.keyJump.isPressed() && !mc.options.keySneak.isPressed() && fastTower.get()) mc.player.setVelocity(0, 0.42f, 0);

        if (BlockUtils.place(bp, item, rotate.get(), 50, swing.get(), true)) {
            if (!mc.world.isOutOfHeightLimit(bp.getY())) renderBlocks.add(renderBlockPool.get().set(bp));
            if (mc.options.keyJump.isPressed() && !mc.options.keySneak.isPressed() && !mc.player.isOnGround() && !mc.world.getBlockState(bp).isAir() && fastTower.get()) mc.player.setVelocity(0, -0.28f, 0);
        }

        if (!mc.world.getBlockState(bp).isAir()) prevBp.set(bp);
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(block)) return false;

        if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, pos))) return false;
        return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(mc.world.getBlockState(pos));
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 8;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;

            event.renderer.box(pos, sides, lines, shapeMode, 0);

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }

    public enum ListMode {
        Whitelist("Whitelist"),
        Blacklist("Blacklist");

        private final String title;

        ListMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
