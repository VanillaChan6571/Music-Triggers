package mods.thecomputerizer.musictriggers.api.network;

import io.netty.buffer.ByteBuf;
import mods.thecomputerizer.musictriggers.api.data.channel.ChannelHelper;
import mods.thecomputerizer.theimpossiblelibrary.api.network.NetworkHelper;
import mods.thecomputerizer.theimpossiblelibrary.api.network.message.MessageAPI;

public class MessageCurrentSong<CTX> extends MessageAPI<CTX> {
    
    private final String channel;
    private final String song;
    
    public MessageCurrentSong(String channel, String song) {
        this.channel = channel;
        this.song = song;
    }
    
    public MessageCurrentSong(ByteBuf buf) {
        this.channel = NetworkHelper.readString(buf);
        this.song = NetworkHelper.readString(buf);
    }
    
    @Override public void encode(ByteBuf buf) {
        NetworkHelper.writeString(buf,this.channel);
        NetworkHelper.writeString(buf,this.song);
    }
    
    @Override public MessageAPI<CTX> handle(CTX ctx) {
        ChannelHelper.setCurrentSong(this.channel,this.song);
        return null;
    }
}