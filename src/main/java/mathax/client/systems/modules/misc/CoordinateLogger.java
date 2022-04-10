package mathax.client.systems.modules.misc;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.packets.PacketEvent;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.DoubleSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class CoordinateLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTeleports = settings.createGroup("Teleports");
    private final SettingGroup sgWorldEvents = settings.createGroup("World Events");
    private final SettingGroup sgSounds = settings.createGroup("Sounds");

    // General

    private final Setting<Double> minDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("minimum-distance")
        .description("Minimum distance to log event.")
        .defaultValue(10)
        .range(5, 100)
        .sliderRange(5, 100)
        .build()
    );

    // Teleports

    private final Setting<Boolean> players = sgTeleports.add(new BoolSetting.Builder()
        .name("players")
        .description("Logs player teleports.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> wolves = sgTeleports.add(new BoolSetting.Builder()
        .name("wolves")
        .description("Logs wolf teleports.")
        .defaultValue(false)
        .build()
    );

    // World Events

    private final Setting<Boolean> enderDragons = sgWorldEvents.add(new BoolSetting.Builder()
        .name("ender-dragons")
        .description("Logs killed ender dragons.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> endPortals = sgWorldEvents.add(new BoolSetting.Builder()
        .name("end-portals")
        .description("Logs opened end portals.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> withers = sgWorldEvents.add(new BoolSetting.Builder()
        .name("withers")
        .description("Logs wither spawns.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> otherEvents = sgWorldEvents.add(new BoolSetting.Builder()
        .name("other-global-events")
        .description("Logs other global events.")
        .defaultValue(false)
        .build()
    );

    // Sounds

    private final Setting<Boolean> thunder = sgSounds.add(new BoolSetting.Builder()
        .name("thunder")
        .description("Logs thunder sounds.")
        .defaultValue(false)
        .build()
    );

    public CoordinateLogger() {
        super(Categories.Misc, Items.COMPASS, "coordinate-logger", "Logs coordinates of various events. Might not work on Spigot/Paper servers.");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        // Teleports
        if (event.packet instanceof EntityPositionS2CPacket) {
            EntityPositionS2CPacket packet = (EntityPositionS2CPacket) event.packet;

            try {
                Entity entity = mc.world.getEntityById(packet.getId());

                // Player teleport
                if (entity.getType().equals(EntityType.PLAYER) && players.get()) {
                    Vec3d packetPosition = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
                    Vec3d playerPosition = entity.getPos();

                    if (playerPosition.distanceTo(packetPosition) >= minDistance.get()) info("Player '(highlight)%s(default)' has teleported to (highlight)%s(default).", entity.getEntityName(), String.format("%.1f %.1f %.1f", packetPosition.x, packetPosition.y, packetPosition.z));
                }

                // World teleport
                else if (entity.getType().equals(EntityType.WOLF) && wolves.get()) {
                    Vec3d packetPosition = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
                    Vec3d wolfPosition = entity.getPos();

                    UUID ownerUUID = ((TameableEntity) entity).getOwnerUuid();

                    if (ownerUUID != null && wolfPosition.distanceTo(packetPosition) >= minDistance.get()) info("Wolf has teleported to (highlight)%s(default).", String.format("%.1f %.1f %.1f", packetPosition.x, packetPosition.y, packetPosition.z));
                }
            } catch(NullPointerException ignored) {}

            // World events
        } else if (event.packet instanceof WorldEventS2CPacket) {
            WorldEventS2CPacket packet = (WorldEventS2CPacket) event.packet;
            Vec3d packetPosition = new Vec3d(packet.getPos().getX(), packet.getPos().getY(), packet.getPos().getZ());
            String position = String.format("%.1f %.1f %.1f", packetPosition.x, packetPosition.y, packetPosition.z);

            if (packet.isGlobal()) {
                // Min distance
                if (PlayerUtils.distanceTo(packet.getPos()) <= minDistance.get()) return;

                switch (packet.getEventId()) {
                    case 1023:
                        if (withers.get()) info("Wither spawned at (highlight)%s(default).", position);
                        break;
                    case 1038:
                        if (endPortals.get()) info("End portal opened at (highlight)%s(default).", position);
                        break;
                    case 1028:
                        if (enderDragons.get()) info("Ender dragon killed at (highlight)%s(default).", position);
                        break;
                    default:
                        if (otherEvents.get()) info("Unknown global event at (highlight)%s(default).", position);
                }
            }

            // Sounds
        } else if (thunder.get() && event.packet instanceof PlaySoundS2CPacket soundPacket) {
            // Check for thunder sound
            if (soundPacket.getSound() != SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT) return;

            // Min distance
            if (PlayerUtils.distanceTo(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ()) <= minDistance.get()) return;
            Vec3d soundPacketPosition = new Vec3d(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ());

            info("Thunder noise at (highlight)%s(default).", String.format("%.1f %.1f %.1f", soundPacketPosition.x, soundPacketPosition.y, soundPacketPosition.z));
        }
    }
}
