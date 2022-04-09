package mathax.client.systems.modules.movement.elytrafly.modes;

import mathax.client.MatHax;
import mathax.client.events.packets.PacketEvent;
import mathax.client.systems.modules.movement.elytrafly.ElytraFlightMode;
import mathax.client.systems.modules.movement.elytrafly.ElytraFlightModes;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Packet extends ElytraFlightMode {
    private final Vec3d vec3d = new Vec3d(0,0,0);

    public Packet() {
        super(ElytraFlightModes.Packet);
    }

    @Override
    public void onDeactivate() {
        MatHax.mc.player.getAbilities().flying = false;
        MatHax.mc.player.getAbilities().allowFlying = false;
    }

    @Override
    public void onTick() {
        if (MatHax.mc.player.getInventory().getArmorStack(2).getItem() != Items.ELYTRA || MatHax.mc.player.fallDistance <= 0.2 || MatHax.mc.options.keySneak.isPressed()) return;

        if (MatHax.mc.options.keyForward.isPressed()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.rotateY(-(float) Math.toRadians(MatHax.mc.player.getYaw()));
        } else if (MatHax.mc.options.keyBack.isPressed()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.rotateY((float) Math.toRadians(MatHax.mc.player.getYaw()));
        }

        if (MatHax.mc.options.keyJump.isPressed()) vec3d.add(0, elytraFly.verticalSpeed.get(), 0);
        else if (!MatHax.mc.options.keyJump.isPressed()) vec3d.add(0, -elytraFly.verticalSpeed.get(), 0);

        MatHax.mc.player.setVelocity(vec3d);
        MatHax.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MatHax.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        MatHax.mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
    }

    @Override
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket) MatHax.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MatHax.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    @Override
    public void onPlayerMove() {
        MatHax.mc.player.getAbilities().flying = true;
        MatHax.mc.player.getAbilities().setFlySpeed(elytraFly.horizontalSpeed.get().floatValue() / 20);
    }
}
