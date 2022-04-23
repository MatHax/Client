package mathax.client.systems.modules.movement;

import baritone.api.BaritoneAPI;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.entity.player.PlayerMoveEvent;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.mixin.PlayerMoveC2SPacketAccessor;
import mathax.client.mixin.PlayerPositionLookS2CPacketAccessor;
import mathax.client.mixininterface.IPlayerMoveC2SPacket;
import mathax.client.mixininterface.IVec3d;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.entity.EntityUtils;
import mathax.client.utils.player.FindItemResult;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.player.Rotations;
import mathax.client.utils.world.BlockUtils;
import mathax.client.utils.world.Dimension;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Predicate;

/*/--------------------------------------------------------------------------------------------------------------------------------------------------/*/
/*/ Used from Meteor Client and modified by Matejko06 using No Fall+ made by cally72jhb                                                              /*/
/*/                                                                                                                                                  /*/
/*/ https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/movement/NoFall.java /*/
/*/ https://github.com/cally72jhb/vector-addon/blob/main/src/main/java/cally72jhb/addon/system/modules/movement/NoFallPlus.java                      /*/
/*/--------------------------------------------------------------------------------------------------------------------------------------------------/*/

public class NoFall extends Module {
    private final ArrayList<PlayerMoveC2SPacket> packets = new ArrayList<>();

    private final Random random = new Random();

    private boolean placedWater;

