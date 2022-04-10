package mathax.client.systems.waypoints;

import mathax.client.MatHax;
import mathax.client.utils.misc.Vec3;
import mathax.client.utils.player.PlayerUtils;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.renderer.GL;
import mathax.client.renderer.Renderer2D;
import mathax.client.utils.misc.ISerializable;
import mathax.client.utils.world.Dimension;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public class Waypoint implements ISerializable<Waypoint> {
    public SettingColor color = new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b);

    public String name = "Location";
    public String icon = "Square";

    public int x, y, z;
    public int maxVisibleDistance = 1000;

    public boolean visible = true;

    public double scale = 1;
    public double minScale = 0.75;

    public boolean overworld, nether, end;

    public Dimension actualDimension;

    public void validateIcon() {
        Map<String, AbstractTexture> icons = Waypoints.get().icons;

        AbstractTexture texture = icons.get(icon);
        if (texture == null && !icons.isEmpty()) icon = icons.keySet().iterator().next();
    }

    public void renderIcon(double x, double y, double a, double size) {
        validateIcon();

        AbstractTexture texture = Waypoints.get().icons.get(icon);
        if (texture == null) return;

        int preA = color.a;
        color.a *= a;

        GL.bindTexture(texture.getGlId());
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, size, size, color);
        Renderer2D.TEXTURE.render(null);

        color.a = preA;
    }

    private int findIconIndex() {
        int i = 0;
        for (String icon : Waypoints.get().icons.keySet()) {
            if (this.icon.equals(icon)) return i;
            i++;
        }

        return -1;
    }

    private int correctIconIndex(int i) {
        if (i < 0) return Waypoints.get().icons.size() + i;
        else if (i >= Waypoints.get().icons.size()) return i - Waypoints.get().icons.size();
        return i;
    }

    private String getIcon(int i) {
        i = correctIconIndex(i);

        int _i = 0;
        for (String icon : Waypoints.get().icons.keySet()) {
            if (_i == i) return icon;
            _i++;
        }

        return "Square";
    }

    public void prevIcon() {
        icon = getIcon(findIconIndex() - 1);
    }

    public void nextIcon() {
        icon = getIcon(findIconIndex() + 1);
    }

    public Vec3 getCoords() {
        double x = this.x;
        double y = this.y;
        double z = this.z;

        if (actualDimension == Dimension.Overworld && PlayerUtils.getDimension() == Dimension.Nether) {
            x = x / 8f;
            z = z / 8f;
        } else if (actualDimension == Dimension.Nether && PlayerUtils.getDimension() == Dimension.Overworld) {
            x = x * 8;
            z = z * 8;
        }

        return new Vec3(x, y, z);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.putString("icon", icon);

        tag.put("color", color.toTag());

        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        tag.putInt("maxVisibleDistance", maxVisibleDistance);

        tag.putDouble("scale", scale);
        tag.putDouble("minScale", minScale);

        tag.putString("dimension", actualDimension.name());

        tag.putBoolean("visible", visible);
        tag.putBoolean("overworld", overworld);
        tag.putBoolean("nether", nether);
        tag.putBoolean("end", end);

        return tag;
    }

    @Override
    public Waypoint fromTag(NbtCompound tag) {
        name = tag.getString("name");
        icon = tag.getString("icon");

        color.fromTag(tag.getCompound("color"));

        x = tag.getInt("x");
        y = tag.getInt("y");
        z = tag.getInt("z");
        maxVisibleDistance = tag.getInt("maxVisibleDistance");

        scale = tag.getDouble("scale");
        minScale = tag.getDouble("minScale");

        actualDimension = Dimension.valueOf(tag.getString("dimension"));

        visible = tag.getBoolean("visible");
        overworld = tag.getBoolean("overworld");
        nether = tag.getBoolean("nether");
        end = tag.getBoolean("end");

        if (!Waypoints.get().icons.containsKey(icon)) icon = "Square";

        return this;
    }
}
