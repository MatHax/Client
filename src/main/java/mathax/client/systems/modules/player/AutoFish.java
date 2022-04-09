package mathax.client.systems.modules.player;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.mathax.KeyEvent;
import mathax.client.events.world.PlaySoundEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.Utils;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Items;

public class AutoFish extends Module {
    private boolean ticksEnabled;
    private int ticksToRightClick;
    private int ticksData;

    private int autoCastTimer;
    private boolean autoCastEnabled;

    private int autoCastCheckTimer;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSplashRangeDetection = settings.createGroup("Splash Detection");

    // General

    private final Setting<Boolean> autoCast = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-cast")
        .description("Automatically casts when not fishing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> ticksAutoCast = sgGeneral.add(new IntSetting.Builder()
        .name("ticks-auto-cast")
        .description("The amount of ticks to wait before recasting automatically.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 60)
        .build()
    );

    private final Setting<Integer> ticksCatch = sgGeneral.add(new IntSetting.Builder()
        .name("catch-delay")
        .description("The amount of ticks to wait before catching the fish.")
        .defaultValue(6)
        .min(0)
        .sliderRange(0, 60)
        .build()
    );

    private final Setting<Integer> ticksThrow = sgGeneral.add(new IntSetting.Builder()
        .name("throw-delay")
        .description("The amount of ticks to wait before throwing the bobber.")
        .defaultValue(14)
        .min(0)
        .sliderRange(0, 60)
        .build()
    );

    // Splash Detection

    private final Setting<Boolean> splashDetectionRangeEnabled = sgSplashRangeDetection.add(new BoolSetting.Builder()
        .name("range-enabled")
        .description("Allows you to use multiple accounts next to each other.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> splashDetectionRange = sgSplashRangeDetection.add(new DoubleSetting.Builder()
        .name("range")
        .description("The detection range of a splash. Lower values will not work when the TPS is low.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    public AutoFish() {
        super(Categories.Player, Items.FISHING_ROD, "auto-fish", "Automatically fishes for you.");
    }

    @Override
    public void onActivate() {
        ticksEnabled = false;
        autoCastEnabled = false;
        autoCastCheckTimer = 0;
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        SoundInstance p = event.sound;
        FishingBobberEntity b = mc.player.fishHook;

        if (p.getId().getPath().equals("entity.fishing_bobber.splash")) {
            if (!splashDetectionRangeEnabled.get() || Utils.distance(b.getX(), b.getY(), b.getZ(), p.getX(), p.getY(), p.getZ()) <= splashDetectionRange.get()) {
                ticksEnabled = true;
                ticksToRightClick = ticksCatch.get();
                ticksData = 0;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Auto cast
        if (autoCastCheckTimer <= 0) {
            autoCastCheckTimer = 30;

            if (autoCast.get() && !ticksEnabled && !autoCastEnabled && mc.player.fishHook == null && mc.player.getMainHandStack().getItem() instanceof FishingRodItem) {
                autoCastTimer = 0;
                autoCastEnabled = true;
            }
        } else autoCastCheckTimer--;

        // Check for auto cast timer
        if (autoCastEnabled) {
            autoCastTimer++;

            if (autoCastTimer > ticksAutoCast.get()) {
                autoCastEnabled = false;
                Utils.rightClick();
            }
        }

        // Handle logic
        if (ticksEnabled && ticksToRightClick <= 0) {
            if (ticksData == 0) {
                Utils.rightClick();
                ticksToRightClick = ticksThrow.get();
                ticksData = 1;
            } else if (ticksData == 1) {
                Utils.rightClick();
                ticksEnabled = false;
            }
        }

        ticksToRightClick--;
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc.options.keyUse.isPressed()) ticksEnabled = false;
    }
}
