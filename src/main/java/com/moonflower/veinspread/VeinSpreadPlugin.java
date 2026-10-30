package com.moonflower.veinspread;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("override")
public class VeinSpreadPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private VeinConfig veinConfig;
    private VeinManager veinManager;
    private FileConfiguration messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultMessages();
        loadMessages();

        veinConfig = new VeinConfig(getConfig());

        veinManager = new VeinManager(this);
        veinManager.loadData();
        veinManager.start();

        Bukkit.getScheduler().runTaskTimer(this, () -> veinManager.saveData(), 6000L, 6000L);

        getServer().getPluginManager().registerEvents(this, this);
        
        PluginCommand veinCmd = getCommand("vein");
        if (veinCmd != null) {
            veinCmd.setExecutor(this);
            veinCmd.setTabCompleter(this);
        }

        getLogger().info(getConsoleMessage("plugin-enabled", 
                "%version%", getPluginMeta().getVersion(),
                "%catalyst%", veinConfig.catalystBlock.name()));
    }

    @Override
    public void onDisable() {
        if (veinManager != null) {
            veinManager.stop();
            veinManager.saveData();
        }
        getLogger().info(getConsoleMessage("plugin-disabled"));
    }

    public VeinConfig getVeinConfig() { return veinConfig; }

    private void saveDefaultMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
    }

    public void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }

    private @NotNull String getString(@NotNull CommandSender sender, @NotNull String key) {
        String lang = "en";
        if (sender instanceof Player p) {
            String locale = p.locale().toString(); 
            if (locale.startsWith("ru")) lang = "ru";
        }
        String path = lang + "." + key;
        String msg = messages.getString(path, messages.getString("en." + key, "Missing message: " + key));
        return msg.replace("&", "§");
    }

    public @NotNull String getConsoleMessage(@NotNull String key, @NotNull String... replacements) {
        String lang = getConfig().getString("console-language", "en").toLowerCase();
        String path = "console." + lang + "." + key;
        String msg = messages.getString(path, messages.getString("console.en." + key, "Missing console message: " + key));
        if (msg != null) {
            for (int i = 0; i < replacements.length; i += 2) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }
        return msg;
    }

    private void sendMsg(@NotNull CommandSender sender, @NotNull String key, @NotNull String... replacements) {
        String msg = getString(sender, key);
        for (int i = 0; i < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(msg);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        if (!s.hasPermission("veinspread.admin")) {
            sendMsg(s, "no-permission");
            return true;
        }
        if (a.length == 0) { sendHelp(s); return true; }

        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                reloadConfig();
                veinConfig.load(getConfig());
                loadMessages();
                veinManager.start();
                sendMsg(s, "reloaded");
            }
            case "info" -> {
                sendMsg(s, "info-header");
                sendMsg(s, "info-catalyst", "%value%", veinConfig.catalystBlock.name());
                sendMsg(s, "info-result", "%value%", veinConfig.resultBlock.name());
                sendMsg(s, "info-blocks", "%value%", String.valueOf(veinConfig.replaceableBlocks.size()));
                sendMsg(s, "info-growth-chance", "%value%", pct(veinConfig.growthChance));
                sendMsg(s, "info-thicken", "%value%", String.valueOf(veinConfig.thickenPerTick));
                sendMsg(s, "info-radius", "%value%", String.valueOf(veinConfig.maxThicknessRadius));
                sendMsg(s, "info-limit", "%value%", veinConfig.maxBlocksPerCatalyst == -1 ? "Unlimited" : String.valueOf(veinConfig.maxBlocksPerCatalyst));
                sendMsg(s, "info-energy", "%value%", String.valueOf(veinConfig.tipEnergy));
                sendMsg(s, "info-branch", "%value%", pct(veinConfig.branchChance));
                sendMsg(s, "info-persistence", "%value%", pct(veinConfig.persistence));
                sendMsg(s, "info-vertical", "%value%", pct(veinConfig.verticalBranchChance));
                sendMsg(s, "info-depth", "%value%", String.valueOf(veinConfig.maxBranchDepth));
                sendMsg(s, "info-maxtips", "%value%", String.valueOf(veinConfig.maxTipsPerCatalyst));
                String timeStr = veinConfig.timeEnabled ? (veinConfig.startTime + "–" + veinConfig.endTime) : getString(s, "info-time-off");
                sendMsg(s, "info-time", "%value%", timeStr);
            }
            case "list" -> {
                sendMsg(s, "list-catalysts", "%value%", String.valueOf(veinManager.getActiveCount()));
                sendMsg(s, "list-tips", "%value%", String.valueOf(veinManager.getTotalTips()));
            }
            case "blocks" -> {
                sendMsg(s, "blocks-header");
                if (veinConfig.replaceableBlocks.isEmpty()) {
                    sendMsg(s, "blocks-empty");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Material m : veinConfig.replaceableBlocks) {
                        sb.append("§f").append(m.name()).append("§7, ");
                    }
                    if (sb.length() > 2) sb.setLength(sb.length() - 2);
                    s.sendMessage(sb.toString());
                }
            }
            case "set" -> {
                if (a.length < 3) { sendMsg(s, "usage-set"); return true; }
                try { handleSet(s, a[1].toLowerCase(Locale.ROOT), a[2]); }
                catch (Exception e) { sendMsg(s, "error", "%message%", e.getMessage()); }
            }
            case "addblock" -> {
                if (a.length < 2) { sendMsg(s, "usage-add"); return true; }
                try {
                    Material m = Material.valueOf(a[1].toUpperCase(Locale.ROOT));
                    if (veinConfig.replaceableBlocks.contains(m)) { sendMsg(s, "block-already"); return true; }
                    veinConfig.replaceableBlocks.add(m);
                    saveReplaceableBlocks();
                    sendMsg(s, "block-added", "%block%", m.name(), "%size%", String.valueOf(veinConfig.replaceableBlocks.size()));
                } catch (IllegalArgumentException e) { sendMsg(s, "block-invalid", "%input%", a[1]); }
            }
            case "removeblock" -> {
                if (a.length < 2) { sendMsg(s, "usage-remove"); return true; }
                try {
                    Material m = Material.valueOf(a[1].toUpperCase(Locale.ROOT));
                    if (!veinConfig.replaceableBlocks.remove(m)) { sendMsg(s, "block-not-found"); return true; }
                    saveReplaceableBlocks();
                    sendMsg(s, "block-removed", "%block%", m.name(), "%size%", String.valueOf(veinConfig.replaceableBlocks.size()));
                } catch (IllegalArgumentException e) { sendMsg(s, "block-invalid", "%input%", a[1]); }
            }
            default -> sendHelp(s);
        }
        return true;
    }

    private void handleSet(CommandSender s, String param, String raw) {
        switch (param) {
            case "catalyst-block", "catalyst" -> {
                Material m = Material.valueOf(raw.toUpperCase(Locale.ROOT));
                veinConfig.catalystBlock = m;
                getConfig().set("vein.catalyst-block", m.name());
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", m.name());
            }
            case "result-block", "result" -> {
                Material m = Material.valueOf(raw.toUpperCase(Locale.ROOT));
                veinConfig.resultBlock = m;
                getConfig().set("vein.result-block", m.name());
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", m.name());
            }
            case "growth-chance", "chance" -> {
                double v = parsePercent(raw);
                veinConfig.growthChance = v;
                getConfig().set("growth-chance", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", pct(v));
            }
            case "thicken-per-tick", "thicken" -> {
                int v = Integer.parseInt(raw);
                if (v < 0) throw new IllegalArgumentException("min 0");
                veinConfig.thickenPerTick = v;
                getConfig().set("vein.thicken-per-tick", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "max-thickness-radius", "radius" -> {
                double v = Double.parseDouble(raw);
                if (v < 1) throw new IllegalArgumentException("min 1");
                veinConfig.maxThicknessRadius = v;
                getConfig().set("vein.max-thickness-radius", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "max-blocks-per-catalyst", "limit" -> {
                int v = Integer.parseInt(raw);
                if (v == 0 || v < -1) throw new IllegalArgumentException("must be -1 or > 0");
                veinConfig.maxBlocksPerCatalyst = v;
                getConfig().set("vein.max-blocks-per-catalyst", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", v == -1 ? "Unlimited" : String.valueOf(v));
            }
            case "call-event-on-thicken", "call-event" -> {
                boolean v = Boolean.parseBoolean(raw);
                veinConfig.callEventOnThicken = v;
                getConfig().set("vein.call-event-on-thicken", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "tip-energy", "energy" -> {
                int v = Integer.parseInt(raw);
                if (v < 1) throw new IllegalArgumentException("min 1");
                veinConfig.tipEnergy = v;
                getConfig().set("vein.tip-energy", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "branch-chance", "branch" -> {
                double v = parsePercent(raw);
                veinConfig.branchChance = v;
                getConfig().set("vein.branch-chance", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", pct(v));
            }
            case "max-branch-depth", "depth" -> {
                int v = Integer.parseInt(raw);
                if (v < 0) throw new IllegalArgumentException("min 0");
                veinConfig.maxBranchDepth = v;
                getConfig().set("vein.max-branch-depth", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "persistence", "inertia" -> {
                double v = parsePercent(raw);
                veinConfig.persistence = v;
                getConfig().set("vein.persistence", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", pct(v));
            }
            case "vertical-branch-chance", "vertical" -> {
                double v = parsePercent(raw);
                veinConfig.verticalBranchChance = v;
                getConfig().set("vein.vertical-branch-chance", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", pct(v));
            }
            case "max-tips-per-catalyst", "maxtips" -> {
                int v = Integer.parseInt(raw);
                if (v < 1) throw new IllegalArgumentException("min 1");
                veinConfig.maxTipsPerCatalyst = v;
                getConfig().set("vein.max-tips-per-catalyst", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "time-enabled", "time" -> {
                boolean v = Boolean.parseBoolean(raw);
                veinConfig.timeEnabled = v;
                getConfig().set("time-conditions.enabled", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "start-time" -> {
                long v = Long.parseLong(raw);
                veinConfig.startTime = v;
                getConfig().set("time-conditions.start-time", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            case "end-time" -> {
                long v = Long.parseLong(raw);
                veinConfig.endTime = v;
                getConfig().set("time-conditions.end-time", v);
                saveConfig();
                sendMsg(s, "set-success", "%param%", param, "%value%", String.valueOf(v));
            }
            default -> throw new IllegalArgumentException("unknown parameter");
        }
    }

    private void saveReplaceableBlocks() {
        List<String> names = new ArrayList<>();
        for (Material mm : veinConfig.replaceableBlocks) names.add(mm.name());
        getConfig().set("vein.replaceable-blocks", names);
        saveConfig();
    }

    private double parsePercent(String raw) {
        double v;
        if (raw.endsWith("%")) { v = Double.parseDouble(raw.substring(0, raw.length() - 1)) / 100.0; }
        else {
            v = Double.parseDouble(raw);
            if (v > 1.0) v /= 100.0;
        }
        if (v < 0 || v > 1) throw new IllegalArgumentException("must be 0–1 or 0–100%");
        return v;
    }

    private String pct(double v) { return String.format(Locale.ROOT, "%.0f%%", v * 100); }

    private void sendHelp(CommandSender s) {
        sendMsg(s, "help-header");
        sendMsg(s, "help-reload");
        sendMsg(s, "help-info");
        sendMsg(s, "help-list");
        sendMsg(s, "help-blocks");
        sendMsg(s, "help-set");
        sendMsg(s, "help-addblock");
        sendMsg(s, "help-removeblock");
        sendMsg(s, "help-params");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String alias, @NotNull String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String sub : List.of("reload", "info", "list", "blocks", "set", "addblock", "removeblock", "help")) {
                if (sub.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(sub);
            }
        } else if (a.length == 2) {
            if (a[0].equalsIgnoreCase("set")) {
                for (String p : List.of("catalyst-block", "result-block", "growth-chance", "thicken-per-tick", "max-thickness-radius", "max-blocks-per-catalyst", "call-event-on-thicken", "tip-energy",
                        "branch-chance", "max-branch-depth", "persistence", "vertical",
                        "maxtips", "time", "start-time", "end-time")) {
                    if (p.startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(p);
                }
            } else if (a[0].equalsIgnoreCase("addblock") || a[0].equalsIgnoreCase("removeblock")) {
                for (Material m : Material.values()) {
                    if (m.isBlock() && m.name().startsWith(a[1].toUpperCase(Locale.ROOT))) out.add(m.name());
                }
            }
        } else if (a.length == 3 && a[0].equalsIgnoreCase("set")) {
            String p = a[1].toLowerCase(Locale.ROOT);
            switch (p) {
                case "growth-chance", "branch-chance", "persistence", "vertical" -> out.addAll(List.of("1%", "5%", "10%", "25%", "50%"));
                case "thicken-per-tick", "thicken" -> out.addAll(List.of("5", "10", "20", "50"));
                case "max-thickness-radius", "radius" -> out.addAll(List.of("5", "10", "15", "20"));
                case "max-blocks-per-catalyst", "limit" -> out.addAll(List.of("-1", "1000", "5000", "10000"));
                case "call-event-on-thicken", "call-event" -> out.addAll(List.of("true", "false"));
                case "tip-energy" -> out.addAll(List.of("6", "12", "20", "30"));
                case "max-branch-depth" -> out.addAll(List.of("2", "5", "10", "15"));
                case "maxtips" -> out.addAll(List.of("10", "50", "100", "200", "500"));
                case "time" -> out.addAll(List.of("true", "false"));
                case "catalyst-block", "result-block" -> {
                    for (Material m : Material.values()) {
                        if (m.isBlock() && m.name().startsWith(a[2].toUpperCase(Locale.ROOT))) out.add(m.name());
                    }
                }
            }
        }
        return out;
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent e) {
        if (e.getBlock().getType() == veinConfig.catalystBlock) {
            veinManager.registerCatalyst(e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        if (e.getBlock().getType() == veinConfig.catalystBlock) {
            veinManager.unregisterCatalyst(e.getBlock().getLocation());
        }
        if (e.getBlock().getType() == veinConfig.resultBlock) {
            veinManager.onBlockDestroyed(e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBlockExplode(@NotNull BlockExplodeEvent e) {
        for (org.bukkit.block.Block b : e.blockList()) {
            if (b.getType() == veinConfig.catalystBlock) {
                veinManager.unregisterCatalyst(b.getLocation());
            }
            if (b.getType() == veinConfig.resultBlock) {
                veinManager.onBlockDestroyed(b.getLocation());
            }
        }
    }

    @EventHandler
    public void onEntityExplode(@NotNull EntityExplodeEvent e) {
        for (org.bukkit.block.Block b : e.blockList()) {
            if (b.getType() == veinConfig.catalystBlock) {
                veinManager.unregisterCatalyst(b.getLocation());
            }
            if (b.getType() == veinConfig.resultBlock) {
                veinManager.onBlockDestroyed(b.getLocation());
            }
        }
    }
}