    private int preBaritoneFallHeight;
    private int ticksExisted = 0;
    private int teleportId;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How to cancel the fall damage.")
        .defaultValue(Mode.Packet)
        .build()
    );

    private final Setting<PacketMode> packetMode = sgGeneral.add(new EnumSetting.Builder<PacketMode>()
        .name("packet-mode")
        .description("Which packets to send to the server.")
        .defaultValue(PacketMode.Down)
        .build()
    );

    private final Setting<Boolean> tryPreventingFlightDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-flight-damage")
        .description("Tries to prevent getting damage when using Flight.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-distance")
        .description("After what fall distance to trigger this module.")
        .defaultValue(5)
        .range(3, 6)
        .build()
    );

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("After what fall distance to trigger this module.")
        .defaultValue(0.062)
        .min(0)
        .sliderRange(0.01, 1)
        .build()
    );

    private final Setting<Boolean> bounds = sgGeneral.add(new BoolSetting.Builder()
        .name("bounds")
        .description("Bounds for the player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<PlaceMode> airPlaceMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("Determines if place mode places before you die or before you take damage.")
        .defaultValue(PlaceMode.Before_Death)
        .visible(() -> mode.get() == Mode.Air_Place)
        .build()
    );

    private final Setting<Boolean> anchor = sgGeneral.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Centers the player and reduces movement when using bucket or air place mode.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Bucket || mode.get() == Mode.Air_Place)
        .build()
    );

    public NoFall() {
        super(Categories.Movement, Items.FEATHER, "no-fall", "Attempts to prevent you from taking fall damage.");
    }

    @Override
    public void onActivate() {
        preBaritoneFallHeight = BaritoneAPI.getSettings().maxFallHeightNoWater.value;
        if (mode.get() == Mode.Packet) BaritoneAPI.getSettings().maxFallHeightNoWater.value = 255;
        ticksExisted = 0;
    }

    @Override
    public void onDeactivate() {
        BaritoneAPI.getSettings().maxFallHeightNoWater.value = preBaritoneFallHeight;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (tryPreventingFlightDamage.get() || mc.player.getAbilities().creativeMode || !(event.packet instanceof PlayerMoveC2SPacket) || ((IPlayerMoveC2SPacket) event.packet).getNbt() == 1337) return;

        if (!Modules.get().isActive(Flight.class)) {
            if (mc.player.isFallFlying()) return;
            if (mc.player.getVelocity().y > -0.5) return;
            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
        } else ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.getAbilities().creativeMode) return;

        switch (mode.get()) {
            case Air_Place -> {
                if (!airPlaceMode.get().test(mc.player.fallDistance)) return;

                if (anchor.get()) PlayerUtils.centerPlayer();

                Rotations.rotate(mc.player.getYaw(), 90, Integer.MAX_VALUE, () -> {
                    double preY = mc.player.getVelocity().y;
                    ((IVec3d) mc.player.getVelocity()).setY(0);

                    BlockUtils.place(mc.player.getBlockPos().down(), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), false, 0, true);

                    ((IVec3d) mc.player.getVelocity()).setY(preY);
                });
            }
            case Bucket -> {
                if (mc.player.fallDistance > 3 && !EntityUtils.isAboveWater(mc.player)) {
                    FindItemResult waterBucket = InvUtils.findInHotbar(Items.WATER_BUCKET);
                    if (!waterBucket.found() || PlayerUtils.getDimension().equals(Dimension.Nether)) waterBucket = InvUtils.findInHotbar(Items.POWDER_SNOW_BUCKET);

                    if (!waterBucket.found()) return;

                    if (anchor.get()) PlayerUtils.centerPlayer();

                    BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos(), mc.player.getPos().subtract(0, 5, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

                    if (result != null && result.getType() == HitResult.Type.BLOCK) useBucket(waterBucket, true);
                }

                if (placedWater && (mc.player.getBlockStateAtPos().getFluidState().getFluid() == Fluids.WATER || mc.player.getBlockStateAtPos().getBlock() == Blocks.POWDER_SNOW)) useBucket(InvUtils.findInHotbar(Items.BUCKET), false);
            }
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        ticksExisted++;

        switch (mode.get()) {
            case Packet_Plus:
                if (mc.player.fallDistance > fallDistance.get()) {
                    if (teleportId <= 0) {
                        PlayerMoveC2SPacket boundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
                        packets.add(boundsPos);
                        mc.getNetworkHandler().sendPacket(boundsPos);
                    } else {
                        PlayerMoveC2SPacket nextPos = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.062, mc.player.getZ(), mc.player.isOnGround());
                        packets.add(nextPos);
                        mc.getNetworkHandler().sendPacket(nextPos);

                        PlayerMoveC2SPacket downPacket = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), 1, mc.player.getZ(), mc.player.isOnGround());
                        packets.add(downPacket);
                        mc.getNetworkHandler().sendPacket(downPacket);

                        teleportId++;

                        mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId - 1));
                        mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId));
                        mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId + 1));
                    }
                }
                break;
            case TP:
                if (mc.player.fallDistance > fallDistance.get()) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), 10000, mc.player.getZ(), mc.player.isOnGround()));
                break;
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && mode.get() == Mode.Vanilla && mc.player.fallDistance > fallDistance.get()) ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player.isAlive() && mode.get() == Mode.Packet_Plus && mc.player.fallDistance > fallDistance.get() && event.packet instanceof PlayerPositionLookS2CPacket packet) {
            if (teleportId <= 0) teleportId = packet.getTeleportId();
            else {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mode.get() == Mode.Packet_Plus && mc.player.fallDistance > fallDistance.get() && !mc.player.isOnGround()) ((IVec3d) event.movement).set(0, -fallSpeed.get(), 0);
    }

    private void useBucket(FindItemResult bucket, boolean placedWater) {
        if (!bucket.found()) return;

        Rotations.rotate(mc.player.getYaw(), 90, 10, true, () -> {
            if (bucket.isOffhand()) mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
            else {
                InvUtils.swap(bucket.slot(), true);
                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                InvUtils.swapBack();
            }

            this.placedWater = placedWater;
        });
    }

    private double randomHorizontal() {
        int randomValue = random.nextInt(bounds.get() ? 80 : (packetMode.get() == PacketMode.Obscure ? (ticksExisted % 2 == 0 ? 480 : 100) : 29000000)) + (bounds.get() ? 5 : 500);
        if (random.nextBoolean()) return randomValue;
        return -randomValue;
    }

    public enum Mode {
        Vanilla("Vanilla"),
        Packet("Packet"),
        Packet_Plus("Packet+"),
        Bucket("Bucket"),
        Air_Place("Air Place"),
        TP("TP");

        private final String title;

        Mode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum PacketMode {
        Preserve("Preserve"),
        Down("Down"),
        Bypass("Bypass"),
        Obscure("Obscure");

        private final String title;

        PacketMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum PlaceMode {
        Before_Damage("Before Damage", height -> height > 2),
        Before_Death("Before Death", height -> height > Math.max(PlayerUtils.getTotalHealth(), 2));

        private final String title;
        private final Predicate<Float> fallHeight;

        PlaceMode(String title, Predicate<Float> fallHeight) {
            this.title = title;
            this.fallHeight = fallHeight;
        }

        public boolean test(float fallHeight) {
            return this.fallHeight.test(fallHeight);
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
