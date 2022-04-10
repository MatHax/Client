package mathax.client.mixin;

import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.world.Ambience;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FluidRenderer.class)
public class FluidRendererMixin {

    /**
     * @author Walaryne
     */
    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), index = 8)
    private int modifyColorIfLava(int color, BlockRenderView world, BlockPos pos, VertexConsumer vertexConsumer, FluidState state) {
        Ambience ambience = Modules.get().get(Ambience.class);
        if (ambience.isActive() && ambience.customLavaColor.get() && state.isIn(FluidTags.LAVA)) return ambience.lavaColor.get().getPacked();

        return color;
    }
}
