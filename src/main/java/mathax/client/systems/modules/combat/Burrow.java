package mathax.client.systems.modules.combat;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.mathax.KeyEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.Utils;
import mathax.client.utils.misc.input.KeyAction;
import mathax.client.utils.player.FindItemResult;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.player.Rotations;
import mathax.client.systems.modules.world.Timer;
import net.minecraft.block.AnvilBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Burrow extends Module {
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private boolean shouldBurrow;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Block> block = sgGeneral.add(new EnumSetting.Builder<Block>()
        .name("block-to-use")
        .description("The block to use for Burrow.")
        .defaultValue(Block.EChest)
        .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Jumps with packets rather than vanilla jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> automatic = sgGeneral.add(new BoolSetting.Builder()
        .name("automatic")
        .description("Automatically burrows on activate rather than waiting for jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> triggerHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("trigger-height")
        .description("How high you have to jump before a rubberband is triggered.")
        .defaultValue(1.12)
        .range(0.01, 1.4)
        .sliderRange(0.01, 1.4)
        .build()
    );

    private final Setting<Double> rubberbandHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("rubberband-height")
        .description("How far to attempt to cause rubberband.")
        .defaultValue(12)
        .sliderRange(-30, 30)
        .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("Timer override.")
        .defaultValue(1)
        .min(0.01)
        .sliderRange(0.01, 10)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-holes")
        .description("Stops you from burrowing when not in a hole.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers you to the middle of the block before burrowing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block you place server-side.")
        .defaultValue(true)
        .build()
    );

    public Burrow() {
        super(Categories.Combat, Items.OBSIDIAN, "burrow", "Attempts to clip you into a block.");
    }

    @Override
    public void onActivate() {
        if (!mc.world.getBlockState(mc.player.getBlockPos()).getMaterial().isReplaceable()) {
            error("Already burrowed, disabling...");
            toggle();
            return;
        }

        if (!PlayerUtils.isInHole2(false) && onlyInHole.get()) {
            error("Not in a hole, disabling...");
            toggle();
            return;
        }

        if (!checkHead()) {
            error("Not enough headroom to burrow, disabling...");
            toggle();
            return;
        }

        FindItemResult result = getItem();

        if (!result.isHotbar() && !result.isOffhand()) {
            error("No burrow block found, disabling...");
            toggle();
            return;
        }

        blockPos.set(mc.player.getBlockPos());

        Modules.get().get(Timer.class).setOverride(this.timer.get());

        shouldBurrow = false;

        if (automatic.get()) {
            if (instant.get()) shouldBurrow = true;
            else mc.player.jump();
        } else info("Waiting for manual jump.");
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!instant.get()) shouldBurrow = mc.player.getY() > blockPos.getY() + triggerHeight.get();
        if (!shouldBurrow && instant.get()) blockPos.set(mc.player.getBlockPos());

        if (shouldBurrow) {
            //When automatic was on (and instant was off), this would just always toggle off without trying to burrow.
            /*if (!mc.player.isOnGround()) {
                toggle();
                return;
            }*/

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(mc.player.getBlockPos()), Rotations.getPitch(mc.player.getBlockPos()), 50, this::burrow);
            else burrow();

            toggle();
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (instant.get() && !shouldBurrow) {
            if (event.action == KeyAction.Press && mc.options.keyJump.matchesKey(event.key, 0)) shouldBurrow = true;
            blockPos.set(mc.player.getBlockPos());
        }
    }

    private void burrow() {
        if (center.get()) PlayerUtils.centerPlayer();

        if (instant.get()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), false));
        }

        FindItemResult block = getItem();

        if (!(mc.player.getInventory().getStack(block.slot()).getItem() instanceof BlockItem)) return;
        InvUtils.swap(block.slot(), true);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(blockPos), Direction.UP, blockPos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        InvUtils.swapBack();

        if (instant.get()) mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ(), false));
        else mc.player.updatePosition(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ());
    }

    private FindItemResult getItem() {
        return switch (block.get()) {
            case EChest -> InvUtils.findInHotbar(Items.ENDER_CHEST);
            case Anvil -> InvUtils.findInHotbar(itemStack -> net.minecraft.block.Block.getBlockFromItem(itemStack.getItem()) instanceof AnvilBlock);
            case Held -> new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());
            default -> InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN);
        };
    }

    private boolean checkHead() {
        return mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() + .3)).getMaterial().isReplaceable() & mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() - .3)).getMaterial().isReplaceable() & mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() - .3)).getMaterial().isReplaceable() & mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() + .3)).getMaterial().isReplaceable();
    }

    public enum Block {
        EChest("EChest"),
        Obsidian("Obsidian"),
        Anvil("Anvil"),
        Held("Held");

        private final String title;

        Block(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
