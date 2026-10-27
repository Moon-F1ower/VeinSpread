package com.moonflower.veinspread;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VeinSpreadPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private VeinConfig veinConfig;
    private VeinManager veinManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        veinConfig = new VeinConfig(getConfig());

        veinManager = new VeinManager(this);
        veinManager.loadData();
        veinManager.start(veinConfig.intervalTicks);

        Bukkit.getScheduler().runTaskTimer(this, () -> veinManager.saveData(), 6000L, 6000L);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("vein").setExecutor(this);
        getCommand("vein").setTabCompleter(this);

        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║  VeinSpread v1.1 — by Moon Flower  ║");
        getLogger().info("║  Catalyst: " + veinConfig.catalystBlock);
        getLogger().info("╚══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (veinManager != null) {
            veinManager.stop();
            veinManager.saveData();
        }
        getLogger().info("VeinSpread by Moon Flower — выключен.");
    }

    public VeinConfig getVeinConfig() { return veinConfig; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("veinspread.admin")) {
            s.sendMessage("§cНет прав.");
            return true;
        }
        if (a.length == 0) { sendHelp(s); return true; }

        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                reloadConfig();
                veinConfig.load(getConfig());
                veinManager.start(veinConfig.intervalTicks);
                s.sendMessage("§aКонфиг перезагружен. Интервал: §f" + veinConfig.intervalTicks + "т");
            }
            case "info" -> {
                s.sendMessage("§6=== §eVeinSpread §7by §dMoon Flower §6===");
                s.sendMessage("§eКатализатор: §f" + veinConfig.catalystBlock);
                s.sendMessage("§eРезультат: §f" + veinConfig.resultBlock);
                s.sendMessage("§eЗаменяемых блоков: §f" + veinConfig.replaceableBlocks.size());
                s.sendMessage("§eИнтервал: §f" + veinConfig.intervalTicks + " тиков");
                s.sendMessage("§eЭнергия кончика: §f" + veinConfig.tipEnergy);
                s.sendMessage("§eШанс ветвления: §f" + pct(veinConfig.branchChance));
                s.sendMessage("§eИнерция (persistence): §f" + pct(veinConfig.persistence));
                s.sendMessage("§eВерт. ветвление: §f" + pct(veinConfig.verticalBranchChance));
                s.sendMessage("§eМакс. глубина веток: §f" + veinConfig.maxBranchDepth);
                s.sendMessage("§eМакс. кончиков: §f" + veinConfig.maxTipsPerCatalyst);
                s.sendMessage("§eВремя: §f" + (veinConfig.timeEnabled
                        ? veinConfig.startTime + "–" + veinConfig.endTime : "выключено"));
            }
            case "list" -> {
                s.sendMessage("§eАктивных катализаторов: §f" + veinManager.getActiveCount());
                s.sendMessage("§eЖивых кончиков: §f" + veinManager.getTotalTips());
            }
            case "set" -> {
                if (a.length < 3) { s.sendMessage("§cИспользование: /vein set <параметр> <значение>"); return true; }
                try { handleSet(s, a[1].toLowerCase(Locale.ROOT), a[2]); }
                catch (Exception e) { s.sendMessage("§cОшибка: §f" + e.getMessage()); }
            }
            case "addblock" -> {
                if (a.length < 2) { s.sendMessage("§cИспользование: /vein addblock <Material>"); return true; }
                try {
                    Material m = Material.valueOf(a[1].toUpperCase(Locale.ROOT));
                    if (veinConfig.replaceableBlocks.contains(m)) { s.sendMessage("§cУже в списке."); return true; }
                    veinConfig.replaceableBlocks.add(m);
                    List<String> names = new ArrayList<>();
                    for (Material mm : veinConfig.replaceableBlocks) names.add(mm.name());
                    getConfig().set("vein.replaceable-blocks", names);
                    saveConfig();
                    s.sendMessage("§aДобавлено: §f" + m + " §7(всего: " + veinConfig.replaceableBlocks.size() + ")");
                } catch (IllegalArgumentException e) { s.sendMessage("§cНеизвестный Material: §f" + a[1]); }
            }
            case "removeblock" -> {
                if (a.length < 2) { s.sendMessage("§cИспользование: /vein removeblock <Material>"); return true; }
                try {
                    Material m = Material.valueOf(a[1].toUpperCase(Locale.ROOT));
                    if (!veinConfig.replaceableBlocks.remove(m)) { s.sendMessage("§cЭтого блока и не было в списке."); return true; }
                    List<String> names = new ArrayList<>();
                    for (Material mm : veinConfig.replaceableBlocks) names.add(mm.name());
                    getConfig().set("vein.replaceable-blocks", names);
                    saveConfig();
                    s.sendMessage("§aУдалено: §f" + m + " §7(осталось: " + veinConfig.replaceableBlocks.size() + ")");
                } catch (IllegalArgumentException e) { s.sendMessage("§cНеизвестный Material: §f" + a[1]); }
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
                s.sendMessage("§aКатализатор: §f" + m);
            }
            case "result-block", "result" -> {
                Material m = Material.valueOf(raw.toUpperCase(Locale.ROOT));
                veinConfig.resultBlock = m;
                getConfig().set("vein.result-block", m.name());
                saveConfig();
                s.sendMessage("§aРезультат: §f" + m);
            }
            case "interval", "interval-ticks" -> {
                int v = Integer.parseInt(raw);
                if (v < 20) throw new IllegalArgumentException("минимум 20 тиков (1 секунда)");
                veinConfig.intervalTicks = v;
                getConfig().set("vein.interval-ticks", v);
                saveConfig();
                veinManager.start(v);
                s.sendMessage("§aИнтервал: §f" + v + " тиков §7(" + (v / 20.0) + "с)");
            }
            case "tip-energy", "energy" -> {
                int v = Integer.parseInt(raw);
                if (v < 1) throw new IllegalArgumentException("минимум 1");
                veinConfig.tipEnergy = v;
                getConfig().set("vein.tip-energy", v);
                saveConfig();
                s.sendMessage("§aЭнергия кончика: §f" + v);
            }
            case "branch-chance", "branch" -> {
                double v = parsePercent(raw);
                veinConfig.branchChance = v;
                getConfig().set("vein.branch-chance", v);
                saveConfig();
                s.sendMessage("§aШанс ветвления: §f" + pct(v));
            }
            case "max-branch-depth", "depth" -> {
                int v = Integer.parseInt(raw);
                if (v < 0) throw new IllegalArgumentException("минимум 0");
                veinConfig.maxBranchDepth = v;
                getConfig().set("vein.max-branch-depth", v);
                saveConfig();
                s.sendMessage("§aМакс. глубина веток: §f" + v);
            }
            case "persistence", "inertia" -> {
                double v = parsePercent(raw);
                veinConfig.persistence = v;
                getConfig().set("vein.persistence", v);
                saveConfig();
                s.sendMessage("§aИнерция: §f" + pct(v));
            }
            case "vertical-branch-chance", "vertical" -> {
                double v = parsePercent(raw);
                veinConfig.verticalBranchChance = v;
                getConfig().set("vein.vertical-branch-chance", v);
                saveConfig();
                s.sendMessage("§aВерт. ветвление: §f" + pct(v));
            }
            case "max-tips-per-catalyst", "maxtips" -> {
                int v = Integer.parseInt(raw);
                if (v < 1) throw new IllegalArgumentException("минимум 1");
                veinConfig.maxTipsPerCatalyst = v;
                getConfig().set("vein.max-tips-per-catalyst", v);
                saveConfig();
                s.sendMessage("§aМакс. кончиков: §f" + v);
            }
            case "time-enabled", "time" -> {
                boolean v = Boolean.parseBoolean(raw);
                veinConfig.timeEnabled = v;
                getConfig().set("time-conditions.enabled", v);
                saveConfig();
                s.sendMessage("§aУсловие по времени: §f" + (v ? "включено" : "выключено"));
            }
            case "start-time" -> {
                long v = Long.parseLong(raw);
                veinConfig.startTime = v;
                getConfig().set("time-conditions.start-time", v);
                saveConfig();
                s.sendMessage("§aСтарт-время: §f" + v);
            }
            case "end-time" -> {
                long v = Long.parseLong(raw);
                veinConfig.endTime = v;
                getConfig().set("time-conditions.end-time", v);
                saveConfig();
                s.sendMessage("§aКонец-время: §f" + v);
            }
            default -> throw new IllegalArgumentException("неизвестный параметр");
        }
    }

    private double parsePercent(String raw) {
        double v;
        if (raw.endsWith("%")) { v = Double.parseDouble(raw.substring(0, raw.length() - 1)) / 100.0; }
        else {
            v = Double.parseDouble(raw);
            if (v > 1.0) v /= 100.0;
        }
        if (v < 0 || v > 1) throw new IllegalArgumentException("должно быть 0–1 или 0–100%");
        return v;
    }

    private String pct(double v) { return String.format(Locale.ROOT, "%.0f%%", v * 100); }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§6=== §eVeinSpread §7by §dMoon Flower §6===");
        s.sendMessage("§e/vein reload §7— перезагрузить конфиг");
        s.sendMessage("§e/vein info §7— текущие настройки");
        s.sendMessage("§e/vein list §7— активные катализаторы и кончики");
        s.sendMessage("§e/vein set <параметр> <значение> §7— изменить настройку");
        s.sendMessage("§e/vein addblock <Material> §7— добавить блок в список заменяемых");
        s.sendMessage("§e/vein removeblock <Material> §7— убрать блок из списка");
        s.sendMessage("§7Параметры: §fcatalyst-block, result-block, interval, tip-energy, ");
        s.sendMessage("§fbranch-chance, max-branch-depth, persistence, vertical, maxtips, ");
        s.sendMessage("§ftime, start-time, end-time");
        s.sendMessage("§7Проценты: §f30% §7или §f0.3");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String sub : List.of("reload", "info", "list", "set", "addblock", "removeblock", "help")) {
                if (sub.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(sub);
            }
        } else if (a.length == 2) {
            if (a[0].equalsIgnoreCase("set")) {
                for (String p : List.of("catalyst-block", "result-block", "interval", "tip-energy",
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
                case "branch-chance", "persistence", "vertical" -> out.addAll(List.of("10%", "25%", "50%", "75%", "0.3"));
                case "interval" -> out.addAll(List.of("40", "100", "200"));
                case "tip-energy" -> out.addAll(List.of("6", "12", "20"));
                case "max-branch-depth" -> out.addAll(List.of("2", "3", "5"));
                case "maxtips" -> out.addAll(List.of("4", "8", "16"));
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
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() == veinConfig.catalystBlock) {
            veinManager.registerCatalyst(e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == veinConfig.catalystBlock) {
            veinManager.unregisterCatalyst(e.getBlock().getLocation());
        }
    }
}