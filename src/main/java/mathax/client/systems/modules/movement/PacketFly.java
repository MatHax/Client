package mathax.client.systems.modules.movement;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.entity.player.PlayerMoveEvent;
import mathax.client.events.game.GameJoinedEvent;
import mathax.client.events.game.GameLeftEvent;
import mathax.client.events.mathax.KeyEvent;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.CollisionShapeEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.mixin.PlayerPositionLookS2CPacketAccessor;
import mathax.client.mixininterface.IVec3d;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.entity.EntityUtils;
import mathax.client.utils.misc.KeyBind;
import mathax.client.utils.misc.Timer;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.world.Dimension;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/*/----------------------------------------------------------------------------------------------------------------------------/*/
/*/ Made by cally72jhb                                                                                                         /*/
/*/ https://github.com/cally72jhb/vector-addon/blob/main/src/main/java/cally72jhb/addon/system/modules/movement/PacketFly.java /*/
/*/----------------------------------------------------------------------------------------------------------------------------/*/

public class PacketFly extends Module {
    private final Timer intervalTimer = new Timer();

    private PlayerMoveC2SPacket.PositionAndOnGround startingOutOfBoundsPos;

    private final Map<Integer, TimeVec3d> posLooks = new ConcurrentHashMap<>();
    private final ArrayList<PlayerMoveC2SPacket> packets = new ArrayList<>();

    private static final Random random = new Random();

    private boolean forceAntiKick = true;
    private boolean limitStrict = false;
    private boolean forceLimit = true;
    private boolean oddJitter = false;
    private boolean lastDown = false;

    private int factorCounter = 0;
    private int antiKickTicks = 0;
    private int ticksExisted = 0;
    private int jitterTicks = 0;
    private int limitTicks = 0;
    private int vDelay = 0;
    private int hDelay = 0;
    private int teleportId;

    double speedX = 0;
    double speedY = 0;
    double speedZ = 0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFly = settings.createGroup("Fly");
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");
    private final SettingGroup sgKeybind = settings.createGroup("KeyBind");
    private final SettingGroup sgPhase = settings.createGroup("Phase");

    // General

    private final Setting<Type> type = sgGeneral.add(new EnumSetting.Builder<Type>()
        .name("fly-type")
        .description("The way you are moved by this module.")
        .defaultValue(Type.Factor)
        .onChanged(this::updateFlying)
        .build()
    );

