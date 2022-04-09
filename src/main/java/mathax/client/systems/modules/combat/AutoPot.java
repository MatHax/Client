package mathax.client.systems.modules.combat;

import baritone.api.BaritoneAPI;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.entity.player.ItemUseCrosshairTargetEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.Utils;
import mathax.client.utils.player.Rotations;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*/--------------------------------------------------------------------------------------------------------------/*/
/*/ Taken from Meteor Rejects and edited by Matejko06                                                            /*/
/*/ https://github.com/AntiCope/meteor-rejects/blob/master/src/main/java/anticope/rejects/modules/AutoPot.java /*/
/*/--------------------------------------------------------------------------------------------------------------/*/

public class AutoPot extends Module {
    private final List<Class<? extends Module>> wasAura = new ArrayList<>();
    private static final Class<? extends Module>[] AURA_LIST = new Class[] {
        KillAura.class,
        CrystalAura.class,
        AnchorAura.class,
        BedAura.class
    };

    private float prevPitch;

    private int slot, prevSlot;

    private boolean drinking, splashing, pitched;
    private boolean wasBaritone;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgRotation = settings.createGroup("Rotation");

    // General

    private final Setting<Boolean> healing = sgGeneral.add(new BoolSetting.Builder()
        .name("healing")
        .description("Enables healing potions.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("If health goes below this point, Healing Pot will trigger.")
        .defaultValue(15)
        .min(1)
        .sliderRange(1, 36)
        .build()
    );

    private final Setting<Boolean> strength = sgGeneral.add(new BoolSetting.Builder()
        .name("strength")
        .description("Enables strength potions.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useSplashPots = sgGeneral.add(new BoolSetting.Builder()
        .name("splash-pots")
        .description("Allow the use of splash pots")
        .defaultValue(true)
        .build()
    );

    // Automations

    private final Setting<Boolean> pauseAuras = sgAutomation.add(new BoolSetting.Builder()
        .name("pause-auras")
        .description("Pauses all auras when eating.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseBaritone = sgAutomation.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Pause baritone when eating.")
        .defaultValue(true)
        .build()
    );

    // Rotation

    private final Setting<Boolean> rotate = sgRotation.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Forces you to rotate downwards when throwing bottles.")
        .defaultValue(true)
        .build()
    );

    private final Setting<RotateMode> rotateMode = sgRotation.add(new EnumSetting.Builder<RotateMode>()
        .name("mode")
        .description("Determines how to rotate.")
        .defaultValue(RotateMode.Server)
        .build()
    );

    public final Setting<Double> splashingPitch = sgRotation.add(new DoubleSetting.Builder()
        .name("splashing")
        .description("The pitch when you are splashing potions.")
        .defaultValue(90)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .build()
    );

    private final Setting<Boolean> previousPitch = sgRotation.add(new BoolSetting.Builder()
        .name("previous")
        .description("Returns back to previous Pitch when finished splashing.")
        .defaultValue(true)
        .visible(() -> rotateMode.get() == RotateMode.Client)
        .build()
    );

    public final Setting<Double> stoppedPitch = sgRotation.add(new DoubleSetting.Builder()
        .name("stopped")
        .description("The pitch when stopping splashing.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> rotateMode.get() == RotateMode.Client && !previousPitch.get())
        .build()
    );

    public AutoPot() {
        super(Categories.Combat, Items.POTION, "auto-pot", "Automatically uses potions.");
    }

    @Override
    public void onDeactivate() {
        if (drinking) stopDrinking();
        if (splashing) stopSplashing();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (healing.get() && !strength.get() && !shouldDrinkHealth() && splashing) {
            stopSplashing();
            return;
        }

        if (healing.get()) {
            if (shouldDrinkHealth()) {
                int slot = healingPotionSlot();

                if (slot != -1) startDrinking();
                else if (healingPotionSlot() == -1 && useSplashPots.get()) {
                    slot = healingSplashPotionSlot();
                    if (slot != -1) startSplashing();
                }
            }

            if (drinking) {
                if (shouldDrinkHealth()) {
                    if (isNotPotion(mc.player.getInventory().getStack(slot))) {
                        slot = healingPotionSlot();
                        if (slot == -1) {
                            stopDrinking();
                            return;
                        }
                    } else changeSlot(slot);
                }

                drink();

                if (shouldNotDrinkHealth()) {
                    stopDrinking();
                    return;
                }
            }

            if (splashing) {
                if (shouldDrinkHealth()) {
                    if (isNotSplashPotion(mc.player.getInventory().getStack(slot))) {
                        slot = healingSplashPotionSlot();
                        if (slot == -1) {
                            stopSplashing();
                            return;
                        } else changeSlot(slot);
                    }

                    splash();

                    if (shouldNotDrinkHealth()) {
                        stopSplashing();
                        return;
                    }
                }
            }
        }

        if (strength.get()) {
            if (shouldDrinkStrength()) {
                int slot = strengthPotionSlot();

                if (slot != -1) startDrinking();
                else if (strengthPotionSlot() == -1 && useSplashPots.get()) {
                    slot = strengthSplashPotionSlot();
                    if (slot != -1) startSplashing();
                }
            }

            if (drinking) {
                if (shouldDrinkStrength()) {
                    if (isNotPotion(mc.player.getInventory().getStack(slot))) {
                        slot = strengthPotionSlot();
                        if (slot == -1) {
                            stopDrinking();
                            return;
                        } else changeSlot(slot);
                    }

                    drink();
                } else stopDrinking();
            }

            if (splashing) {
                if (shouldDrinkStrength()) {
                    if (isNotSplashPotion(mc.player.getInventory().getStack(slot))) {
                        slot = strengthSplashPotionSlot();
                        if (slot == -1) {
                            stopSplashing();
                            return;
                        } else changeSlot(slot);
                    }

                    splash();
                } else stopSplashing();
            }
        }
    }

    @EventHandler
    private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
        if (drinking) event.target = null;
    }

    private void setPressed(boolean pressed) {
        mc.options.keyUse.setPressed(pressed);
    }

    private void startDrinking() {
        prevSlot = mc.player.getInventory().selectedSlot;

        drink();

        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURA_LIST) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }

        wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }
    }

    private void startSplashing() {
        prevSlot = mc.player.getInventory().selectedSlot;
        if (rotate.get()) {
            switch (rotateMode.get()) {
                case Server:
                    Rotations.rotate(mc.player.getYaw(), splashingPitch.get().floatValue());
                    splash();
                case Client:
                    if (mc.player.getPitch() != splashingPitch.get().floatValue()) prevPitch = mc.player.getPitch();
                    pitched = true;
                    mc.player.setPitch(splashingPitch.get().floatValue());
                    splash();
            }
        }

        splash();

        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURA_LIST) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }

        wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }
    }

    private void drink() {
        changeSlot(slot);
        setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();
        drinking = true;
    }

    private void splash() {
        changeSlot(slot);
        setPressed(true);
        splashing = true;
    }

    private void stopDrinking() {
        changeSlot(prevSlot);
        setPressed(false);
        drinking = false;

        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURA_LIST) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }

        if (pauseBaritone.get() && wasBaritone) BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
    }

    private void stopSplashing() {
        changeSlot(prevSlot);
        setPressed(false);
        if (pitched) {
            if (previousPitch.get()) mc.player.setPitch(prevPitch);
            else mc.player.setPitch(stoppedPitch.get().floatValue());
            pitched = false;
        }

        splashing = false;

        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURA_LIST) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) module.toggle();
            }
        }

        if (pauseBaritone.get() && wasBaritone) BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
    }

    private double trueHealth() {
        assert mc.player != null;
        return mc.player.getHealth();
    }

    private void changeSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
        this.slot = slot;
    }

    private int healingPotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.instant_health")) {
                        slot = i;
                        break;
                    }
                }
            }
        }

        return slot;
    }

    private int healingSplashPotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.SPLASH_POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.instant_health")) {
                        slot = i;
                        break;
                    }
                }
            }
        }

        return slot;
    }

    private int strengthSplashPotionSlot () {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.SPLASH_POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.strength")) {
                        slot = i;
                        break;
                    }
                }
            }
        }

        return slot;
    }

    private int strengthPotionSlot () {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.strength")) {
                        slot = i;
                        break;
                    }
                }
            }
        }

        return slot;
    }

    private boolean isNotPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.POTION;
    }

    private boolean isNotSplashPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.SPLASH_POTION;
    }

    private boolean shouldDrinkHealth(){
        return trueHealth() < health.get();
    }

    private boolean shouldNotDrinkHealth(){
        return trueHealth() >= health.get();
    }

    private boolean shouldDrinkStrength(){
        Map<StatusEffect, StatusEffectInstance> effects = mc.player.getActiveStatusEffects();
        return !effects.containsKey(StatusEffects.STRENGTH);
    }

    public enum RotateMode {
        Server("Server"),
        Client("Client");

        private final String title;

        RotateMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
