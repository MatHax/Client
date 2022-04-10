package mathax.client.systems.modules.render;

import com.google.common.reflect.TypeToken;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render2DEvent;
import mathax.client.mixin.ProjectileEntityAccessor;
import mathax.client.renderer.Renderer2D;
import mathax.client.renderer.text.TextRenderer;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.DoubleSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.misc.Vec3;
import mathax.client.utils.network.HTTP;
import mathax.client.utils.network.MatHaxExecutor;
import mathax.client.utils.render.NametagUtils;
import mathax.client.utils.render.color.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityOwner extends Module {
    private final Vec3 pos = new Vec3();
    private final Map<UUID, String> uuidToName = new HashMap<>();

    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);
    private static final Type RESPONSE_TYPE = new TypeToken<List<UuidNameHistoryResponseItem>>() {}.getType();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the text.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> projectiles = sgGeneral.add(new BoolSetting.Builder()
        .name("projectiles")
        .description("Display owner names of projectiles.")
        .defaultValue(false)
        .build()
    );

    public EntityOwner() {
        super(Categories.Render, Items.NAME_TAG, "entity-owner", "Displays the name of the player who owns the entity near you.");
    }

    @Override
    public void onDeactivate() {
        uuidToName.clear();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entity entity : mc.world.getEntities()) {
            UUID ownerUuid;

            if (entity instanceof TameableEntity) ownerUuid = ((TameableEntity) entity).getOwnerUuid();
            else if (entity instanceof HorseBaseEntity) ownerUuid = ((HorseBaseEntity) entity).getOwnerUuid();
            else if (entity instanceof ProjectileEntity && projectiles.get()) ownerUuid = ((ProjectileEntityAccessor) entity).getOwnerUuid();
            else continue;

            if (ownerUuid != null) {
                pos.set(entity, event.tickDelta);
                pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.75, 0);

                if (NametagUtils.to2D(pos, scale.get())) renderNametag(getOwnerName(ownerUuid));
            }
        }
    }

    private void renderNametag(String name) {
        TextRenderer text = TextRenderer.get();

        NametagUtils.begin(pos);
        text.beginBig();

        double w = text.getWidth(name);

        double x = -w / 2;
        double y = -text.getHeight();

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, w + 2, text.getHeight() + 2, BACKGROUND);
        Renderer2D.COLOR.render(null);

        text.render(name, x, y, TEXT);

        text.end();
        NametagUtils.end();
    }

    private String getOwnerName(UUID uuid) {
        // Check if the player is online
        PlayerEntity player = mc.world.getPlayerByUuid(uuid);
        if (player != null) return player.getEntityName();

        // Check cache
        String name = uuidToName.get(uuid);
        if (name != null) return name;

        // Makes a HTTP request to Mojang API
        MatHaxExecutor.execute(() -> {
            if (isActive()) {
                List<UuidNameHistoryResponseItem> res = HTTP.get("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names").sendJson(RESPONSE_TYPE);

                if (isActive()) {
                    if (res == null || res.size() <= 0) uuidToName.put(uuid, "Failed to get name");
                    else uuidToName.put(uuid, res.get(res.size() - 1).name);
                }
            }
        });

        name = "Retrieving";
        uuidToName.put(uuid, name);
        return name;
    }

    public static class UuidNameHistoryResponseItem {
        public String name;
    }
}
