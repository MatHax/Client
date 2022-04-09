package mathax.client.systems.modules.movement;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.mixin.EntityVelocityUpdateS2CPacketAccessor;
import mathax.client.mixininterface.IVec3d;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.DoubleSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class Velocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<Boolean> knockback = sgGeneral.add(new BoolSetting.Builder()
        .name("knockback")
        .description("Modifies the amount of knockback you take from attacks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> knockbackHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-horizontal")
        .description("How much horizontal knockback you will take.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Double> knockbackVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-vertical")
        .description("How much vertical knockback you will take.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
        .name("explosions")
        .description("Modifies your knockback from explosions.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> explosionsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-horizontal")
        .description("How much velocity you will take from explosions horizontally.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Double> explosionsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-vertical")
        .description("How much velocity you will take from explosions vertically.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
        .name("liquids")
        .description("Modifies the amount you are pushed by flowing liquids.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> liquidsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-horizontal")
        .description("How much velocity you will take from liquids horizontally.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Double> liquidsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-vertical")
        .description("How much velocity you will take from liquids vertically.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Boolean> entityPush = sgGeneral.add(new BoolSetting.Builder()
        .name("entity-push")
        .description("Modifies the amount you are pushed by entities.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> entityPushAmount = sgGeneral.add(new DoubleSetting.Builder()
        .name("entity-push-amount")
        .description("How much you will be pushed.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .visible(entityPush::get)
        .build()
    );

    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Prevents you from being pushed out of blocks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> sinking = sgGeneral.add(new BoolSetting.Builder()
        .name("sinking")
        .description("Prevents you from sinking in liquids.")
        .defaultValue(false)
        .build()
    );

    public Velocity() {
        super(Categories.Movement, Items.COMMAND_BLOCK, "velocity", "Prevents you from being moved by external forces.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!sinking.get()) return;
        if (mc.options.keyJump.isPressed() || mc.options.keySneak.isPressed()) return;

        if ((mc.player.isTouchingWater() || mc.player.isInLava()) && mc.player.getVelocity().y < 0) ((IVec3d) mc.player.getVelocity()).setY(0);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (knockback.get() && event.packet instanceof EntityVelocityUpdateS2CPacket packet && ((EntityVelocityUpdateS2CPacket) event.packet).getId() == mc.player.getId()) {
            double velX = (packet.getVelocityX() / 8000d - mc.player.getVelocity().x) * knockbackHorizontal.get();
            double velY = (packet.getVelocityY() / 8000d - mc.player.getVelocity().y) * knockbackVertical.get();
            double velZ = (packet.getVelocityZ() / 8000d - mc.player.getVelocity().z) * knockbackHorizontal.get();
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setX((int) (velX * 8000 + mc.player.getVelocity().x * 8000));
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setY((int) (velY * 8000 + mc.player.getVelocity().y * 8000));
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) (velZ * 8000 + mc.player.getVelocity().z * 8000));
        }
    }

    public double getHorizontal(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

    public double getVertical(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }
}
