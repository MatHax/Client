package mathax.client.systems.modules.misc;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.packets.PacketEvent;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ResourcePackSendS2CPacket;
import net.minecraft.text.BaseText;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public class ResourcePackSpoof extends Module {
    public ResourcePackSpoof() {
        super(Categories.Misc, Items.COMMAND_BLOCK, "resource-pack-spoof", "When connecting to a server it spoofs accepting the resource pack.");

        MatHax.EVENT_BUS.subscribe(new Listener());
    }

    private class Listener {
        @EventHandler
        private void onPacketRecieve(PacketEvent.Receive event) {
            if (!isActive() || !(event.packet instanceof ResourcePackSendS2CPacket packet)) return;

            event.cancel();
            BaseText msg = new LiteralText("This server has ");
            msg.append(packet.isRequired() ? "a required " : "an optional ");
            BaseText link = new LiteralText("resource pack.");
            link.setStyle(link.getStyle()
                .withColor(Formatting.BLUE)
                .withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, packet.getURL()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to download")))
            );
            msg.append(link);
            msg.append(".");
            info(msg);
        }
    }
}
