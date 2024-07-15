package mods.thecomputerizer.musictriggers.api.data.channel;

import mods.thecomputerizer.musictriggers.api.client.gui.MTScreenInfo;
import mods.thecomputerizer.musictriggers.api.client.gui.parameters.DataLink;
import mods.thecomputerizer.musictriggers.api.client.gui.parameters.WrapperLink;
import mods.thecomputerizer.musictriggers.api.data.MTDataRef.ParameterRef;
import mods.thecomputerizer.musictriggers.api.data.MTDataRef.TableRef;
import mods.thecomputerizer.musictriggers.api.data.parameter.Parameter;
import mods.thecomputerizer.musictriggers.api.data.parameter.ParameterWrapper;
import mods.thecomputerizer.theimpossiblelibrary.api.toml.Toml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static mods.thecomputerizer.musictriggers.api.data.MTDataRef.EVENT_RUNNER;

public abstract class ChannelElementRunner extends ChannelElement implements ChannelEventRunner {

    protected EventInstance instance;

    protected ChannelElementRunner(ChannelAPI channel, String name) {
        super(channel,name);
    }
    
    @Override public void activate() {
        if(checkRun("activate")) run();
    }
    
    public boolean checkRun(String type) {
        return Objects.nonNull(this.instance) && this.instance.checkSide() && this.instance.canRun(type);
    }
    
    @Override public void deactivate() {
        if(checkRun("deactivate")) run();
    }
    
    @Override public Collection<DataLink> getChildWrappers(MTScreenInfo parent) {
        if(Objects.isNull(this.instance)) this.instance = new EventInstance(this.channel,this);
        return Collections.singletonList(this.instance.getLink(parent.next("event")));
    }
    
    @Override public String getLogPrefix() {
        return super.getLogPrefix();
    }
    
    @Override
    public boolean parse(Toml table) {
        if(super.parse(table)) {
            if(table.hasTable("event")) {
                EventInstance instance = new EventInstance(this.channel,this);
                if(instance.parse(table.getTable("event"))) this.instance = instance;
                else logError("Failed to parse event instance");
            }
            return true;
        }
        return false;
    }
    
    @Override public void play() {
        if(checkRun("play")) run();
    }
    
    @Override public void playable() {
        if(checkRun("playable")) run();
    }
    
    @Override public void playing() {
        if(checkRun("playing")) run();
    }
    
    @Override public void queue() {
        if(checkRun("queue")) run();
    }
    
    @Override
    public void run() {
        this.instance.resetTimer();
    }
    
    @Override public void stop() {
        if(checkRun("stop")) run();
    }
    
    @Override public void stopped() {
        if(checkRun("stopped")) run();
    }
    
    @Override
    public boolean tick() {
        return this.instance.tick();
    }
    
    @Override public void tickActive() {
        if(checkRun("tick_active")) run();
    }
    
    @Override public void tickPlayable() {
        if(checkRun("tick_playable")) run();
    }
    
    @Override public void unplayable() {
        if(checkRun("unplayable")) run();
    }
}
