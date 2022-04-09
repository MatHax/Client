package mathax.client.systems.waypoints;

import mathax.client.MatHax;
import mathax.client.renderer.text.TextRenderer;
import mathax.client.utils.render.color.Color;
import mathax.client.events.game.GameJoinedEvent;
import mathax.client.events.game.GameLeftEvent;
import mathax.client.events.render.Render2DEvent;
import mathax.client.renderer.Renderer2D;
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
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

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

    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    public final Map<String, AbstractTexture> icons = new ConcurrentHashMap<>();

    public Map<String, Waypoint> waypoints = new ConcurrentHashMap<>();

    private final Vec3 pos = new Vec3();

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
        Waypoint removed = waypoints.remove(waypoint.name);
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

    public Vec3d getCoords(Waypoint waypoint) {
        double x = waypoint.x;
        double y = waypoint.y;
        double z = waypoint.z;

        if (waypoint.actualDimension == Dimension.Overworld && PlayerUtils.getDimension() == Dimension.Nether) {
            x = waypoint.x / 8f;
            z = waypoint.z / 8f;
        } else if (waypoint.actualDimension == Dimension.Nether && PlayerUtils.getDimension() == Dimension.Overworld) {
            x = waypoint.x * 8;
            z = waypoint.z * 8;
        }

        return new Vec3d(x, y, z);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!Modules.get().isActive(WaypointsModule.class)) return;

        TextRenderer text = TextRenderer.get();

        for (Waypoint waypoint : this) {
            if (!waypoint.visible || !checkDimension(waypoint)) continue;

            Camera camera = mc.gameRenderer.getCamera();

            double x = getCoords(waypoint).x;
            double y = getCoords(waypoint).y;
            double z = getCoords(waypoint).z;

            double dist = PlayerUtils.distanceToCamera(x, y, z);
            if (dist > waypoint.maxVisibleDistance) continue;
            double scale = waypoint.scale;

            double a = 1;
            if (dist < 10) {
                a = dist / 10;
                if (a < 0.1) continue;
            }

            double maxViewDist = mc.options.viewDistance * 16;
            if (dist > maxViewDist) {
                double dx = x - camera.getPos().x;
                double dy = y - camera.getPos().y;
                double dz = z - camera.getPos().z;

                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= length;
                dy /= length;
                dz /= length;

                dx *= maxViewDist;
                dy *= maxViewDist;
                dz *= maxViewDist;

                x = camera.getPos().x + dx;
                y = camera.getPos().y + dy;
                z = camera.getPos().z + dz;

                scale /= dist / 15;
                scale *= maxViewDist / 15;
            }

            scale = Utils.clamp(scale, waypoint.minScale, Integer.MAX_VALUE);

            pos.set(x, y, z);
            if (!NametagUtils.to2D(pos, scale)) continue;

            NametagUtils.begin(pos);

            int preBgA = BACKGROUND.a;
            int preTextA = TEXT.a;
            BACKGROUND.a *= a;
            TEXT.a *= a;

            String distText = Math.round(dist) + " blocks";

            text.beginBig();
            double w = text.getWidth(waypoint.name) / 2.0;
            double w2 = text.getWidth(distText) / 2.0;
            double h = text.getHeight();

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-w, -h, w * 2, h * 2, BACKGROUND);
            Renderer2D.COLOR.render(null);

            waypoint.renderIcon(-16, h + 1, a, 32);

            text.render(waypoint.name, -w, -h + 1, TEXT);
            text.render(distText, -w2, 0, TEXT);

            text.end();
            NametagUtils.end();

            BACKGROUND.a = preBgA;
            TEXT.a = preTextA;
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
