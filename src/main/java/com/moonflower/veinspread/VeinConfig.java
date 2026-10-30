package com.moonflower.veinspread;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class VeinConfig {
    public Material catalystBlock;
    public Material resultBlock;
    public List<Material> replaceableBlocks = new ArrayList<>();
    public double growthChance;
    public int tipEnergy;
    public double branchChance;
    public int maxBranchDepth;
    public double persistence;
    public double verticalBranchChance;
    public int thickenPerTick;
    public double maxThicknessRadius;
    public int maxBlocksPerCatalyst;
    public int maxTipsPerCatalyst;
    public boolean timeEnabled;
    public long startTime;
    public long endTime;
    public boolean callEventOnThicken;

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

        this.growthChance = cfg.getDouble("growth-chance", 0.02);
        this.tipEnergy = cfg.getInt("vein.tip-energy", 25);
        this.branchChance = cfg.getDouble("vein.branch-chance", 0.35);
        this.maxBranchDepth = cfg.getInt("vein.max-branch-depth", 10);
        this.persistence = cfg.getDouble("vein.persistence", 0.5);
        this.verticalBranchChance = cfg.getDouble("vein.vertical-branch-chance", 0.15);
        this.thickenPerTick = cfg.getInt("vein.thicken-per-tick", 5);
        this.maxThicknessRadius = cfg.getDouble("vein.max-thickness-radius", 12.0);
        this.maxBlocksPerCatalyst = cfg.getInt("vein.max-blocks-per-catalyst", 5000);
        this.maxTipsPerCatalyst = cfg.getInt("vein.max-tips-per-catalyst", 500);
        this.timeEnabled = cfg.getBoolean("time-conditions.enabled", false);
        this.startTime = cfg.getLong("time-conditions.start-time", 13000);
        this.endTime = cfg.getLong("time-conditions.end-time", 23000);
        this.callEventOnThicken = cfg.getBoolean("vein.call-event-on-thicken", false);
    }
}