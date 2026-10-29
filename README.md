# 🌿 VeinSpread

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green.svg)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1bd96a?logo=modrinth)](https://modrinth.com/plugin/veinspread)
[![GitHub](https://img.shields.io/badge/GitHub-Source-black?logo=github)](https://github.com/Moon-F1ower/VeinSpread)

**by Moon Flower**

> Organic vein-based block spreading — like mycelium, sculk, or vines.
> A lightweight Paper/Spigot plugin for Minecraft 1.21+. Fully configurable via
> `config.yml` or in-game commands.

---

## ✨ Features

- **Veins, not circles.** Infection grows in winding lines with branches — like mycelium, sculk veins, or tree roots.
- **Catalyst-driven.** Place a catalyst block — and veins start crawling from it. Break it — growth stops, but the blocks that have already grown remain in the world.
- **Full control.** Step speed, vein length, branching frequency, direction "inertia", vertical branches, tip limit — everything is configurable.
- **Replaceable blocks list.** You decide what the vein is allowed to "eat". Use `/vein blocks` to see the current list.
- **Time conditions.** Veins can grow only at night, only during the day, or always.
- **In-game management.** The `/vein` command with tab-completion: change percentages, blocks, and speed without restarting the server. Changes are saved to the config immediately.
- **Persistent growth.** Active veins are saved to `veins.json` and seamlessly resume after a server restart.
- **Localization.** Player messages support both English and Russian out of the box.

---

## 🎯 Target Audience

- **SMP servers** looking to add a "living world" without mods or new dimensions.
- **Lore-based survival** — veins as an "infection", "curse", or "ancient corruption".
- **Builders and decorators** — beautiful organic patterns made of sculk or mycelium.
- **Admins** who need a customizable alternative to the sculk catalyst with flexible logic.

---

## 📦 Installation

1. Make sure you have **Paper 1.21+** (or Spigot 1.21+) and **Java 21**.
2. Download `VeinSpread-1.1.1.jar` from the releases page on Modrinth or GitHub.
3. Place the `.jar` into the `plugins/` folder on your server.
4. Restart the server.
5. Open `plugins/VeinSpread/config.yml` — or use `/vein` directly in-game.

---

## ⚙️ Quick Start

Minimal example: **mycelium** turns **stone** and **dirt** into **mycelium**,
grows every 3 seconds, veins are 12 blocks long, branching chance is 25%.

```yaml
vein:
  catalyst-block: "MYCELIUM"
  result-block: "MYCELIUM"
  replaceable-blocks:
    - "STONE"
    - "DIRT"
    - "GRASS_BLOCK"
  interval-ticks: 60
  tip-energy: 12
  branch-chance: 0.25
  persistence: 0.75
  max-branch-depth: 3
  vertical-branch-chance: 0.1
  max-tips-per-catalyst: 8

time-conditions:
  enabled: false
```

Place a mycelium block on stone — and step back. In a couple of seconds, you will see veins crawling across the stone.

---

## 🧩 Configuration

### `vein` section

| Parameter | Type | Default | Description |
|---|---|---|---|
| `catalyst-block` | Material | `SCULK_CATALYST` | The block that triggers growth when placed |
| `result-block` | Material | `SCULK` | What the "eaten" blocks turn into |
| `replaceable-blocks` | list | stone, cobblestone, dirt, grass_block, deepslate | Blocks the vein is allowed to replace |
| `interval-ticks` | int | `100` | Interval between steps (20 ticks = 1 sec) |
| `tip-energy` | int | `12` | How many steps a single tip makes |
| `branch-chance` | 0.0–1.0 | `0.2` | Chance to branch at each step |
| `max-branch-depth` | int | `3` | Maximum branching depth |
| `persistence` | 0.0–1.0 | `0.7` | Inertia: 1.0 — straight lines, 0.0 — chaos |
| `vertical-branch-chance` | 0.0–1.0 | `0.1` | Chance to branch up/down |
| `max-tips-per-catalyst` | int | `8` | Lag protection |

### `time-conditions` section

| Parameter | Type | Default | Description |
|---|---|---|---|
| `enabled` | bool | `false` | Enable in-game time condition |
| `start-time` | long | `13000` | Start of the window (ticks, 0–24000) |
| `end-time` | long | `23000` | End of the window (supports wrap-around past 0) |

---

## 🎮 Commands

All commands require the `veinspread.admin` permission (default is `op`).

| Command | Description |
|---|---|
| `/vein reload` | Reload the config |
| `/vein info` | Show current settings |
| `/vein list` | List active catalysts and tips |
| `/vein blocks` | List all replaceable blocks |
| `/vein set <parameter> <value>` | Change a setting on the fly |
| `/vein addblock <Material>` | Add a block to the list |
| `/vein removeblock <Material>` | Remove a block from the list |
| `/vein help` | Show help |

### Examples

```bash
/vein set branch-chance 30%
/vein set persistence 0.85
/vein set interval 60
/vein set tip-energy 20
/vein set vertical 5%
/vein set max-branch-depth 4
/vein set maxtips 12
/vein set catalyst-block MYCELIUM
/vein set result-block MYCELIUM
/vein addblock RED_SAND
/vein removeblock DIRT
/vein set time true
/vein set start-time 13000
/vein set end-time 23000
```

Percentages are accepted as `30%`, `0.3`, or `30` — the parser understands all three forms.

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `veinspread.admin` | Access to `/vein` commands | `op` |

---

## 🎨 Use Cases

### Survival "Corruption"

```yaml
vein:
  catalyst-block: "SCULK_CATALYST"
  result-block: "SCULK"
  replaceable-blocks: ["STONE", "DEEPSLATE", "COBBLESTONE", "GRAVEL"]
  interval-ticks: 120
  tip-energy: 25
  branch-chance: 0.15
  persistence: 0.85
  max-branch-depth: 2
  vertical-branch-chance: 0.05
  max-tips-per-catalyst: 4

time-conditions:
  enabled: true
  start-time: 13000
  end-time: 23000
```

At night, the corruption crawls across the stone; during the day, it freezes. Perfect for a "cursed biome".

### "Living Forest"

```yaml
vein:
  catalyst-block: "OAK_LOG"
  result-block: "MOSS_BLOCK"
  replaceable-blocks: ["DIRT", "GRASS_BLOCK", "STONE", "COBBLESTONE"]
  interval-ticks: 80
  tip-energy: 15
  branch-chance: 0.35
  persistence: 0.6
  vertical-branch-chance: 0.2
  max-tips-per-catalyst: 12
```

---

## 🛠️ Building from Source

```bash
git clone https://github.com/Moon-F1ower/VeinSpread.git
cd VeinSpread
mvn clean package
```

The compiled `.jar` will appear in `target/VeinSpread-x.y.z.jar`.  
Requirements: Maven 3.9+, JDK 21.

---

## ❓ FAQ

**Can I use it on Spigot instead of Paper?**  
Yes, but Paper is recommended — it was tested on Paper, and the 1.21 API is more stable there.

**Does it work with WorldGuard?**  
Not yet — the plugin places blocks via `Block.setType()` without firing `BlockSpreadEvent`. WorldGuard won't see it. It's on the roadmap.

**Are veins saved after a restart?**  
Yes! Active veins are saved to `veins.json` every 5 minutes and on server shutdown. They will seamlessly resume growing when the server starts again.

**Can I have multiple catalysts with different settings?**  
In the current version — no, there is a single config for the server. Profiles by `Material` are on the roadmap.

**Are thin veins (sculk veins, lichen) supported?**  
No, only full block replacement. It's on the roadmap.

**Why aren't the veins growing?**  
1. The catalyst is placed (not just in the inventory).  
2. Blocks in `replaceable-blocks` match by exact `Material`.  
3. If `time-conditions` is enabled — it is currently the right time.  
4. Chunks are loaded (a player is nearby).

---

## 🗺️ Roadmap

- [x] Save active veins to disk (YAML/JSON) *(Done in v1.1.0)*
- [x] In-game localization (English/Russian) *(Done in v1.1.0)*
- [ ] Support thin blocks (`SCULK_VEIN`, `GLOW_LICHEN`, `VINE`)
- [ ] Different catalyst profiles (`Map<Material, Profile>`)
- [ ] Light and biome conditions
- [ ] Fire `BlockSpreadEvent` for WorldGuard / GriefPrevention compatibility
- [ ] bStats
- [ ] `BlockFadeEvent` — "decaying" veins without a catalyst

---

## 📜 License & Source

This project is licensed under the MIT License — see [LICENSE](LICENSE).
Source code is available on GitHub: [github.com/Moon-F1ower/VeinSpread](https://github.com/Moon-F1ower/VeinSpread)

---

## 💬 Feedback

Found a bug or have a suggestion? Please open an issue on GitHub: [github.com/Moon-F1ower/VeinSpread/issues](https://github.com/Moon-F1ower/VeinSpread/issues)

If you use the plugin on your server — I'd love to see screenshots in the Modrinth comments 🌿
