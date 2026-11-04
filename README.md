# Lucky 7 — 3D Casino Slot Machine Mod for Minecraft (Fabric 1.21.1)

**English Version** | [Русская версия (Russian Version)](README_RU.md)

**Lucky 7** is a fully-featured, authentic 3D casino slot machine mod for **Minecraft Java Edition 1.21.1** powered by **Fabric Loader** (Java 21).

---

## 🎰 Key Features

- **Full-Size 2-Block Tall 3D Animated Machine**:
  - Life-sized arcade cabinet with golden trims, coin tray, glowing marquee sign, and an animated mechanical pull lever.
  - Solid geometric construction without holes, gaps, or transparent visual artifacts.
- **Physical In-World 3D Reels**:
  - 3 independent cylindrical reels (18 distinct symbols each) physically spin behind clear protective glass.
  - Smooth acceleration, staggered stopping sequences, and mechanical click sounds aligning symbols to the illuminated red payline.
- **Intuitive GUI with Tier-Aligned Odds**:
  - **9 Stake Slots** supporting dynamic deposit calculation.
  - **Live Dynamic Probabilities**: tier labels and live win percentages are perfectly aligned horizontally with the corresponding rows of the prize showcase.
  - **Auto-Close on Start**: pressing START with a deposit immediately closes the GUI, letting the player watch the mechanical reels spin in the 3D world.
- **Organized 27-Slot Prize Showcase**:
  - **Row 0 (Jackpot)**: 16 Netherite Blocks, 32 Netherite Ingots, 64 Diamond Blocks, 16 Ancient Debris, Smithing Templates, Enchanted Golden Apples, 64 Emerald Blocks, 64 Gold Blocks.
  - **Row 1 (Big Win)**: 64 Diamonds, 64 Emeralds, 64 Gold Ingots, 64 Iron Blocks, Golden Apples, 64 XP Bottles, 8-minute utility potions (x3 each).
  - **Row 2 (Common/Utility)**: 64 Iron Ingots, 64 Redstone, 64 Quartz, 64 Lapis, 64 Copper Blocks, 32 Amethyst Shards, 8-minute utility potions (x1 each).
  - **Full-Stack Payout Guarantee**: players always win the exact, full stack displayed in the showcase slot without arbitrary stack-halving.
  - **Uniform RNG**: pure, unbiased random selection across all slots in a winning tier.
- **Extensive Stake Support**:
  - All standard ores, ingots, and mineral blocks: Iron, Gold, Diamond, Netherite, Emerald (including Emerald Blocks), Copper, Lapis, Redstone, Quartz, Coal, Amethyst.
  - All tools & armor from Iron to Netherite.
  - Enchanted Golden Apples.
  - Rare Minecraft relics: Elytra, Totem of Undying, Nether Star, Dragon Egg, Heart of the Sea, Trident, Mace, Heavy Core, Trial Keys, Beacons, and Wither Skeleton Skulls.
- **Secret Easter Egg: 3 Skulls Curse**:
  - The classic `SKULL` symbol is present on the reels. If 3 skulls land on the payline during a non-winning spin, the player suffers 3 HP (1.5 hearts) of magic damage with dark smoke and a Wither sound effect. Kept hidden from public in-game statistics.

---

## 🛠️ Canonical Crafting Recipe

Craft the Lucky 7 slot machine in a standard 3×3 crafting table. The recipe mirrors the visual outline of the machine:

```
[ Gold Ingot ] [ Redstone Lamp ] [ Gold Ingot ]  -> Top golden marquee crown & illuminated sign
[ Gold Ingot ] [  Glass Pane   ] [    Lever   ]  -> Glass reel chamber with right-side pull lever
[ Iron Block ] [ Redstone Block] [ Iron Block ]  -> Heavy-duty mechanical cabinet base & power
```

Output: **1x Lucky 7 Slot Machine** (`lucky7:slot_machine`).

---

## ⚙️ Configuration (`config/lucky7.json`)

Automatically generated upon first launch:
- `winBase`: base win rate for minimal stakes (default `0.24` = 24%).
- `winMax`: maximum capped win rate for massive stakes (up to `0.85` = 85%).
- `jackpotBase` & `jackpotMax`: jackpot probability range (`0.008` to `0.25`).
- `resourceValues`: fully customizable stake point values for every item.

---

## 📦 Requirements & Building

- **Requirements**:
  - Minecraft Java Edition 1.21.1
  - Fabric Loader >= 0.16.10
  - Fabric API >= 0.116.0
  - Java 21

- **Build from Source**:
  ```bash
  ./gradlew build
  ```
  The compiled mod JAR will be located at `build/libs/lucky7-1.0.0.jar`.
