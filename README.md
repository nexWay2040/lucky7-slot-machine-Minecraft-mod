# Lucky 7 v2.0.0 — 3D Story Casino Mod for Minecraft 26.3 (Fabric)

**English Version** | [Русская версия (Russian Version)](README_RU.md)

**Lucky 7 v2.0.0 (Leafxup Syndicate Edition)** is a full-featured, story-driven, non-linear 3D casino mod for **Minecraft Java Edition 26.3** powered by **Fabric Loader 0.20.1+** and **Java 25**.

---

## 🎰 What's New in v2.0.0 (Minecraft 26.3)

1. **Full Migration to Official Fabric 26.3 Template**:
   - Built on `minecraft_version=26.3`, `loom_version=1.19-SNAPSHOT`, `loader_version=0.20.1`, `fabric_api_version=0.165.2+26.3`, Gradle `9.8.2`, and Java `25` using Official Mojang Mappings (`ValueInput`/`ValueOutput`, `BlockEntityRenderState`, `SubmitNodeCollector`, `GuiGraphicsExtractor`).
2. **Non-Linear Comedy Storyline ("Don Creeperione & The Lucky 7 Syndicate")**:
   - Play freely in survival or craft **Don Creeperione's Loan Contract** (`lucky7:casino_contract` — Paper + Emerald + Gold Nugget).
   - **Act I (The Loan)**: Right-click the Contract to receive an instant **32 Diamond + 8 Golden Chip** loan from Don Creeperione to gamble at the casino.
   - **Act II (Good Ending — Syndicate Boss)**: Return **48 Diamonds** by right-clicking the Contract with 48+ diamonds in your inventory to become Co-Owner of the Syndicate and receive **Leafxup's Emerald VIP Trophy (`lucky7:leafxup_trophy`)**, 3 Enchanted Golden Apples, 4 Netherite Ingots, and 16 Golden Chips!
   - **Act II (Chaos Ending — Collector Ambush)**: Right-click the Contract without 48 diamonds and Don Creeperione sends his armored skeleton debt collectors **"🕶 Вышибала Боб"** and **"💼 Коллектор Джо"** after you!
3. **Three 3D Animated Casino Games**:
   - **Lucky 7 Slot Machine (`lucky7:slot_machine`)**: 2-block tall 3D cabinet with 3 rotating 18-facet reels, physical lever & button hitboxes, bottom hopper payout fountain, dedicated **🔥 БЕСПЛАТНЫЙ СПИН 🔥** button during Fever Mode, and a special **Leafxup VIP Trophy** mode (returns the Trophy to your inventory while triggering VIP jackpot odds!).
   - **Wheel of Samsara (`lucky7:lucky_wheel`)**: 3D 12-sector Wheel of Fortune on a golden pedestal with mechanical peg-clicking sounds and 12 hilarious/epic streamer outcomes (32-Diamond Jackpot, Chicken Paratrooper Apocalypse, Space Levitation, Fake Creeper Hiss + Emeralds, God Apple, Ancient Debris, Haste II + Speed II, Totem of Undying).
   - **Hustler's Shell Game (`lucky7:shell_game`)**: 3D mahogany & green velvet table with 3 animated golden cups that lift to reveal a sparkling diamond, orbit smoothly in 3D during shuffling, and let you click Left/Center/Right cup in the world to win **3x Diamonds + Golden Chip**.
4. **Custom Casino Currency & Trophy Items**:
   - **Golden Casino Chip (`lucky7:golden_chip`)**, **Lucky Gold Coin (`lucky7:lucky_coin`)**, **Don Creeperione's Contract (`lucky7:casino_contract`)**, and **Leafxup's Emerald VIP Trophy (`lucky7:leafxup_trophy`)** with custom pixel-art textures and 26.3 item models.

---

## 📦 Building from Source (Minecraft 26.3 / Java 25)

```bash
./gradlew build
```

Output JAR: `build/libs/lucky7-2.0.0.jar` (Legacy 1.21.1 version is preserved on the `mc-1.21.1` branch).
