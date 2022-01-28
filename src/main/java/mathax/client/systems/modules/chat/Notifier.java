/*
 * Decompiled with CFR 0.151.
 */
package minegame159.meteorclient.systems.modules.bedtrap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.class_124;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2554;
import net.minecraft.class_2561;
import net.minecraft.class_2585;
import net.minecraft.class_2620;
import net.minecraft.class_2673;
import net.minecraft.class_2703;
import net.minecraft.class_2777;
import net.minecraft.class_310;
import org.apache.commons.io.IOUtils;

public class Notifier
extends Module {
    private class_2338 prevBreakPos;
    private final Setting<Integer> minDistance;
    private final Setting<Boolean> portal;
    private boolean bLeggings;
    static final boolean $assertionsDisabled = !Notifier.class.desiredAssertionStatus();
    private final Setting<Boolean> wither;
    private final Setting<Boolean> antiVanish;
    private final Setting<Boolean> dragon;
    private final SettingGroup sExploit;
    private final List<class_1657> burrowedPlayers;
    private static final class_310 mc = class_310.method_1551();
    private final Setting<Integer> msgNum;
    private final SettingGroup sVanish;
    private final SettingGroup sSurround;
    Boolean obb;
    private long lastTick;
    private final Setting<Boolean> tpExploit;
    private final Setting<Boolean> armorDurability;
    private final Setting<Boolean> coordExploit;
    private boolean bBoots;
    private final Setting<Boolean> surroundBreak;
    private final Setting<Integer> damage;
    private final SettingGroup sGroup;
    private int num;
    private boolean bHead;
    private boolean bArmor;
    private final Queue<UUID> toLookup;

    static class_310 access$000() {
        return mc;
    }

    @EventHandler
    public void coordExploit(PacketEvent.Receive receive) {
        if (Notifier.mc.field_1724 != null) {
            class_2673 class_26732;
            PacketEvent.Receive receive2 = receive;
            if (receive2.packet instanceof class_2673 && (class_26732 = (class_2673)receive2.packet).method_11533()) {
                System.out.println(class_26732.method_11532());
                switch (class_26732.method_11532()) {
                    case 1023: {
                        if (!this.wither.get().booleanValue()) break;
                        ChatUtils.info(String.valueOf(new StringBuilder().append("Wither spawned at: ").append(class_26732.method_11531().method_23854())), new Object[0]);
                        break;
                    }
                    case 1038: {
                        if (!this.portal.get().booleanValue()) break;
                        ChatUtils.info(String.valueOf(new StringBuilder().append("End Portal Activated at: ").append(class_26732.method_11531().method_23854())), new Object[0]);
                        break;
                    }
                    case 1028: {
                        if (!this.dragon.get().booleanValue()) break;
                        ChatUtils.info(String.valueOf(new StringBuilder().append("Ender Dragon killed at: ").append(class_26732.method_11531().method_23854())), new Object[0]);
                        break;
                    }
                    default: {
                        ChatUtils.info(String.valueOf(new StringBuilder().append("Unknown global event at: ").append(class_26732.method_11531().method_23854())), new Object[0]);
                    }
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (this.antiVanish.get().booleanValue()) {
            this.toLookup.clear();
        }
    }

    public Notifier() {
        super(Categories.BedTrap, "notifier+", "Notifies you on triggered event.");
        this.sGroup = this.settings.createGroup("Armor Break");
        this.sVanish = this.settings.createGroup("Anti Vanish");
        this.sSurround = this.settings.createGroup("Surround Break");
        this.sExploit = this.settings.createGroup("Tp Exploit");
        this.armorDurability = this.sGroup.add(new BoolSetting.Builder().name("armor-durability").description("yes").defaultValue(true).build());
        this.damage = this.sGroup.add(new IntSetting.Builder().name("damage").description("What damage should an armor item have to be notified.").defaultValue(100).visible(this.armorDurability::get).build());
        this.msgNum = this.sGroup.add(new IntSetting.Builder().name("messages").description("What damage should an armor item have to be notified.").defaultValue(5).visible(this.armorDurability::get).build());
        this.antiVanish = this.sVanish.add(new BoolSetting.Builder().name("anti-vanish").description("Place obsidian on TNT position.").defaultValue(true).build());
        this.surroundBreak = this.sSurround.add(new BoolSetting.Builder().name("surround-break").description("Place obsidian on TNT position.").defaultValue(true).build());
        this.coordExploit = this.sExploit.add(new BoolSetting.Builder().name("coord-exploit").description("Place obsidian on TNT position.").defaultValue(true).build());
        this.wither = this.sExploit.add(new BoolSetting.Builder().name("wither-boss").description("Place obsidian on TNT position.").defaultValue(true).visible(this.coordExploit::get).build());
        this.portal = this.sExploit.add(new BoolSetting.Builder().name("end-portal").description("Place obsidian on TNT position.").defaultValue(true).visible(this.coordExploit::get).build());
        this.dragon = this.sExploit.add(new BoolSetting.Builder().name("end-dragon").description("Place obsidian on TNT position.").defaultValue(true).visible(this.coordExploit::get).build());
        this.tpExploit = this.sExploit.add(new BoolSetting.Builder().name("tp-exploit").description("Place obsidian on TNT position.").defaultValue(true).visible(this.coordExploit::get).build());
        this.minDistance = this.sExploit.add(new IntSetting.Builder().name("min-distance").description("Minimal distance for tp exploit.").defaultValue(6).min(0).max(10000).sliderMax(10000).visible(this.tpExploit::get).build());
        this.num = 0;
        this.bBoots = false;
        this.bLeggings = false;
        this.bArmor = false;
        this.bHead = false;
        this.toLookup = new ConcurrentLinkedQueue<UUID>();
        this.burrowedPlayers = new ArrayList<class_1657>();
        this.lastTick = 0L;
    }

    @Override
    public void onActivate() {
        this.num = 1;
        this.bBoots = false;
        this.bLeggings = false;
        this.bArmor = false;
        this.bHead = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post post) {
        if (!$assertionsDisabled && Notifier.mc.field_1724 == null) {
            throw new AssertionError();
        }
        if (!$assertionsDisabled && Notifier.mc.field_1687 == null) {
            throw new AssertionError();
        }
        class_1799 class_17992 = Notifier.mc.field_1724.field_7514.method_7372(0);
        int n = class_17992.method_7936() - class_17992.method_7919();
        class_1799 class_17993 = Notifier.mc.field_1724.field_7514.method_7372(1);
        int n2 = class_17993.method_7936() - class_17993.method_7919();
        class_1799 class_17994 = Notifier.mc.field_1724.field_7514.method_7372(2);
        int n3 = class_17994.method_7936() - class_17994.method_7919();
        class_1799 class_17995 = Notifier.mc.field_1724.field_7514.method_7372(3);
        int n4 = class_17995.method_7936() - class_17995.method_7919();
        if (n < this.damage.get() && !this.bBoots) {
            if (Notifier.mc.field_1724.field_7514.method_7372(0).method_7960()) {
                ChatUtils.warning("Your Boots slot is empty!", new Object[0]);
                this.bBoots = true;
                this.num = 0;
                return;
            }
            ChatUtils.warning("Your Boots is about to break.", new Object[0]);
            if (this.num < this.msgNum.get()) {
                ++this.num;
                return;
            }
            this.bBoots = true;
            this.num = 1;
        } else if (n > this.damage.get() && this.bBoots) {
            this.bBoots = false;
            this.num = 1;
        }
        if (n2 < this.damage.get() && !this.bLeggings) {
            if (Notifier.mc.field_1724.field_7514.method_7372(1).method_7960()) {
                ChatUtils.warning("Your Leggings slot is empty!", new Object[0]);
                this.bLeggings = true;
                this.num = 0;
                return;
            }
            ChatUtils.warning("Your Leggings is about to break.", new Object[0]);
            if (this.num < this.msgNum.get()) {
                ++this.num;
                return;
            }
            this.bLeggings = true;
            this.num = 1;
        } else if (n2 > this.damage.get() && this.bLeggings) {
            this.bLeggings = false;
            this.num = 1;
        }
        if (n3 < this.damage.get() && !this.bArmor) {
            if (Notifier.mc.field_1724.field_7514.method_7372(2).method_7960()) {
                ChatUtils.warning("Your Chestplate slot is empty!", new Object[0]);
                this.bArmor = true;
                this.num = 0;
                return;
            }
            ChatUtils.warning("Your Chestplate is about to break.", new Object[0]);
            if (this.num < this.msgNum.get()) {
                ++this.num;
                return;
            }
            this.bArmor = true;
            this.num = 1;
        } else if (n3 > this.damage.get() && this.bArmor) {
            this.bArmor = false;
            this.num = 1;
        }
        if (n4 < this.damage.get() && !this.bHead) {
            if (Notifier.mc.field_1724.field_7514.method_7372(3).method_7960()) {
                ChatUtils.warning("Your Helmet slot is empty!", new Object[0]);
                this.bHead = true;
                this.num = 0;
                return;
            }
            ChatUtils.warning("Your Helmet is about to break.", new Object[0]);
            if (this.num < this.msgNum.get()) {
                ++this.num;
                return;
            }
            this.bHead = true;
            this.num = 1;
        } else if (n4 > this.damage.get() && this.bHead) {
            this.bHead = false;
            this.num = 1;
        }
    }

    @EventHandler
    public void onBreakPacket(PacketEvent.Receive receive) {
        if (this.surroundBreak.get().booleanValue()) {
            if (!$assertionsDisabled && Notifier.mc.field_1687 == null) {
                throw new AssertionError();
            }
            if (!$assertionsDisabled && Notifier.mc.field_1724 == null) {
                throw new AssertionError();
            }
            if (receive.packet instanceof class_2620) {
                class_2620 class_26202 = (class_2620)receive.packet;
                class_2338 class_23382 = class_26202.method_11277();
                if (class_23382.equals((Object)this.prevBreakPos) && class_26202.method_11278() > 0) {
                    return;
                }
                class_1657 class_16572 = (class_1657)Notifier.mc.field_1687.method_8469(class_26202.method_11280());
                class_2338 class_23383 = Notifier.mc.field_1724.method_24515();
                this.obb = Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_10540;
                if (class_16572.equals((Object)Notifier.mc.field_1724)) {
                    return;
                }
                if (this.obb.booleanValue() && class_23382.equals((Object)class_23383.method_10095())) {
                    this.notifySurroundBreak(class_2350.field_11043, class_16572);
                } else if (this.obb.booleanValue() && class_23382.equals((Object)class_23383.method_10078())) {
                    this.notifySurroundBreak(class_2350.field_11034, class_16572);
                } else if (this.obb.booleanValue() && class_23382.equals((Object)class_23383.method_10072())) {
                    this.notifySurroundBreak(class_2350.field_11035, class_16572);
                } else if (this.obb.booleanValue() && class_23382.equals((Object)class_23383.method_10067())) {
                    this.notifySurroundBreak(class_2350.field_11039, class_16572);
                }
                this.prevBreakPos = class_23382;
            }
        }
    }

    public static String getPlayerNameFromUUID(UUID uUID) {
        try {
            NameLookup nameLookup = new NameLookup(uUID);
            Thread thread = new Thread(nameLookup);
            thread.start();
            thread.join();
            return nameLookup.getName();
        }
        catch (Exception exception) {
            return null;
        }
    }

    public class_2554 formatMessage(String string, class_2338 class_23382) {
        return this.formatMessage(string, new class_243((double)class_23382.method_10263(), (double)class_23382.method_10264(), (double)class_23382.method_10260()));
    }

    private void notifySurroundBreak(class_2350 class_23502, class_1657 class_16572) {
        if (this.surroundBreak.get().booleanValue()) {
            switch (1.$SwitchMap$net$minecraft$util$math$Direction[class_23502.ordinal()]) {
                case 1: {
                    ChatUtils.warning(String.valueOf(new StringBuilder().append("Your surround is being broken by ").append(class_16572.method_5820())), new Object[0]);
                    break;
                }
                case 2: {
                    ChatUtils.warning(String.valueOf(new StringBuilder().append("Your surround is being broken by ").append(class_16572.method_5820())), new Object[0]);
                    break;
                }
                case 3: {
                    ChatUtils.warning(String.valueOf(new StringBuilder().append("Your surround is being broken by ").append(class_16572.method_5820())), new Object[0]);
                    break;
                }
                case 4: {
                    ChatUtils.warning(String.valueOf(new StringBuilder().append("Your surround is being broken by ").append(class_16572.method_5820())), new Object[0]);
                }
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive receive) {
        if (receive.packet instanceof class_2777) {
            class_2777 class_27772 = (class_2777)receive.packet;
            try {
                class_1297 class_12972 = Notifier.mc.field_1687.method_8469(class_27772.method_11916());
                if (class_12972.method_5864().equals(class_1299.field_6097) && this.tpExploit.get().booleanValue()) {
                    class_243 class_2432 = new class_243(class_27772.method_11917(), class_27772.method_11919(), class_27772.method_11918());
                    class_243 class_2433 = class_12972.method_19538();
                    if (class_2433.method_1022(class_2432) >= (double)this.minDistance.get().intValue()) {
                        this.info((class_2561)this.formatMessage(String.valueOf(new StringBuilder().append("Player '").append(class_12972.method_5820()).append("' has teleported to ")), class_2432));
                        return;
                    }
                }
            }
            catch (NullPointerException nullPointerException) {
                // empty catch block
            }
        }
    }

    public class_2554 formatMessage(String string, class_243 class_2432) {
        class_2585 class_25852 = new class_2585(string);
        class_25852.method_10852((class_2561)ChatUtils.formatCoords(class_2432));
        class_25852.method_27693(String.valueOf(new StringBuilder().append(class_124.field_1080.toString()).append(".")));
        return class_25852;
    }

    @EventHandler
    public void onLeave(GameLeftEvent gameLeftEvent) {
        if (this.antiVanish.get().booleanValue()) {
            this.toLookup.clear();
        }
    }

    private boolean isInBurrow(class_1657 class_16572) {
        class_2338 class_23382 = new class_2338(class_16572.method_23317(), class_16572.method_23318(), class_16572.method_23321());
        return Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_10540 || Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_10443 || Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_22423 || Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_22108 || Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_22109 || Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_23152 || Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_10485 || Notifier.mc.field_1687.method_8320(class_23382).method_26204() == class_2246.field_10535;
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive receive) {
        class_2703 class_27032;
        if (this.antiVanish.get().booleanValue() && receive.packet instanceof class_2703 && (class_27032 = (class_2703)receive.packet).method_11723() == class_2703.class_2704.field_12371) {
            try {
                for (class_2703.class_2705 class_27052 : class_27032.method_11722()) {
                    if (mc.method_1562().method_2871(class_27052.method_11726().getId()) != null) continue;
                    this.toLookup.add(class_27052.method_11726().getId());
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @EventHandler
    public void antiVanesh(TickEvent.Post post) {
        UUID uUID;
        long l;
        if (this.antiVanish.get().booleanValue() && Math.abs(this.lastTick - (l = Notifier.mc.field_1687.method_8510())) > 100L && (uUID = this.toLookup.poll()) != null) {
            try {
                String string = Notifier.getPlayerNameFromUUID(uUID);
                if (string != null) {
                    ChatUtils.warning(String.valueOf(new StringBuilder().append(string).append(" has gone into vanish.")), new Object[0]);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.lastTick = l;
        }
    }

    public static class NameLookup
    implements Runnable {
        private final String uuidstr;
        private final UUID uuid;
        private String name;

        public NameLookup(UUID uUID) {
            this.uuid = uUID;
            this.uuidstr = uUID.toString();
        }

        public String getName() {
            return this.name;
        }

        public String lookUpName() {
            class_1657 class_16572 = null;
            if (Notifier.access$000().field_1687 != null) {
                class_16572 = Notifier.access$000().field_1687.method_18470(this.uuid);
            }
            if (class_16572 == null) {
                String string = String.valueOf(new StringBuilder().append("https://api.mojang.com/user/profiles/").append(this.uuidstr.replace("-", "")).append("/names"));
                try {
                    JsonParser jsonParser = new JsonParser();
                    String string2 = IOUtils.toString((URL)new URL(string), (Charset)StandardCharsets.UTF_8);
                    JsonElement jsonElement = jsonParser.parse(string2);
                    JsonArray jsonArray = jsonElement.getAsJsonArray();
                    String string3 = jsonArray.get(jsonArray.size() - 1).toString();
                    JsonObject jsonObject = jsonParser.parse(string3).getAsJsonObject();
                    return jsonObject.get("name").toString();
                }
                catch (Exception exception) {
                    return null;
                }
            }
            return class_16572.method_5477().method_10851();
        }

        @Override
        public void run() {
            this.name = this.lookUpName();
        }

        public NameLookup(String string) {
            this.uuidstr = string;
            this.uuid = UUID.fromString(string);
        }
    }
}

