package mathax.client.systems.modules.combat;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.entity.EntityUtils;
import mathax.client.utils.entity.SortPriority;
import mathax.client.utils.entity.TargetUtils;
import mathax.client.utils.player.DamageUtils;
import mathax.client.utils.player.FindItemResult;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.Rotations;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PistonAura extends Module {
    private PlayerEntity target;

    private BlockPos headPos;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius in which players get targeted.")
        .defaultValue(5.0)
        .min(0.0)
        .sliderRange(0, 10.0)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.Lowest_Health)
        .build()
    );

    private final Setting<Boolean> trap = sgGeneral.add(new BoolSetting.Builder()
        .name("trap")
        .description("Traps the enemy player.")
        .defaultValue(true)
        .build()
    );

    public PistonAura() {
        super(Categories.Combat, Items.PISTON, "piston-aura", "Moves crystals into people using pistons and attacks them.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (TargetUtils.isBadTarget(target, targetRange.get())) target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (TargetUtils.isBadTarget(target, targetRange.get())) return;
        if (mc.player.distanceTo(target) < 4) firstPlace();
        else if (mc.player.distanceTo(target) >= 4) secondPlace();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystalEntity && DamageUtils.crystalDamage(target, crystalEntity.getPos()) >= 8) mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
        }
    }

    private void firstPlace() {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.isHotbar() && !obsidian.isOffhand()) {
            info("No obsidian found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult piston = InvUtils.findInHotbar(Items.PISTON);
        if (!piston.isHotbar() && !piston.isOffhand()) {
            info("No piston found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.isHotbar() && !crystal.isOffhand()) {
            info("No crystal found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult redstoneBlock = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
        if (!redstoneBlock.isHotbar() && !redstoneBlock.isOffhand()) {
            info("No redstone block found in hotbar, disabling...");
            toggle();
            return;
        }

        headPos = target.getBlockPos().up();
        if (trap.get() && mc.world.getBlockState(headPos.up()).isAir()) placeBlock(headPos.up(), obsidian);
        if ((mc.world.getBlockState(headPos.west().down()).isAir() || mc.world.getBlockState(headPos.west().down()).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(headPos.west().down()).getBlock().equals(Blocks.BEDROCK)) && (mc.world.getBlockState(headPos.west()).isAir() || mc.world.getBlockState(headPos.west()).getBlock().equals(Blocks.MOVING_PISTON) || mc.world.getBlockState(headPos.west()).getBlock().equals(Blocks.PISTON_HEAD)) && (mc.world.getBlockState(headPos.west(2)).isAir() || mc.world.getBlockState(headPos.west(2)).getBlock().equals(Blocks.PISTON) || mc.world.getBlockState(headPos.west(2)).getBlock().equals(Blocks.MOVING_PISTON) || mc.world.getBlockState(headPos.west(2)).getBlock().equals(Blocks.PISTON_HEAD)) && (mc.world.getBlockState(headPos.west(3)).isAir() || mc.world.getBlockState(headPos.west(3)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(headPos.west(3)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
            Rotations.rotate(Rotations.getYaw(headPos.west(4)), Rotations.getPitch(headPos.west(4)));
            if (mc.world.getBlockState(headPos.west().down()).isAir()) placeBlock(headPos.west().down(), obsidian);
            if (mc.world.getBlockState(headPos.west(2).down()).isAir()) placeBlock(headPos.west(2).down(), obsidian);
            if (mc.world.getBlockState(headPos.west(3).down()).isAir()) placeBlock(headPos.west(3).down(), obsidian);
            if (mc.world.getBlockState(headPos.west(2)).isAir()) placeBlock(headPos.west(2), piston);
            placeBlock(headPos.west().down(), crystal);
            if (mc.world.getBlockState(headPos.west(3)).isAir()) placeBlock(headPos.west(3), redstoneBlock);
        } else if ((mc.world.getBlockState(headPos.east().down()).isAir() || mc.world.getBlockState(headPos.east().down()).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(headPos.east().down()).getBlock().equals(Blocks.BEDROCK)) && (mc.world.getBlockState(headPos.east()).isAir() || mc.world.getBlockState(headPos.east()).getBlock().equals(Blocks.PISTON_HEAD) || mc.world.getBlockState(headPos.east()).getBlock().equals(Blocks.MOVING_PISTON)) && (mc.world.getBlockState(headPos.east(2)).isAir() || mc.world.getBlockState(headPos.east(2)).getBlock().equals(Blocks.PISTON) || mc.world.getBlockState(headPos.east(2)).getBlock().equals(Blocks.MOVING_PISTON) || mc.world.getBlockState(headPos.east(2)).getBlock().equals(Blocks.PISTON_HEAD)) && (mc.world.getBlockState(headPos.east(3)).isAir() || mc.world.getBlockState(headPos.east(3)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(headPos.east(3)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
            Rotations.rotate(Rotations.getYaw(headPos.east(4)), Rotations.getPitch(headPos.east(4)));
            if (mc.world.getBlockState(headPos.east().down()).isAir()) placeBlock(headPos.east().down(), obsidian);
            if (mc.world.getBlockState(headPos.east(2).down()).isAir()) placeBlock(headPos.east(2).down(), obsidian);
            if (mc.world.getBlockState(headPos.east(3).down()).isAir()) placeBlock(headPos.east(3).down(), obsidian);
            if (mc.world.getBlockState(headPos.east(2)).isAir()) placeBlock(headPos.east(2), piston);
            placeBlock(headPos.east().down(), crystal);
            if (mc.world.getBlockState(headPos.east(3)).isAir()) placeBlock(headPos.east(3), redstoneBlock);
        } else if ((mc.world.getBlockState(headPos.south().down()).isAir() || mc.world.getBlockState(headPos.south().down()).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(headPos.south().down()).getBlock().equals(Blocks.BEDROCK)) && (mc.world.getBlockState(headPos.south()).isAir() || mc.world.getBlockState(headPos.south()).getBlock().equals(Blocks.PISTON_HEAD) || mc.world.getBlockState(headPos.south()).getBlock().equals(Blocks.MOVING_PISTON)) && (mc.world.getBlockState(headPos.south(2)).isAir() || mc.world.getBlockState(headPos.south(2)).getBlock().equals(Blocks.PISTON) || mc.world.getBlockState(headPos.south(2)).getBlock().equals(Blocks.MOVING_PISTON) || mc.world.getBlockState(headPos.south(2)).getBlock().equals(Blocks.PISTON_HEAD)) && (mc.world.getBlockState(headPos.south(3)).isAir() || mc.world.getBlockState(headPos.south(3)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(headPos.south(3)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
            Rotations.rotate(Rotations.getYaw(headPos.south(4)), Rotations.getPitch(headPos.south(4)));
            if (mc.world.getBlockState(headPos.south().down()).isAir()) placeBlock(headPos.south().down(), obsidian);
            if (mc.world.getBlockState(headPos.south(2).down()).isAir()) placeBlock(headPos.south(2).down(), obsidian);
            if (mc.world.getBlockState(headPos.south(3).down()).isAir()) placeBlock(headPos.south(3).down(), obsidian);
            if (mc.world.getBlockState(headPos.south(2)).isAir()) placeBlock(headPos.south(2), piston);
            placeBlock(headPos.south().down(), crystal);
            if (mc.world.getBlockState(headPos.south(3)).isAir()) placeBlock(headPos.south(3), redstoneBlock);
        } else if ((mc.world.getBlockState(headPos.north().down()).isAir() || mc.world.getBlockState(headPos.north().down()).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(headPos.north().down()).getBlock().equals(Blocks.BEDROCK)) && (mc.world.getBlockState(headPos.north()).isAir() || mc.world.getBlockState(headPos.north()).getBlock().equals(Blocks.PISTON_HEAD) || mc.world.getBlockState(headPos.north()).getBlock().equals(Blocks.MOVING_PISTON)) && (mc.world.getBlockState(headPos.north(2)).isAir() || mc.world.getBlockState(headPos.north(2)).getBlock().equals(Blocks.PISTON) || mc.world.getBlockState(headPos.north(2)).getBlock().equals(Blocks.MOVING_PISTON) || mc.world.getBlockState(headPos.north(2)).getBlock().equals(Blocks.PISTON_HEAD)) && (mc.world.getBlockState(headPos.north(3)).isAir() || mc.world.getBlockState(headPos.north(3)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(headPos.north(3)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
            Rotations.rotate(Rotations.getYaw(headPos.north(4)), Rotations.getPitch(headPos.north(4)));
            if (mc.world.getBlockState(headPos.north().down()).isAir()) placeBlock(headPos.north().down(), obsidian);
            if (mc.world.getBlockState(headPos.north(2).down()).isAir()) placeBlock(headPos.north(2).down(), obsidian);
            if (mc.world.getBlockState(headPos.north(3).down()).isAir()) placeBlock(headPos.north(3).down(), obsidian);
            if (mc.world.getBlockState(headPos.north(2)).isAir()) placeBlock(headPos.north(2), piston);
            placeBlock(headPos.north().down(), crystal);
            if (mc.world.getBlockState(headPos.north(3)).isAir()) placeBlock(headPos.north(3), redstoneBlock);
        }
    }

    private void secondPlace() {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.isHotbar() && !obsidian.isOffhand()) {
            info("No obsidian found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult piston = InvUtils.findInHotbar(Items.PISTON);
        if (!piston.isHotbar() && !piston.isOffhand()) {
            info("No piston found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.isHotbar() && !crystal.isOffhand()) {
            info("No crystal found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult redstoneBlock = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
        if (!redstoneBlock.isHotbar() && !redstoneBlock.isOffhand()) {
            info("No redstone block found in hotbar, disabling...");
            toggle();
            return;
        }

        BlockPos pos = EntityUtils.getCityBlock(target);
        if (pos == target.getBlockPos().west())
            if ((mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).getBlock().equals(Blocks.BEDROCK)) && mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().west()).isAir() || mc.world.getBlockState(pos.up().west()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().west(2)).isAir() || mc.world.getBlockState(pos.up().west(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().west(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                Rotations.rotate(Rotations.getYaw(pos.up().east(2)), Rotations.getPitch(pos.up().east(2)));
                if (mc.world.getBlockState(headPos.west().down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                if (mc.world.getBlockState(headPos.west(2).down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                if (mc.world.getBlockState(headPos.west(3).down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                if (mc.world.getBlockState(headPos.west(2)).isAir()) placeBlock(headPos.west(2), piston);
                if (mc.world.getBlockState(headPos.west(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.west().down(), crystal);
                if (mc.world.getBlockState(headPos.west(3)).isAir()) placeBlock(headPos.west(3), redstoneBlock);
            } else {
                pos = target.getBlockPos().south();
                double distanceToSouth = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                pos = target.getBlockPos().north();
                double distanceToNorth = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                if (distanceToSouth < distanceToNorth) {
                    pos = target.getBlockPos().south();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().south()).isAir() || mc.world.getBlockState(pos.up().south()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().south(2)).isAir() || mc.world.getBlockState(pos.up().south(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().south(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().north(2)), Rotations.getPitch(pos.up().north(2)));
                        if (mc.world.getBlockState(headPos.south().down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                        if (mc.world.getBlockState(headPos.south(2).down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                        if (mc.world.getBlockState(headPos.south(3).down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                        if (mc.world.getBlockState(headPos.south(2)).isAir()) placeBlock(headPos.south(2), piston);
                        if (mc.world.getBlockState(headPos.south(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.south().down(), crystal);
                        if (mc.world.getBlockState(headPos.south(3)).isAir()) placeBlock(headPos.south(3), redstoneBlock);
                    }
                } else if (distanceToSouth > distanceToNorth) {
                    pos = target.getBlockPos().north();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().north()).isAir() || mc.world.getBlockState(pos.up().north()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().north(2)).isAir() || mc.world.getBlockState(pos.up().north(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().north(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().south(2)), Rotations.getPitch(pos.up().south(2)));
                        if (mc.world.getBlockState(headPos.north().down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                        if (mc.world.getBlockState(headPos.north(2).down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                        if (mc.world.getBlockState(headPos.north(3).down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                        if (mc.world.getBlockState(headPos.north(2)).isAir()) placeBlock(headPos.north(2), piston);
                        if (mc.world.getBlockState(headPos.north(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.north().down(), crystal);
                        if (mc.world.getBlockState(headPos.north(3)).isAir()) placeBlock(headPos.north(3), redstoneBlock);
                    }
                } else return;
            }
        if (pos == target.getBlockPos().east())
            if ((mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).getBlock().equals(Blocks.BEDROCK)) && mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().east()).isAir() || mc.world.getBlockState(pos.up().east()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().east(2)).isAir() || mc.world.getBlockState(pos.up().east(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().east(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                Rotations.rotate(Rotations.getYaw(pos.up().west(2)), Rotations.getPitch(pos.up().west(2)));
                if (mc.world.getBlockState(headPos.east().down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                if (mc.world.getBlockState(headPos.east(2).down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                if (mc.world.getBlockState(headPos.east(3).down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                if (mc.world.getBlockState(headPos.east(2)).isAir()) placeBlock(headPos.east(2), piston);
                if (mc.world.getBlockState(headPos.east(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.east().down(), crystal);
                if (mc.world.getBlockState(headPos.east(3)).isAir()) placeBlock(headPos.east(3), redstoneBlock);
            } else {
                pos = target.getBlockPos().south();
                double distanceToSouth = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                pos = target.getBlockPos().north();
                double distanceToNorth = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                if (distanceToSouth < distanceToNorth) {
                    pos = target.getBlockPos().south();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().south()).isAir() || mc.world.getBlockState(pos.up().south()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().south(2)).isAir() || mc.world.getBlockState(pos.up().south(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().south(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().north(2)), Rotations.getPitch(pos.up().north(2)));
                        if (mc.world.getBlockState(headPos.south().down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                        if (mc.world.getBlockState(headPos.south(2).down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                        if (mc.world.getBlockState(headPos.south(3).down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                        if (mc.world.getBlockState(headPos.south(2)).isAir()) placeBlock(headPos.south(2), piston);
                        if (mc.world.getBlockState(headPos.south(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.south().down(), crystal);
                        if (mc.world.getBlockState(headPos.south(3)).isAir()) placeBlock(headPos.south(3), redstoneBlock);
                    }
                } else if (distanceToSouth > distanceToNorth) {
                    pos = target.getBlockPos().north();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().north()).isAir() || mc.world.getBlockState(pos.up().north()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().north(2)).isAir() || mc.world.getBlockState(pos.up().north(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().north(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().south(2)), Rotations.getPitch(pos.up().south(2)));
                        if (mc.world.getBlockState(headPos.north().down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                        if (mc.world.getBlockState(headPos.north(2).down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                        if (mc.world.getBlockState(headPos.north(3).down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                        if (mc.world.getBlockState(headPos.north(2)).isAir()) placeBlock(headPos.north(2), piston);
                        if (mc.world.getBlockState(headPos.north(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.north().down(), crystal);
                        if (mc.world.getBlockState(headPos.north(3)).isAir()) placeBlock(headPos.north(3), redstoneBlock);
                    }
                } else return;
            }
        if (pos == target.getBlockPos().south())
            if ((mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).getBlock().equals(Blocks.BEDROCK)) && mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().south()).isAir() || mc.world.getBlockState(pos.up().south()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().south(2)).isAir() || mc.world.getBlockState(pos.up().south(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().south(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                Rotations.rotate(Rotations.getYaw(pos.up().north(2)), Rotations.getPitch(pos.up().north(2)));
                if (mc.world.getBlockState(headPos.south().down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                if (mc.world.getBlockState(headPos.south(2).down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                if (mc.world.getBlockState(headPos.south(3).down()).isAir()) placeBlock(headPos.south().down(), obsidian);
                if (mc.world.getBlockState(headPos.south(2)).isAir()) placeBlock(headPos.south(2), piston);
                if (mc.world.getBlockState(headPos.south(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.south().down(), crystal);
                if (mc.world.getBlockState(headPos.south(3)).isAir()) placeBlock(headPos.south(3), redstoneBlock);
            } else {
                pos = target.getBlockPos().east();
                double distanceToEast = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                pos = target.getBlockPos().west();
                double distanceToWest = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                if (distanceToEast < distanceToWest) {
                    pos = target.getBlockPos().east();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().east()).isAir() || mc.world.getBlockState(pos.up().east()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().east(2)).isAir() || mc.world.getBlockState(pos.up().east(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().east(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().west(2)), Rotations.getPitch(pos.up().west(2)));
                        if (mc.world.getBlockState(headPos.east().down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                        if (mc.world.getBlockState(headPos.east(2).down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                        if (mc.world.getBlockState(headPos.east(3).down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                        if (mc.world.getBlockState(headPos.east(2)).isAir()) placeBlock(headPos.east(2), piston);
                        if (mc.world.getBlockState(headPos.east(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.east().down(), crystal);
                        if (mc.world.getBlockState(headPos.east(3)).isAir()) placeBlock(headPos.east(3), redstoneBlock);
                    }
                } else if (distanceToEast > distanceToWest) {
                    pos = target.getBlockPos().west();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().west()).isAir() || mc.world.getBlockState(pos.up().west()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().west(2)).isAir() || mc.world.getBlockState(pos.up().west(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().west(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().east(2)), Rotations.getPitch(pos.up().east(2)));
                        if (mc.world.getBlockState(headPos.west().down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                        if (mc.world.getBlockState(headPos.west(2).down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                        if (mc.world.getBlockState(headPos.west(3).down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                        if (mc.world.getBlockState(headPos.west(2)).isAir()) placeBlock(headPos.west(2), piston);
                        if (mc.world.getBlockState(headPos.west(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.west().down(), crystal);
                        if (mc.world.getBlockState(headPos.west(3)).isAir()) placeBlock(headPos.west(3), redstoneBlock);
                    }
                } else return;
            }
        if (pos == target.getBlockPos().north())
            if ((mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).getBlock().equals(Blocks.BEDROCK)) && mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().north()).isAir() || mc.world.getBlockState(pos.up().north()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().north(2)).isAir() || mc.world.getBlockState(pos.up().north(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().north(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                Rotations.rotate(Rotations.getYaw(pos.up().south(2)), Rotations.getPitch(pos.up().south(2)));
                if (mc.world.getBlockState(headPos.north().down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                if (mc.world.getBlockState(headPos.north(2).down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                if (mc.world.getBlockState(headPos.north(3).down()).isAir()) placeBlock(headPos.north().down(), obsidian);
                if (mc.world.getBlockState(headPos.north(2)).isAir()) placeBlock(headPos.north(2), piston);
                if (mc.world.getBlockState(headPos.north(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.north().down(), crystal);
                if (mc.world.getBlockState(headPos.north(3)).isAir()) placeBlock(headPos.north(3), redstoneBlock);
            } else {
                pos = target.getBlockPos().east();
                double distanceToEast = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                pos = target.getBlockPos().west();
                double distanceToWest = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                if (distanceToEast < distanceToWest) {
                    pos = target.getBlockPos().east();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().east()).isAir() || mc.world.getBlockState(pos.up().east()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().east(2)).isAir() || mc.world.getBlockState(pos.up().east(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().east(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().west(2)), Rotations.getPitch(pos.up().west(2)));
                        if (mc.world.getBlockState(headPos.east().down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                        if (mc.world.getBlockState(headPos.east(2).down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                        if (mc.world.getBlockState(headPos.east(3).down()).isAir()) placeBlock(headPos.east().down(), obsidian);
                        if (mc.world.getBlockState(headPos.east(2)).isAir()) placeBlock(headPos.east(2), piston);
                        if (mc.world.getBlockState(headPos.east(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.east().down(), crystal);
                        if (mc.world.getBlockState(headPos.east(3)).isAir()) placeBlock(headPos.east(3), redstoneBlock);
                    }
                } else if (distanceToEast > distanceToWest) {
                    pos = target.getBlockPos().west();
                    if (mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up().west()).isAir() || mc.world.getBlockState(pos.up().west()).getBlock().equals(Blocks.PISTON)) && (mc.world.getBlockState(pos.up().west(2)).isAir() || mc.world.getBlockState(pos.up().west(2)).getBlock().equals(Blocks.REDSTONE_BLOCK) || mc.world.getBlockState(pos.up().west(2)).getBlock().equals(Blocks.REDSTONE_TORCH))) {
                        Rotations.rotate(Rotations.getYaw(pos.up().east(2)), Rotations.getPitch(pos.up().east(2)));
                        if (mc.world.getBlockState(headPos.west().down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                        if (mc.world.getBlockState(headPos.west(2).down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                        if (mc.world.getBlockState(headPos.west(3).down()).isAir()) placeBlock(headPos.west().down(), obsidian);
                        if (mc.world.getBlockState(headPos.west(2)).isAir()) placeBlock(headPos.west(2), piston);
                        if (mc.world.getBlockState(headPos.west(2)).getBlock().equals(Blocks.PISTON)) placeBlock(headPos.west().down(), crystal);
                        if (mc.world.getBlockState(headPos.west(3)).isAir()) placeBlock(headPos.west(3), redstoneBlock);
                    }
                }
            }
    }

    private void placeBlock(BlockPos pos, FindItemResult item) {
        if (!item.isHotbar()) {
            warning("No %s found in hotbar, disabling...", item);
            toggle();
            return;
        }

        InvUtils.updateSlot(item.slot());
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
    }
}
