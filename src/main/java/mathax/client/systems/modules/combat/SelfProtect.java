package mathax.client.systems.modules.combat;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.EnumSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.player.Rotations;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Iterator;

public class SelfProtect extends Module {
    private boolean tntAured = false;
    private boolean bedAured = false;
    private boolean ceved = false;
    private boolean breaking;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAnchorAura = settings.createGroup("Anchor Aura");
    private final SettingGroup sgBedAura = settings.createGroup("Bed Aura");
    private final SettingGroup sgCEVBreaker = settings.createGroup("CEV Breaker");
    private final SettingGroup sgTNTAura = settings.createGroup("TNT Aura");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates you towards the blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeStringTop = sgGeneral.add(new BoolSetting.Builder()
        .name("top")
        .description("Places string above you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> placeStringMiddle = sgGeneral.add(new BoolSetting.Builder()
        .name("middle")
        .description("Places string in your head.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeStringBottom = sgGeneral.add(new BoolSetting.Builder()
        .name("bottom")
        .description("Places string at your feet.")
        .defaultValue(false)
        .build()
    );

    // Anchor Aura

    private final Setting<Boolean> antiAnchorAura = sgAnchorAura.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Prevents Anchor Aura.")
        .defaultValue(true)
        .build()
    );

    // Bed Aura

    private final Setting<Boolean> antiBedAura = sgBedAura.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Prevents Bed Aura.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smart = sgBedAura.add(new BoolSetting.Builder()
        .name("smart")
        .description("Makes the anti bed aura smart.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Use> useStringWeb = sgGeneral.add(new EnumSetting.Builder<Use>()
        .name("use")
        .description("Determines what to use.")
        .defaultValue(Use.String)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgBedAura.add(new BoolSetting.Builder()
        .name("only-hole")
        .description("Prevents bed aura only in hole.")
        .defaultValue(true)
        .build()
    );

    // CEV Breaker

    private final Setting<Boolean> antiCEVBreaker = sgCEVBreaker.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Prevents CEV Breaker.")
        .defaultValue(true)
        .build()
    );

    // TNT Aura

    private final Setting<Boolean> antiTNTAura = sgTNTAura.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Prevents TNT Aura.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeObsidian = sgTNTAura.add(new BoolSetting.Builder()
        .name("place-obdisian")
        .description("Places obsidian.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings your hand client-side when placing or interacting.")
        .defaultValue(true)
        .build()
    );

    public SelfProtect() {
        super(Categories.Combat, Items.OBSIDIAN, "self-protect", "Protects you from various combat modules.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (antiAnchorAura.get() && mc.world.getBlockState(mc.player.getBlockPos().up(2)).getBlock() == Blocks.RESPAWN_ANCHOR && mc.world.getBlockState(mc.player.getBlockPos().up()).getBlock() == Blocks.AIR) BlockUtils.place(mc.player.getBlockPos().add(0, 1, 0), InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof SlabBlock), rotate.get(), 15, swing.get(), false, true);

        BlockPos top = mc.player.getBlockPos().up(2);
        if (antiCEVBreaker.get() && mc.world.getBlockState(top).getBlock() == Blocks.OBSIDIAN) {
            Iterator<Entity> iterator = mc.world.getEntities().iterator();

            ceved:
            while (true) {
                while (true) {
                    if (!iterator.hasNext()) break ceved;

                    Entity crystal = iterator.next();
                    if (crystal instanceof EndCrystalEntity && crystal.getBlockPos().equals(top.up())) {
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                        ceved = true;
                    } else if (ceved) {
                        BlockUtils.place(top.up(), InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 50, true);
                        ceved = false;
                    }
                }
            }
        }

        if (antiTNTAura.get()) {
            if (mc.world.getBlockState(top).getBlock().equals(Blocks.TNT)) {
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(top), Rotations.getPitch(top), () -> mine(top));
                else mine(top);

                tntAured = true;
            } else if (tntAured) {
                if (placeObsidian.get()) BlockUtils.place(top, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 50, true);
                tntAured = false;
            }
        }

        if (antiBedAura.get()) {
            if (onlyInHole.get() && !PlayerUtils.isInHole2(true)) return;

            BlockPos head = mc.player.getBlockPos().up();
            if (mc.world.getBlockState(head).getBlock() instanceof BedBlock && !breaking) {
                Rotations.rotate(Rotations.getYaw(head), Rotations.getPitch(head), 50, () -> sendMinePackets(head));
                breaking = true;
            } else if (breaking) {
                Rotations.rotate(Rotations.getYaw(head), Rotations.getPitch(head), 50, () -> sendStopPackets(head));
                breaking = false;
            }

            if (smart.get()) {
                if (mc.world.getBlockState(head).getBlock() instanceof BedBlock) bedAured = true;
                else if (bedAured) {
                    if (placeStringTop.get()) place(mc.player.getBlockPos().up(2));
                    if (placeStringMiddle.get()) place(mc.player.getBlockPos().up(1));
                    if (placeStringBottom.get()) place(mc.player.getBlockPos());

                    tntAured = false;
                }
            } else if (!smart.get()) {
                if (placeStringTop.get()) place(mc.player.getBlockPos().up(2));
                if (placeStringMiddle.get()) place(mc.player.getBlockPos().up(1));
                if (placeStringBottom.get()) place(mc.player.getBlockPos());
            }
        }
    }

    private void place(BlockPos blockPos) {
        if (useStringWeb.get() == Use.String && mc.world.getBlockState(blockPos).getBlock().asItem() != Items.STRING) BlockUtils.place(blockPos, InvUtils.findInHotbar(Items.STRING), 50, swing.get(), false);
        else if (useStringWeb.get() == Use.Web && mc.world.getBlockState(blockPos).getBlock().asItem() != Items.COBWEB) BlockUtils.place(blockPos, InvUtils.findInHotbar(Items.COBWEB), 50, swing.get(), false);
    }

    private void mine(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    private void sendMinePackets(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    private void sendStopPackets(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    public enum Use {
        String("String"),
        Web("Web");

        private final String title;

        Use(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
