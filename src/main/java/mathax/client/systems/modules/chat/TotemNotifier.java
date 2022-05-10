package mathax.client.systems.modules.chat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.game.GameJoinedEvent;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.friends.Friends;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.misc.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Formatting;

import java.util.Random;
import java.util.UUID;

public class TotemNotifier extends Module {
    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();

    private final Random random = new Random();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-own")
        .description("Notifies you of your own totem pops.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends totem pops.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreOthers = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-others")
        .description("Ignores other players totem pops.")
        .defaultValue(false)
        .build()
    );

    // TODO: Notify modes2

    public TotemNotifier() {
        super(Categories.Chat, Items.TOTEM_OF_UNDYING, "totem-notifier", "Notifies you when a player pops a totem.");
    }

    @Override
    public void onActivate() {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;

        if (packet.getStatus() != 35) return;

        Entity entity = packet.getEntity(mc.world);

        if (!(entity instanceof PlayerEntity player)) return;

        if (shouldSkip(player)) return;

        synchronized (totemPopMap) {
            int pops = totemPopMap.getOrDefault(player.getUuid(), 0);
            totemPopMap.put(player.getUuid(), ++pops);

            ChatUtils.sendMsg(getChatId(entity), Formatting.GRAY, "(highlight)%s(default) popped (highlight)%d(default) %s.", player.getEntityName(), pops, pops == 1 ? "totem" : "totems");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        synchronized (totemPopMap) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPopMap.containsKey(player.getUuid())) continue;
                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPopMap.removeInt(player.getUuid());
                    ChatUtils.sendMsg(getChatId(player), Formatting.GRAY, "(highlight)%s(default) died after popping (highlight)%d(default) %s.", player.getEntityName(), pops, pops == 1 ? "totem" : "totems");
                    chatIdMap.removeInt(player.getUuid());
                }
            }
        }
    }

    private boolean shouldSkip(PlayerEntity player) {
        if (ignoreOwn.get() && player == mc.player) return true;
        else if (ignoreFriends.get() && Friends.get().isFriend(player)) return true;
        else return ignoreOthers.get() && !Friends.get().isFriend(player);
    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIfAbsent(entity.getUuid(), value -> random.nextInt());
    }
}
