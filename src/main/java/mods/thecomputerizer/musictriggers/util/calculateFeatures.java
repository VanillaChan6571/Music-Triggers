package mods.thecomputerizer.musictriggers.util;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import mods.thecomputerizer.musictriggers.util.packets.InfoFromBiome;
import mods.thecomputerizer.musictriggers.util.packets.InfoFromMob;
import mods.thecomputerizer.musictriggers.util.packets.InfoFromStructure;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.*;

public class calculateFeatures {

    public static HashMap<Integer, Map<LivingEntity, Integer>> victoryMobs = new HashMap<>();
    private static boolean infernalLoaded = false;
    private static final HashMap<Integer, Boolean> dead = new HashMap<>();
    public static boolean boss;

    public static void calculateStructAndSend(String struct, BlockPos pos, UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server.getPlayerList().getPlayer(uuid)!=null) {
            ServerWorld world = server.getLevel(Objects.requireNonNull(server.getPlayerList().getPlayer(uuid)).level.dimension());
            if (world != null) {
                boolean good = false;
                String curStruct = null;
                for (Structure<?> structureFeature : net.minecraftforge.registries.ForgeRegistries.STRUCTURE_FEATURES) {
                    if(world.structureFeatureManager().getStructureAt(pos,true,structureFeature.getStructure()).isValid()) {
                        if(structureFeature.getRegistryName()!=null) {
                            curStruct = structureFeature.getRegistryName().toString().replace("minecraft:", "");
                            if(curStruct.matches(struct)) {
                                good = true;
                            }
                        }
                    }
                }
                PacketHandler.sendTo(new InfoFromStructure(good,struct,curStruct), Objects.requireNonNull(server.getPlayerList().getPlayer(uuid)));
            }
        }
    }

    public static void calculateBiomeAndSend(String biome, BlockPos pos, UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server.getPlayerList().getPlayer(uuid)!=null) {
            ServerWorld world = server.getLevel(Objects.requireNonNull(server.getPlayerList().getPlayer(uuid)).level.dimension());
            if (world != null) {
                String curBiome = Objects.requireNonNull(world.getBiome(pos).getRegistryName()).toString();
                if (curBiome.contains(biome)) {
                    PacketHandler.sendTo(new InfoFromBiome(true,biome,curBiome), Objects.requireNonNull(server.getPlayerList().getPlayer(uuid)));
                } else {
                    PacketHandler.sendTo(new InfoFromBiome(false,biome,curBiome), Objects.requireNonNull(server.getPlayerList().getPlayer(uuid)));
                }
            }
        }
    }

    public static void calculateMobAndSend(UUID uuid, String mobname, int detectionrange, boolean targetting, int targettingpercentage, int health, int healthpercentage, boolean victory, int victoryID, String i, int num, int persistence, int timeout) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayerEntity player = server.getPlayerList().getPlayer(uuid);
        assert player != null;
        ServerWorld world = server.getLevel(player.level.dimension());
        boolean pass = false;
        assert world != null;
        List<LivingEntity> mobTempList = world.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(player.getX() - detectionrange, player.getY() - (detectionrange / 2f), player.getZ() - detectionrange, player.getX() + detectionrange, player.getY() + (detectionrange / 2f), player.getZ() + detectionrange));
        List<MobEntity> mobList = new ArrayList<>();
        for (LivingEntity e : mobTempList) {
            if (e instanceof MobEntity) {
                mobList.add((MobEntity) e);
            }
        }
        int trackingCounter = 0;
        int healthCounter = 0;
        boolean infernal = true;
        boolean infernalChecked = false;
        boolean infernalDone = false;
        if (mobname.matches("MOB")) {
            for (Iterator<MobEntity> it = mobList.iterator(); it.hasNext(); ) {
                MobEntity e = it.next();
                boolean isMonster = true;
                if (e instanceof AnimalEntity) {
                    it.remove();
                    isMonster = false;
                }
                if (isMonster) {
                    if (e.getTarget() instanceof PlayerEntity) {
                        trackingCounter++;
                    }
                    if (e.getHealth() / e.getMaxHealth() <= health / 100F) {
                        healthCounter++;
                    }
                    infernalChecked = infernalChecker(e, i);
                    if (!infernalLoaded || (infernalLoaded && infernalChecked)) {
                        infernalDone = true;
                    }
                    if (victory) {
                        victoryMobs.computeIfAbsent(victoryID, k -> new HashMap<>());
                        if (!victoryMobs.get(victoryID).containsKey(e) && victoryMobs.get(victoryID).size() < num) {
                            victoryMobs.get(victoryID).put(e, timeout);
                        }
                    }
                }
            }
            if (mobList.size() >= num && ((!targetting || (float) trackingCounter / num >= targettingpercentage / 100F) && infernalDone && (float) healthCounter / num >= healthpercentage / 100F)) {
                pass = true;
            }
        } else if (mobname.matches("BOSS")) {
            //Map<UUID, ClientBossInfo> info = ObfuscationReflectionHelper.getPrivateValue(BossOverlayGui.class, mc.gui.getBossOverlay(), "field_184060_g");
            //if (!info.isEmpty()) {
                //pass = true;
            //}
        } else {
            int mobCounter = 0;
            List<MobEntity> mobListSpecific = new ArrayList<>();
            for (LivingEntity e : mobTempList) {
                if (e.getDisplayName().getString().matches(mobname) || Objects.requireNonNull(e.getType().getRegistryName()).toString().matches(mobname)) {
                    if(e instanceof  MobEntity) {
                        mobCounter++;
                        mobListSpecific.add((MobEntity) e);
                    }
                }
            }
            for (MobEntity e : mobListSpecific) {
                if (e.getTarget() instanceof PlayerEntity) {
                    trackingCounter++;
                }
                if (e.getHealth() / e.getMaxHealth() <= health / 100F) {
                    healthCounter++;
                }
                try {
                    infernalChecked = infernalChecker(e, i);
                } catch (NoSuchMethodError ignored) {
                    infernal = false;
                }
                if (!infernal || infernalChecked) {
                    infernalDone = true;
                }
                if (victory) {
                    victoryMobs.computeIfAbsent(victoryID, k -> new HashMap<>());
                    if (!victoryMobs.get(victoryID).containsKey(e) && victoryMobs.get(victoryID).size() < num) {
                        victoryMobs.get(victoryID).put(e, timeout);
                    }
                }
            }
            if (mobCounter >= num && ((!targetting || (float) trackingCounter / num >= targettingpercentage / 100F) && infernalDone && (float) healthCounter / num >= healthpercentage / 100F)) {
                pass = true;
            }
        }
        if (persistence > 0) {
            pass = true;
        } else {
            for (Map.Entry<Integer, Map<LivingEntity, Integer>> integerMapEntry : victoryMobs.entrySet()) {
                boolean alldead = true;
                for (Map.Entry<LivingEntity, Integer> entityLivingIntegerEntry : victoryMobs.get((integerMapEntry).getKey()).entrySet()) {
                    if (!entityLivingIntegerEntry.getKey().isDeadOrDying()) {
                        alldead = false;
                        break;
                    }
                }
                dead.put(victoryID, alldead);
            }
        }
        PacketHandler.sendTo(new InfoFromMob(mobname,pass),player);
    }

    private static boolean infernalChecker(LivingEntity m, String s) {
        if (ModList.get().isLoaded("infernalmobs")) {
            infernalLoaded = true;
            if (s == null) {
                return true;
            }
            return InfernalMobsCore.getMobModifiers(m).getModName().matches(s);
        }
        return false;
    }
}