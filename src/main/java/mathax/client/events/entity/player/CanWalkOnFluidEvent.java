package mathax.client.events.entity.player;

import net.minecraft.fluid.Fluid;

public class CanWalkOnFluidEvent {
    private static final CanWalkOnFluidEvent INSTANCE = new CanWalkOnFluidEvent();

    public Fluid fluid;
    public boolean walkOnFluid;

    public static CanWalkOnFluidEvent get(Fluid fluid) {
        INSTANCE.fluid = fluid;
        INSTANCE.walkOnFluid = false;
        return INSTANCE;
    }
}
