package mods.thecomputerizer.musictriggers.client.audio;

import mods.thecomputerizer.musictriggers.MusicTriggersCommon;
import mods.thecomputerizer.musictriggers.client.ClientSync;
import mods.thecomputerizer.musictriggers.common.SoundHandler;
import mods.thecomputerizer.musictriggers.config.*;
import mods.thecomputerizer.musictriggers.util.PacketHandler;
import mods.thecomputerizer.musictriggers.util.packets.PacketQueryServerInfo;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.InputStream;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ChannelManager {
    private static final HashMap<String,Channel> channelMap = new HashMap<>();
    private static final HashMap<String, SoundHandler> handlerMap = new HashMap<>();
    private static final HashMap<String, Redirect> redirectMap = new HashMap<>();
    private static final HashMap<String, ConfigMain> mainConfigMap = new HashMap<>();
    private static final HashMap<String, ConfigTransitions> transitionsConfigMap = new HashMap<>();
    private static final HashMap<String, ConfigCommands> commandsConfigMap = new HashMap<>();

    private static final List<String> songsInFolder = new ArrayList<>();
    public static final HashMap<String, File> openAudioFiles = new HashMap<>();
    public static final List<InputStream> openStreams = new ArrayList<>();
    private static final List<Channel> wasAlreadyPaused = new ArrayList<>();

    private static int tickCounter = 0;
    public static boolean reloading = false;

    public static void createChannel(String channel, String mainFileName, String transitionsFileName, String commandsFileName, String redirectFileName, boolean clientSide, boolean pausedByJukeBox, boolean overridesNormalMusic) {
        if(getChannel(channel)==null) {
            if(clientSide) {
                handlerMap.put(channel, new SoundHandler(channel));
                channelMap.put(channel, new Channel(channel,pausedByJukeBox,overridesNormalMusic));
                redirectMap.put(channel, new Redirect(new File(MusicTriggersCommon.configDir,redirectFileName+".txt")));
            }
            mainConfigMap.put(channel,new ConfigMain(new File(MusicTriggersCommon.configDir,mainFileName+".toml"),channel));
            if(clientSide) {
                transitionsConfigMap.put(channel, new ConfigTransitions(new File(MusicTriggersCommon.configDir, transitionsFileName + ".toml"),channelMap.get(channel)));
                commandsConfigMap.put(channel, new ConfigCommands(new File(MusicTriggersCommon.configDir, commandsFileName + ".toml")));
            }
            if(clientSide) channelMap.get(channel).passThroughConfigObjects(mainConfigMap.get(channel),transitionsConfigMap.get(channel),commandsConfigMap.get(channel),redirectMap.get(channel),handlerMap.get(channel));
        }
        else MusicTriggersCommon.logger.error("Channel already exists for category "+channel+"! Cannot assign 2 config files to the same music category.");
    }

    public static void parseConfigFiles() {
        collectSongs();
        for(ConfigMain toml : mainConfigMap.values()) {
            toml.parse();
            handlerMap.get(toml.getChannel()).registerSounds(toml,toml.getChannel());
        }
        for(Channel channel : channelMap.values()) channel.parseRedirect(redirectMap.get(channel.getChannelName()));
        for(ConfigTransitions transitions: transitionsConfigMap.values()) transitions.parse();
        for(ConfigCommands commands : commandsConfigMap.values()) commands.parse();
        ConfigDebug.parse(new File(MusicTriggersCommon.configDir,"debug.toml"));
        ConfigRegistry.parse(new File(MusicTriggersCommon.configDir,"registration.toml"));
    }

    public static void collectSongs() {
        File folder = new File(MusicTriggersCommon.configDir,"songs");
        if(!folder.exists()) {
            try {
                folder.createNewFile();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        File[] listOfFiles = folder.listFiles((dir, name) -> dir.canRead());
        if(listOfFiles!=null) {
            String curfile;
            songsInFolder.clear();
            openAudioFiles.clear();
            try {
                for (InputStream stream : openStreams) stream.close();
            } catch (Exception e) {
                MusicTriggersCommon.logger.error("Could not close one of the open audio streams. Resources may be lost!",e);
            }
            for (File f : listOfFiles) {
                curfile = FilenameUtils.getBaseName(f.getName());
                if (!songsInFolder.contains(curfile)) {
                    openAudioFiles.put(curfile, f);
                    songsInFolder.add(curfile);
                }
            }
        }
    }

    public static Channel getChannel(String channel) {
        return channelMap.get(channel);
    }

    public static Collection<Channel> getAllChannels() {
        return channelMap.values();
    }

    public static boolean canAnyChannelOverrideMusic() {
        for(Channel channel : getAllChannels()) if(channel.overridesNormalMusic() && channel.isPlaying()) return true;
        return false;
    }

    public static Map<String, String> getSongHolder(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).songholder;
    }

    public static Map<String, Map<String, String[]>> getTriggerHolder(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).triggerholder;
    }

    public static Map<String, Map<String, String>> getTriggerMapper(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).triggerMapper;
    }

    public static Map<String, String[]> getOtherInfo(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).otherinfo;
    }

    public static Map<String, Map<String, String[]>> getOtherLinkingInfo(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).otherlinkinginfo;
    }

    public static Map<String, Map<String, String[]>> getTriggerLinking(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).triggerlinking;
    }

    public static Map<String, Map<Integer, String[]>> getLoopPoints(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).loopPoints;
    }

    public static Map<String, Map<String, Map<Integer, String[]>>> getLinkingLoopsPoints(Channel channel) {
        return mainConfigMap.get(channel.getChannelName()).linkingLoopPoints;
    }

    public static Map<Integer, ConfigTransitions.Title> getTitleCards(Channel channel) {
        return transitionsConfigMap.get(channel.getChannelName()).titlecards;
    }

    public static Map<Integer, ConfigTransitions.Image> getImageCards(Channel channel) {
        return transitionsConfigMap.get(channel.getChannelName()).imagecards;
    }

    public static Map<Integer, Boolean> getIsMoving(Channel channel) {
        return transitionsConfigMap.get(channel.getChannelName()).ismoving;
    }

    public static void syncInfoFromServer(ClientSync sync) {
        try {
            getChannel(sync.getChannel()).sync(sync);
        } catch (NullPointerException exception) {
            MusicTriggersCommon.logger.error("Channel "+sync.getChannel()+" did not exist and could not be synced!");
        }
    }

    public static void pauseAllChannels(boolean fromJukebox) {
        for(Channel channel : channelMap.values()) {
            if(channel.isPaused()) wasAlreadyPaused.add(channel);
            channel.setPaused(true,fromJukebox);
        }
    }

    public static void unPauseAllChannels() {
        for(Channel channel : channelMap.values()) if(!wasAlreadyPaused.contains(channel) && channel.isPaused()) channel.setPaused(false,false);
        wasAlreadyPaused.clear();
    }

    public static void reloadAllChannels() {
        collectSongs();
        for(Channel channel : channelMap.values()) channel.reload();
        refreshDebug();
    }

    public static void tickChannels() {
        if(!reloading) {
            tickCounter++;
            if(!MinecraftClient.getInstance().isWindowFocused()) pauseAllChannels(false);
            else if(!MinecraftClient.getInstance().isPaused()) {
                if (checkForJukeBox()) {
                    pauseAllChannels(true);
                    for (Channel channel : channelMap.values()) if(!channel.isPaused()) channel.tickFast();
                    if (tickCounter % 5 == 0) {
                        for (Channel channel : channelMap.values()) if(!channel.isPaused()) channel.tickSlow();
                        sendUpdatePacket();
                    }
                } else {
                    unPauseAllChannels();
                    for (Channel channel : channelMap.values()) if(!channel.isPaused()) channel.tickFast();
                    if (tickCounter % 5 == 0) {
                        for (Channel channel : channelMap.values()) channel.tickSlow();
                        sendUpdatePacket();
                    }
                }
            }
            if(tickCounter>=100) tickCounter=0;
        }
    }

    private static boolean checkForJukeBox() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if(player!=null) {
            for (int x = player.getChunkPos().x - 3; x <= player.getChunkPos().x + 3; x++) {
                for (int z = player.getChunkPos().z - 3; z <= player.getChunkPos().z + 3; z++) {
                    Set<BlockPos> currentChunkTEPos = player.world.getChunk(x, z).getBlockEntityPositions();
                    for (BlockPos b : currentChunkTEPos)
                        return player.world.getChunk(x, z).getBlockEntity(b) instanceof JukeboxBlockEntity te && te.getCachedState().get(JukeboxBlock.HAS_RECORD);
                }
            }
        }
        return false;
    }

    public static void refreshDebug() {
        ConfigDebug.parse(new File(MusicTriggersCommon.configDir,"debug.toml"));
    }

    private static void sendUpdatePacket() {
        if(MinecraftClient.getInstance().player!=null)
            PacketHandler.sendToServer(PacketQueryServerInfo.id,PacketQueryServerInfo.encode(new PacketQueryServerInfo(new ArrayList<>(channelMap.values()))));
    }
}