    private final Setting<Mode> packetMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("packet-mode")
        .description("Which packets to send to the server.")
        .defaultValue(Mode.Down)
        .build()
    );

    private final Setting<Bypass> bypass = sgGeneral.add(new EnumSetting.Builder<Bypass>()
        .name("bypass-mode")
        .description("What bypass mode to use.")
        .defaultValue(Bypass.None)
        .build()
    );

    private final Setting<Boolean> onlyOnMove = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-move")
        .description("Only sends packets if your moving.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-on-ground")
        .description("Disables Anti Kick when you are on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("How to handle the vertical movement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bounds = sgGeneral.add(new BoolSetting.Builder()
        .name("bounds")
        .description("Bounds for the player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> multiAxis = sgGeneral.add(new BoolSetting.Builder()
        .name("multi-axis")
        .description("Whether or not to phase in every direction.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle")
        .description("Toggles the module on join and leave.")
        .defaultValue(true)
        .build()
    );

    // Fly

    private final Setting<Double> factor = sgFly.add(new DoubleSetting.Builder()
        .name("factor")
        .description("Your flight factor.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 10)
        .visible(() -> type.get() == Type.Factor || type.get() == Type.Desync)
        .build()
    );

    private final Setting<Integer> ignoreSteps = sgFly.add(new IntSetting.Builder()
        .name("ignore-steps")
        .description("How many steps in a row should be ignored.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> type.get() == Type.Factor || type.get() == Type.Desync)
        .build()
    );

    private final Setting<KeyBind> factorize = sgFly.add(new KeyBindSetting.Builder()
        .name("factorize")
        .description("Key to toggle factor mode.")
        .build()
    );

    private final Setting<Boolean> boost = sgFly.add(new BoolSetting.Builder()
        .name("boost")
        .description("Boost the player.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speed = sgFly.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your flight speed.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    private final Setting<Double> motion = sgFly.add(new DoubleSetting.Builder()
        .name("factorize-motion")
        .description("The motion applied when factorize is pressed.")
        .defaultValue(100)
        .min(0)
        .sliderRange(50, 200)
        .visible(() -> type.get() == Type.Factor || type.get() == Type.Desync)
        .build()
    );

    private final Setting<Double> boostTimer = sgFly.add(new DoubleSetting.Builder()
        .name("boost-timer")
        .description("The timer for boost.")
        .defaultValue(1.1)
        .min(0)
        .visible(boost::get)
        .build()
    );

    // Anti Kick

    private final Setting<AntiKick> antiKick = sgAntiKick.add(new EnumSetting.Builder<AntiKick>()
        .name("anti-kick")
        .description("The anti kick mode.")
        .defaultValue(AntiKick.Normal)
        .build()
    );

    private final Setting<Limit> limit = sgAntiKick.add(new EnumSetting.Builder<Limit>()
        .name("limit")
        .description("The flight limit.")
        .defaultValue(Limit.Strict)
        .build()
    );

    private final Setting<Boolean> constrict = sgAntiKick.add(new BoolSetting.Builder()
        .name("constrict")
        .description("Already send the packets before the tick (only if the limit is none).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> jitter = sgAntiKick.add(new BoolSetting.Builder()
        .name("jitter")
        .description("Randomize the movement.")
        .defaultValue(false)
        .build()
    );

    // KeyBind

    private final Setting<Boolean> message = sgKeybind.add(new BoolSetting.Builder()
        .name("keybind-message")
        .description("Whether or not to send you a message when toggled a mode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<KeyBind> toggleLimit = sgKeybind.add(new KeyBindSetting.Builder()
        .name("toggle-limit")
        .description("Key to toggle Limit on or off.")
        .build()
    );

    private final Setting<KeyBind> toggleAntiKick = sgKeybind.add(new KeyBindSetting.Builder()
        .name("toggle-anti-kick")
        .description("Key to toggle anti kick on or off.")
        .build()
    );

    // Phase

    private final Setting<Phase> phase = sgPhase.add(new EnumSetting.Builder<Phase>()
        .name("phase")
        .description("Whether or not to phase through blocks.")
        .defaultValue(Phase.None)
        .build()
    );

    private final Setting<Boolean> noPhaseSlow = sgPhase.add(new BoolSetting.Builder()
        .name("no-phase-slow")
        .description("Whether or not to phase fast or slow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noCollision = sgPhase.add(new BoolSetting.Builder()
        .name("no-collision")
        .description("Whether or not to disable block collisions.")
        .defaultValue(false)
        .build()
    );

    public PacketFly() {
        super(Categories.Movement, Items.COMMAND_BLOCK, "packet-fly", "Allows you to fly with packets.");
    }

    @Override
    public void onActivate() {
        packets.clear();
        posLooks.clear();
        teleportId = 0;
        vDelay = 0;
        hDelay = 0;
        antiKickTicks = 0;
        limitTicks = 0;
        jitterTicks = 0;
        ticksExisted = 0;
        speedX = 0;
        speedY = 0;
        speedZ = 0;
        lastDown = false;
        oddJitter = false;
        forceAntiKick = true;
        forceLimit = true;
        startingOutOfBoundsPos = null;
        startingOutOfBoundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
        packets.add(startingOutOfBoundsPos);
        mc.getNetworkHandler().sendPacket(startingOutOfBoundsPos);
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) mc.player.setVelocity(0, 0, 0);

        GameMode mode = EntityUtils.getGameMode(mc.player);
        if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }

        Modules.get().get(mathax.client.systems.modules.world.Timer.class).setOverride(mathax.client.systems.modules.world.Timer.OFF);
    }

    // Info Sting

    @Override
    public String getInfoString() {
        String info = "";

        info += "[" + type.get().name().substring(0, 1).toUpperCase() + type.get().name().substring(1).toLowerCase() + "] ";
        if (forceAntiKick) info += "[Anti Kick] ";
        if (forceLimit) info += "[Limit]";

        return info;
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        if (autoToggle.get()) toggle();
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        if (autoToggle.get()) toggle();
    }

    @EventHandler
    public void isCube(CollisionShapeEvent event) {
        if (phase.get() != Phase.None && noCollision.get()) event.shape = VoxelShapes.empty();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!(mc.currentScreen == null)) return;

        if (toggleLimit.get().isPressed()) {
            forceLimit = !forceLimit;
            if (message.get()) info(Text.of(forceLimit ? "Activated Packet Limit" : "Disabled Packet Limit"));
        }

        if (toggleAntiKick.get().isPressed()) {
            forceAntiKick = !forceAntiKick;
            if (message.get()) info(Text.of(forceAntiKick ? "Activated Anti Kick" : "Disabled Anti Kick"));
        }
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (boost.get()) Modules.get().get(mathax.client.systems.modules.world.Timer.class).setOverride(boostTimer.get().floatValue());
        else Modules.get().get(mathax.client.systems.modules.world.Timer.class).setOverride(mathax.client.systems.modules.world.Timer.OFF);

        if (type.get() == Type.Off_Ground) {
            if (isMoving() && onGround()) {
                for (double i = 0.0625; i < factor.get(); i += 0.262) {
                    double[] dir = directionSpeed(i);
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1], mc.player.isOnGround()));
                }

                if (strict.get()) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + (bounds.get() ? mc.player.getVelocity().x : 0), mc.player.getY() + 1, mc.player.getZ() + (bounds.get() ? mc.player.getVelocity().z : 0), mc.player.isOnGround()));
            }
        }
    }

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        if (type.get() == Type.Elytra) {
            Vec3d vec3d = new Vec3d(0,0,0);

            if (mc.player.fallDistance <= 0.2) return;

            if (mc.options.keyForward.isPressed()) {
                vec3d.add(0, 0, speed.get());
                vec3d.rotateY(-(float) Math.toRadians(mc.player.getYaw()));
            } else if (mc.options.keyBack.isPressed()) {
                vec3d.add(0, 0, speed.get());
                vec3d.rotateY((float) Math.toRadians(mc.player.getYaw()));
            }

            if (mc.options.keyJump.isPressed()) vec3d.add(0, speed.get(), 0);
            else if (mc.options.keySneak.isPressed()) vec3d.add(0, -speed.get(), 0);

            mc.player.setVelocity(vec3d);
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));

            return;
        }

        if (type.get() == Type.Off_Ground) return;

        if (ticksExisted % 20 == 0) {
            posLooks.forEach((tp, timeVec3d) -> {
                if (System.currentTimeMillis() - timeVec3d.getTime() > TimeUnit.SECONDS.toMillis(30L)) posLooks.remove(tp);
            });
        }

        ticksExisted++;

        mc.player.setVelocity(0.0D, 0.0D, 0.0D);

        if (teleportId <= 0 && type.get() != Type.Set_Back) {
            startingOutOfBoundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
            packets.add(startingOutOfBoundsPos);
            mc.getNetworkHandler().sendPacket(startingOutOfBoundsPos);
            return;
        }

        boolean phasing = checkCollisionBox();

        speedX = 0;
        speedY = 0;
        speedZ = 0;

        if (mc.options.keyJump.isPressed() && (hDelay < 1 || (multiAxis.get() && phasing))) {
            if (ticksExisted % (type.get() == Type.Set_Back || type.get() == Type.Slow || limit.get() == Limit.Strict && forceLimit ? 10 : 20) == 0) speedY = (antiKick.get() != AntiKick.None && forceAntiKick && onGround()) ? -0.032 : 0.062;
            else speedY = 0.062;
            antiKickTicks = 0;
            vDelay = 5;
        } else if (mc.options.keySneak.isPressed() && (hDelay < 1 || (multiAxis.get() && phasing))) {
            speedY = -0.062;
            antiKickTicks = 0;
            vDelay = 5;
        }

        if ((multiAxis.get() && phasing) || !(mc.options.keySneak.isPressed() && mc.options.keyJump.isPressed())) {
            if (isPlayerMoving()) {
                double[] dir = directionSpeed((((phasing && phase.get() == Phase.NCP ) || bypass.get() == Bypass.NCP) ? (noPhaseSlow.get() ? (multiAxis.get() ? 0.0465 : 0.062) : 0.031) : 0.26) * speed.get());
                if ((dir[0] != 0 || dir[1] != 0) && (vDelay < 1 || (multiAxis.get() && phasing))) {
                    speedX = dir[0];
                    speedZ = dir[1];
                    hDelay = 5;
                }
            }

            if (antiKick.get() != AntiKick.None && forceAntiKick && onGround() && ((limit.get() == Limit.None && forceLimit) || limitTicks != 0)) {
                if (antiKickTicks < (packetMode.get() == Mode.Bypass && !bounds.get() ? 1 : 3)) antiKickTicks++;
                else {
                    antiKickTicks = 0;
                    if ((antiKick.get() != AntiKick.Limited && forceAntiKick && onGround()) || !phasing) speedY = (antiKick.get() == AntiKick.Strict && forceAntiKick && onGround()) ? -0.08 : -0.04;
                }
            }
        }

        if (((phasing && phase.get() == Phase.NCP) || bypass.get() == Bypass.NCP) && (double) mc.player.forwardSpeed != 0.0 || (double) mc.player.sidewaysSpeed != 0.0 && speedY != 0) speedY /= 2.5;

        if (limit.get() != Limit.None && forceLimit) {
            if (limitTicks == 0) {
                speedX = 0;
                speedY = 0;
                speedZ = 0;
            } else if (limitTicks == 2 && jitter.get()) {
                if (oddJitter) {
                    speedX = 0;
                    speedY = 0;
                    speedZ = 0;
                }

                oddJitter = !oddJitter;
            }
        } else if (jitter.get() && jitterTicks == 7) {
            speedX = 0;
            speedY = 0;
            speedZ = 0;
        }

        switch (type.get()) {
            case Fast -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, false);
            }
            case Slow -> {
                if (!isMoving()) break;
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, false);
            }
            case Set_Back -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), false, false);
            }
            case Vector -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, true);
            }
            case Factor, Desync -> {
                float rawFactor = factor.get().floatValue();
                if (factorize.get().isPressed() && intervalTimer.passedMillis(3500)) {
                    intervalTimer.reset();
                    rawFactor = motion.get().floatValue();
                }
                int factorInt = (int) Math.floor(rawFactor);
                int ignore = 0;
                factorCounter++;
                if (factorCounter > (int) (20D / ((rawFactor - (double) factorInt) * 20D))) {
                    factorInt += 1;
                    factorCounter = 0;
                }
                for (int i = 1; i <= factorInt; ++i) {
                    if (ignore <= 0) {
                        ignore = ignoreSteps.get();
                        mc.player.setVelocity(speedX * i, speedY * i, speedZ * i);
                        sendPackets(isMoving() ? speedX * i : 0, speedY * i, isMoving() ? speedZ * i : 0, packetMode.get(), true, false);
                    } else ignore--;
                }
                speedX = mc.player.getVelocity().x;
                speedY = mc.player.getVelocity().y;
                speedZ = mc.player.getVelocity().z;
            }
        }

        vDelay--;
        hDelay--;

        if (constrict.get() && ((limit.get() == Limit.None && forceLimit) || limitTicks > 1)) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));

        limitTicks++;
        jitterTicks++;

        if (limitTicks > ((limit.get() == Limit.Strict && forceLimit) ? (limitStrict ? 1 : 2) : 3)) {
            limitTicks = 0;
            limitStrict = !limitStrict;
        }

        if (jitterTicks > 7) jitterTicks = 0;
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (type.get() == Type.Elytra) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
            if (mc.player.isAlive()) {
                if (this.teleportId <= 0) this.teleportId = ((PlayerPositionLookS2CPacket) event.packet).getTeleportId();
                else {
                    if (mc.world.isPosLoaded(mc.player.getBlockX(), mc.player.getBlockZ()) &&
                        type.get() != Type.Set_Back) {
                        if (type.get() == Type.Desync) {
                            posLooks.remove(packet.getTeleportId());
                            event.cancel();
                            if (type.get() == Type.Slow) mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                            return;
                        } else if (posLooks.containsKey(packet.getTeleportId())) {
                            TimeVec3d vec = posLooks.get(packet.getTeleportId());
                            if (vec.x == packet.getX() && vec.y == packet.getY() && vec.z == packet.getZ()) {
                                posLooks.remove(packet.getTeleportId());
                                event.cancel();
                                if (type.get() == Type.Slow) mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                                return;
                            }
                        }
                    }
                }
            }

            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
            teleportId = packet.getTeleportId();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (type.get() == Type.Off_Ground) return;
        if (type.get() == Type.Elytra) {
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(speed.get().floatValue() / 20);
            return;
        }

        if (type.get() != Type.Set_Back && teleportId <= 0) return;
        if (type.get() != Type.Slow) ((IVec3d) event.movement).set(speedX, speedY, speedZ);
    }

    @EventHandler
    public void onSend(PacketEvent.Send event) {
        if (type.get() == Type.Off_Ground) return;

        if (type.get() == Type.Elytra && event.packet instanceof PlayerMoveC2SPacket) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return;
        }

        if (event.packet instanceof PlayerMoveC2SPacket && !(event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround)) event.cancel();

        if (event.packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;
            if (this.packets.contains(packet)) {
                this.packets.remove(packet);
                return;
            }

            event.cancel();
        }
    }

    public void updateFlying(Type type) {
        if (mc.world != null && mc.player != null && type != Type.Elytra) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }
    }

    private void sendPackets(double x, double y, double z, Mode mode, boolean confirmTeleport, boolean sendExtraConfirmTeleport) {
        Vec3d nextPos = new Vec3d(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
        Vec3d bounds = getBoundsVec(x, y, z, mode);

        PlayerMoveC2SPacket nextPosPacket = new PlayerMoveC2SPacket.PositionAndOnGround(nextPos.x, nextPos.y, nextPos.z, mc.player.isOnGround());
        packets.add(nextPosPacket);
        mc.getNetworkHandler().sendPacket(nextPosPacket);

        if ((limit.get() != Limit.None && forceLimit) && limitTicks == 0) return;

        PlayerMoveC2SPacket boundsPacket = new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, mc.player.isOnGround());
        packets.add(boundsPacket);
        mc.getNetworkHandler().sendPacket(boundsPacket);

        if (confirmTeleport) {
            teleportId++;

            if (sendExtraConfirmTeleport) mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId - 1));

            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId));

            posLooks.put(teleportId, new TimeVec3d(nextPos.x, nextPos.y, nextPos.z, System.currentTimeMillis()));

            if (sendExtraConfirmTeleport) mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId + 1));
        }
    }

    private Vec3d getBoundsVec(double x, double y, double z, Mode mode) {
        switch (mode) {
            case Up:
                return new Vec3d(mc.player.getX() + x, bounds.get() ? (strict.get() ? 255 : 256) : mc.player.getY() + 420, mc.player.getZ() + z);
            case Preserve:
                return new Vec3d(bounds.get() ? mc.player.getX() + randomHorizontal() : randomHorizontal(), strict.get() ? (Math.max(mc.player.getY(), 2D)) : mc.player.getY(), bounds.get() ? mc.player.getZ() + randomHorizontal() : randomHorizontal());
            case Limit_Jitter:
                return new Vec3d(mc.player.getX() + (strict.get() ? x : randomLimitedHorizontal()), mc.player.getY() + randomLimitedVertical(), mc.player.getZ() + (strict.get() ? z : randomLimitedHorizontal()));
            case Bypass:
                if (bounds.get()) {
                    double rawY = y * 510;
                    return new Vec3d(mc.player.getX() + x, mc.player.getY() + ((rawY > ((PlayerUtils.getDimension() == Dimension.End) ? 127 : 255)) ? -rawY : (rawY < 1) ? -rawY : rawY), mc.player.getZ() + z);
                } else return new Vec3d(mc.player.getX() + (x == 0D ? (random.nextBoolean() ? -10 : 10) : x * 38), mc.player.getY() + y, mc.player.getX() + (z == 0D ? (random.nextBoolean() ? -10 : 10) : z * 38));
            case Obscure:
                return new Vec3d(mc.player.getX() + randomHorizontal(), Math.max(1.5D, Math.min(mc.player.getY() + y, 253.5D)), mc.player.getZ() + randomHorizontal());
            default:
                return new Vec3d(mc.player.getX() + x, bounds.get() ? (strict.get() ? 1 : 0) : mc.player.getY() - 1337, mc.player.getZ() + z);
        }
    }

    public double randomHorizontal() {
        int randomValue = random.nextInt(bounds.get() ? 80 : (packetMode.get() == Mode.Obscure ? (ticksExisted % 2 == 0 ? 480 : 100) : 29000000)) + (bounds.get() ? 5 : 500);
        if (random.nextBoolean()) return randomValue;
        return -randomValue;
    }

    public static double randomLimitedVertical() {
        int randomValue = random.nextInt(22);
        randomValue += 70;
        if (random.nextBoolean()) return randomValue;
        return -randomValue;
    }

    public static double randomLimitedHorizontal() {
        int randomValue = random.nextInt(10);
        if (random.nextBoolean()) return randomValue;
        return -randomValue;
    }

    private double[] directionSpeed(double speed) {
        float forward = mc.player.forwardSpeed;
        float side = mc.player.sidewaysSpeed;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);

        if (forward != 0.0f) {
            if (side > 0.0f) yaw += ((forward > 0.0f) ? -45 : 45);
            else if (side < 0.0f) yaw += ((forward > 0.0f) ? 45 : -45);
            side = 0.0f;
            if (forward > 0.0f) forward = 1.0f;
            else if (forward < 0.0f) forward = -1.0f;
        }

        final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        final double posX = forward * speed * cos + side * speed * sin;
        final double posZ = forward * speed * sin - side * speed * cos;

        return new double[] {
            posX,
            posZ
        };
    }

    private boolean checkCollisionBox() {
        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox()).iterator().hasNext();
    }

    private boolean onGround() {
        if (stopOnGround.get()) return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.01, 0)).iterator().hasNext();

        return true;
    }

    private boolean isPlayerMoving() {
        if (mc.options.keyJump.isPressed()) return true;
        if (mc.options.keyForward.isPressed()) return true;
        if (mc.options.keyBack.isPressed()) return true;
        if (mc.options.keyLeft.isPressed()) return true;
        return mc.options.keyRight.isPressed();
    }

    private boolean isMoving() {
        if (onlyOnMove.get()) {
            if (mc.options.keyJump.isPressed()) return true;
            if (mc.options.keySneak.isPressed()) return true;
            if (mc.options.keyForward.isPressed()) return true;
            if (mc.options.keyBack.isPressed()) return true;
            if (mc.options.keyLeft.isPressed()) return true;
            return mc.options.keyRight.isPressed();
        }

        return true;
    }

    private static class TimeVec3d extends Vec3d {
        private final long time;

        public TimeVec3d(double xIn, double yIn, double zIn, long time) {
            super(xIn, yIn, zIn);
            this.time = time;
        }

        public long getTime() {
            return time;
        }
    }

    public enum Type {
        Factor,
        Set_Back,
        Fast,
        Slow,
        Elytra,
        Desync,
        Vector,
        Off_Ground;

        @Override
        public String toString() {
            return super.toString().replace("_", " ");
        }
    }

    public enum Mode {
        Preserve,
        Up,
        Down,
        Limit_Jitter,
        Bypass,
        Obscure;

        @Override
        public String toString() {
            return super.toString().replace("_", " ");
        }
    }

    public enum Bypass {
        None,
        Default,
        NCP
    }

    public enum Phase {
        None,
        Vanilla,
        NCP
    }

    public enum AntiKick {
        None,
        Normal,
        Limited,
        Strict
    }

    public enum Limit {
        None,
        Strong,
        Strict
    }
}
