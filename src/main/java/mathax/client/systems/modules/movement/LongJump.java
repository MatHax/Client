package mathax.client.systems.modules.movement;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.entity.player.PlayerMoveEvent;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.mixininterface.IVec3d;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.Utils;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.systems.modules.world.Timer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class LongJump extends Module {
    private boolean jumping = false;
    private boolean jumped = false;

    private double moveSpeed;

    private int groundTicks;
    private int airTicks;
    private int stage;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<Mode> jumpMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The method of jumping.")
        .defaultValue(Mode.Vanilla)
        .build()
    );

    private final Setting<Double> vanillaBoostFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("vanilla-boost-factor")
        .description("The amount by which to boost the jump.")
        .visible(() -> jumpMode.get() == Mode.Vanilla)
        .defaultValue(1.261)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    private final Setting<Double> burstInitialSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("burst-initial-speed")
        .description("The initial speed of the runup.")
        .visible(() -> jumpMode.get() == Mode.Burst)
        .defaultValue(6)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Double> burstBoostFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("burst-boost-factor")
        .description("The amount by which to boost the jump.")
        .visible(() -> jumpMode.get() == Mode.Burst)
        .defaultValue(2.149)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only performs the jump if you are on the ground.")
        .visible(() -> jumpMode.get() == Mode.Burst)
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onJump = sgGeneral.add(new BoolSetting.Builder()
        .name("on-jump")
        .description("Whether the player needs to jump first or not.")
        .visible(() -> jumpMode.get() == Mode.Burst)
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> glideMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("glide-multiplier")
        .description("The amount by to multiply the glide velocity.")
        .visible(() -> jumpMode.get() == Mode.Glide)
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    public final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("Timer override.")
        .defaultValue(1)
        .min(0.01)
        .sliderRange(0.01, 3)
        .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Automatically disabled the module after jumping.")
        .visible(() -> jumpMode.get() != Mode.Vanilla)
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnRubberband = sgGeneral.add(new BoolSetting.Builder()
        .name("rubberband-disable")
        .description("Disables the module when you get lagged back.")
        .defaultValue(true)
        .build()
    );

    public LongJump() {
        super(Categories.Movement, Items.FEATHER, "long-jump", "Allows you to jump further than normal.");
    }

    @Override
    public void onActivate() {
        stage = 0;
        jumping = false;
        airTicks = 0;
        groundTicks = -5;
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && disableOnRubberband.get()) {
            info("Rubberband detected! Disabling...");
            toggle();
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        Modules.get().get(Timer.class).setOverride(PlayerUtils.isMoving() ? timer.get() : Timer.OFF);
        switch (jumpMode.get()) {
            case Vanilla -> {
                if (PlayerUtils.isMoving() && mc.options.keyJump.isPressed()) {
                    double dir = getDir();

                    double xDir = Math.cos(Math.toRadians(dir + 90));
                    double zDir = Math.sin(Math.toRadians(dir + 90));

                    if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision) ((IVec3d) event.movement).setXZ(xDir * 0.29F, zDir * 0.29F);
                    if ((event.movement.getY() == .33319999363422365)) ((IVec3d) event.movement).setXZ(xDir * vanillaBoostFactor.get(), zDir * vanillaBoostFactor.get());
                }
            }
            case Burst -> {
                if (stage != 0 && !mc.player.isOnGround() && autoDisable.get()) jumping = true;
                if (jumping && (mc.player.getY() - (int) mc.player.getY() < 0.01)) {
                    jumping = false;
                    toggle();
                    info("Disabling after jump.");
                }

                if (onlyOnGround.get() && !mc.player.isOnGround() && stage == 0) return;

                double xDist = mc.player.getX() - mc.player.prevX;
                double zDist = mc.player.getZ() - mc.player.prevZ;
                double lastDist = Math.sqrt((xDist * xDist) + (zDist * zDist));

                if (PlayerUtils.isMoving() && (!onJump.get() || mc.options.keyJump.isPressed()) && !mc.player.isInLava() && !mc.player.isTouchingWater()) {
                    if (stage == 0) moveSpeed = getMoveSpeed() * burstInitialSpeed.get();
                    else if (stage == 1) {
                        ((IVec3d) event.movement).setY(0.42);
                        moveSpeed *= burstBoostFactor.get();
                    } else if (stage == 2) {
                        final double difference = lastDist - getMoveSpeed();
                        moveSpeed = lastDist - difference;
                    } else moveSpeed = lastDist - lastDist / 159;

                    setMoveSpeed(event, moveSpeed = Math.max(getMoveSpeed(), moveSpeed));
                    if (!mc.player.verticalCollision && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, -0.4, 0.0))) ((IVec3d) event.movement).setY(-0.001);

                    stage++;
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (Utils.canUpdate() && jumpMode.get() == Mode.Glide) {
            if (!PlayerUtils.isMoving()) return;

            float yaw = mc.player.getYaw() + 90;
            double forward = ((mc.player.forwardSpeed != 0) ? ((mc.player.forwardSpeed > 0) ? 1 : -1) : 0);
            float[] motion = {0.4206065F, 0.4179245F, 0.41525924F, 0.41261F, 0.409978F, 0.407361F, 0.404761F, 0.402178F, 0.399611F, 0.39706F, 0.394525F, 0.392F, 0.3894F, 0.38644F, 0.383655F, 0.381105F, 0.37867F, 0.37625F, 0.37384F, 0.37145F, 0.369F, 0.3666F, 0.3642F, 0.3618F, 0.35945F, 0.357F, 0.354F, 0.351F, 0.348F, 0.345F, 0.342F, 0.339F, 0.336F, 0.333F, 0.33F, 0.327F, 0.324F, 0.321F, 0.318F, 0.315F, 0.312F, 0.309F, 0.307F, 0.305F, 0.303F, 0.3F, 0.297F, 0.295F, 0.293F, 0.291F, 0.289F, 0.287F, 0.285F, 0.283F, 0.281F, 0.279F, 0.277F, 0.275F, 0.273F, 0.271F, 0.269F, 0.267F, 0.265F, 0.263F, 0.261F, 0.259F, 0.257F, 0.255F, 0.253F, 0.251F, 0.249F, 0.247F, 0.245F, 0.243F, 0.241F, 0.239F, 0.237F};
            float[] glide = {0.3425F, 0.5445F, 0.65425F, 0.685F, 0.675F, 0.2F, 0.895F, 0.719F, 0.76F};

            final double cos = Math.cos(Math.toRadians(yaw));
            final double sin = Math.sin(Math.toRadians(yaw));

            if (!mc.player.verticalCollision && !mc.player.isOnGround()) {
                jumped = true;
                airTicks += 1;
                groundTicks = -5;

                double velocityY = mc.player.getVelocity().y;

                if (airTicks - 6 >= 0 && airTicks - 6 < glide.length) updateY(velocityY * glide[(airTicks - 6)] * glideMultiplier.get());

                if (velocityY < -0.2 && velocityY > -0.24) updateY(velocityY * 0.7 * glideMultiplier.get());
                else if (velocityY < -0.25 && velocityY > -0.32) updateY(velocityY * 0.8 * glideMultiplier.get());
                else if (velocityY < -0.35 && velocityY > -0.8) updateY(velocityY * 0.98 * glideMultiplier.get());

                if (airTicks - 1 >= 0 && airTicks - 1 < motion.length) mc.player.setVelocity((forward * motion[(airTicks - 1)] * 3 * cos) * glideMultiplier.get(), mc.player.getVelocity().y, (forward * motion[(airTicks - 1)] * 3 * sin) * glideMultiplier.get());
                else mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            } else {
                if (autoDisable.get() && jumped) {
                    jumped = false;
                    toggle();
                    info("Disabling after jump.");
                }

                airTicks = 0;
                groundTicks += 1;
                if (groundTicks <= 2) mc.player.setVelocity(forward * 0.009999999776482582 * cos * glideMultiplier.get(), mc.player.getVelocity().y, forward * 0.009999999776482582 * sin * glideMultiplier.get());
                else mc.player.setVelocity(forward * 0.30000001192092896  * cos * glideMultiplier.get(), 0.42399999499320984, forward * 0.30000001192092896 * sin * glideMultiplier.get());
            }
        }
    }

    private void updateY(double amount) {
        mc.player.setVelocity(mc.player.getVelocity().x, amount, mc.player.getVelocity().z);
    }

    private double getDir() {
        double dir = 0;

        if (Utils.canUpdate()) {
            dir = mc.player.getYaw() + ((mc.player.forwardSpeed < 0) ? 180 : 0);

            if (mc.player.sidewaysSpeed > 0) dir += -90F * ((mc.player.forwardSpeed < 0) ? -0.5F : ((mc.player.forwardSpeed > 0) ? 0.5F : 1F));
            else if (mc.player.sidewaysSpeed < 0) dir += 90F * ((mc.player.forwardSpeed < 0) ? -0.5F : ((mc.player.forwardSpeed > 0) ? 0.5F : 1F));
        }

        return dir;
    }

    private double getMoveSpeed() {
        double base = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) base *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        return base;
    }

    private void setMoveSpeed(PlayerMoveEvent event, double speed) {
        double forward = mc.player.forwardSpeed;
        double strafe = mc.player.sidewaysSpeed;
        float yaw = mc.player.getYaw();

        if (!PlayerUtils.isMoving()) ((IVec3d) event.movement).setXZ(0, 0);
        else {
            if (forward != 0) {
                if (strafe > 0) yaw += ((forward > 0) ? -45 : 45);
                else if (strafe < 0) yaw += ((forward > 0) ? 45 : -45);
            }

            strafe = 0;
            if (forward > 0) forward = 1;
            else if (forward < 0) forward = -1;
        }

        double cos = Math.cos(Math.toRadians(yaw + 90));
        double sin = Math.sin(Math.toRadians(yaw + 90));
        ((IVec3d) event.movement).setXZ((forward * speed * cos) + (strafe * speed * sin), (forward * speed * sin) + (strafe * speed * cos));
    }

    public enum Mode {
        Vanilla("Vanilla"),
        Burst("Burst"),
        Glide("Glide");

        private final String title;

        Mode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
