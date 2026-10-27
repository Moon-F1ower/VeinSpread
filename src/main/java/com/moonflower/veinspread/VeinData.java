package com.moonflower.veinspread;

import java.util.ArrayList;
import java.util.List;

public class VeinData {
    public String world;
    public int catX, catY, catZ;
    public List<SavedTip> tips = new ArrayList<>();

    public static class SavedTip {
        public int x, y, z;
        public String direction;
        public int energy;
        public int branchDepth;
    }
}