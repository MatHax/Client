package mathax.client.utils.world;

import mathax.client.systems.friends.Friends;
import mathax.client.utils.entity.fakeplayer.FakePlayerEntity;
import mathax.client.utils.entity.fakeplayer.FakePlayerManager;
import mathax.client.mixin.AbstractBlockAccessor;
import mathax.client.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static mathax.client.MatHax.mc;

public class CityUtils {
    private static final BlockPos[] surround = new BlockPos[]{
        new BlockPos(0, 0, -1),
        new BlockPos(1, 0, 0),
        new BlockPos(0, 0, 1),
        new BlockPos(-1, 0, 0)
    };

    static final boolean assertionsDisabled = !CityUtils.class.desiredAssertionStatus();

    private static ArrayList<BlockPos> getTargetSurround(PlayerEntity playerEntity) {
        ArrayList<BlockPos> arrayList = new ArrayList<>();
        boolean bl = false;
        for (int i = 0; i < 4; ++i) {
            BlockPos blockPos;
            if (playerEntity == null || (blockPos = CityUtils.getSurround(playerEntity, surround[i])) == null) continue;
            if (!assertionsDisabled && mc.world == null) throw new AssertionError();
            if (mc.world.getBlockState(blockPos) == null) continue;
            if (!((AbstractBlockAccessor) mc.world.getBlockState(blockPos).getBlock()).isCollidable()) bl = true;
            if (mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN) continue;
            arrayList.add(blockPos);
        }

        if (bl) return null;
        return arrayList;
    }

    public static BlockPos getTargetBlock(PlayerEntity playerEntity) {
        BlockPos blockPos = null;
        ArrayList<BlockPos> arrayList = CityUtils.getTargetSurround(playerEntity);
        ArrayList<BlockPos> arrayList2 = CityUtils.getTargetSurround(mc.player);
        if (arrayList == null) return null;
        for (BlockPos BlockPos3 : arrayList) {
            if (arrayList2 != null && !arrayList2.isEmpty() && arrayList2.contains(BlockPos3)) continue;
            if (blockPos == null) {
                blockPos = BlockPos3;
                continue;
            }
            if (!(mc.player.squaredDistanceTo(Utils.vec3d(BlockPos3)) < mc.player.squaredDistanceTo(Utils.vec3d(blockPos)))) continue;
            blockPos = BlockPos3;
        }

        return blockPos;
    }

    public static BlockPos getSurround(Entity entity, BlockPos blockPos) {
        Vec3d vec3d = entity.getPos();
        if (blockPos == null) return new BlockPos(vec3d.x, vec3d.y, vec3d.z);
        return new BlockPos(vec3d.x, vec3d.y, vec3d.z).add(blockPos);
    }

    public static PlayerEntity getPlayerTarget(double d) {
        if (mc.player.isDead()) return null;
        PlayerEntity object = null;
        for (PlayerEntity object2 : mc.world.getPlayers()) {
            if (object2 == mc.player || object2.isDead() || !Friends.get().shouldAttack(object2) || (double)mc.player.distanceTo(object2) > d) continue;
            if (object == null) {
                object = object2;
                continue;
            }

            if (!(mc.player.distanceTo(object2) < mc.player.distanceTo(object))) continue;
            object = object2;
        }

        if (object == null) {
            for (FakePlayerEntity fakePlayerEntity : FakePlayerManager.getPlayers()) {
                if (fakePlayerEntity.isDead() || !Friends.get().shouldAttack(fakePlayerEntity) || (double)mc.player.distanceTo(fakePlayerEntity) > d) continue;
                if (object == null) {
                    object = fakePlayerEntity;
                    continue;
                }

                if (!(mc.player.distanceTo(fakePlayerEntity) < mc.player.distanceTo(object))) continue;
                object = fakePlayerEntity;
            }
        }

        return object;
    }
}
