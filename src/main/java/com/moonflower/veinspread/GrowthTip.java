package com.moonflower.veinspread;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class GrowthTip {
    public Location position;
    public BlockFace direction;
    public int energy;
    public int branchDepth;

    public GrowthTip(Location position, BlockFace direction, int energy, int branchDepth) {
        this.position = position;
        this.direction = direction;
        this.energy = energy;
        this.branchDepth = branchDepth;
    }

    public boolean isDead() {
        return energy <= 0;
    }
}