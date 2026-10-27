package com.moonflower.veinspread;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class VeinManager extends BukkitRunnable {

    private final VeinSpreadPlugin plugin;
    private final Map<Location, List<GrowthTip>> activeTips = new HashMap<>();
    private BukkitTask task;

    public VeinManager(VeinSpreadPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(int intervalTicks) {
        stop();
        this.task = runTaskTimer(plugin, 20L, intervalTicks);
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
        List<GrowthTip> tips = new ArrayList<>();
        for (BlockFace face : new BlockFace[]{
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN}) {
            tips.add(new GrowthTip(loc.clone(), face, plugin.getVeinConfig().tipEnergy, 0));
        }
        activeTips.put(loc, tips);
    }

    public void unregisterCatalyst(Location loc) {
        activeTips.remove(loc);
    }

    @Override
    public void run() {
        if (!isActiveTime()) return;
        VeinConfig cfg = plugin.getVeinConfig();
        Iterator<Map.Entry<Location, List<GrowthTip>>> it = activeTips.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Location, List<GrowthTip>> entry = it.next();
            List<GrowthTip> tips = entry.getValue();

            tips.removeIf(GrowthTip::isDead);
            if (tips.isEmpty()) { it.remove(); continue; }

            List<GrowthTip> newBranches = new ArrayList<>();

            for (GrowthTip tip : new ArrayList<>(tips)) {
                if (tip.isDead()) continue;

                Block next = tip.position.getBlock().getRelative(tip.direction);
                if (!next.getChunk().isLoaded()) { tip.energy = 0; continue; }
                if (!cfg.replaceableBlocks.contains(next.getType())) { tip.energy = 0; continue; }

                next.setType(cfg.resultBlock, false);
                tip.position = next.getLocation();
                tip.energy--;

                if (Math.random() > cfg.persistence) tip.direction = rotateRandom(tip.direction);

                if (tip.branchDepth < cfg.maxBranchDepth && Math.random() < cfg.branchChance) {
                    newBranches.add(new GrowthTip(next.getLocation(), rotateRandom(tip.direction),
                            cfg.tipEnergy / 2, tip.branchDepth + 1));
                }
                if (Math.random() < cfg.verticalBranchChance) {
                    newBranches.add(new GrowthTip(next.getLocation(), BlockFace.UP,
                            cfg.tipEnergy / 3, tip.branchDepth + 1));
                }
            }

            int canAdd = cfg.maxTipsPerCatalyst - tips.size();
            if (canAdd > 0) tips.addAll(newBranches.subList(0, Math.min(canAdd, newBranches.size())));
        }
    }

    private boolean isActiveTime() {
        VeinConfig cfg = plugin.getVeinConfig();
        if (!cfg.timeEnabled) return true;
        World world = Bukkit.getWorlds().get(0);
        long t = world.getTime();
        return cfg.startTime <= cfg.endTime
                ? t >= cfg.startTime && t <= cfg.endTime
                : t >= cfg.startTime || t <= cfg.endTime;
    }

    private BlockFace rotateRandom(BlockFace face) {
        BlockFace[] all = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
                BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        return all[new Random().nextInt(all.length)];
    }
}