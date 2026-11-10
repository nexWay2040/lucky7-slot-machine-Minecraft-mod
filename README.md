# Lucky 7 v2.0.0 — 3D Casino Mod for Minecraft 26.3 (Fabric)

**English Version** | [Русская версия (Russian Version)](README_RU.md)

**Lucky 7 v2.0.0** is a feature-rich 3D casino mod for **Minecraft Java Edition 26.3** powered by **Fabric Loader 0.20.1+** and **Java 25**.

---

## 🎰 What's New in v2.0.0 (Minecraft 26.3)

1. **Full Migration to Official Fabric 26.3 Template**:
   - Built on `minecraft_version=26.3`, `loom_version=1.19-SNAPSHOT`, `loader_version=0.20.1`, `fabric_api_version=0.165.2+26.3`, Gradle `9.8.2`, and Java `25` using Official Mojang Mappings (`ValueInput`/`ValueOutput`, `BlockEntityRenderState`, `SubmitNodeCollector`, `GuiGraphicsExtractor`).
2. **Mafia Loan Contract Side-Quest ("Don Creeperione & The Syndicate")**:
   - Every player joining the world (singleplayer or co-op) automatically receives **Don Creeperione's Loan Contract** (`lucky7:casino_contract`, also craftable with Paper + Emerald + Gold Nugget).
   - **Act I (The Loan)**: Right-click the Contract to receive an instant **32 Diamond + 8 Golden Chip** loan from Don Creeperione to gamble at the casino.
   - **Act II (Good Ending — Syndicate Boss)**: Return **48 Diamonds** by right-clicking the Contract with 48+ diamonds in your inventory to become Co-Owner of the Syndicate and receive the **Royal Casino VIP Trophy (`lucky7:vip_trophy`)**, 3 Enchanted Golden Apples, 4 Netherite Ingots, and 16 Golden Chips!
   - **Act II (Chaos Ending — Collector Ambush)**: Right-click the Contract without 48 diamonds and Don Creeperione sends his armored skeleton debt collectors **"🕶 Вышибала Боб"** and **"💼 Коллектор Джо"** after you!
3. **Three 3D Animated Casino Games**:
   - **Lucky 7 Slot Machine (`lucky7:slot_machine`)**: 2-block tall 3D cabinet with 3 rotating 18-facet reels, physical lever & button hitboxes, bottom hopper payout fountain, dedicated **🔥 БЕСПЛАТНЫЙ СПИН 🔥** button during Fever Mode, and a special **Royal VIP Trophy** mode (returns the Trophy to your inventory while triggering VIP jackpot odds).
   - **Wheel of Risk (`lucky7:lucky_wheel`)**: 3D 12-sector Wheel of Fortune on a mahogany & gold pedestal with balanced 50/50 Good vs Danger outcomes clearly labeled with `+` and `X` badges (32-Emerald/16-Diamond Jackpot, Ancient Debris, God Apple, Totem of Undying, vs TNT explosion, Bouncer Trap, Sky Catapult, Wither Curse, and Lightning).
   - **Hustler's Shell Game (`lucky7:shell_game`)**: 3D mahogany & emerald velvet table with 3 animated golden cups that lift to reveal a spinning 3D diamond crystal, orbit smoothly during shuffling, and let you click Left/Center/Right cup in the world to win **3x Diamonds + Golden Chip**.
4. **Custom Casino Currency, Lucky Items & Upgraded Textures**:
   - **Golden Casino Chip (`lucky7:golden_chip`)**, **Emerald VIP Chip (`lucky7:emerald_chip`)**, **Diamond High-Roller Chip (`lucky7:diamond_chip`)**, and **Lucky Gold Coin (`lucky7:lucky_coin`)**.
   - **Interactive Lucky Casino Dice (`lucky7:lucky_dice`)**: Right-click to roll 2d6 (`2..12`) for Luck IV + Emeralds on double-6s or funny snake-eyes debuffs.
   - **Ace of Spades Trump Card (`lucky7:lucky_ace`)**, **Four-Leaf Lucky Clover (`lucky7:lucky_clover`)**, **Don Creeperione's Contract (`lucky7:casino_contract`)**, and **Royal Casino VIP Trophy (`lucky7:vip_trophy`)**.
   - Upgraded 3D textures sourced cleanly from **CC0 / MIT** assets (see [`CREDITS.txt`](CREDITS.txt)) featuring dedicated mahogany wood, 512×512 labeled 12-sector Wheel of Risk (`+` / `X` indicators & pixel icons), and emerald velvet Shell Game felt.

---

## 📦 Building from Source (Minecraft 26.3 / Java 25)

```bash
./gradlew build
```

Output JAR: `build/libs/lucky7-2.0.0.jar` (Legacy 1.21.1 version is preserved on the `mc-1.21.1` branch).
