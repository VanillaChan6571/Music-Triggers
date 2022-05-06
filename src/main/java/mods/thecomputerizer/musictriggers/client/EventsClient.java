package mods.thecomputerizer.musictriggers.client;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.client.gui.GuiMain;
import mods.thecomputerizer.musictriggers.client.gui.GuiTriggerInfo;
import mods.thecomputerizer.musictriggers.config.ConfigDebug;
import mods.thecomputerizer.musictriggers.config.ConfigObject;
import mods.thecomputerizer.musictriggers.config.ConfigTitleCards;
import mods.thecomputerizer.musictriggers.util.CustomTick;
import mods.thecomputerizer.musictriggers.util.RegistryHandler;
import mods.thecomputerizer.musictriggers.util.packets.PacketBossInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mod.EventBusSubscriber(modid = MusicTriggers.MODID, value = Side.CLIENT)
public class EventsClient {
    public static ResourceLocation IMAGE_CARD = null;
    public static int curImageIndex;
    public static boolean isWorldRendered;
    public static float fadeCount = 1000;
    public static float startDelayCount = 0;
    public static Boolean activated = false;
    public static long timer=0;
    public static int GuiCounter = 0;
    public static int reloadCounter = 0;
    public static boolean ismoving;
    public static List<ResourceLocation> pngs = new ArrayList<>();
    public static int movingcounter = 0;
    public static String lastAdvancement;
    public static boolean advancement;
    public static EntityPlayer PVPTracker;
    public static boolean renderDebug = true;
    public static boolean zone = false;
    public static boolean firstPass = false;
    public static GuiTriggerInfo parentScreen = null;
    public static int x1 = 0;
    public static int y1 = 0;
    public static int z1 = 0;
    public static int x2 = 0;
    public static int y2 = 0;
    public static int z2 = 0;
    private static int bossBarCounter = 0;

