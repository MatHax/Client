/*
 * Decompiled with CFR 0.151.
 */
package minegame159.meteorclient.systems.modules.bedtrap;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.renderer.ShapeMode;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.Utils;
import mathax.client.utils.misc.Pool;
import mathax.client.utils.render.color.Color;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.utils.world.BlockIterator;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.bedtrap.PlayerUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_2596;
import net.minecraft.class_2680;
import net.minecraft.class_2828;
import net.minecraft.class_3532;

public class AutoBuild
extends Module {
    private final Setting<Build> buildPlacement;
    private final SettingGroup sgHighway;
    private boolean sentMessage;
    int tickskip;
    private final Setting<Integer> size;
    private static final String[] BUILD;
    private DirectionLite directionLite;
    private final Setting<Boolean> selfToggle;
    private final class_2338.class_2339 blockPos;
    private final Setting<Boolean> sideBlocks;
    static final boolean $assertionsDisabled;
    private final Setting<Boolean> center;
    private int prevSlot;
    private final Setting<Integer> cooldown;
    private final SettingGroup sgGeneral;
    private Direction direction;
    private boolean return_;

    private boolean findSlot() {
        if (!$assertionsDisabled && this.mc.field_1724 == null) {
            throw new AssertionError();
        }
        this.prevSlot = this.mc.field_1724.field_7514.field_7545;
        for (int i = 0; i < 9; ++i) {
            class_1792 class_17922 = this.mc.field_1724.field_7514.method_5438(i).method_7909();
            if (!(class_17922 instanceof class_1747) || class_17922 != class_1802.field_8281) continue;
            this.mc.field_1724.field_7514.field_7545 = i;
            return true;
        }
        return false;
    }

    @Override
    public void onActivate() {
        if (this.center.get().booleanValue()) {
            if (!$assertionsDisabled && this.mc.field_1724 == null) {
                throw new AssertionError();
            }
            double d = (double)class_3532.method_15357((double)this.mc.field_1724.method_23317()) + 0.5;
            double d2 = (double)class_3532.method_15357((double)this.mc.field_1724.method_23321()) + 0.5;
            this.mc.field_1724.method_5814(d, this.mc.field_1724.method_23318(), d2);
            this.mc.field_1724.field_3944.method_2883((class_2596)new class_2828.class_2829(this.mc.field_1724.method_23317(), this.mc.field_1724.method_23318(), this.mc.field_1724.method_23321(), this.mc.field_1724.method_24828()));
        }
        if (!$assertionsDisabled && this.mc.field_1724 == null) {
            throw new AssertionError();
        }
        this.blockPos.method_10101((class_2382)this.mc.field_1724.method_24515());
        this.direction = this.getDirection((class_1657)this.mc.field_1724);
        this.directionLite = this.getDirectionLite((class_1657)this.mc.field_1724);
    }

    private void setBlockPos(int n, int n2, int n3) {
        if (!$assertionsDisabled && this.mc.field_1724 == null) {
            throw new AssertionError();
        }
        this.blockPos.method_10102(this.mc.field_1724.method_23317() + (double)n, this.mc.field_1724.method_23318() + (double)n2, this.mc.field_1724.method_23321() + (double)n3);
    }

    static {
        $assertionsDisabled = !AutoBuild.class.desiredAssertionStatus();
        BUILD = new String[4];
        AutoBuild.BUILD[0] = "[NomadHut]";
        AutoBuild.BUILD[1] = "[Penis]";
        AutoBuild.BUILD[2] = "[Swastika]";
        AutoBuild.BUILD[3] = "[Highway]";
    }

    private DirectionLite getDirectionLite(class_1657 class_16572) {
        double d = class_16572.field_6031;
        if (d == 0.0) {
            return DirectionLite.SOUTH;
        }
        if (d < 0.0) {
            if ((d -= (double)(class_3532.method_15384((double)(d / 360.0)) * 360)) < -180.0) {
                d = 360.0 + d;
            }
        } else if ((d -= (double)(class_3532.method_15357((double)(d / 360.0)) * 360)) > 180.0) {
            d = -360.0 + d;
        }
        if (d >= 135.5 || d < -135.5) {
            return DirectionLite.NORTH;
        }
        if (d >= -135.5 && d < -44.5) {
            return DirectionLite.EAST;
        }
        if (d >= -44.5 && d <= 44.5) {
            return DirectionLite.SOUTH;
        }
        if (d >= 44.5 && d < 135.5) {
            return DirectionLite.WEST;
        }
        return DirectionLite.SOUTH;
    }

    @Override
    public String getInfoString() {
        if (this.buildPlacement.get() == Build.NomadHut) {
            return BUILD[0];
        }
        if (this.buildPlacement.get() == Build.Penis) {
            return BUILD[1];
        }
        if (this.buildPlacement.get() == Build.Swastika) {
            return BUILD[2];
        }
        if (this.buildPlacement.get() == Build.Highway) {
            return BUILD[3];
        }
        return "None";
    }

    private void resetSlot() {
        if (!$assertionsDisabled && this.mc.field_1724 == null) {
            throw new AssertionError();
        }
        this.mc.field_1724.field_7514.field_7545 = this.prevSlot;
    }

    private boolean place(int n, int n2, int n3) {
        boolean bl;
        this.setBlockPos(n, n2, n3);
        if (!$assertionsDisabled && this.mc.field_1687 == null) {
            throw new AssertionError();
        }
        class_2680 class_26802 = this.mc.field_1687.method_8320((class_2338)this.blockPos);
        boolean bl2 = class_26802.method_26204() == class_2246.field_10540;
        boolean bl3 = bl = !class_26802.method_26207().method_15800();
        if (!bl && this.findSlot()) {
            boolean bl4;
            bl = PlayerUtils.placeBlock((class_2338)this.blockPos, class_1268.field_5808, true);
            this.resetSlot();
            boolean bl5 = bl4 = this.mc.field_1687.method_8320((class_2338)this.blockPos).method_26204() == class_2246.field_10540;
            if (!bl2 && bl4) {
                this.return_ = true;
            }
        }
        return bl;
    }

    @EventHandler
    private void onTick(TickEvent.Post post) {
        boolean bl;
        boolean bl2;
        boolean bl3;
        boolean bl4;
        boolean bl5;
        boolean bl6;
        boolean bl7;
        boolean bl8;
        boolean bl9;
        boolean bl10;
        boolean bl11;
        boolean bl12;
        boolean bl13;
        boolean bl14;
        boolean bl15;
        boolean bl16;
        boolean bl17;
        boolean bl18;
        boolean bl19;
        boolean bl20;
        boolean bl21;
        boolean bl22;
        boolean bl23;
        boolean bl24;
        boolean bl25;
        boolean bl26;
        boolean bl27;
        boolean bl28;
        boolean bl29;
        boolean bl30;
        boolean bl31;
        boolean bl32;
        boolean bl33;
        boolean bl34;
        boolean bl35;
        boolean bl36;
        boolean bl37;
        boolean bl38;
        boolean bl39;
        boolean bl40;
        boolean bl41;
        boolean bl42;
        boolean bl43;
        boolean bl44;
        boolean bl45;
        boolean bl46;
        boolean bl47;
        boolean bl48;
        boolean bl49;
        boolean bl50;
        boolean bl51;
        boolean bl52;
        boolean bl53;
        boolean bl54;
        boolean bl55;
        boolean bl56;
        boolean bl57;
        boolean bl58;
        boolean bl59;
        boolean bl60;
        boolean bl61;
        boolean bl62;
        boolean bl63;
        boolean bl64;
        boolean bl65;
        boolean bl66;
        boolean bl67;
        boolean bl68;
        int n;
        int n2 = -1;
        for (n = 0; n < 9; ++n) {
            if (!$assertionsDisabled && this.mc.field_1724 == null) {
                throw new AssertionError();
            }
            if (this.mc.field_1724.field_7514.method_5438(n).method_7909() != class_2246.field_10540.method_8389()) continue;
            n2 = n;
            break;
        }
        if (n2 == -1 && this.selfToggle.get().booleanValue()) {
            if (!this.sentMessage) {
                ChatUtils.warning("No obsidian found\u2026 disabling.", new Object[0]);
                this.sentMessage = true;
            }
            this.toggle();
            return;
        }
        if (n2 == -1) {
            return;
        }
        n = this.mc.field_1724.field_7514.field_7545;
        if (this.buildPlacement.get() == Build.NomadHut) {
            this.mc.field_1724.field_7514.field_7545 = n2;
            if (this.tickskip != 0) {
                --this.tickskip;
                return;
            }
            this.tickskip = this.cooldown.get();
            this.return_ = false;
            switch (this.directionLite) {
                case EAST: {
                    bl68 = this.place(2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(0, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(-1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(-2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(-2, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(-2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(-1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    bl59 = this.place(0, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    bl58 = this.place(1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    bl57 = this.place(2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl56 = this.place(2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl55 = this.place(1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl54 = this.place(-1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl53 = this.place(-2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl52 = this.place(-2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl51 = this.place(-1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl50 = this.place(1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl49 = this.place(2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl48 = this.place(2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl47 = this.place(2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl46 = this.place(1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl45 = this.place(0, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl44 = this.place(-1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl43 = this.place(-2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl42 = this.place(-2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl41 = this.place(-2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl40 = this.place(-1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    bl39 = this.place(0, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    bl38 = this.place(1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    bl37 = this.place(1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    bl36 = this.place(1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    bl35 = this.place(1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    bl34 = this.place(0, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    bl33 = this.place(0, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    bl32 = this.place(0, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    bl31 = this.place(-1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    bl30 = this.place(-1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    bl29 = this.place(-1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62 && bl61 && bl60 && bl59 && bl58 && bl57 && bl56 && bl55 && bl54 && bl53 && bl52 && bl51 && bl50 && bl49 && bl48 && bl47 && bl46 && bl45 && bl44 && bl43 && bl42 && bl41 && bl40 && bl39 && bl38 && bl37 && bl36 && bl35 && bl34 && bl33 && bl32 && bl31 && bl30 && bl29) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case NORTH: {
                    bl28 = this.place(2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl27 = this.place(2, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    bl26 = this.place(2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl25 = this.place(1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl24 = this.place(0, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl23 = this.place(-1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl22 = this.place(-2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl21 = this.place(-2, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    bl20 = this.place(-2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl19 = this.place(-1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    bl18 = this.place(1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    bl17 = this.place(2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl16 = this.place(2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl15 = this.place(1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl14 = this.place(-1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl13 = this.place(-2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl12 = this.place(-2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl11 = this.place(-1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl10 = this.place(1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl9 = this.place(2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl8 = this.place(2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl7 = this.place(2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl6 = this.place(1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl5 = this.place(0, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl4 = this.place(-1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl3 = this.place(-2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl2 = this.place(-2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl = this.place(-2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl69 = this.place(-1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl70 = this.place(0, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl71 = this.place(1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl72 = this.place(1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl73 = this.place(1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl74 = this.place(1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl75 = this.place(0, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl76 = this.place(0, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl77 = this.place(0, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl78 = this.place(-1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl79 = this.place(-1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl80 = this.place(-1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    if (bl28 && bl27 && bl26 && bl25 && bl24 && bl23 && bl22 && bl21 && bl20 && bl19 && bl18 && bl17 && bl16 && bl15 && bl14 && bl13 && bl12 && bl11 && bl10 && bl9 && bl8 && bl7 && bl6 && bl5 && bl4 && bl3 && bl2 && bl && bl69 && bl70 && bl71 && bl72 && bl73 && bl74 && bl75 && bl76 && bl77 && bl78 && bl79 && bl80) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case WEST: {
                    boolean bl81 = this.place(2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl82 = this.place(2, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl83 = this.place(2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl84 = this.place(1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl85 = this.place(0, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl86 = this.place(-1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl87 = this.place(-2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl88 = this.place(-2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl89 = this.place(-1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl90 = this.place(0, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl91 = this.place(1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl92 = this.place(2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl93 = this.place(2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl94 = this.place(1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl95 = this.place(-1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl96 = this.place(-2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl97 = this.place(-2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl98 = this.place(-1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl99 = this.place(1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl100 = this.place(2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl101 = this.place(2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl102 = this.place(2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl103 = this.place(1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl104 = this.place(0, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl105 = this.place(-1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl106 = this.place(-2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl107 = this.place(-2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl108 = this.place(-2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl109 = this.place(-1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl110 = this.place(0, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl111 = this.place(1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl112 = this.place(1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl113 = this.place(1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl114 = this.place(1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl115 = this.place(0, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl116 = this.place(0, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl117 = this.place(0, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl118 = this.place(-1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl119 = this.place(-1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl120 = this.place(-1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    if (bl81 && bl82 && bl83 && bl84 && bl85 && bl86 && bl87 && bl88 && bl89 && bl90 && bl91 && bl92 && bl93 && bl94 && bl95 && bl96 && bl97 && bl98 && bl99 && bl100 && bl101 && bl102 && bl103 && bl104 && bl105 && bl106 && bl107 && bl108 && bl109 && bl110 && bl111 && bl112 && bl113 && bl114 && bl115 && bl116 && bl117 && bl118 && bl119 && bl120) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case SOUTH: {
                    boolean bl121 = this.place(2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl122 = this.place(2, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl123 = this.place(2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl124 = this.place(1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl125 = this.place(-1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl126 = this.place(-2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl127 = this.place(-2, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl128 = this.place(-2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl129 = this.place(-1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl130 = this.place(0, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl131 = this.place(1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl132 = this.place(2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl133 = this.place(2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl134 = this.place(1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl135 = this.place(-1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl136 = this.place(-2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl137 = this.place(-2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl138 = this.place(-1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl139 = this.place(1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl140 = this.place(2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl141 = this.place(2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl142 = this.place(2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl143 = this.place(1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl144 = this.place(0, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl145 = this.place(-1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl146 = this.place(-2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl147 = this.place(-2, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl148 = this.place(-2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl149 = this.place(-1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl150 = this.place(0, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl151 = this.place(1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    boolean bl152 = this.place(1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl153 = this.place(1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl154 = this.place(1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl155 = this.place(0, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl156 = this.place(0, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl157 = this.place(0, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl158 = this.place(-1, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    boolean bl159 = this.place(-1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    boolean bl160 = this.place(-1, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    if (bl121 && bl122 && bl123 && bl124 && bl125 && bl126 && bl127 && bl128 && bl129 && bl130 && bl131 && bl132 && bl133 && bl134 && bl135 && bl136 && bl137 && bl138 && bl139 && bl140 && bl141 && bl142 && bl143 && bl144 && bl145 && bl146 && bl147 && bl148 && bl149 && bl150 && bl151 && bl152 && bl153 && bl154 && bl155 && bl156 && bl157 && bl158 && bl159 && bl160) {
                        this.toggle();
                        break;
                    }
                    return;
                }
            }
        }
        if (this.buildPlacement.get() == Build.Penis) {
            this.mc.field_1724.field_7514.field_7545 = n2;
            if (this.tickskip != 0) {
                --this.tickskip;
                return;
            }
            this.tickskip = this.cooldown.get();
            this.return_ = false;
            switch (this.directionLite) {
                case EAST: {
                    bl68 = this.place(1, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(1, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(1, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(1, 1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(1, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case NORTH: {
                    bl62 = this.place(-1, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(0, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(1, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl59 = this.place(0, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl58 = this.place(0, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl57 = this.place(0, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    if (bl62 && bl61 && bl60 && bl59 && bl58 && bl57) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case WEST: {
                    bl56 = this.place(-1, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl55 = this.place(-1, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    bl54 = this.place(-1, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl53 = this.place(-1, 1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl52 = this.place(-1, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl51 = this.place(-1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    if (bl56 && bl55 && bl54 && bl53 && bl52 && bl51) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case SOUTH: {
                    bl50 = this.place(-1, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl49 = this.place(0, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl48 = this.place(1, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl47 = this.place(0, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl46 = this.place(0, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl45 = this.place(0, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    if (bl50 && bl49 && bl48 && bl47 && bl46 && bl45) {
                        this.toggle();
                        break;
                    }
                    return;
                }
            }
        }
        if (this.buildPlacement.get() == Build.Swastika) {
            this.mc.field_1724.field_7514.field_7545 = n2;
            if (this.tickskip != 0) {
                --this.tickskip;
                return;
            }
            this.tickskip = this.cooldown.get();
            this.return_ = false;
            switch (this.directionLite) {
                case EAST: {
                    bl68 = this.place(1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(1, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(1, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(1, 1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(1, 1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(1, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(1, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl59 = this.place(1, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl58 = this.place(1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    bl57 = this.place(1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    bl56 = this.place(1, 3, -2);
                    if (this.return_) {
                        return;
                    }
                    bl55 = this.place(1, 4, 2);
                    if (this.return_) {
                        return;
                    }
                    bl54 = this.place(1, 4, 1);
                    if (this.return_) {
                        return;
                    }
                    bl53 = this.place(1, 4, 0);
                    if (this.return_) {
                        return;
                    }
                    bl52 = this.place(1, 4, -2);
                    if (this.return_) {
                        return;
                    }
                    if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62 && bl61 && bl60 && bl59 && bl58 && bl57 && bl56 && bl55 && bl54 && bl53 && bl52) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case NORTH: {
                    bl51 = this.place(2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl50 = this.place(0, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl49 = this.place(-1, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl48 = this.place(-2, 0, -1);
                    if (this.return_) {
                        return;
                    }
                    bl47 = this.place(2, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl46 = this.place(0, 1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl45 = this.place(2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl44 = this.place(1, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl43 = this.place(0, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl42 = this.place(-1, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl41 = this.place(-2, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl40 = this.place(0, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    bl39 = this.place(-2, 3, -1);
                    if (this.return_) {
                        return;
                    }
                    bl38 = this.place(2, 4, -1);
                    if (this.return_) {
                        return;
                    }
                    bl37 = this.place(1, 4, -1);
                    if (this.return_) {
                        return;
                    }
                    bl36 = this.place(0, 4, -1);
                    if (this.return_) {
                        return;
                    }
                    bl35 = this.place(-2, 4, -1);
                    if (this.return_) {
                        return;
                    }
                    if (bl51 && bl50 && bl49 && bl48 && bl47 && bl46 && bl45 && bl44 && bl43 && bl42 && bl41 && bl40 && bl39 && bl38 && bl37 && bl36 && bl35) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case WEST: {
                    bl34 = this.place(-1, 0, -2);
                    if (this.return_) {
                        return;
                    }
                    bl33 = this.place(-1, 0, 0);
                    if (this.return_) {
                        return;
                    }
                    bl32 = this.place(-1, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl31 = this.place(-1, 0, 2);
                    if (this.return_) {
                        return;
                    }
                    bl30 = this.place(-1, 1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl29 = this.place(-1, 1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl28 = this.place(-1, 2, -2);
                    if (this.return_) {
                        return;
                    }
                    bl27 = this.place(-1, 2, -1);
                    if (this.return_) {
                        return;
                    }
                    bl26 = this.place(-1, 2, 0);
                    if (this.return_) {
                        return;
                    }
                    bl25 = this.place(-1, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl24 = this.place(-1, 2, 2);
                    if (this.return_) {
                        return;
                    }
                    bl23 = this.place(-1, 3, 0);
                    if (this.return_) {
                        return;
                    }
                    bl22 = this.place(-1, 3, 2);
                    if (this.return_) {
                        return;
                    }
                    bl21 = this.place(-1, 4, -2);
                    if (this.return_) {
                        return;
                    }
                    bl20 = this.place(-1, 4, -1);
                    if (this.return_) {
                        return;
                    }
                    bl19 = this.place(-1, 4, 0);
                    if (this.return_) {
                        return;
                    }
                    bl18 = this.place(-1, 4, 2);
                    if (this.return_) {
                        return;
                    }
                    if (bl34 && bl33 && bl32 && bl31 && bl30 && bl29 && bl28 && bl27 && bl26 && bl25 && bl24 && bl23 && bl22 && bl21 && bl20 && bl19 && bl18) {
                        this.toggle();
                        break;
                    }
                    return;
                }
                case SOUTH: {
                    bl17 = this.place(-2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl16 = this.place(0, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl15 = this.place(1, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl14 = this.place(2, 0, 1);
                    if (this.return_) {
                        return;
                    }
                    bl13 = this.place(-2, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl12 = this.place(0, 1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl11 = this.place(-2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl10 = this.place(-1, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl9 = this.place(0, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl8 = this.place(1, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl7 = this.place(2, 2, 1);
                    if (this.return_) {
                        return;
                    }
                    bl6 = this.place(0, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    bl5 = this.place(2, 3, 1);
                    if (this.return_) {
                        return;
                    }
                    bl4 = this.place(-2, 4, 1);
                    if (this.return_) {
                        return;
                    }
                    bl3 = this.place(-1, 4, 1);
                    if (this.return_) {
                        return;
                    }
                    bl2 = this.place(0, 4, 1);
                    if (this.return_) {
                        return;
                    }
                    bl = this.place(2, 4, 1);
                    if (this.return_) {
                        return;
                    }
                    if (bl17 && bl16 && bl15 && bl14 && bl13 && bl12 && bl11 && bl10 && bl9 && bl8 && bl7 && bl6 && bl5 && bl4 && bl3 && bl2 && bl) {
                        this.toggle();
                        break;
                    }
                    return;
                }
            }
        }
        if (this.buildPlacement.get() == Build.Highway) {
            this.mc.field_1724.field_7514.field_7545 = n2;
            if (this.tickskip != 0) {
                --this.tickskip;
                return;
            }
            this.tickskip = this.cooldown.get();
            this.return_ = false;
            switch (this.direction) {
                case EAST: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl67 = this.place(1, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl66 = this.place(1, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            if (bl67 && bl66) {
                                return;
                            }
                        }
                        if (bl68) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(1, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(1, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(1, 0, 3);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(1, 0, -3);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(1, -1, 3);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(1, -1, -3);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(1, 0, 4);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(1, 0, -4);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() != 5) break;
                    bl68 = this.place(1, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(1, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(1, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(1, -1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(1, -1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(1, -1, 3);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(1, -1, -3);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(1, -1, 4);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(1, -1, -4);
                    if (this.return_) {
                        return;
                    }
                    if (this.sideBlocks.get().booleanValue()) {
                        bl59 = this.place(1, 0, 5);
                        if (this.return_) {
                            return;
                        }
                        bl58 = this.place(1, 0, -5);
                        if (this.return_) {
                            return;
                        }
                        if (bl59 && bl58) {
                            return;
                        }
                    }
                    if (!bl68 || !bl67 || !bl66 || !bl65 || !bl64 || !bl63 || !bl62 || !bl61 || !bl60) break;
                    return;
                }
                case NORTH: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl67 = this.place(1, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl66 = this.place(-1, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            if (bl67 && bl66) {
                                return;
                            }
                        }
                        if (bl68) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(2, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(-2, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(3, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(-3, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(3, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-3, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(4, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(-4, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() != 5) break;
                    bl68 = this.place(0, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(1, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(-1, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(2, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(-2, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(3, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(-3, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(4, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(-4, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    if (this.sideBlocks.get().booleanValue()) {
                        bl59 = this.place(5, 0, -1);
                        if (this.return_) {
                            return;
                        }
                        bl58 = this.place(-5, 0, -1);
                        if (this.return_) {
                            return;
                        }
                        if (bl59 && bl58) {
                            return;
                        }
                    }
                    if (!bl68 || !bl67 || !bl66 || !bl65 || !bl64 || !bl63 || !bl62 || !bl61 || !bl60) break;
                    return;
                }
                case WEST: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl67 = this.place(-1, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl66 = this.place(-1, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            if (bl67 && bl66) {
                                return;
                            }
                        }
                        if (bl68) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(-1, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(-1, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(-1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(-1, 0, 3);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(-1, 0, -3);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(-1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(-1, -1, 3);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-1, -1, -3);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(-1, 0, 4);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(-1, 0, -4);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() != 5) break;
                    bl68 = this.place(-1, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(-1, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(-1, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(-1, -1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(-1, -1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(-1, -1, 3);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(-1, -1, -3);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(-1, -1, 4);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(-1, -1, -4);
                    if (this.return_) {
                        return;
                    }
                    if (this.sideBlocks.get().booleanValue()) {
                        bl59 = this.place(-1, 0, 5);
                        if (this.return_) {
                            return;
                        }
                        bl58 = this.place(-1, 0, -5);
                        if (this.return_) {
                            return;
                        }
                        if (bl59 && bl58) {
                            return;
                        }
                    }
                    if (!bl68 || !bl67 || !bl66 || !bl65 || !bl64 || !bl63 || !bl62 || !bl61 || !bl60) break;
                    return;
                }
                case SOUTH: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl67 = this.place(1, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl66 = this.place(-1, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            if (bl67 && bl66) {
                                return;
                            }
                        }
                        if (bl68) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(2, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(-2, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(3, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(-3, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(3, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-3, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(4, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(-4, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() != 5) break;
                    bl68 = this.place(0, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(1, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(-1, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(2, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(-2, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(3, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(-3, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(4, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(-4, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    if (this.sideBlocks.get().booleanValue()) {
                        bl59 = this.place(5, 0, 1);
                        if (this.return_) {
                            return;
                        }
                        bl58 = this.place(-5, 0, 1);
                        if (this.return_) {
                            return;
                        }
                        if (bl59 && bl58) {
                            return;
                        }
                    }
                    if (!bl68 || !bl67 || !bl66 || !bl65 || !bl64 || !bl63 || !bl62 || !bl61 || !bl60) break;
                    return;
                }
                case EAST_SOUTH: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(2, 0, 0);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(0, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(0, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(2, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(-1, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(0, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(3, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(-1, 0, 3);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(0, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl61 = this.place(3, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl60 = this.place(-1, -1, 3);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl59 = this.place(3, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            bl58 = this.place(-2, 0, 3);
                            if (this.return_) {
                                return;
                            }
                            if (bl59 && bl58) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62 && bl61 && bl60) {
                            return;
                        }
                    }
                    if (this.size.get() != 5) break;
                    bl68 = this.place(0, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(1, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(1, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(2, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(0, -1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(2, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(-1, -1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(3, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(-1, -1, 3);
                    if (this.return_) {
                        return;
                    }
                    bl59 = this.place(3, -1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl58 = this.place(-2, -1, 3);
                    if (this.return_) {
                        return;
                    }
                    if (this.sideBlocks.get().booleanValue()) {
                        bl57 = this.place(4, 0, -2);
                        if (this.return_) {
                            return;
                        }
                        bl56 = this.place(-2, 0, 4);
                        if (this.return_) {
                            return;
                        }
                        if (bl57 && bl56) {
                            return;
                        }
                    }
                    if (!bl68 || !bl67 || !bl66 || !bl65 || !bl64 || !bl63 || !bl62 || !bl61 || !bl60 || !bl59 || !bl58) break;
                    return;
                }
                case SOUTH_WEST: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(0, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(-2, 0, 0);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(-2, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(1, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(-3, 0, -1);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(1, 0, 3);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(0, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(1, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-2, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl61 = this.place(-3, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl60 = this.place(1, -1, 3);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl59 = this.place(-3, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            bl58 = this.place(2, 0, 3);
                            if (this.return_) {
                                return;
                            }
                            if (bl59 && bl58) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62 && bl61 && bl60) {
                            return;
                        }
                    }
                    if (this.size.get() != 5) break;
                    bl68 = this.place(0, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(-1, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(-1, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(0, -1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(-2, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(1, -1, 2);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(-2, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(-3, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(1, -1, 3);
                    if (this.return_) {
                        return;
                    }
                    bl59 = this.place(-3, -1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl58 = this.place(2, -1, 3);
                    if (this.return_) {
                        return;
                    }
                    if (this.sideBlocks.get().booleanValue()) {
                        bl57 = this.place(2, 0, 4);
                        if (this.return_) {
                            return;
                        }
                        bl56 = this.place(-4, 0, -2);
                        if (this.return_) {
                            return;
                        }
                        if (bl57 && bl56) {
                            return;
                        }
                    }
                    if (!bl68 || !bl67 || !bl66 || !bl65 || !bl64 || !bl63 || !bl62 || !bl61 || !bl60 || !bl59 || !bl58) break;
                    return;
                }
                case WEST_NORTH: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(0, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(-2, 0, 0);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(-2, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(1, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(-2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(-3, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(1, 0, -3);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(-2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl61 = this.place(-3, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl60 = this.place(1, -1, -3);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl59 = this.place(-3, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            bl58 = this.place(2, 0, -3);
                            if (this.return_) {
                                return;
                            }
                            if (bl59 && bl58) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62 && bl61 && bl60) {
                            return;
                        }
                    }
                    if (this.size.get() == 5) {
                        bl68 = this.place(-1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(-1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(-2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(-2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl61 = this.place(-3, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl60 = this.place(1, -1, -3);
                        if (this.return_) {
                            return;
                        }
                        bl59 = this.place(-3, -1, 2);
                        if (this.return_) {
                            return;
                        }
                        bl58 = this.place(2, -1, -3);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl57 = this.place(-4, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            bl56 = this.place(2, 0, -4);
                            if (this.return_) {
                                return;
                            }
                            if (bl57 && bl56) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62 && bl61 && bl60 && bl59 && bl58) {
                            return;
                        }
                    }
                }
                case NORTH_EAST: {
                    if (this.size.get() == 1) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl65 = this.place(0, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            bl64 = this.place(2, 0, 0);
                            if (this.return_) {
                                return;
                            }
                            if (bl65 && bl64) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66) {
                            return;
                        }
                    }
                    if (this.size.get() == 2) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl63 = this.place(-1, 0, -2);
                            if (this.return_) {
                                return;
                            }
                            bl62 = this.place(2, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            if (bl63 && bl62) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64) {
                            return;
                        }
                    }
                    if (this.size.get() == 3) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl61 = this.place(-1, 0, -3);
                            if (this.return_) {
                                return;
                            }
                            bl60 = this.place(3, 0, 1);
                            if (this.return_) {
                                return;
                            }
                            if (bl61 && bl60) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62) {
                            return;
                        }
                    }
                    if (this.size.get() == 4) {
                        bl68 = this.place(1, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl67 = this.place(1, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl66 = this.place(0, -1, -1);
                        if (this.return_) {
                            return;
                        }
                        bl65 = this.place(0, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl64 = this.place(2, -1, 0);
                        if (this.return_) {
                            return;
                        }
                        bl63 = this.place(2, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        bl62 = this.place(-1, -1, -2);
                        if (this.return_) {
                            return;
                        }
                        bl61 = this.place(-1, -1, -3);
                        if (this.return_) {
                            return;
                        }
                        bl60 = this.place(3, -1, 1);
                        if (this.return_) {
                            return;
                        }
                        if (this.sideBlocks.get().booleanValue()) {
                            bl59 = this.place(-2, 0, -3);
                            if (this.return_) {
                                return;
                            }
                            bl58 = this.place(3, 0, 2);
                            if (this.return_) {
                                return;
                            }
                            if (bl59 && bl58) {
                                return;
                            }
                        }
                        if (bl68 && bl67 && bl66 && bl65 && bl64 && bl63 && bl62 && bl61 && bl60) {
                            return;
                        }
                    }
                    if (this.size.get() != 5) break;
                    bl68 = this.place(1, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl67 = this.place(1, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl66 = this.place(0, -1, -1);
                    if (this.return_) {
                        return;
                    }
                    bl65 = this.place(0, -1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl64 = this.place(2, -1, 0);
                    if (this.return_) {
                        return;
                    }
                    bl63 = this.place(2, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl62 = this.place(-1, -1, -2);
                    if (this.return_) {
                        return;
                    }
                    bl61 = this.place(-1, -1, -3);
                    if (this.return_) {
                        return;
                    }
                    bl60 = this.place(3, -1, 1);
                    if (this.return_) {
                        return;
                    }
                    bl59 = this.place(-2, -1, -3);
                    if (this.return_) {
                        return;
                    }
                    bl58 = this.place(3, -1, 2);
                    if (this.return_) {
                        return;
                    }
                    if (this.sideBlocks.get().booleanValue()) {
                        bl57 = this.place(-2, 0, -4);
                        if (this.return_) {
                            return;
                        }
                        bl56 = this.place(4, 0, 2);
                        if (this.return_) {
                            return;
                        }
                        if (bl57 && bl56) {
                            return;
                        }
                    }
                    if (!bl68 || !bl67 || !bl66 || !bl65 || !bl64 || !bl63 || !bl62 || !bl61 || !bl60 || !bl59 || !bl58) break;
                    return;
                }
            }
        }
    }

    public AutoBuild() {
        super(Categories.BedTrap, "auto-build", "Places build that you choose.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgHighway = this.settings.createGroup("Highway");
        this.buildPlacement = this.sgGeneral.add(new EnumSetting.Builder().name("structure").description("Which structure gonna build.").defaultValue(Build.Swastika).build());
        this.cooldown = this.sgGeneral.add(new IntSetting.Builder().name("place-delay").description("Block per tick.").defaultValue(0).min(0).sliderMax(20).build());
        this.center = this.sgGeneral.add(new BoolSetting.Builder().name("center").description("Moves you to the center of the block.").defaultValue(true).build());
        this.selfToggle = this.sgGeneral.add(new BoolSetting.Builder().name("self-toggle").description("Toggles when you run out of obsidian.").defaultValue(false).build());
        this.size = this.sgHighway.add(new IntSetting.Builder().name("highway-size").description("The size of highway.").defaultValue(3).min(1).sliderMin(1).max(5).sliderMax(5).build());
        this.sideBlocks = this.sgHighway.add(new BoolSetting.Builder().name("side-blocks").description("Placing side blocks.").defaultValue(true).build());
        this.sentMessage = false;
        this.blockPos = new class_2338.class_2339();
        this.tickskip = this.cooldown.get();
    }

    private Direction getDirection(class_1657 class_16572) {
        double d = class_16572.field_6031;
        if (d == 0.0) {
            return Direction.SOUTH;
        }
        if (d < 0.0) {
            if ((d -= (double)(class_3532.method_15384((double)(d / 360.0)) * 360)) < -180.0) {
                d = 360.0 + d;
            }
        } else if ((d -= (double)(class_3532.method_15357((double)(d / 360.0)) * 360)) > 180.0) {
            d = -360.0 + d;
        }
        if (d >= 157.5 || d < -157.5) {
            return Direction.NORTH;
        }
        if (d >= -157.5 && d < -112.5) {
            return Direction.NORTH_EAST;
        }
        if (d >= -112.5 && d < -67.5) {
            return Direction.EAST;
        }
        if (d >= -67.5 && d < -22.5) {
            return Direction.EAST_SOUTH;
        }
        if (d >= -22.5 && d <= 0.0 || d > 0.0 && d < 22.5) {
            return Direction.SOUTH;
        }
        if (d >= 22.5 && d < 67.5) {
            return Direction.SOUTH_WEST;
        }
        if (d >= 67.5 && d < 112.5) {
            return Direction.WEST;
        }
        if (d >= 112.5 && d < 157.5) {
            return Direction.WEST_NORTH;
        }
        return Direction.SOUTH;
    }

    private static enum Direction {
        SOUTH,
        SOUTH_WEST,
        WEST,
        WEST_NORTH,
        NORTH,
        NORTH_EAST,
        EAST,
        EAST_SOUTH;

    }

    private static enum DirectionLite {
        SOUTH,
        WEST,
        NORTH,
        EAST;

    }

    public static enum Build {
        NomadHut,
        Penis,
        Swastika,
        Highway;

    }
}

