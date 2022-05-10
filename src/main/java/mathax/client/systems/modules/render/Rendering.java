package mathax.client.systems.modules.render;

import mathax.client.settings.BoolSetting;
import mathax.client.settings.EnumSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.io.IOException;

/*/--------------------------------------------------------------------------------------------------------------/*/
/*/ Used from Meteor Rejects                                                                                     /*/
/*/ https://github.com/AntiCope/meteor-rejects/blob/master/src/main/java/anticope/rejects/modules/Rendering.java /*/
/*/--------------------------------------------------------------------------------------------------------------/*/

public class Rendering extends Module {
    private ShaderEffect shader = null;

    private final SettingGroup sgInvisible = settings.createGroup("Invisible");
    private final SettingGroup sgFun = settings.createGroup("Fun");

    private final Setting<Boolean> structureVoid = sgInvisible.add(new BoolSetting.Builder()
        .name("structure-void")
        .description("Render structure void blocks.")
        .defaultValue(true)
        .onChanged(onChanged -> {
            if (isActive()) mc.worldRenderer.reload();
        })
        .build()
    );

    private final Setting<Shader> shaderEnum = sgFun.add(new EnumSetting.Builder<Shader>()
        .name("shader")
        .description("Determines which shader to use.")
        .defaultValue(Shader.None)
        .onChanged(this::onChanged)
        .build()
    );

    private final Setting<Boolean> dinnerbone = sgFun.add(new BoolSetting.Builder()
        .name("dinnerbone")
        .description("Applies dinnerbone effects to all entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> deadmau5Ears = sgFun.add(new BoolSetting.Builder()
        .name("deadmau5-ears")
        .description("Adds deadmau5 ears to all players.")
        .defaultValue(false)
        .build()
    );

    public Rendering() {
        super(Categories.Render, Items.CREEPER_HEAD, "rendering", "Various render tweaks.");
    }

    @Override
    public void onActivate() {
        mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        mc.worldRenderer.reload();
    }

    public void onChanged(Shader s) {
        String name;
        if (s == Shader.Vibrant) name = "color_convolve";
        else if (s == Shader.Scanline) name = "scan_pincushion";
        else name = s.toString().toLowerCase();

        Identifier shaderID = new Identifier(String.format("shaders/post/%s.json", name));

        try {
            this.shader = new ShaderEffect(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), shaderID);
        } catch (IOException exception) {
            this.shader = null;
        }
    }

    public boolean renderStructureVoid() {
        return this.isActive() && structureVoid.get();
    }

    public ShaderEffect getShaderEffect() {
        if (!isActive()) return null;
        return shader;
    }

    public boolean dinnerboneEnabled() {
        if (!isActive()) return false;
        return dinnerbone.get();
    }

    public boolean deadmau5EarsEnabled() {
        if (!isActive()) return false;
        return deadmau5Ears.get();
    }

    public enum Shader {
        None,
        Notch,
        FXAA,
        Art,
        Bumpy,
        Blobs,
        Blobs2,
        Pencil,
        Vibrant,
        Deconverge,
        Flip,
        Invert,
        NTSC,
        Outline,
        Phosphor,
        Scanline,
        Sobel,
        Bits,
        Desaturate,
        Green,
        Blur,
        Wobble,
        Antialias,
        Creeper,
        Spider
    }
}
