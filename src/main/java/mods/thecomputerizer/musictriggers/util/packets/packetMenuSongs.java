package mods.thecomputerizer.musictriggers.util.packets;

import io.netty.buffer.ByteBuf;
import mods.thecomputerizer.musictriggers.common.eventsCommon;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class packetMenuSongs implements IMessageHandler<packetMenuSongs.packetMenuSongsMessage, IMessage> {

    @Override
    public IMessage onMessage(packetMenuSongs.packetMenuSongsMessage message, MessageContext ctx)
    {
        if(message.getSongsWithUUIDAttached()==null || message.getSongsWithUUIDAttached().isEmpty()) {
            return null;
        }
        eventsCommon.recordMenu = message.getSongsWithUUIDAttached();
        return null;
    }

    public static class packetMenuSongsMessage implements IMessage {
        String s;

        public packetMenuSongsMessage() {}

        public packetMenuSongsMessage(String name) {
            this.s = name;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.s = ((String) buf.readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8));
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeCharSequence(s, StandardCharsets.UTF_8);
        }

        public HashMap<UUID, List<String>> getSongsWithUUIDAttached() {
            if(s==null) {
                return null;
            }
            String[] broken = stringBreaker(s);
            ArrayList<String> ret = new ArrayList<>();
            for(int i=1;i<broken.length;i++) {
                ret.add(broken[i]);
            }
            HashMap<UUID, List<String>> builtMap = new HashMap<>();
            builtMap.put(UUID.fromString(broken[0]),ret);
            return builtMap;
        }

        public static String[] stringBreaker(String s) {
            return s.split(",");
        }
    }
}