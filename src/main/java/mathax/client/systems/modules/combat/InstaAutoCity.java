package mathax.client.systems.modules.combat;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.renderer.ShapeMode;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.Rotations;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.utils.world.BlockUtils;
import mathax.client.utils.world.CityUtils;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.ColorSetting;
import mathax.client.settings.DoubleSetting;
import mathax.client.settings.EnumSetting;
import mathax.client.settings.IntSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

public class InstaAutoCity extends Module {
    static final boolean assertionsDisabled = !InstaAutoCity.class.desiredAssertionStatus();

    private Direction direction;

    private PlayerEntity target;

    private BlockPos targetBlockPos;
    private BlockPos mineTarget;

    private boolean mining;

    private int delayLeft;
    private int count;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range a city-able block will be found.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
        .name("support")
        .description("If there is no block below a city block it will place one before mining.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-info")
        .description("Sends a client-side message if you city a player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining blocks in ticks.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ironPickaxe = sgGeneral.add(new BoolSetting.Builder()
        .name("iron-pickaxe")
        .description("Uses iron pickaxe.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Automatically toggles off after activation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> toggle = sgGeneral.add(new IntSetting.Builder()
        .name("auto-toggle-delay")
        .description("Amount of ticks the block has to be air to auto toggle off.")
        .defaultValue(20)
        .min(0)
        .sliderRange(0, 40)
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
        .description("Renders a block overlay where the obsidian will be placed.")
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
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 255))
        .build()
    );

    public InstaAutoCity() {
        super(Categories.Combat, Items.DIAMOND_PICKAXE, "insta-auto-city", "Automatically instamines the closest city block.");
    }

    @Override
    public void onDeactivate() {
        if (mineTarget != null) mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, mineTarget, direction));
        mineTarget = null;
        target = null;
    }

    @Override
    public void onActivate() {
        count = 0;
        mining = false;
        target = CityUtils.getPlayerTarget(range.get() + 1.0);
        if (target != null && CityUtils.getTargetBlock(target) != null) {
            Vec3d vec3d = new Vec3d(mc.player.getPos().x, mc.player.getPos().y + 1.0, mc.player.getPos().z);
            Vec3d vec3d1 = new Vec3d(CityUtils.getTargetBlock(target).getX(), CityUtils.getTargetBlock(target).getY(), CityUtils.getTargetBlock(target).getZ());
            if (vec3d.distanceTo(vec3d1) <= range.get()) mineTarget = CityUtils.getTargetBlock(target);
        }

        if (mineTarget != null && target != null) {
            if (mc.player.squaredDistanceTo(mineTarget.getX(), mineTarget.getY(), mineTarget.getZ()) > range.get()) {
                if (selfToggle.get()) {
                    if (chatInfo.get()) info("Target block out of reach, disabling...");
                    toggle();
                    return;
                }
            }

            if (chatInfo.get()) info("Attempting to city (highlight)%s(default).", target.getEntityName());

            targetBlockPos = target.getBlockPos();
            int slot = InvUtils.findInHotbar(Items.BARRIER).slot();
            if (ironPickaxe.get() && slot == -1) slot = InvUtils.findInHotbar(Items.IRON_PICKAXE).slot();
            if (slot == -1) slot = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).slot();
            if (slot == -1) slot = InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).slot();
            if (mc.player.getAbilities().creativeMode) slot = mc.player.getInventory().selectedSlot;
            if (slot == -1) {
                if (chatInfo.get()) info("No pickaxe found, disabling...");
                toggle();
                return;
            }

            if (support.get()) {
                int n2 = InvUtils.findInHotbar(Items.OBSIDIAN).slot();
                BlockPos blockPos = mineTarget.down(1);
                if (!BlockUtils.canPlace(blockPos) && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN && mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && chatInfo.get()) info("Couldn't place support block, mining anyway.");
                else if (n2 == -1) if (chatInfo.get()) info("No obsidian found for support, mining anyway.");
                else BlockUtils.placeEnhanced(blockPos, Hand.MAIN_HAND, n2, rotate.get(), 0, true);
            }

            mc.player.getInventory().selectedSlot = slot;
        } else {
            mineTarget = null;
            target = null;

            if (selfToggle.get()) {
                if (chatInfo.get()) info("No target block found, disabling...");
                toggle();
            }
        }
    }

    private void doMine() {
        --delayLeft;

        if (!mining) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(mineTarget), Rotations.getPitch(mineTarget));
            if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            else mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, mineTarget, direction));
            mining = true;
        }

        if (delayLeft <= 0) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(mineTarget), Rotations.getPitch(mineTarget));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mineTarget, direction));
            delayLeft = delay.get();
        }
    }

    public BlockPos getMineTarget() {
        return mineTarget != null ? mineTarget : null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre pre) {
        if (!assertionsDisabled && mc.world == null) throw new AssertionError();

        if (selfToggle.get()) {
            direction = BlockUtils.rayTraceCheck(mineTarget, true);

            if (!mc.world.isAir(mineTarget)) doMine();
            else ++count;

            if (target == null || !target.isAlive() || count >= toggle.get() || !mineTarget.isWithinDistance(mc.player.getPos(), range.get()) || target.getBlockPos() != targetBlockPos) toggle();
        } else {
            if (target == null) return;
            direction = BlockUtils.rayTraceCheck(mineTarget, true);
            if (!mc.world.isAir(mineTarget)) doMine();
            if (target == null || !target.isAlive() || !mineTarget.isWithinDistance(mc.player.getPos(), range.get()) || target.getBlockPos() != targetBlockPos) toggle();
        }
    }

    @Override
    public String getInfoString() {
        return target != null ? target.getEntityName() : null;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        if (mineTarget == null) return;
        event.renderer.box(mineTarget, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