    @SubscribeEvent
    public static void playSound(PlaySoundEvent e) {
        PositionedSoundRecord silenced = new PositionedSoundRecord(e.getSound().getSoundLocation(), SoundCategory.MUSIC, Float.MIN_VALUE*1000, 1F, false, 1, ISound.AttenuationType.LINEAR, 0F, 0F, 0F);
        for(String s : ConfigDebug.blockedmods) {
            if(e.getSound().getSoundLocation().toString().contains(s) && e.getSound().getCategory()==SoundCategory.MUSIC) {
                if(!(MusicPlayer.curMusic==null && ConfigDebug.SilenceIsBad)) e.setResultSound(silenced);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent e) {
        if(e.getEntityLiving() instanceof EntityPlayer && e.getSource().getTrueSource() instanceof EntityPlayer) {
            if (e.getEntityLiving() == MusicPicker.player) {
                PVPTracker = (EntityPlayer)e.getSource().getTrueSource();
                MusicPicker.setPVP = true;
            }
            else if(e.getSource().getTrueSource() == MusicPicker.player) {
                PVPTracker = (EntityPlayer)e.getEntityLiving();
                MusicPicker.setPVP = true;
            }
        }
    }

    @SubscribeEvent
    public static void guiScreen(GuiScreenEvent e) {
        if (ConfigDebug.ShowGUIName) e.getGui().drawHoveringText(e.getGui().getClass().getName(), 0, 0);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent e) {
        lastAdvancement = e.getAdvancement().getId().toString();
        advancement = true;
    }

    @SubscribeEvent
    public static void worldRender(RenderWorldLastEvent e) {
        isWorldRendered=true;
    }

    @SubscribeEvent
    public static void clientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        MusicPicker.mc.getSoundHandler().stopSounds();
        MusicPlayer.fadingOut = false;
        MusicPlayer.fadingIn = false;
        MusicPlayer.linkedFadingIn = new HashMap<>();
        MusicPlayer.linkedFadingOut = new HashMap<>();
        isWorldRendered=false;
        MusicPicker.player=null;
    }

    @SubscribeEvent
    public static void cancelRenders(RenderGameOverlayEvent.Pre e) {
        if(e.getType()==RenderGameOverlayEvent.ElementType.ALL && !renderDebug) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void customTick(CustomTick ev) {
        if(ConfigTitleCards.imagecards.get(curImageIndex)!=null) {
            if (timer > ConfigTitleCards.imagecards.get(curImageIndex).getTime()) {
                activated = false;
                timer = 0;
                ismoving = false;
                movingcounter = 0;
            }
            if (ismoving) {
                if (timer % ConfigTitleCards.imagecards.get(curImageIndex).getDelay() == 0) {
                    movingcounter++;
                    if (movingcounter >= pngs.size()) movingcounter = 0;
                }
                IMAGE_CARD = pngs.get(movingcounter);
            }
            if (activated) {
                timer++;
                startDelayCount++;
                if (startDelayCount > 0) {
                    if (fadeCount > 1) {
                        fadeCount -= ConfigTitleCards.imagecards.get(curImageIndex).getFadeIn();
                        if (fadeCount < 1) fadeCount = 1;
                    }
                }
            } else {
                if (fadeCount < 1000) {
                    fadeCount += ConfigTitleCards.imagecards.get(curImageIndex).getFadeOut();
                    if (fadeCount > 1000) {
                        fadeCount = 1000;
                        ismoving = false;
                    }
                }
                startDelayCount = 0;
            }
        }
    }

    @SubscribeEvent
    public static void imageCards(RenderGameOverlayEvent.Post e) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if(e.getType()== RenderGameOverlayEvent.ElementType.ALL) {
            ScaledResolution res = e.getResolution();
            if (player != null && ConfigTitleCards.imagecards.get(curImageIndex)!=null) {
                int x = res.getScaledWidth();
                int y = res.getScaledHeight();
                Vector4f color = new Vector4f(1, 1, 1, 1);
                if (fadeCount != 1000 && IMAGE_CARD!=null) {
                    GlStateManager.enableBlend();
                    GlStateManager.pushMatrix();
                    mc.getTextureManager().bindTexture(IMAGE_CARD);

                    float opacity = (int) (17 - (fadeCount / 80));
                    opacity = (opacity * 1.15f) / 15;
                    GlStateManager.color(color.getX(), color.getY(), color.getZ(), Math.max(0, Math.min(0.95f, opacity)));

                    float scale_x = (0.25f*((float)y/(float)x))*(ConfigTitleCards.imagecards.get(curImageIndex).getScaleX()/100f);
                    float scale_y = 0.25f*(ConfigTitleCards.imagecards.get(curImageIndex).getScaleY()/100f);
                    GlStateManager.scale(scale_x,scale_y,1f);

                    float posX = ((x*(1f/scale_x))/2f)-(x/2f);
                    float posY = (y*(1f/scale_y))/8f;
                    GuiScreen.drawModalRectWithCustomSizedTexture((int)((posX)+(ConfigTitleCards.imagecards.get(curImageIndex).getHorizontal()*(1/scale_x))),
                            (int)((posY)+(ConfigTitleCards.imagecards.get(curImageIndex).getVertical()*(1/scale_y))),x,y,x,y,x,y);

                    GlStateManager.color(1F, 1F, 1F, 1);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if(MusicPlayer.RELOAD.isKeyDown()) {
            BlockPos pos = MusicPicker.roundedPos(Minecraft.getMinecraft().player);
            if(!zone) Minecraft.getMinecraft().displayGuiScreen(new GuiMain(ConfigObject.createFromCurrent()));
            else if(!firstPass) {
                x1 = pos.getX();
                y1 = pos.getY();
                z1 = pos.getZ();
                firstPass = true;
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.MUSIC, 1f, 1f, pos));
            } else {
                x2 = pos.getX();
                y2 = pos.getY();
                z2 = pos.getZ();
                int temp;
                if(x1>x2) {
                    temp=x1;
                    x1=x2;
                    x2=temp;
                }
                if(y1>y2) {
                    temp=y1;
                    y1=y2;
                    y2=temp;
                }
                if(z1>z2) {
                    temp=z1;
                    z1=z2;
                    z2=temp;
                }
                firstPass = false;
                zone = false;
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.MUSIC, 1f, 1f, pos));
                String compiledZoneCoords = x1+","+y1+","+z1+","+x2+","+y2+","+z2;
                parentScreen.holder.editTriggerInfoParameter(parentScreen.songCode, parentScreen.trigger, parentScreen.scrollingSongs.index, compiledZoneCoords);
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if(!Minecraft.getMinecraft().isGamePaused() && !renderDebug) renderDebug = true;
        if(reloadCounter>0) {
            reloadCounter-=1;
            if(reloadCounter==1) {
                Reload.readAndReload();
                MusicPicker.player.sendMessage(new TextComponentString("\u00A7a\u00A7oFinished!"));
                IMAGE_CARD = null;
                fadeCount = 1000;
                timer = 0;
                activated = false;
                ismoving = false;
                MusicPlayer.fadingIn=false;
                MusicPlayer.fadingOut = false;
                MusicPlayer.curMusic = null;
                MusicPlayer.curTrack = null;
                MusicPlayer.curTrackList = null;
                MusicPlayer.cards = true;
                MusicPlayer.reloading = false;
            }
        }
    }

    @SubscribeEvent
    public static void debugInfo(RenderGameOverlayEvent.Text e) {
        if(ConfigDebug.ShowDebugInfo && isWorldRendered && renderDebug) {
            if(MusicPlayer.curTrackHolder!=null) e.getLeft().add("Music Triggers Current Song: " + MusicPlayer.curTrackHolder);
            if(!ConfigDebug.ShowJustCurSong) {
                int displayCount = 0;
                if(!MusicPlayer.formatSongTime().matches("No song playing")) e.getLeft().add("Music Triggers Current Song Time: " + MusicPlayer.formatSongTime());
                if(MusicPlayer.fadingOut) e.getLeft().add("Music Triggers Fading Out: "+MusicPlayer.formattedTimeFromMilliseconds(MusicPlayer.tempFadeOut*50));
                if(MusicPlayer.fadingIn) e.getLeft().add("Music Triggers Fading In: "+MusicPlayer.formattedTimeFromMilliseconds(MusicPlayer.tempFadeIn*50));
                if(MusicPicker.playableList!=null && !MusicPicker.playableList.isEmpty()) {
                    StringBuilder s = new StringBuilder();
                    for (String ev : MusicPicker.playableList) {
                        if(Minecraft.getMinecraft().fontRenderer.getStringWidth(s+" "+ev)>0.75f*(new ScaledResolution(Minecraft.getMinecraft())).getScaledWidth()) {
                            if(displayCount==0) {
                                e.getLeft().add("Music Triggers Playable Events: " + s);
                                displayCount++;
                            } else e.getLeft().add(s.toString());
                            s = new StringBuilder();
                        }
                        s.append(" ").append(ev);
                    }
                    if(displayCount==0) e.getLeft().add("Music Triggers Playable Events: " + s);
                    else e.getLeft().add(s.toString());
                }
                displayCount=0;
                StringBuilder sm = new StringBuilder();
                sm.append("minecraft");
                for (String ev : ConfigDebug.blockedmods) {
                    if(Minecraft.getMinecraft().fontRenderer.getStringWidth(sm+" "+ev)>0.75f*(new ScaledResolution(Minecraft.getMinecraft())).getScaledWidth()) {
                        if(displayCount==0) {
                            e.getLeft().add("Music Triggers Blocked Mods: " + sm);
                            displayCount++;
                        } else e.getLeft().add(sm.toString());
                        sm = new StringBuilder();
                    }
                    sm.append(" ").append(ev);
                }
                if(displayCount==0) e.getLeft().add("Music Triggers Blocked Mods: " + sm);
                else e.getLeft().add(sm.toString());
                displayCount=0;
                if(MusicPicker.player!=null && MusicPicker.world!=null) {
                    e.getLeft().add("Music Triggers Current Biome: " + MusicPicker.world.getBiome(MusicPicker.player.getPosition()).getRegistryName());
                    e.getLeft().add("Music Triggers Current Dimension: " + MusicPicker.player.dimension);
                    e.getLeft().add("Music Triggers Current Total Light: " + MusicPicker.world.getLight(MusicPicker.roundedPos(MusicPicker.player), true));
                    e.getLeft().add("Music Triggers Current Block Light: " + MusicPicker.world.getLightFor(EnumSkyBlock.BLOCK, MusicPicker.roundedPos(MusicPicker.player)));
                    if (MusicPicker.effectList != null && !MusicPicker.effectList.isEmpty()) {
                        StringBuilder se = new StringBuilder();
                        for (String ev : MusicPicker.effectList) {
                            if(Minecraft.getMinecraft().fontRenderer.getStringWidth(se+" "+ev)>0.75f*(new ScaledResolution(Minecraft.getMinecraft())).getScaledWidth()) {
                                if(displayCount==0) {
                                    e.getLeft().add("Music Triggers Effect List: " + se);
                                    displayCount++;
                                } else e.getLeft().add(se.toString());
                                se = new StringBuilder();
                            }
                            se.append(" ").append(ev);
                        }
                        if(displayCount==0) e.getLeft().add("Music Triggers Effect List: " + se);
                        else e.getLeft().add(se.toString());
                    }
                    if(Minecraft.getMinecraft().objectMouseOver != null) {
                        if (getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit) != null)
                            e.getLeft().add("Music Triggers Current Entity Name: " + getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit).getName());
                        try {
                            if (infernalChecker(getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit)) != null)
                                e.getLeft().add("Music Triggers Infernal Mob Mod Name: " + infernalChecker(getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit)));
                        } catch (NoSuchMethodError ignored) { }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderBoss(RenderGameOverlayEvent.BossInfo e) {
        if (bossBarCounter % 11 == 0) {
            RegistryHandler.network.sendToServer(new PacketBossInfo.packetBossInfoMessage(e.getBossInfo().getName().getUnformattedText(), e.getBossInfo().getPercent()));
            bossBarCounter = 0;
        }
        bossBarCounter++;
    }

    private static String infernalChecker(@Nullable EntityLiving m) {
        if(Loader.isModLoaded("infernalmobs") && m!=null) return InfernalMobsCore.getMobModifiers(m) == null ? null : InfernalMobsCore.getMobModifiers(m).getModName();
        return null;
    }

    private static EntityLiving getLivingFromEntity(Entity e) {
        if(e instanceof EntityLiving) return (EntityLiving) e;
        return null;
    }
}
