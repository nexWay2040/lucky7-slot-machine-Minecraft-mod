# Lucky 7 v2.0.0 — Minecraft 26.3 (Fabric)

## Highlights
- **Minecraft 26.3 & Java 25 Support**: Migrated completely to official Fabric 26.3 Loom / Mojang Mappings (`ValueInput`/`ValueOutput`, `BlockEntityRenderState`, `SubmitNodeCollector`, `GuiGraphicsExtractor`).
- **Two New 3D Casino Attractions**:
  - **Wheel of Risk (`lucky7:lucky_wheel`)**: 12-sector `512×512` prize & danger wheel with balanced 50/50 outcomes clearly marked `+` / `X` on a mahogany & gold pedestal.
  - **Hustler's Shell Game (`lucky7:shell_game`)**: Emerald velvet & mahogany table with 3 animated golden cups and a spinning 3D diamond crystal.
- **Mafia Loan Contract Side-Quest (`lucky7:casino_contract`)**:
  - Automatically given to every player on world join (singleplayer & co-op).
  - Borrow 32 Diamonds + 8 Golden Chips from Don Creeperione and either repay 48 Diamonds for the **Royal Casino VIP Trophy (`lucky7:vip_trophy`)** or fight off armored skeleton collectors!
- **5 New Casino Items & Upgraded CC0/MIT Textures** (see `CREDITS.txt`):
  - `lucky7:emerald_chip`, `lucky7:diamond_chip`, `lucky7:lucky_dice` (right-click `2d6` roll), `lucky7:lucky_ace`, `lucky7:lucky_clover`, plus upgraded textures for chips, coins, contract, trophy, wheel, and shell table.
- **Bugfixes**:
  - Fixed showcase prizes dropping when breaking the slot machine (`Lucky7BlockEntity.preRemoveSideEffects`).
  - Fixed click hitboxes on East/West facing tables and slot machines.
  - Balanced block breaking speed with pickaxes and removed `needs_iron_tool` restriction.

*(Legacy Minecraft 1.21.1 source is preserved on the `mc-1.21.1` branch.)*
