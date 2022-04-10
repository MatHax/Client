package mathax.client.mixin;

import mathax.client.mixininterface.ISlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen$CreativeSlot")
public class CreativeSlotMixin implements ISlot {
    @Shadow @Final Slot slot;

    @Override
    public int getId() {
        return slot.id;
    }

    @Override
    public int getIndex() {
        return slot.getIndex();
    }
}
