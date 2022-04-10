package mathax.client.systems.modules.combat;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.eventbus.EventPriority;
import mathax.client.events.entity.EntityAddedEvent;
import mathax.client.events.entity.EntityRemovedEvent;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.render.Render2DEvent;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.mixininterface.IBox;
import mathax.client.mixininterface.IRaycastContext;
import mathax.client.mixininterface.IVec3d;
import mathax.client.renderer.ShapeMode;
import mathax.client.renderer.text.TextRenderer;
import mathax.client.settings.*;
import mathax.client.systems.friends.Friends;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.Utils;
import mathax.client.utils.entity.EntityUtils;
import mathax.client.utils.entity.Target;
import mathax.client.utils.entity.fakeplayer.FakePlayerManager;
import mathax.client.utils.misc.KeyBind;
import mathax.client.utils.misc.Vec3;
import mathax.client.utils.player.*;
import mathax.client.utils.render.NametagUtils;
import mathax.client.utils.render.color.Color;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.utils.world.BlockIterator;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CrystalAura extends Module {
    private final Box box = new Box(0, 0, 0, 0, 0, 0);

    private final Vec3d lastRotationPos = new Vec3d(0, 0 ,0);
    private final Vec3d playerEyePos = new Vec3d(0, 0, 0);
    private final Vec3d vec3dRayTraceEnd = new Vec3d(0, 0, 0);
    private final Vec3d vec3d = new Vec3d(0, 0, 0);
    private final Vec3 vec3 = new Vec3();

    private final BlockPos.Mutable placingCrystalBlockPos = new BlockPos.Mutable();
    private final BlockPos.Mutable breakRenderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private final Int2IntMap waitingToExplode = new Int2IntOpenHashMap();
    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();

    private final List<LivingEntity> targets = new ArrayList<>();

    private final IntSet placedCrystals = new IntOpenHashSet();
    private final IntSet removed = new IntOpenHashSet();

    private RaycastContext raycastContext;

    private LivingEntity bestTarget;

    private double bestTargetDamage;
    private double renderDamage;
    private double lastPitch;
    private double serverYaw;
    private double lastYaw;

    private boolean didRotateThisTick;
    private boolean isLastRotationPos;
    private boolean placing;

    private int lastRotationTimer;
    private int breakRenderTimer;
    private int bestTargetTimer;
    private int placingTimer;
    private int renderTimer;
    private int switchTimer;
    private int ticksPassed;
    private int placeTimer;
    private int breakTimer;
    private int attacks;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgFacePlace = settings.createGroup("Face Place");
    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Determines which entities to attack.")
        .defaultValue(Utils.asO2BMap(EntityType.PLAYER))
        .onlyAttackable()
        .build()
    );

    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Range in which to target entities.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 16)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predicts target movement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-terrain")
        .description("Completely ignores terrain if it can be blown up by end crystals.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("Minimum damage the crystal needs to deal to your target.")
        .defaultValue(6)
        .range(0, 36)
        .sliderRange(0, 36)
        .build()
    );

    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-damage")
        .description("Maximum damage crystals can deal to yourself.")
        .defaultValue(6)
        .range(0, 36)
        .sliderRange(0, 36)
        .build()
    );

    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
        .name("auto-switch")
        .description("Switches to crystals in your hotbar once a target is found.")
        .defaultValue(AutoSwitchMode.Normal)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates server-side towards the crystals being hit/placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<YawStepMode> yawStepMode = sgGeneral.add(new EnumSetting.Builder<YawStepMode>()
        .name("yaw-steps-mode")
        .description("When to run the yaw steps check.")
        .defaultValue(YawStepMode.Break)
        .visible(rotate::get)
        .build()
    );

    private final Setting<Double> yawSteps = sgGeneral.add(new DoubleSetting.Builder()
        .name("yaw-steps")
        .description("Maximum number of degrees its allowed to rotate in one tick.")
        .defaultValue(180)
        .range(1, 180)
        .sliderRange(1, 180)
        .visible(rotate::get)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break crystals if they will kill you.")
        .defaultValue(true)
        .build()
    );

    // Place

    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder()
        .name("place")
        .description("Determines if should Crystal Aura place crystals.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minPlaceDamage = sgPlace.add(new DoubleSetting.Builder()
        .name("min-place-damage")
        .description("Minimum place damage the crystal needs to deal to your target.")
        .defaultValue(6)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The delay to wait to place a crystal after it's exploded in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Range in which to place crystals.")
        .defaultValue(4.5)
        .min(0)
        .sliderRange(0, 6)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-walls-range")
        .description("Range in which to place crystals when behind blocks.")
        .defaultValue(4.5)
        .min(0)
        .sliderRange(0, 6)
        .build()
    );

    private final Setting<Boolean> placement112 = sgPlace.add(new BoolSetting.Builder()
        .name("1.12-placement")
        .description("Uses 1.12 crystal placement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SupportMode> support = sgPlace.add(new EnumSetting.Builder<SupportMode>()
        .name("support")
        .description("Places a support block in air if no other position have been found.")
        .defaultValue(SupportMode.Disabled)
        .build()
    );

    private final Setting<Integer> supportDelay = sgPlace.add(new IntSetting.Builder()
        .name("support-delay")
        .description("Delay after placing support block in ticks.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> support.get() != SupportMode.Disabled)
        .build()
    );

    // Face place

    private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder()
        .name("face-place")
        .description("Will face-place when target is below a certain health or armor durability threshold.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> slowFacePlace = sgFacePlace.add(new BoolSetting.Builder()
        .name("slow-face-place")
        .description("Will slow down face-place to save crystals.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SlowFacePlace> slowFacePlaceMode = sgFacePlace.add(new EnumSetting.Builder<SlowFacePlace>()
        .name("slow-face-place-mode")
        .description("Determines how slow face-place operates.")
        .defaultValue(SlowFacePlace.Auto)
        .visible(slowFacePlace::get)
        .build()
    );

    private final Setting<Integer> slowFacePlaceDelay = sgFacePlace.add(new IntSetting.Builder()
        .name("slow-face-place-delay")
        .description("The delay to wait to break a crystal for custom face place delay in ticks.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(() -> slowFacePlace.get() && slowFacePlaceMode.get() == SlowFacePlace.Custom)
        .build()
    );

    private final Setting<Boolean> greenHolers = sgFacePlace.add(new BoolSetting.Builder()
        .name("green-holers")
        .description("Will automatically face-place when target is in greenhole.")
        .defaultValue(false)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Boolean> faceSurrounded = sgFacePlace.add(new BoolSetting.Builder()
        .name("face-surrounded")
        .description("Will face-place even when target's face is surrounded.")
        .defaultValue(false)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder()
        .name("face-place-health")
        .description("The health the target has to be at to start face-placing.")
        .defaultValue(8)
        .range(1, 36)
        .sliderRange(1, 36)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Double> facePlaceDurability = sgFacePlace.add(new DoubleSetting.Builder()
        .name("face-place-durability")
        .description("The durability threshold percentage to be able to face-place.")
        .defaultValue(2)
        .range(1, 100)
        .sliderRange(1, 100)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder()
        .name("face-place-missing-armor")
        .description("Automatically starts face-placing when a target misses a piece of armor.")
        .defaultValue(false)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<KeyBind> forceFacePlace = sgFacePlace.add(new KeyBindSetting.Builder()
        .name("force-face-place")
        .description("Starts face-place when this button is pressed.")
        .build()
    );

    private final Setting<Boolean> surroundHoldPause = sgFacePlace.add(new BoolSetting.Builder()
        .name("pause-on-surround-hold")
        .description("Will pause face-placing when surround hold is active.")
        .defaultValue(true)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Boolean> killAuraPause = sgFacePlace.add(new BoolSetting.Builder()
        .name("pause-on-kill-aura")
        .description("Will pause face-placing when Kill Aura is active.")
        .defaultValue(true)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Boolean> cevPause = sgFacePlace.add(new BoolSetting.Builder()
        .name("pause-on-cev-break")
        .description("Will pause face-placing when CEV Breaker is active.")
        .defaultValue(false)
        .visible(facePlace::get)
        .build()
    );

    // Surround

    private final Setting<Boolean> surroundBreak = sgSurround.add(new BoolSetting.Builder()
        .name("surround-break")
        .description("Will automatically places a crystal next to target's surround.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SurroundBreakHoldWhen> surroundBreakWhen = sgSurround.add(new EnumSetting.Builder<SurroundBreakHoldWhen>()
        .name("surround-break-when")
        .description("When to start surround breaking.")
        .defaultValue(SurroundBreakHoldWhen.Face_Trapped)
        .visible(surroundBreak::get)
        .build()
    );

    private final Setting<Boolean> facePlacePause = sgSurround.add(new BoolSetting.Builder()
        .name("pause-while-face-placing")
        .description("Will pause surround breaking while face-placing targets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreBurrowed = sgSurround.add(new BoolSetting.Builder()
        .name("ignore-burrowed")
        .description("Will not try to surround break targets that are burrowed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> surroundHold = sgSurround.add(new BoolSetting.Builder()
        .name("surround-hold")
        .description("Break crystals slower to hold on to their surround when their surround is broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SurroundBreakHoldWhen> surroundHoldWhen = sgSurround.add(new EnumSetting.Builder<SurroundBreakHoldWhen>()
        .name("surround-hold-when")
        .description("When to start surround holding.")
        .defaultValue(SurroundBreakHoldWhen.Any_Trapped)
        .visible(surroundHold::get)
        .build()
    );

    private final Setting<SurroundHold> surroundHoldMode = sgSurround.add(new EnumSetting.Builder<SurroundHold>()
        .name("surround-hold-mode")
        .description("Determines how Surround Hold operates.")
        .defaultValue(SurroundHold.Auto)
        .visible(surroundHold::get)
        .build()
    );

    private final Setting<Integer> surroundHoldDelay = sgSurround.add(new IntSetting.Builder()
        .name("surround-hold-delay")
        .description("The delay to wait to break a crystal for custom surround hold in ticks.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(() -> surroundHold.get() && surroundHoldMode.get() == SurroundHold.Custom)
        .build()
    );

    // Break

    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder()
        .name("break")
        .description("Determines if should Crystal Aura break crystals.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minBreakDamage = sgBreak.add(new DoubleSetting.Builder()
        .name("min-break-damage")
        .description("Minimum break damage the crystal needs to deal to your target.")
        .defaultValue(6)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The delay to wait to break a crystal after it's placed in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> smartDelay = sgBreak.add(new BoolSetting.Builder()
        .name("smart-delay")
        .description("Only breaks crystals when the target can receive damage.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> switchDelay = sgBreak.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("The delay to wait to break a crystal after switching hotbar slot in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 60)
        .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("Range in which to break crystals.")
        .defaultValue(4.5)
        .min(0)
        .sliderRange(0, 6)
        .build()
    );

    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-walls-range")
        .description("Range in which to break crystals when behind blocks.")
        .defaultValue(4.5)
        .min(0)
        .sliderRange(0, 6)
        .build()
    );

    private final Setting<Boolean> onlyBreakOwn = sgBreak.add(new BoolSetting.Builder()
        .name("only-own")
        .description("Only breaks own crystals.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder()
        .name("break-attempts")
        .description("How many times to hit a crystal before stopping to target it.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Integer> ticksExisted = sgBreak.add(new IntSetting.Builder()
        .name("ticks-existed")
        .description("Amount of ticks a crystal needs to have lived for it to be attacked by Crystal Aura.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 60)
        .build()
    );

    private final Setting<Integer> attackFrequency = sgBreak.add(new IntSetting.Builder()
        .name("attack-frequency")
        .description("Maximum hits to do per second.")
        .defaultValue(25)
        .min(1)
        .sliderRange(1, 30)
        .build()
    );

    private final Setting<Boolean> fastBreak = sgBreak.add(new BoolSetting.Builder()
        .name("fast-break")
        .description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiWeakness = sgBreak.add(new BoolSetting.Builder()
        .name("anti-weakness")
        .description("Switches to tools with high enough damage to explode the crystal with weakness effect.")
        .defaultValue(true)
        .build()
    );

    // Pause

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses Crystal Aura when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses Crystal Aura when drinking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses Crystal Aura when mining.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings your hand client-side when placing or interacting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay over the block the crystals are being placed on.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
        .name("break")
        .description("Renders a block overlay over the block the crystals are broken on.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Determines how the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the block overlay.")
        .defaultValue(new SettingColor(255, 255, 255, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the block overlay.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder()
        .name("damage")
        .description("Renders crystal damage text in the block overlay.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder()
        .name("damage-scale")
        .description("Determines how big the damage text should be.")
        .defaultValue(1.25)
        .min(1)
        .sliderRange(1, 4)
        .visible(renderDamageText::get)
        .build()
    );

    private final Setting<ColorMode> textColorMode = sgRender.add(new EnumSetting.Builder<ColorMode>()
        .name("text-color-mode")
        .description("Determines the color mode of the damage text.")
        .defaultValue(ColorMode.Damage)
        .build()
    );

    private final Setting<Integer> textColorDamageA = sgRender.add(new IntSetting.Builder()
        .name("text-color-alpha")
        .description("The alpha channel value of damage text of the block overlay.")
        .defaultValue(255)
        .range(0, 255)
        .sliderRange(0, 255)
        .visible(() -> textColorMode.get() == ColorMode.Damage)
        .build()
    );

    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder()
        .name("text-color")
        .description("The damage text color of the block overlay.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 255))
        .visible(() -> textColorMode.get() == ColorMode.Static)
        .build()
    );

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
        .name("render-time")
        .description("How long to render for.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> renderBreakTime = sgRender.add(new IntSetting.Builder()
        .name("break-time")
        .description("How long to render breaking for.")
        .defaultValue(13)
        .min(0)
        .sliderRange(0, 20)
        .visible(renderBreak::get)
        .build()
    );

    public CrystalAura() {
        super(Categories.Combat, Items.END_CRYSTAL, "crystal-aura", "Automatically places and attacks crystals.");
    }

    @Override
    public void onActivate() {
        breakTimer = 0;
        placeTimer = 0;
        ticksPassed = 0;

        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        placing = false;
        placingTimer = 0;

        attacks = 0;

        serverYaw = mc.player.getYaw();

        bestTargetDamage = 0;
        bestTargetTimer = 0;

        lastRotationTimer = getLastRotationStopDelay();

        renderTimer = 0;
        breakRenderTimer = 0;
    }

    @Override
    public void onDeactivate() {
        targets.clear();

        placedCrystals.clear();

        attemptedBreaks.clear();
        waitingToExplode.clear();

        removed.clear();

        bestTarget = null;
    }

    private int getLastRotationStopDelay() {
        return Math.max(10, placeDelay.get() / 2 + breakDelay.get() / 2 + 10);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {
        didRotateThisTick = false;
        lastRotationTimer++;

        if (placing) {
            if (placingTimer > 0) placingTimer--;
            else placing = false;
        }

        if (ticksPassed < 20) ticksPassed++;
        else {
            ticksPassed = 0;
            attacks = 0;
        }

        if (bestTargetTimer > 0) bestTargetTimer--;
        bestTargetDamage = 0;

        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (switchTimer > 0) switchTimer--;

        if (renderTimer > 0) renderTimer--;
        if (breakRenderTimer > 0) breakRenderTimer--;

        for (IntIterator it = waitingToExplode.keySet().iterator(); it.hasNext();) {
            int id = it.nextInt();
            int ticks = waitingToExplode.get(id);

            if (ticks > 3) {
                it.remove();
                removed.remove(id);
            } else waitingToExplode.put(id, ticks + 1);
        }

        if (PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get())) return;

        ((IVec3d) playerEyePos).set(mc.player.getPos().x, mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getPos().z);

        findTargets();

        if (targets.size() > 0) {
            if (!didRotateThisTick) doBreak();
            if (!didRotateThisTick) doPlace();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST - 666)
    private void onPreTickLast(TickEvent.Pre event) {
        if (rotate.get() && lastRotationTimer < getLastRotationStopDelay() && !didRotateThisTick) Rotations.rotate(isLastRotationPos ? Rotations.getYaw(lastRotationPos) : lastYaw, isLastRotationPos ? Rotations.getPitch(lastRotationPos) : lastPitch, -100, null);
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (placing && event.entity.getBlockPos().equals(placingCrystalBlockPos)) {
            placing = false;
            placingTimer = 0;
            placedCrystals.add(event.entity.getId());
        }

        if (fastBreak.get() && !didRotateThisTick && attacks < attackFrequency.get()) {
            if (!isSurroundHolding() || (facePlace.get() && slowFacePlace.get() && (bestTarget.getY() < placingCrystalBlockPos.getY()))) {
                double damage = getBreakDamage(event.entity, true);
                if (damage > minBreakDamage.get()) doBreak(event.entity);
            }
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity) {
            placedCrystals.remove(event.entity.getId());
            removed.remove(event.entity.getId());
            waitingToExplode.remove(event.entity.getId());
        }
    }

    private void setRotation(boolean isPos, Vec3d pos, double yaw, double pitch) {
        didRotateThisTick = true;
        isLastRotationPos = isPos;

        if (isPos) ((IVec3d) lastRotationPos).set(pos.x, pos.y, pos.z);
        else {
            lastYaw = yaw;
            lastPitch = pitch;
        }

        lastRotationTimer = 0;
    }

    private void getDelay() {
        if (isSurroundHolding()) {
            if (surroundHoldMode.get() == SurroundHold.Auto) breakTimer = 10;
            else if (surroundHoldMode.get() == SurroundHold.Custom) breakTimer = surroundHoldDelay.get();
        } else if (facePlace.get() && slowFacePlace.get() && (bestTarget.getY() < placingCrystalBlockPos.getY())) {
            if (slowFacePlaceMode.get() == SlowFacePlace.Auto) breakTimer = 10;
            else if (slowFacePlaceMode.get() == SlowFacePlace.Custom) breakTimer = slowFacePlaceDelay.get();
        } else breakTimer = breakDelay.get();
    }

    private void doBreak() {
        if (!doBreak.get() || breakTimer > 0 || switchTimer > 0 || attacks >= attackFrequency.get()) return;

        double bestDamage = 0;
        Entity crystal = null;

        for (Entity entity : mc.world.getEntities()) {
            double damage = getBreakDamage(entity, true);

            if (damage > bestDamage) {
                bestDamage = damage;
                crystal = entity;
            }
        }

        if (crystal != null) doBreak(crystal);
    }

    private void doBreak(Entity crystal) {
        if (antiWeakness.get()) {
            StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);

            if (weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())) {
                if (!isValidWeaknessItem(mc.player.getMainHandStack())) {
                    if (!InvUtils.swap(InvUtils.findInHotbar(this::isValidWeaknessItem).slot(), false)) return;

                    switchTimer = 1;
                    return;
                }
            }
        }

        boolean attacked = true;

        if (!rotate.get()) {
            attackCrystal(crystal);
            getDelay();
        } else {
            double yaw = Rotations.getYaw(crystal);
            double pitch = Rotations.getPitch(crystal, Target.Feet);

            if (doYawSteps(yaw, pitch)) {
                setRotation(true, crystal.getPos(), 0, 0);
                Rotations.rotate(yaw, pitch, 50, () -> attackCrystal(crystal));
                getDelay();
            } else {
                attacked = false;
            }
        }

        if (attacked) {
            removed.add(crystal.getId());
            attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);
            waitingToExplode.put(crystal.getId(), 0);

            breakRenderPos.set(crystal.getBlockPos().down());
            breakRenderTimer = renderBreakTime.get();
        }
    }

    private double getBreakDamage(Entity entity, boolean checkCrystalAge) {
        if (!(entity instanceof EndCrystalEntity)) return 0;

        if (onlyBreakOwn.get() && !placedCrystals.contains(entity.getId())) return 0;

        if (removed.contains(entity.getId())) return 0;

        if (attemptedBreaks.get(entity.getId()) > breakAttempts.get()) return 0;

        if (checkCrystalAge && entity.age < ticksExisted.get()) return 0;

        if (isOutOfRange(entity.getPos(), entity.getBlockPos(), false)) return 0;

        blockPos.set(entity.getBlockPos()).move(0, -1, 0);
        double selfDamage = DamageUtils.crystalDamage(mc.player, entity.getPos(), predictMovement.get(), blockPos, ignoreTerrain.get());
        if (selfDamage > maxDamage.get() || (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player))) return 0;

        double damage = getDamageToTargets(entity.getPos(), blockPos, true, false);
        boolean facePlaced = (facePlace.get() && shouldFacePlace(entity.getBlockPos()) || forceFacePlace.get().isPressed());

        if (!facePlaced && damage < minDamage.get()) return 0;

        return damage;
    }

    private boolean isValidWeaknessItem(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ToolItem) || itemStack.getItem() instanceof HoeItem) return false;

        ToolMaterial material = ((ToolItem) itemStack.getItem()).getMaterial();
        return material == ToolMaterials.DIAMOND || material == ToolMaterials.NETHERITE;
    }

    private void attackCrystal(Entity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));

        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
        if (hand == null) hand = Hand.MAIN_HAND;

        if (swing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        attacks++;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) switchTimer = switchDelay.get();
    }

    private void doPlace() {
        if (!doPlace.get() || placeTimer > 0) return;

        if (!InvUtils.findInHotbar(Items.END_CRYSTAL).found()) return;

        if (autoSwitch.get() == AutoSwitchMode.None && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;

        for (Entity entity : mc.world.getEntities()) {
            if (getBreakDamage(entity, false) > 0) return;
        }

        AtomicDouble bestDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBlockPos = new AtomicReference<>(new BlockPos.Mutable());
        AtomicBoolean isSupport = new AtomicBoolean(support.get() != SupportMode.Disabled);

        BlockIterator.register((int) Math.ceil(placeRange.get()), (int) Math.ceil(placeRange.get()), (bp, blockState) -> {
            boolean hasBlock = blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN);
            if (!hasBlock && (!isSupport.get() || !blockState.getMaterial().isReplaceable())) return;

            blockPos.set(bp.getX(), bp.getY() + 1, bp.getZ());
            if (!mc.world.getBlockState(blockPos).isAir()) return;

            if (placement112.get()) {
                blockPos.move(0, 1, 0);
                if (!mc.world.getBlockState(blockPos).isAir()) return;
            }

            ((IVec3d) vec3d).set(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
            blockPos.set(bp).move(0, 1, 0);
            if (isOutOfRange(vec3d, blockPos, true)) return;

            double selfDamage = DamageUtils.crystalDamage(mc.player, vec3d, predictMovement.get(), bp, ignoreTerrain.get());
            if (selfDamage > maxDamage.get() || (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player))) return;

            double damage = getDamageToTargets(vec3d, bp, false, !hasBlock && support.get() == SupportMode.Fast);

            boolean facePlaced = (facePlace.get() && shouldFacePlace(blockPos)) || (forceFacePlace.get().isPressed());

            if (!facePlaced && damage < minDamage.get()) return;

            boolean surroundBreaking = (isSurroundBreaking() && shouldSurroundBreak(blockPos));

            if ((!facePlaced && !surroundBreaking) && damage < minPlaceDamage.get()) return;

            double x = bp.getX();
            double y = bp.getY() + 1;
            double z = bp.getZ();
            ((IBox) box).set(x, y, z, x + 1, y + (placement112.get() ? 1 : 2), z + 1);

            if (intersectsWithEntities(box)) return;

            if (damage > bestDamage.get() || (isSupport.get() && hasBlock)) {
                bestDamage.set(damage);
                bestBlockPos.get().set(bp);
            }

            if (hasBlock) isSupport.set(false);
        });

        BlockIterator.after(() -> {
            if (bestDamage.get() == 0) return;

            BlockHitResult result = getPlaceInfo(bestBlockPos.get());

            ((IVec3d) vec3d).set(
                result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 1.0 / 2.0,
                result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 1.0 / 2.0,
                result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 1.0 / 2.0
            );

            if (rotate.get()) {
                double yaw = Rotations.getYaw(vec3d);
                double pitch = Rotations.getPitch(vec3d);

                if (yawStepMode.get() == YawStepMode.Break || doYawSteps(yaw, pitch)) {
                    setRotation(true, vec3d, 0, 0);
                    Rotations.rotate(yaw, pitch, 50, () -> placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null));

                    placeTimer += placeDelay.get();
                }
            } else {
                placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null);
                placeTimer += placeDelay.get();
            }
        });
    }

    private BlockHitResult getPlaceInfo(BlockPos blockPos) {
        ((IVec3d) vec3d).set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        for (Direction side : Direction.values()) {
            ((IVec3d) vec3dRayTraceEnd).set(
                blockPos.getX() + 0.5 + side.getVector().getX() * 0.5,
                blockPos.getY() + 0.5 + side.getVector().getY() * 0.5,
                blockPos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );

            ((IRaycastContext) raycastContext).set(vec3d, vec3dRayTraceEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);

            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) return result;
        }

        Direction side = blockPos.getY() > vec3d.y ? Direction.DOWN : Direction.UP;
        return new BlockHitResult(vec3d, side, blockPos, false);
    }

    private void placeCrystal(BlockHitResult result, double damage, BlockPos supportBlock) {
        Item targetItem = supportBlock == null ? Items.END_CRYSTAL : Items.OBSIDIAN;

        FindItemResult item = InvUtils.findInHotbar(targetItem);
        if (!item.found()) return;

        int prevSlot = mc.player.getInventory().selectedSlot;

        if (autoSwitch.get() != AutoSwitchMode.None && !item.isOffhand()) InvUtils.swap(item.slot(), false);

        Hand hand = item.getHand();
        if (hand == null) return;

        if (supportBlock == null) {
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

            if (swing.get()) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

            placing = true;
            placingTimer = 4;
            placingCrystalBlockPos.set(result.getBlockPos()).move(0, 1, 0);

            renderTimer = renderTime.get();
            renderPos.set(result.getBlockPos());
            renderDamage = damage;
        } else {
            BlockUtils.place(supportBlock, item, false, 0, swing.get(), true, false);
            placeTimer += supportDelay.get();

            if (supportDelay.get() == 0) placeCrystal(result, damage, null);
        }

        if (autoSwitch.get() == AutoSwitchMode.Silent) InvUtils.swap(prevSlot, false);
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket) serverYaw = ((PlayerMoveC2SPacket) event.packet).getYaw((float) serverYaw);
    }

    public boolean doYawSteps(double targetYaw, double targetPitch) {
        targetYaw = MathHelper.wrapDegrees(targetYaw) + 180;
        double serverYaw = MathHelper.wrapDegrees(this.serverYaw) + 180;

        if (distanceBetweenAngles(serverYaw, targetYaw) <= yawSteps.get()) return true;

        double delta = Math.abs(targetYaw - serverYaw);
        double yaw = this.serverYaw;

        if (serverYaw < targetYaw) {
            if (delta < 180) yaw += yawSteps.get();
            else yaw -= yawSteps.get();
        } else {
            if (delta < 180) yaw -= yawSteps.get();
            else yaw += yawSteps.get();
        }

        setRotation(false, null, yaw, targetPitch);
        Rotations.rotate(yaw, targetPitch, -100, null);
        return false;
    }

    private static double distanceBetweenAngles(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;
        return phi > 180 ? 360 - phi : phi;
    }

    private boolean shouldFacePlace(BlockPos crystal) {
        for (LivingEntity target : targets) {
            BlockPos pos = target.getBlockPos();
            if (cevPause.get() && Modules.get().isActive(CEVBreaker.class)) return false;
            if (killAuraPause.get() && (Modules.get().isActive(KillAura.class))) return false;
            if (!faceSurrounded.get() && EntityUtils.isFaceSurrounded(target)) return false;
            if (surroundHoldPause.get() && surroundHold.get() && EntityUtils.isSurroundBroken(target)) return false;

            if (crystal.getY() == pos.getY() + 1 && Math.abs(pos.getX() - crystal.getX()) <= 1 && Math.abs(pos.getZ() - crystal.getZ()) <= 1) {
                if (greenHolers.get() && EntityUtils.isGreenHole(target)) return true;
                if (EntityUtils.getTotalHealth(target) <= facePlaceHealth.get()) return true;

                for (ItemStack itemStack : target.getArmorItems()) {
                    if (itemStack == null || itemStack.isEmpty()) {
                        if (facePlaceArmor.get()) return true;
                    } else {
                        if ((double) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage() * 100 <= facePlaceDurability.get()) return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean shouldSurroundBreak(BlockPos crystal) {
        for (LivingEntity target : targets) {
            if (target != bestTarget) continue;
            BlockPos pos = bestTarget.getBlockPos();
            if (!isSurroundBreaking()) return false;

            if (!EntityUtils.isBedrock(pos.north(1)) && crystal.equals(pos.north(2))) return true;
            if (!EntityUtils.isBedrock(pos.west(1)) && crystal.equals(pos.west(2))) return true;
            if (!EntityUtils.isBedrock(pos.south(1)) && crystal.equals(pos.south(2))) return true;
            if (!EntityUtils.isBedrock(pos.east(1)) && crystal.equals(pos.east(2))) return true;
        }
        return false;
    }

    private boolean isSurroundBreaking() {
        if (surroundBreak.get() && bestTarget != null) {
            if (facePlacePause.get() && shouldFacePlace(blockPos)) return false;
            if (!EntityUtils.isSurrounded(bestTarget)) return false;
            if (EntityUtils.isGreenHole(bestTarget)) return false;
            if (ignoreBurrowed.get() && EntityUtils.isBurrowed(bestTarget)) return false;
            if (surroundBreakWhen.get() == SurroundBreakHoldWhen.Both_Trapped && (EntityUtils.isTopTrapped(bestTarget) && EntityUtils.isFaceSurrounded(bestTarget))) return true;
            else if (surroundBreakWhen.get() == SurroundBreakHoldWhen.Any_Trapped && (EntityUtils.isTopTrapped(bestTarget) || EntityUtils.isFaceSurrounded(bestTarget))) return true;
            else if (surroundBreakWhen.get() == SurroundBreakHoldWhen.Top_Trapped && EntityUtils.isTopTrapped(bestTarget)) return true;
            else if (surroundBreakWhen.get() == SurroundBreakHoldWhen.Face_Trapped && EntityUtils.isFaceSurrounded(bestTarget)) return true;
            else return surroundBreakWhen.get() == SurroundBreakHoldWhen.Always;
        }

        return false;
    }

    private boolean isSurroundHolding() {
        if (surroundHold.get() && bestTarget != null && EntityUtils.isSurroundBroken(bestTarget)) {
            if (surroundHoldWhen.get() == SurroundBreakHoldWhen.Both_Trapped && (EntityUtils.isTopTrapped(bestTarget) && EntityUtils.isFaceSurrounded(bestTarget))) return true;
            else if (surroundHoldWhen.get() == SurroundBreakHoldWhen.Any_Trapped && (EntityUtils.isTopTrapped(bestTarget) || EntityUtils.isFaceSurrounded(bestTarget))) return true;
            else if (surroundHoldWhen.get() == SurroundBreakHoldWhen.Top_Trapped && EntityUtils.isTopTrapped(bestTarget)) return true;
            else if (surroundHoldWhen.get() == SurroundBreakHoldWhen.Face_Trapped && EntityUtils.isFaceSurrounded(bestTarget)) return true;
            else return surroundHoldWhen.get() == SurroundBreakHoldWhen.Always;
        }

        return false;
    }

    private boolean isOutOfRange(Vec3d vec3d, BlockPos blockPos, boolean place) {
        ((IRaycastContext) raycastContext).set(playerEyePos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        BlockHitResult result = mc.world.raycast(raycastContext);
        boolean behindWall = result == null || !result.getBlockPos().equals(blockPos);
        double distance = mc.player.getPos().distanceTo(vec3d);

        return distance > (behindWall ? (place ? placeWallsRange : breakWallsRange).get() : (place ? placeRange : breakRange).get());
    }

    private LivingEntity getNearestTarget() {
        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity target : targets) {
            double distance = target.squaredDistanceTo(mc.player);
            if (distance < nearestDistance) {
                nearestTarget = target;
                nearestDistance = distance;
            }
        }

        return nearestTarget;
    }

    private double getDamageToTargets(Vec3d vec3d, BlockPos obsidianPos, boolean breaking, boolean fast) {
        double damage = 0;

        if (fast) {
            LivingEntity target = getNearestTarget();
            if (!(smartDelay.get() && breaking && target.hurtTime > 0)) damage = DamageUtils.crystalDamageLivingEntity(target, vec3d, predictMovement.get(), obsidianPos, ignoreTerrain.get());
        } else {
            for (LivingEntity target : targets) {
                if (smartDelay.get() && breaking && target.hurtTime > 0) continue;

                double dmg = DamageUtils.crystalDamageLivingEntity(target, vec3d, predictMovement.get(), obsidianPos, ignoreTerrain.get());

                if (dmg > bestTargetDamage) {
                    bestTarget = target;
                    bestTargetDamage = dmg;
                    bestTargetTimer = 10;
                }

                damage += dmg;
            }
        }

        return damage;
    }

    @Override
    public String getInfoString() {
        if (bestTarget != null && bestTarget instanceof PlayerEntity bestPlayerTarget) return bestTargetTimer > 0 ? bestPlayerTarget.getGameProfile().getName() : null;
        if (bestTarget != null) return bestTargetTimer > 0 ? bestTarget.getType().getName().getString() : null;
        return null;
    }

    private void findTargets() {
        targets.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity || !entities.get().containsKey(entity.getType())) continue;

            if (entity instanceof LivingEntity livingEntity && entity.isAlive() && entity.distanceTo(mc.player) <= targetRange.get()) targets.add(livingEntity);
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!entities.get().containsKey(EntityType.PLAYER) || player.getAbilities().creativeMode || player == mc.player) continue;

            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) targets.add(player);
        }

        for (PlayerEntity player : FakePlayerManager.getPlayers()) {
            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) targets.add(player);
        }
    }

    public PlayerEntity getPlayerTarget() {
        if (bestTarget == null) return null;
        if (bestTarget instanceof PlayerEntity player) return player;
        else return null;
    }

    private boolean intersectsWithEntities(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !removed.contains(entity.getId()));
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (renderTimer > 0 && render.get()) event.renderer.box(renderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        if (breakRenderTimer > 0 && renderBreak.get() && !mc.world.getBlockState(breakRenderPos).isAir()) {
            int preSideA = sideColor.get().a;
            sideColor.get().a -= 20;
            sideColor.get().validate();

            int preLineA = lineColor.get().a;
            lineColor.get().a -= 20;
            lineColor.get().validate();

            event.renderer.box(breakRenderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

            sideColor.get().a = preSideA;
            lineColor.get().a = preLineA;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || renderTimer <= 0 || !renderDamageText.get()) return;

        vec3.set(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5);

        if (NametagUtils.to2D(vec3, damageTextScale.get())) {
            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1, false, true);

            String text = String.format("%.1f", renderDamage);
            double w = TextRenderer.get().getWidth(text) / 2;
            if (textColorMode.get() == ColorMode.Damage) TextRenderer.get().render(text, -w, 0, getDamageTextColor(renderDamage), true);
            else TextRenderer.get().render(text, -w, 0, textColor.get(), true);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    private Color getDamageTextColor(double renderDamage) {
        if (renderDamage < 7) return new Color(255, 0, 0, textColorDamageA.get());
        else if (renderDamage < 17) return new Color(255, 255, 0, textColorDamageA.get());
        else return new Color(0, 255, 0, textColorDamageA.get());
    }

    public enum ColorMode {
        Damage("Damage"),
        Static("Static");

        private final String title;

        ColorMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum YawStepMode {
        Break("Break"),
        All("All");

        private final String title;

        YawStepMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum AutoSwitchMode {
        Normal("Normal"),
        Silent("Silent"),
        None("None");

        private final String title;

        AutoSwitchMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum SupportMode {
        Disabled("Disabled"),
        Accurate("Accurate"),
        Fast("Fast");

        private final String title;

        SupportMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum SurroundHold {
        Auto("Auto"),
        Custom("Custom");

        private final String title;

        SurroundHold(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum SurroundBreakHoldWhen {
        Always("Always"),
        Top_Trapped("Top Trapped"),
        Face_Trapped("Face Trapped"),
        Both_Trapped("Both Trapped"),
        Any_Trapped("Any Trapped");

        private final String title;

        SurroundBreakHoldWhen(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum SlowFacePlace {
        Auto("Auto"),
        Custom("Custom");

        private final String title;

        SlowFacePlace(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
