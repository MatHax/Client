package mathax.client.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.EntityTypeListSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.friends.Friends;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class TriggerBot extends Module {
    private Entity target;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Object2BooleanMap<EntityType<?>>> blacklistedEntities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("blacklist")
        .description("Entities to not attack.")
        .onlyAttackable()
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Won't attack friends.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> whenHoldingAttack = this.sgGeneral.add(new BoolSetting.Builder()
        .name("when-holding-attack")
        .description("Attacks only when you are holding bound attack key.")
        .defaultValue(false)
        .build()
    );

    public TriggerBot() {
        super(Categories.Combat, Items.COMMAND_BLOCK, "trigger-bot", "Automatically swings when you look at entities.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        target = null;

        if (mc.player.getHealth() <= 0.0f || mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;
        if (!(mc.targetedEntity instanceof LivingEntity)) return;
        if (!mc.targetedEntity.isAlive()) return;
        if (blacklistedEntities.get().containsKey(mc.targetedEntity.getType())) return;
        if (ignoreFriends.get() && mc.targetedEntity instanceof PlayerEntity player) {
            if (Friends.get().isFriend(player)) return;
        }

        target = mc.targetedEntity;

        if (whenHoldingAttack.get()) {
            if (mc.options.keyAttack.isPressed()) attack(target);
        } else attack(target);
    }

    private void attack(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity playerTarget) return playerTarget.getGameProfile().getName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
}
