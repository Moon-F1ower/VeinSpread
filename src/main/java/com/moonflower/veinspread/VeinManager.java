package com.moonflower.veinspread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

public class VeinManager extends BukkitRunnable {

    private final VeinSpreadPlugin plugin;
    private final Map<Location, List<GrowthTip>> activeTips = new HashMap<>();
    private final Map<Location, Set<Location>> placedBlocks = new HashMap<>();
    private final Map<Location, List<Location>> activeThickenBlocks = new HashMap<>();
    private final Map<Location, Integer> totalPlaced = new HashMap<>();
    private final Map<Location, Location> blockOwner = new HashMap<>();
    private BukkitTask task;
    private final Random random = new Random();

    private final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    public VeinManager(VeinSpreadPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        this.task = runTaskTimer(plugin, 20L, 1L);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) task.cancel();
        task = null;
    }

    public int getActiveCount() { return activeTips.size(); }

    public int getTotalTips() {
        return activeTips.values().stream().mapToInt(List::size).sum();
    }

    public void registerCatalyst(Location loc) {
        if (activeTips.containsKey(loc)) return;
        activeTips.put(loc, new ArrayList<>());
        placedBlocks.put(loc, new HashSet<>());
        activeThickenBlocks.put(loc, new ArrayList<>());
        totalPlaced.put(loc, 0);
    }

    public void unregisterCatalyst(Location loc) {
        activeTips.remove(loc);
        Set<Location> placed = placedBlocks.remove(loc);
        if (placed != null) {
            for (Location blockLoc : placed) {
                blockOwner.remove(blockLoc);
            }
        }
        activeThickenBlocks.remove(loc);
        totalPlaced.remove(loc);
    }

    public void onBlockDestroyed(Location loc) {
        Location catLoc = blockOwner.remove(loc);
        if (catLoc == null) return;

        Set<Location> placed = placedBlocks.get(catLoc);
        if (placed != null) placed.remove(loc);

        List<Location> thickenList = activeThickenBlocks.get(catLoc);
        if (thickenList != null) thickenList.remove(loc);

        int total = totalPlaced.getOrDefault(catLoc, 0);
        if (total > 0) totalPlaced.put(catLoc, total - 1);
    }

    public void saveData() {
        Map<Location, List<GrowthTip>> tipsSnapshot = new HashMap<>();
        for (Map.Entry<Location, List<GrowthTip>> entry : activeTips.entrySet()) {
            List<GrowthTip> tipsCopy = new ArrayList<>();
            for (GrowthTip tip : entry.getValue()) {
                tipsCopy.add(new GrowthTip(tip.position.clone(), tip.direction, tip.energy, tip.branchDepth));
            }
            tipsSnapshot.put(entry.getKey().clone(), tipsCopy);
        }

        Map<Location, Integer> totalSnapshot = new HashMap<>(totalPlaced);
        Map<Location, Set<Location>> placedSnapshot = new HashMap<>();
        for (Map.Entry<Location, Set<Location>> entry : placedBlocks.entrySet()) {
            placedSnapshot.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        Runnable saveTask = () -> {
            List<VeinData> dataList = new ArrayList<>();
            for (Map.Entry<Location, List<GrowthTip>> entry : tipsSnapshot.entrySet()) {
                Location loc = entry.getKey();
                VeinData data = new VeinData();
                data.world = loc.getWorld().getName();
                data.catX = loc.getBlockX();
                data.catY = loc.getBlockY();
                data.catZ = loc.getBlockZ();
                data.totalPlaced = totalSnapshot.getOrDefault(loc, 0);

                for (GrowthTip tip : entry.getValue()) {
                    VeinData.SavedTip st = new VeinData.SavedTip();
                    st.x = tip.position.getBlockX();
                    st.y = tip.position.getBlockY();
                    st.z = tip.position.getBlockZ();
                    st.direction = tip.direction.name();
                    st.energy = tip.energy;
                    st.branchDepth = tip.branchDepth;
                    data.tips.add(st);
                }

                Set<Location> blocks = placedSnapshot.get(loc);
                if (blocks != null) {
                    for (Location bLoc : blocks) {
                        VeinData.SavedLoc sl = new VeinData.SavedLoc();
                        sl.x = bLoc.getBlockX();
                        sl.y = bLoc.getBlockY();
                        sl.z = bLoc.getBlockZ();
                        data.placed.add(sl);
                    }
                }

                dataList.add(data);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            File file = new File(plugin.getDataFolder(), "veins.json");
            try {
                Files.writeString(file.toPath(), gson.toJson(dataList));
            } catch (IOException e) {
                plugin.getLogger().severe(plugin.getConsoleMessage("save-error", "%message%", e.getMessage()));
            }
        };

        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }

    public void loadData() {
        File file = new File(plugin.getDataFolder(), "veins.json");
        if (!file.exists()) return;

        try {
            String json = Files.readString(file.toPath());
            Type listType = new TypeToken<ArrayList<VeinData>>(){}.getType();
            List<VeinData> dataList = new Gson().fromJson(json, listType);

            if (dataList == null) return;

            activeTips.clear();
            placedBlocks.clear();
            activeThickenBlocks.clear();
            totalPlaced.clear();
            blockOwner.clear();

            for (VeinData data : dataList) {
                World world = Bukkit.getWorld(data.world);
                if (world == null) continue;

                Location catLoc = new Location(world, data.catX, data.catY, data.catZ);
                List<GrowthTip> tips = new ArrayList<>();
                for (VeinData.SavedTip st : data.tips) {
                    Location tipLoc = new Location(world, st.x, st.y, st.z);
                    BlockFace face = BlockFace.valueOf(st.direction);
                    tips.add(new GrowthTip(tipLoc, face, st.energy, st.branchDepth));
                }
                activeTips.put(catLoc, tips);
                totalPlaced.put(catLoc, data.totalPlaced);

                Set<Location> blocks = new HashSet<>();
                List<Location> thickenList = new ArrayList<>();
                for (VeinData.SavedLoc sl : data.placed) {
                    Location bLoc = new Location(world, sl.x, sl.y, sl.z);
                    blocks.add(bLoc);
                    thickenList.add(bLoc);
                    blockOwner.put(bLoc, catLoc);
                }
                placedBlocks.put(catLoc, blocks);
                activeThickenBlocks.put(catLoc, thickenList);
            }
            plugin.getLogger().info(plugin.getConsoleMessage("loaded-veins", "%amount%", String.valueOf(activeTips.size())));
        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getConsoleMessage("load-error", "%message%", e.getMessage()));
        }
    }

    @Override
    public void run() {
        if (!isActiveTime()) return;
        VeinConfig cfg = plugin.getVeinConfig();
        Iterator<Map.Entry<Location, List<GrowthTip>>> it = activeTips.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Location, List<GrowthTip>> entry = it.next();
            Location catalystLoc = entry.getKey();
            List<GrowthTip> tips = entry.getValue();
            Set<Location> placed = placedBlocks.computeIfAbsent(catalystLoc, k -> new HashSet<>());
            List<Location> thickenList = activeThickenBlocks.computeIfAbsent(catalystLoc, k -> new ArrayList<>());
            int total = totalPlaced.getOrDefault(catalystLoc, 0);

            tips.removeIf(GrowthTip::isDead);

            if (tips.isEmpty()) {
                if (catalystLoc.getBlock().getType() == cfg.catalystBlock) {
                    if (cfg.maxBlocksPerCatalyst > 0 && total >= cfg.maxBlocksPerCatalyst) {
                        continue;
                    }
                    if (random.nextDouble() < 0.05) {
                        for (BlockFace face : FACES) {
                            tips.add(new GrowthTip(catalystLoc.clone(), face, cfg.tipEnergy, 0));
                        }
                    }
                } else {
                    unregisterCatalyst(catalystLoc);
                    it.remove();
                    continue;
                }
            }

            List<GrowthTip> newBranches = new ArrayList<>();

            for (GrowthTip tip : new ArrayList<>(tips)) {
                if (tip.isDead()) continue;

                if (cfg.maxBlocksPerCatalyst > 0 && total >= cfg.maxBlocksPerCatalyst) {
                    tip.energy = 0;
                    continue;
                }

                if (random.nextDouble() > cfg.growthChance) {
                    continue;
                }

                Block next = tip.position.getBlock().getRelative(tip.direction);

                if (!next.getChunk().isLoaded()) continue;

                if (next.getType() == cfg.resultBlock) {
                    tip.position = next.getLocation();
                    tip.energy--;
                    continue;
                }

                if (!cfg.replaceableBlocks.contains(next.getType())) {
                    tip.energy = 0;
                    continue;
                }

                BlockSpreadEvent spreadEvent = new BlockSpreadEvent(next, tip.position.getBlock(), cfg.resultBlock.createBlockData().createBlockState());
                Bukkit.getPluginManager().callEvent(spreadEvent);

                if (!spreadEvent.isCancelled()) {
                    next.setType(cfg.resultBlock, false);
                    if (placed.add(next.getLocation())) {
                        thickenList.add(next.getLocation());
                        blockOwner.put(next.getLocation(), catalystLoc);
                    }
                    total++;
                    totalPlaced.put(catalystLoc, total);

                    tip.position = next.getLocation();
                    tip.energy--;
                } else {
                    tip.energy = 0;
                    continue;
                }

                if (random.nextDouble() > cfg.persistence) {
                    tip.direction = getPerpendicularDirection(tip.direction);
                }

                if (tip.branchDepth < cfg.maxBranchDepth && random.nextDouble() < cfg.branchChance) {
                    int branchEnergy = (int)(tip.energy * 0.7);
                    BlockFace branchDir = getPerpendicularDirection(tip.direction);
                    if (canGrow(tip.position.getBlock().getRelative(branchDir), cfg)) {
                        newBranches.add(new GrowthTip(tip.position.clone(), branchDir, branchEnergy, tip.branchDepth + 1));
                    }
                }

                if (random.nextDouble() < cfg.verticalBranchChance) {
                    BlockFace vertDir = random.nextBoolean() ? BlockFace.UP : BlockFace.DOWN;
                    if (vertDir != tip.direction.getOppositeFace() && canGrow(tip.position.getBlock().getRelative(vertDir), cfg)) {
                        newBranches.add(new GrowthTip(tip.position.clone(), vertDir, cfg.tipEnergy / 3, tip.branchDepth + 1));
                    }
                }
            }

            if (!thickenList.isEmpty() && cfg.thickenPerTick > 0) {
                int checks = Math.min(cfg.thickenPerTick, thickenList.size());
                double maxRadSq = cfg.maxThicknessRadius * cfg.maxThicknessRadius;
                List<Location> toRemoveThick = new ArrayList<>();

                for (int i = 0; i < checks; i++) {
                    if (cfg.maxBlocksPerCatalyst > 0 && total >= cfg.maxBlocksPerCatalyst) {
                        break;
                    }

                    Location checkLoc = thickenList.get(random.nextInt(thickenList.size()));
                    double distSq = checkLoc.distanceSquared(catalystLoc);

                    if (checkLoc.getBlock().getType() != cfg.resultBlock) {
                        toRemoveThick.add(checkLoc);
                        continue;
                    }

                    if (distSq >= maxRadSq) {
                        toRemoveThick.add(checkLoc);
                        continue;
                    }

                    boolean hasFreeNeighbor = false;
                    for (BlockFace face : FACES) {
                        if (cfg.replaceableBlocks.contains(checkLoc.getBlock().getRelative(face).getType())) {
                            hasFreeNeighbor = true; break;
                        }
                    }

                    if (!hasFreeNeighbor) {
                        toRemoveThick.add(checkLoc);
                        continue;
                    }

                    double chance = 1.0 - (distSq / maxRadSq);
                    if (random.nextDouble() < chance) {
                        BlockFace randFace = FACES[random.nextInt(FACES.length)];
                        Block neighbor = checkLoc.getBlock().getRelative(randFace);
                        if (neighbor.getChunk().isLoaded() && cfg.replaceableBlocks.contains(neighbor.getType())) {

                            if (cfg.callEventOnThicken) {
                                BlockSpreadEvent thickenEvent = new BlockSpreadEvent(neighbor, checkLoc.getBlock(), cfg.resultBlock.createBlockData().createBlockState());
                                Bukkit.getPluginManager().callEvent(thickenEvent);
                                if (thickenEvent.isCancelled()) continue;
                            }

                            neighbor.setType(cfg.resultBlock, false);
                            if (placed.add(neighbor.getLocation())) {
                                thickenList.add(neighbor.getLocation());
                                blockOwner.put(neighbor.getLocation(), catalystLoc);
                            }
                            total++;
                            totalPlaced.put(catalystLoc, total);
                        }
                    }
                }
                for (Location loc : toRemoveThick) {
                    thickenList.remove(loc);
                    placed.remove(loc);
                    blockOwner.remove(loc);
                }
            }

            int canAdd = cfg.maxTipsPerCatalyst - tips.size();
            if (canAdd > 0) tips.addAll(newBranches.subList(0, Math.min(canAdd, newBranches.size())));
        }
    }

    private boolean canGrow(Block block, VeinConfig cfg) {
        if (!block.getChunk().isLoaded()) return false;
        return block.getType() == cfg.resultBlock || cfg.replaceableBlocks.contains(block.getType());
    }

    private BlockFace getPerpendicularDirection(BlockFace current) {
        if (current == BlockFace.UP || current == BlockFace.DOWN) {
            BlockFace[] horiz = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            return horiz[random.nextInt(4)];
        } else if (current == BlockFace.NORTH || current == BlockFace.SOUTH) {
            BlockFace[] perp = {BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
            return perp[random.nextInt(4)];
        } else {
            BlockFace[] perp = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN};
            return perp[random.nextInt(4)];
        }
    }

    private boolean isActiveTime() {
        VeinConfig cfg = plugin.getVeinConfig();
        if (!cfg.timeEnabled) return true;
        World world = Bukkit.getWorlds().getFirst();
        long t = world.getTime();
        return cfg.startTime <= cfg.endTime
                ? t >= cfg.startTime && t <= cfg.endTime
                : t >= cfg.startTime || t <= cfg.endTime;
    }
}
