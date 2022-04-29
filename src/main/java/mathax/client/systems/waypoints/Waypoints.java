package mathax.client.systems.waypoints;

import mathax.client.MatHax;
import mathax.client.renderer.text.TextRenderer;
import mathax.client.utils.render.color.Color;
import mathax.client.events.game.GameJoinedEvent;
import mathax.client.events.game.GameLeftEvent;
import mathax.client.events.render.Render2DEvent;
import mathax.client.systems.System;
import mathax.client.systems.Systems;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.render.WaypointsModule;
import mathax.client.utils.Utils;
import mathax.client.utils.files.StreamUtils;
import mathax.client.utils.misc.NbtUtils;
import mathax.client.utils.misc.Vec3;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.render.NametagUtils;
import mathax.client.utils.world.Dimension;
import mathax.client.eventbus.EventHandler;
import mathax.client.eventbus.EventPriority;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.NbtCompound;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static mathax.client.MatHax.mc;

public class Waypoints extends System<Waypoints> implements Iterable<Waypoint> {
    private static final String[] BUILTIN_ICONS = {
        "square",
        "circle",
        "triangle",
        "star",
        "diamond",
        "skull"
    };

    private static final Color TEXT = new Color(255, 255, 255);

    public final Map<String, AbstractTexture> icons = new ConcurrentHashMap<>();

    public Map<String, Waypoint> waypoints = new ConcurrentHashMap<>();

    public Waypoints() {
        super(null);
    }

    public static Waypoints get() {
        return Systems.get(Waypoints.class);
    }

    @Override
    public void init() {
        File iconsFolder = new File(MatHax.FOLDER, "WayPoint-Icons");
        iconsFolder.mkdirs();

        for (String builtinIcon : BUILTIN_ICONS) {
            File iconFile = new File(iconsFolder, builtinIcon + ".png");
            if (!iconFile.exists()) copyIcon(iconFile);
        }

        File[] files = iconsFolder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.getName().endsWith(".png")) {
                try {
                    String name = file.getName().replace(".png", "");
                    AbstractTexture texture = new NativeImageBackedTexture(NativeImage.read(new FileInputStream(file)));
                    icons.put(name, texture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void add(Waypoint waypoint) {
        waypoints.put(waypoint.name.toLowerCase(Locale.ROOT), waypoint);
        save();
    }

    public void remove(Waypoint waypoint) {
        Waypoint removed = waypoints.remove(waypoint.name.toLowerCase(Locale.ROOT));
        if (removed != null) save();
    }

    public Waypoint get(String name) {
        return waypoints.get(name.toLowerCase(Locale.ROOT));
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        load();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onGameDisconnected(GameLeftEvent event) {
        waypoints.clear();
    }

    private boolean checkDimension(Waypoint waypoint) {
        Dimension dimension = PlayerUtils.getDimension();

        if (waypoint.overworld && dimension == Dimension.Overworld) return true;
        if (waypoint.nether && dimension == Dimension.Nether) return true;
        return waypoint.end && dimension == Dimension.End;
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        WaypointsModule module = Modules.get().get(WaypointsModule.class);
        if (!module.isActive()) return;

        TextRenderer text = TextRenderer.get();
        Vec3 center = new Vec3(mc.getWindow().getFramebufferWidth() / 2.0, mc.getWindow().getFramebufferHeight() / 2.0, 0);
        int textRenderDist = module.textRenderDistance.get();

        for (Waypoint waypoint : this) {
            // Continue if this waypoint should not be rendered
            if (!waypoint.visible || !checkDimension(waypoint)) continue;

            // Calculate distance
            Vec3 pos = waypoint.getCoords().add(0.5, 0, 0.5);
            double dist = PlayerUtils.distanceToCamera(pos.x, pos.y, pos.z);

            // Continue if this waypoint should not be rendered
            if (dist > waypoint.maxVisibleDistance) continue;
            if (!NametagUtils.to2D(pos, 1)) continue;

            // Calculate alpha and distance to center of the screen
            double distToCenter = pos.distanceTo(center);
            double a = 1;

            if (dist < 20) {
                a = (dist - 10) / 10;
                if (a < 0.01) continue;
            }

            // Render
            NametagUtils.scale = waypoint.scale - 0.25;
            NametagUtils.begin(pos);

            // Render icon
            waypoint.renderIcon(-16, -16, a, 32);

            // Render text if cursor is close enough
            if (distToCenter <= textRenderDist) {
                // Setup text rendering
                int preTextA = TEXT.a;
                TEXT.a *= a;
                text.begin();

                // Render name
                text.render(waypoint.name, -text.getWidth(waypoint.name) / 2, -16 - text.getHeight(), TEXT, true);

                // Render distance
                String distText = String.format("%d blocks", (int) Math.round(dist));
                text.render(distText, -text.getWidth(distText) / 2, 16, TEXT, true);

                // End text rendering
                text.end();
                TEXT.a = preTextA;
            }

            NametagUtils.end();
        }
    }

    @Override
    public File getFile() {
        if (!Utils.canUpdate()) return null;
        return new File(new File(MatHax.VERSION_FOLDER, "WayPoints"), Utils.getWorldName() + ".nbt");
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("waypoints", NbtUtils.listToTag(waypoints.values()));
        return tag;
    }

    @Override
    public Waypoints fromTag(NbtCompound tag) {
        Map<String, Waypoint> fromNbt = NbtUtils.listFromTag(tag.getList("waypoints", 10), tag1 -> new Waypoint().fromTag((NbtCompound) tag1)).stream().collect(Collectors.toMap(o -> o.name.toLowerCase(Locale.ROOT), o -> o));
        this.waypoints = new ConcurrentHashMap<>(fromNbt);

        return this;
    }

    @Override
    public Iterator<Waypoint> iterator() {
        return waypoints.values().iterator();
    }

    public ListIterator<Waypoint> iteratorReverse() {
        return new ArrayList<>(waypoints.values()).listIterator(waypoints.size());
    }

    private void copyIcon(File file) {
        StreamUtils.copy(Waypoints.class.getResourceAsStream("/assets/mathax/textures/icons/waypoints/" + file.getName()), file);
    }
}
