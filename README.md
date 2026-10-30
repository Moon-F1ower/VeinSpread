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

- **Living Roots (Random Ticks):** Veins grow organically with a configurable chance per tick, just like crops. No more robotic, synchronized steps.
- **Center Thickening (3D):** Veins don't just draw thin lines. They naturally thicken and merge closer to the catalyst, creating solid, volumetric masses at the center while leaving thin tendrils at the edges.
- **Catalyst-Driven:** Place a catalyst block — and veins start crawling from it. If the growth stops, the catalyst will automatically spawn new roots as long as it's placed.
- **Full Control:** Step chance, vein length, branching frequency, direction "inertia", vertical branches, tip limit, and thickening radius — everything is configurable.
- **Replaceable blocks list:** You decide what the vein is allowed to "eat". Use `/vein blocks` to see the current list.
- **Time conditions:** Veins can grow only at night, only during the day, or always.
- **In-game management:** The `/vein` command with tab-completion: change percentages, blocks, and speed without restarting the server.
- **Persistent growth:** Active veins and placed blocks are saved to `veins.json` and seamlessly resume after a server restart.
- **Localization:** Player messages support both English and Russian out of the box.

---

## 🎯 Target Audience

- **SMP servers** looking to add a "living world" without mods or new dimensions.
- **Lore-based survival** — veins as an "infection", "curse", or "ancient corruption".
- **Builders and decorators** — beautiful organic patterns made of sculk or mycelium.
- **Admins** who need a customizable alternative to the sculk catalyst with flexible logic.

---

## 📦 Installation

1. Make sure you have **Paper 1.21+** (or Spigot 1.21+) and **Java 21**.
2. Download `VeinSpread-1.2.0.jar` from the releases page on Modrinth or GitHub.
3. Place the `.jar` into the `plugins/` folder on your server.
4. Restart the server.
5. Open `plugins/VeinSpread/config.yml` — or use `/vein` directly in-game.

---

## ⚙️ Quick Start

Minimal example: **Sculk Catalyst** turns **Stone** into **Sculk**, grows every ~1 second, and thickens at the center.

```yaml
growth-chance: 0.02

vein:
  catalyst-block: "SCULK_CATALYST"
  result-block: "SCULK"
  
  replaceable-blocks:
    - "STONE"
    - "DEEPSLATE"

  tip-energy: 25
  branch-chance: 0.35
  max-branch-depth: 10
  persistence: 0.5
  vertical-branch-chance: 0.15
  max-tips-per-catalyst: 500

  thicken-per-tick: 5
  max-thickness-radius: 12.0
  call-event-on-thicken: false
```

---

## 🧩 Configuration

### Root parameters
| Parameter | Type | Default | Description |
|---|---|---|---|
| `growth-chance` | double | `0.02` | Chance per tick (1/20s) that a tip makes a step. 0.05 = ~1 step per second. |
| `vein.catalyst-block` | Material | `SCULK_CATALYST` | The block that triggers growth when placed. |
| `vein.result-block` | Material | `SCULK` | What the "eaten" blocks turn into. |
| `vein.replaceable-blocks` | list | stone, deepslate, etc. | Blocks the vein is allowed to replace. |
| `vein.tip-energy` | int | `25` | How many steps a single root tip makes before dying. |
| `vein.branch-chance` | 0.0–1.0 | `0.35` | Chance for a tip to branch out. |
| `vein.max-branch-depth` | int | `10` | Maximum branching depth. |
| `vein.persistence` | 0.0–1.0 | `0.5` | Direction inertia. 1.0 = straight lines, 0.0 = chaotic. |
| `vein.vertical-branch-chance` | 0.0–1.0 | `0.15` | Chance to branch up or down. |
| `vein.max-tips-per-catalyst` | int | `500` | Max active tips per catalyst (lag protection). |

### Thickening parameters
| Parameter | Type | Default | Description |
|---|---|---|---|
| `vein.thicken-per-tick` | int | `5` | How many blocks per tick the plugin tries to fill near the center. |
| `vein.max-thickness-radius` | double | `12.0` | The radius around the catalyst where thickening occurs. |
| `vein.call-event-on-thicken` | boolean | `false` | If true, fires `BlockSpreadEvent` during thickening (for WorldGuard). Might cause lag. |

### Time conditions
| Parameter | Type | Default | Description |
|---|---|---|---|
| `time-conditions.enabled` | bool | `false` | Enable in-game time condition. |
| `time-conditions.start-time` | long | `13000` | Start of the window (ticks, 0–24000). |
| `time-conditions.end-time` | long | `23000` | End of the window (supports wrap-around past 0). |

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

---

## 🗺️ Roadmap

- [x] Random tick growth (Living Roots) *(Done in v1.2.0)*
- [x] Center thickening / 3D filling *(Done in v1.2.0)*
- [ ] Support thin blocks (`SCULK_VEIN`, `GLOW_LICHEN`, `VINE`)
- [ ] Different catalyst profiles (`Map<Material, Profile>`)
- [ ] Light and biome conditions
- [ ] bStats
- [ ] `BlockFadeEvent` — "decaying" veins without a catalyst

---

## 📜 License & Source

This project is licensed under the MIT License — see [LICENSE](LICENSE).
Source code is available on GitHub: [github.com/Moon-F1ower/VeinSpread](https://github.com/Moon-F1ower/VeinSpread)

---

## 💬 Feedback

Found a bug or have a suggestion? Please open an issue on GitHub: [github.com/Moon-F1ower/VeinSpread/issues](https://github.com/Moon-F1ower/VeinSpread/issues)

If you use the plugin on your server — I'd love to see screenshots in the comments 🌿

---

<p align="center">
  <sub>Made with 🌿 by Moon Flower</sub>
</p>
