package com.moonflower.veinspread;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class VeinConfig {
    public Material catalystBlock;
    public Material resultBlock;
    public List<Material> replaceableBlocks = new ArrayList<>();
    public int intervalTicks;
    public int tipEnergy;
    public double branchChance;
    public int maxBranchDepth;
    public double persistence;
    public double verticalBranchChance;
    public int maxTipsPerCatalyst;
    public boolean timeEnabled;
    public long startTime;
    public long endTime;

    public VeinConfig(FileConfiguration cfg) {
        load(cfg);
    }

    public void load(FileConfiguration cfg) {
        this.catalystBlock = Material.valueOf(cfg.getString("vein.catalyst-block", "SCULK_CATALYST"));
        this.resultBlock = Material.valueOf(cfg.getString("vein.result-block", "SCULK"));

        this.replaceableBlocks = new ArrayList<>();
        for (String s : cfg.getStringList("vein.replaceable-blocks")) {
            try { this.replaceableBlocks.add(Material.valueOf(s)); }
            catch (IllegalArgumentException ignored) {}
        }

        this.intervalTicks = cfg.getInt("vein.interval-ticks", 100);
        this.tipEnergy = cfg.getInt("vein.tip-energy", 12);
        this.branchChance = cfg.getDouble("vein.branch-chance", 0.2);
        this.maxBranchDepth = cfg.getInt("vein.max-branch-depth", 3);
        this.persistence = cfg.getDouble("vein.persistence", 0.7);
        this.verticalBranchChance = cfg.getDouble("vein.vertical-branch-chance", 0.1);
        this.maxTipsPerCatalyst = cfg.getInt("vein.max-tips-per-catalyst", 8);
        this.timeEnabled = cfg.getBoolean("time-conditions.enabled", false);
        this.startTime = cfg.getLong("time-conditions.start-time", 13000);
        this.endTime = cfg.getLong("time-conditions.end-time", 23000);
    }
}