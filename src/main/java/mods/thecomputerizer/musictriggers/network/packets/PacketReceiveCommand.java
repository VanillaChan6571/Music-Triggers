package mods.thecomputerizer.musictriggers.network.packets;

import mods.thecomputerizer.musictriggers.client.ClientEvents;
import mods.thecomputerizer.theimpossiblelibrary.util.NetworkUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketReceiveCommand {

    private final String identifier;

    public PacketReceiveCommand(PacketBuffer buf) {
        this.identifier = NetworkUtil.readString(buf);
    }

    public PacketReceiveCommand(String identifier) {
        this.identifier = identifier;
    }

    public static void encode(PacketReceiveCommand packet, PacketBuffer buf) {
        NetworkUtil.writeString(buf,packet.identifier);
    }

    public static void handle(final PacketReceiveCommand packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
        });
        if(packet.getIdentifier()!=null) ClientEvents.commandMap.put(packet.getIdentifier(), true);
        ctx.setPacketHandled(true);
    }

    public String getIdentifier() {
        return identifier;
    }
}