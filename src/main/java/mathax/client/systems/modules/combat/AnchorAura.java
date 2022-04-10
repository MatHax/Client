package mathax.client.systems.modules.combat;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.renderer.ShapeMode;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.Utils;
import mathax.client.utils.entity.EntityUtils;
import mathax.client.utils.entity.SortPriority;
import mathax.client.utils.entity.TargetUtils;
import mathax.client.utils.player.*;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/*/--------------------------------------------------------------------------------------------------------------/*/
/*/ Automation used from Orion Meteor Addon                                                                      /*/
/*/ https://github.com/AntiCope/orion/blob/master/src/main/java/me/ghosttypes/orion/modules/main/AnchorAura.java /*/
/*/--------------------------------------------------------------------------------------------------------------/*/

public class AnchorAura extends Module {
    private PlayerEntity target;

    private boolean sentTrapMine;
    private boolean sentBurrowMine;
    private boolean sentAntiStuck;

    private int placeDelayLeft;
    private int breakDelayLeft;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius in which players get targeted.")
        .defaultValue(4)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    private final Setting<SortPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.Lowest_Health)
        .build()
    );

    private final Setting<RotationMode> rotationMode = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotation-mode")
        .description("The mode to rotate you server-side.")
        .defaultValue(RotationMode.Both)
        .build()
    );

    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("The maximum self-damage allowed.")
        .defaultValue(8)
        .range(0, 36)
        .sliderRange(0, 36)
        .build()
    );

    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-health")
        .description("The minimum health you have to be for Anchor Aura to work.")
        .defaultValue(15)
        .range(1, 36)
        .sliderRange(1, 36)
        .build()
    );

    private final Setting<Boolean> antiStuck = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-stuck")
        .description("Prevent getting stuck when glowstone is placed on the target's head.")
        .defaultValue(true)
        .build()
    );

    // Place

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
        .name("place")
        .description("Allows Anchor Aura to place anchors.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The tick delay between placing anchors.")
        .defaultValue(2)
        .range(0, 20)
        .sliderRange(0, 20)
        .visible(place::get)
        .build()
    );

    private final Setting<Safety> placeMode = sgPlace.add(new EnumSetting.Builder<Safety>()
        .name("place-mode")
        .description("The way anchors are allowed to be placed near you.")
        .defaultValue(Safety.Safe)
        .visible(place::get)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The radius in which anchors are placed in.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 5)
        .visible(place::get)
        .build()
    );

    private final Setting<PlaceMode> placePositions = sgPlace.add(new EnumSetting.Builder<PlaceMode>()
        .name("placement-positions")
        .description("Where the Anchors will be placed on the entity.")
        .defaultValue(PlaceMode.Above_and_Below)
        .visible(place::get)
        .build()
    );

    // Break

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The tick delay between breaking anchors.")
        .defaultValue(10)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Safety> breakMode = sgBreak.add(new EnumSetting.Builder<Safety>()
        .name("break-mode")
        .description("The way anchors are allowed to be broken near you.")
        .defaultValue(Safety.Safe)
        .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("The radius in which anchors are broken in.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    // Automation

    private final Setting<Boolean> breakSelfTrap = sgAutomation.add(new BoolSetting.Builder()
        .name("break-self-trap")
        .description("Break target's self-trap before placing/breaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakBurrow = sgAutomation.add(new BoolSetting.Builder()
        .name("break-burrow")
        .description("Break target's burrow before placing/breaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> fastPlaceAfter = sgAutomation.add(new BoolSetting.Builder()
        .name("fast-place-after-break")
        .description("Place the next anchor immediately after breaking target's self-trap.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderAutomation = sgAutomation.add(new BoolSetting.Builder()
        .name("render-break")
        .description("Render mining self-trap/burrow.")
        .defaultValue(true)
        .build()
    );

    // Pause


    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses while eating.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses while drinking potions.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses while mining blocks.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Determines how the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
        .name("render-place")
        .description("Renders the block where it is placing an anchor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> placeSideColor = sgRender.add(new ColorSetting.Builder()
        .name("place-side-color")
        .description("The side color for positions to be placed.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 75))
        .visible(renderPlace::get)
        .build()
    );

    private final Setting<SettingColor> placeLineColor = sgRender.add(new ColorSetting.Builder()
        .name("place-line-color")
        .description("The line color for positions to be placed.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 255))
        .visible(renderPlace::get)
        .build()
    );

    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
        .name("render-break")
        .description("Renders the block where it is breaking an anchor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> breakSideColor = sgRender.add(new ColorSetting.Builder()
        .name("break-side-color")
        .description("The side color for anchors to be broken.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 75))
        .visible(renderBreak::get)
        .build()
    );

    private final Setting<SettingColor> breakLineColor = sgRender.add(new ColorSetting.Builder()
        .name("break-line-color")
        .description("The line color for anchors to be broken.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 255))
        .visible(renderBreak::get)
        .build()
    );

    public AnchorAura() {
        super(Categories.Combat, Items.RESPAWN_ANCHOR, "anchor-aura", "Automatically places and breaks Respawn Anchors to harm entities.");
    }

    @Override
    public void onActivate() {
        placeDelayLeft = 0;
        breakDelayLeft = 0;
        target = null;
        sentTrapMine = false;
        sentBurrowMine = false;
        sentAntiStuck = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world.getDimension().isRespawnAnchorWorking()) {
            error("You are in the Nether, disabling...");
            toggle();
            return;
        }

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (EntityUtils.getTotalPlayerHealth(mc.player) <= minHealth.get()) return;

        if (TargetUtils.isBadTarget(target, targetRange.get())) target = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
        if (TargetUtils.isBadTarget(target, targetRange.get())) return;

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);

        if (!anchor.found() || !glowStone.found()) return;

        if (antiStuck.get() && !sentAntiStuck) {
            if (findBreakPos(target.getBlockPos()) == null && findPlacePos(target.getBlockPos()) == null && BlockUtils.getBlock(target.getBlockPos().up(2)) == Blocks.GLOWSTONE) {
                FindItemResult pick = InvUtils.findPick();
                if (pick.found()) {
                    InvUtils.updateSlot(pick.slot());
                    PlayerUtils.doPacketMine(target.getBlockPos().up(2));
                    sentAntiStuck = true;
                    return;
                }
            }
        }

        if (sentAntiStuck && BlockUtils.getBlock(target.getBlockPos().up(2)) != Blocks.GLOWSTONE) sentAntiStuck = false;

        if (breakSelfTrap.get() && !sentTrapMine) {
            if (findBreakPos(target.getBlockPos()) == null && findPlacePos(target.getBlockPos()) == null && BlockUtils.isTrapBlock(target.getBlockPos().up(2))) {
                FindItemResult pick = InvUtils.findPick();
                if (pick.found()) {
                    InvUtils.updateSlot(pick.slot());
                    info("Breaking (highlight)%s(default)'s self-trap.", target.getEntityName());
                    PlayerUtils.doPacketMine(target.getBlockPos().up(2));
                    sentTrapMine = true;
                    return;
                }
            }
        }

        if (sentTrapMine && !BlockUtils.isTrapBlock(target.getBlockPos().up(2))) {
            if (fastPlaceAfter.get()) { placeDelayLeft = 0; breakDelayLeft = 0; }
            sentTrapMine = false;
        }

        if (breakBurrow.get() && !sentBurrowMine && PlayerUtils.isBurrowed(target, true)) {
            FindItemResult pick = InvUtils.findPick();
            if (pick.found()) {
                InvUtils.updateSlot(pick.slot());
                info("Breaking (highlight)%s(default)'s burrow.", target.getEntityName());
                PlayerUtils.doPacketMine(target.getBlockPos());
                sentBurrowMine = true;
                return;
            }
        }

        if (sentBurrowMine && PlayerUtils.isBurrowed(target, true)) return;
        else sentBurrowMine = false;

        if (breakDelayLeft >= breakDelay.get()) {
            BlockPos breakPos = findBreakPos(target.getBlockPos());
            if (breakPos != null) {
                breakDelayLeft = 0;
                if (rotationMode.get() == RotationMode.Both || rotationMode.get() == RotationMode.Break) Rotations.rotate(Rotations.getYaw(breakPos), Rotations.getPitch(breakPos), 50, () -> breakAnchor(breakPos, anchor, glowStone));
                else breakAnchor(breakPos, anchor, glowStone);
            }
        }

        if (placeDelayLeft >= placeDelay.get() && place.get()) {
            BlockPos placePos = findPlacePos(target.getBlockPos());

            if (placePos != null) {
                placeDelayLeft = 0;
                BlockUtils.place(placePos, anchor, (rotationMode.get() == RotationMode.Place || rotationMode.get() == RotationMode.Both), 50);
            }
        }

        placeDelayLeft++;
        breakDelayLeft++;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (target == null) return;

        if (renderPlace.get()) {
            BlockPos placePos = findPlacePos(target.getBlockPos());
            if (placePos == null) return;

            event.renderer.box(placePos, placeSideColor.get(), placeLineColor.get(), shapeMode.get(), 0);
        }

        if (renderBreak.get()) {
            BlockPos breakPos = findBreakPos(target.getBlockPos());
            if (breakPos == null) return;

            event.renderer.box(breakPos, breakSideColor.get(), breakLineColor.get(), shapeMode.get(), 0);
        }

        if (renderAutomation.get() && target != null) {
            if (sentBurrowMine) event.renderer.box(target.getBlockPos(), breakSideColor.get(), breakLineColor.get(), shapeMode.get(), 0);
            if (sentTrapMine) event.renderer.box(target.getBlockPos().up(2), breakSideColor.get(), breakLineColor.get(), shapeMode.get(), 0);
        }
    }

    private BlockPos findPlacePos(BlockPos targetPlacePos) {
        switch (placePositions.get()) {
            case All:
                if (isValidPlace(targetPlacePos.down())) return targetPlacePos.down();
                else if (isValidPlace(targetPlacePos.up(2))) return targetPlacePos.up(2);
                else if (isValidPlace(targetPlacePos.add(1, 0, 0))) return targetPlacePos.add(1, 0, 0);
                else if (isValidPlace(targetPlacePos.add(-1, 0, 0))) return targetPlacePos.add(-1, 0, 0);
                else if (isValidPlace(targetPlacePos.add(0, 0, 1))) return targetPlacePos.add(0, 0, 1);
                else if (isValidPlace(targetPlacePos.add(0, 0, -1))) return targetPlacePos.add(0, 0, -1);
                else if (isValidPlace(targetPlacePos.add(1, 1, 0))) return targetPlacePos.add(1, 1, 0);
                else if (isValidPlace(targetPlacePos.add(-1, -1, 0))) return targetPlacePos.add(-1, -1, 0);
                else if (isValidPlace(targetPlacePos.add(0, 1, 1))) return targetPlacePos.add(0, 1, 1);
                else if (isValidPlace(targetPlacePos.add(0, 0, -1))) return targetPlacePos.add(0, 0, -1);
                break;
            case Above:
                if (isValidPlace(targetPlacePos.up(2))) return targetPlacePos.up(2);
                break;
            case Above_and_Below:
                if (isValidPlace(targetPlacePos.down())) return targetPlacePos.down();
                else if (isValidPlace(targetPlacePos.up(2))) return targetPlacePos.up(2);
                break;
        }

        return null;
    }

    private BlockPos findBreakPos(BlockPos targetPos) {
        if (isValidBreak(targetPos.down())) return targetPos.down();
        else if (isValidBreak(targetPos.up(2))) return targetPos.up(2);
        else if (isValidBreak(targetPos.add(1, 0, 0))) return targetPos.add(1, 0, 0);
        else if (isValidBreak(targetPos.add(-1, 0, 0))) return targetPos.add(-1, 0, 0);
        else if (isValidBreak(targetPos.add(0, 0, 1))) return targetPos.add(0, 0, 1);
        else if (isValidBreak(targetPos.add(0, 0, -1))) return targetPos.add(0, 0, -1);
        else if (isValidBreak(targetPos.add(1, 1, 0))) return targetPos.add(1, 1, 0);
        else if (isValidBreak(targetPos.add(-1, -1, 0))) return targetPos.add(-1, -1, 0);
        else if (isValidBreak(targetPos.add(0, 1, 1))) return targetPos.add(0, 1, 1);
        else if (isValidBreak(targetPos.add(0, 0, -1))) return targetPos.add(0, 0, -1);
        return null;
    }

    private boolean getDamagePlace(BlockPos pos) {
        return placeMode.get() == Safety.Suicide || DamageUtils.bedDamage(mc.player, Utils.vec3d(pos.add(0.5, 0.5, 0.5))) <= maxDamage.get();
    }

    private boolean getDamageBreak(BlockPos pos) {
        return breakMode.get() == Safety.Suicide || DamageUtils.anchorDamage(mc.player, Utils.vec3d(pos.add(0.5, 0.5, 0.5))) <= maxDamage.get();
    }

    private boolean isValidPlace(BlockPos pos) {
        return (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getFluidState().getFluid() instanceof FlowableFluid) && Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pos)) <= placeRange.get() && getDamagePlace(pos);
    }

    private boolean isValidBreak(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR && Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pos)) <= breakRange.get() && getDamageBreak(pos);
    }

    private void breakAnchor(BlockPos pos, FindItemResult anchor, FindItemResult glowStone) {
        if (pos == null || mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) return;

        mc.player.setSneaking(false);

        if (glowStone.isOffhand()) mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
        else {
            InvUtils.swap(glowStone.slot(), true);
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
        }

        if (anchor.isOffhand()) mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
        else {
            InvUtils.swap(anchor.slot(), true);
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
        }

        InvUtils.swapBack();
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum PlaceMode {
        Above("Above"),
        Above_and_Below("Above and Below"),
        All("All");

        private final String title;

        PlaceMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum RotationMode {
        Place("Place"),
        Break("Break"),
        Both("Both"),
        None("None");

        private final String title;

        RotationMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
