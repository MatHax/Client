package mathax.client.systems.modules.combat;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.entity.player.StartBreakingBlockEvent;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.renderer.ShapeMode;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.entity.EntityUtils;
import mathax.client.utils.entity.SortPriority;
import mathax.client.utils.entity.TargetUtils;
import mathax.client.utils.player.FindItemResult;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.player.Rotations;
import mathax.client.utils.render.color.Color;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class TNTAura extends Module {
    private final List<BlockPos> obsidianPos = new ArrayList<>();

    private Direction direction;

    private PlayerEntity target;

    private boolean startBreak;
    private boolean toggled;

    private int ticks;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoBreak = settings.createGroup("Auto Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgObsidianRender = settings.createGroup("Obsidian Render");
    private final SettingGroup sgTNTRender = settings.createGroup("TNT Render");
    private final SettingGroup sgBreakRender = settings.createGroup("Break Render");

    // General

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("target-range")
        .description("max range to target.")
        .defaultValue(4)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Delay between obsidian placement in ticks.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 40)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards blocks when interacting.")
        .defaultValue(false)
        .build()
    );

    // Auto Break

    private final Setting<Boolean> autoBreak = sgAutoBreak.add(new BoolSetting.Builder()
        .name("auto-break")
        .description("Attempts to auto break.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Mode> breakMode = sgAutoBreak.add(new EnumSetting.Builder<Mode>()
        .name("break-mode")
        .description("Determines how to break.")
        .defaultValue(Mode.Normal)
        .build()
    );

    // Pause

    private final Setting<Boolean> burrowPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-burrow")
        .description("Will pause if enemy is burrowed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiSelf = sgPause.add(new BoolSetting.Builder()
        .name("anti-self")
        .description("Pause if enemy inside your hole.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> holePause = sgPause.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Pause if enemy isn't in hole")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses while eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses while drinking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses while mining.")
        .defaultValue(true)
        .build()
    );

    // Obsidian Render

    private final Setting<Boolean> obsidianRender = sgObsidianRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where blocks will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> obsidianShapeMode = sgObsidianRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Determines how the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> obsidianSideColor = sgObsidianRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 70))
        .build()
    );

    private final Setting<SettingColor> obsidianLineColor = sgObsidianRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    private final Setting<SettingColor> obsidianNextSideColor = sgObsidianRender.add(new ColorSetting.Builder()
        .name("next-side-color")
        .description("The side color of the next block to be placed.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.r, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.g, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.b, 75))
        .build()
    );

    private final Setting<SettingColor> obsidianNextLineColor = sgObsidianRender.add(new ColorSetting.Builder()
        .name("next-line-color")
        .description("The line color of the next block to be placed.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.r, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.g, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.b))
        .build()
    );

    // TNT Render

    private final Setting<Boolean> tntRender = sgTNTRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where blocks will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> tntShapeMode = sgTNTRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Determines how the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> tntSideColor = sgTNTRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 75))
        .build()
    );

    private final Setting<SettingColor> tntLineColor = sgTNTRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    // Mine Render

    private final Setting<Boolean> breakRender = sgBreakRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where blocks will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> breakShapeMode = sgBreakRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Determines how the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> breakSideColor = sgBreakRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.r, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.g, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.b, 75))
        .build()
    );

    private final Setting<SettingColor> breakLineColor = sgBreakRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.r, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.g, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.b))
        .build()
    );

    public TNTAura() {
        super(Categories.Combat, Items.TNT, "tnt-aura", "Kills enemies using TNT.");
    }

    @Override
    public void onActivate() {
        obsidianPos.clear();
        ticks = 0;
        startBreak = false;
        toggled = false;
    }

    @Override
    public void onDeactivate() {
        obsidianPos.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.isHotbar() && !toggled) {
            obsidianPos.clear();
            error("No obsidian found, disabling...");
            toggle();
            toggled = true;
        }

        FindItemResult flint = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);
        if (!flint.isHotbar() && !toggled) {
            obsidianPos.clear();
            error("No flint and steel found, disabling...");
            toggle();
            toggled = true;
        }

        FindItemResult tnt = InvUtils.findInHotbar(Items.TNT);
        if (!tnt.isHotbar() && !toggled) {
            obsidianPos.clear();
            error("No TNT found, disabling...");
            toggle();
            toggled = true;
        }

        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
        if (!pickaxe.isHotbar() && !toggled) {
            obsidianPos.clear();
            error("No pickaxe found, disabling...");
            toggle();
            toggled = true;
        }

        if (TargetUtils.isBadTarget(target, range.get())) target = TargetUtils.getPlayerTarget(range.get(), SortPriority.Lowest_Distance);

        if (target == null) return;

        if (burrowPause.get() && isBurrowed(target) && !toggled) {
            obsidianPos.clear();
            error("Target is burrowed, disabling...");
            toggle();
            toggled = true;
        }

        if (antiSelf.get() && antiSelf(target) && !toggled) {
            obsidianPos.clear();
            error("Target is in your hole!");
            toggle();
            toggled = true;
        }

        if (holePause.get() && !isSurrounded(target) && !toggled) {
            obsidianPos.clear();
            error("Target isn't surrounded, disabling...");
            toggle();
            toggled = true;
        }

        if (TargetUtils.isBadTarget(target, range.get()) && !toggled) {
            error("Enemy is too far, disabling...");
            toggle();
            toggled = true;
        }

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        if (allowTNT(target)) {
            placeTNT(target);
            igniteTNT(target.getBlockPos().up(2), flint);
        }

        if (!mineBlockstate(target.getBlockPos().up(2)) && autoBreak.get()) mine(target.getBlockPos().up(2), pickaxe);
        placeObsidian(target);

        if (ticks >= delay.get() && obsidianPos.size() > 0) {
            BlockPos blockPos = obsidianPos.get(obsidianPos.size() - 1);

            if (BlockUtils.place(blockPos, obsidian, rotate.get(), 50, true)) obsidianPos.remove(blockPos);
            ticks = 0;
        } else ticks++;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (obsidianRender.get() && !obsidianPos.isEmpty()) {
            for (BlockPos pos : obsidianPos) {
                boolean isFirst = pos.equals(obsidianPos.get(obsidianPos.size() - 1));

                Color side = isFirst ? obsidianNextSideColor.get() : obsidianSideColor.get();
                Color line = isFirst ? obsidianNextLineColor.get() : obsidianLineColor.get();

                event.renderer.box(pos, side, line, obsidianShapeMode.get(), 0);
            }
        }

        if (tntRender.get() && target != null && allowTNT(target)) {
            if (tntBlockstate(target.getBlockPos().add(0, 2, 0))) event.renderer.box(target.getBlockPos().add(0, 2, 0), tntSideColor.get(), tntLineColor.get(), tntShapeMode.get(), 0);
        }

        if (breakRender.get() && target != null && autoBreak.get()) {
            if (!mineBlockstate(target.getBlockPos().add(0, 2, 0))) event.renderer.box(target.getBlockPos().add(0, 2, 0), breakSideColor.get(), breakLineColor.get(), breakShapeMode.get(), 0);
        }
    }

    @EventHandler
    public void onStartBreakingBlock(StartBreakingBlockEvent event) {
        direction = event.direction;
    }

    private void placeObsidian(PlayerEntity target) {
        obsidianPos.clear();
        BlockPos targetPos = target.getBlockPos();
        add(targetPos.add(0, 3, 0));
        add(targetPos.add(1, 2, 0));
        add(targetPos.add(-1, 2, 0));
        add(targetPos.add(0, 2, 1));
        add(targetPos.add(0, 2, -1));
    }

    private void placeTNT(PlayerEntity target) {
        FindItemResult tnt = InvUtils.findInHotbar(Items.TNT);
        BlockPos targetPos = target.getBlockPos();
        BlockUtils.place(targetPos.add(0, 2, 0), tnt, rotate.get(), 50, true, true);
    }

    private void add(BlockPos blockPos) {
        if (!obsidianPos.contains(blockPos) && BlockUtils.canPlace(blockPos)) obsidianPos.add(blockPos);
    }

    private void igniteTNT(BlockPos pos, FindItemResult item) {
        InvUtils.swap(item.slot(), true);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
        InvUtils.swapBack();
    }

    public boolean tntBlockstate(BlockPos Pos) {
        return mc.world.getBlockState(Pos).getBlock() == Blocks.AIR || mc.world.getBlockState(Pos).getBlock() == Blocks.TNT;
    }

    public boolean allowTNT(LivingEntity target) {
        assert mc.world != null;
        return !mc.world.getBlockState(target.getBlockPos().add(1, 2, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(-1, 2, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 2, 1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 2, -1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 3, 0)).isAir();
    }

    public boolean mineBlockstate(BlockPos Pos) {
        return mc.world.getBlockState(Pos).getBlock() == Blocks.AIR || mc.world.getBlockState(Pos).getBlock() == Blocks.TNT || mc.world.getBlockState(Pos).getBlock() == Blocks.BEDROCK;
    }

    public void mine(BlockPos blockPos, FindItemResult item) {
        if (breakMode.get() == Mode.Normal) {
            InvUtils.swap(item.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        }

        if (breakMode.get() == Mode.Instant) {
            InvUtils.swap(item.slot(), false);

            if (!startBreak) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                startBreak = true;
            }

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
            else mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    private boolean isBurrowed(LivingEntity target) {
        assert mc.world != null;
        return !mc.world.getBlockState(target.getBlockPos()).isAir();
    }

    private boolean isSurrounded(LivingEntity target) {
        assert mc.world != null;
        return !mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    private boolean antiSelf(LivingEntity target){
        return mc.player.getBlockPos().getX() == target.getBlockPos().getX() && mc.player.getBlockPos().getZ() == target.getBlockPos().getZ() && mc.player.getBlockPos().getY() == target.getBlockPos().getY();
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum Mode {
        Normal,
        Instant
    }
}
