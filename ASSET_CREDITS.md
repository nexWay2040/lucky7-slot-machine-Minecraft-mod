# Lucky 7 asset credits

All third-party assets below permit redistribution and modification. Original downloads are retained under `assets_sources/` so their provenance can be audited.

## Visual assets

### Slot Machine Resource Pack

- Author: Molly "Cougarmint" Willits
- Source: https://opengameart.org/content/slot-machine-resource-pack
- License: Creative Commons Attribution 3.0 (CC BY 3.0)
- Local source: `assets_sources/textures/original/cougarmint_slot_machine_resource_pack_cc-by-3.0.zip`
- Used for: Seven, Golden Seven, BAR, Double BAR, Cherry, Watermelon, Bell, Horseshoe and Clover reel symbols; physical payline frame.
- Changes: symbols were resized, padded, color-keyed where necessary, and the Seven was recolored for the Golden Seven variant.
- Required attribution: "Slot Machine Resource Pack by Molly 'Cougarmint' Willits".

The pack's bare-bones slot-machine frame graphics are separately declared Public Domain by the author. Lucky 7 uses `paylineframe.png` from that portion.

### Good Fruits (M484 Games)

- Author: Master484
- Source: https://opengameart.org/content/good-fruits-m484-games
- License: CC0 1.0 / Public Domain dedication
- Local source: `assets_sources/textures/original/master484_good_fruits_cc0.png`
- Used for: Lemon, Orange, Grapes, Star and Golden Coin reel symbols.
- Changes: each complete sprite was cropped from its measured opaque bounds, padded, and resized without slicing neighbouring sprites.

### Game-icons.net icons

- Author: Lorc
- Source repository: https://github.com/game-icons/icons
- Project site: https://game-icons.net/
- License: Creative Commons Attribution 3.0 (CC BY 3.0)
- Local sources: `assets_sources/textures/original/gameicons_lorc_*_cc-by-3.0.svg`
- Used for: Diamond (`diamond-hard`), Crown (`crown`), Skull (`skull-crossed-bones`) and Moon (`moon`).
- Changes: vectors were rasterized, recolored, padded and resized; the Diamond was cropped to the complete gem silhouette.
- Required attribution: "Icons by Lorc, available on game-icons.net, CC BY 3.0".

### Metal 009

- Author/site: ambientCG
- Source: https://ambientcg.com/a/Metal009
- License: CC0 1.0 / Public Domain dedication
- Local source: `assets_sources/textures/original/ambientcg_metal009_1k_cc0.zip`
- Used for: brushed cabinet surface and block/item preview texture.
- Changes: downscaled, contrast-adjusted and tinted by the in-world renderer.

## Original project assets

- The procedural BlockEntityRenderer geometry, cabinet layout, physical reel mesh, lever, lamps, glass, LED segments and GUI composition were authored for Lucky 7.
- Editable Blockbench source: `assets_sources/models/lucky7_slot_machine.bbmodel`.
- Reproducible texture build script: `assets_sources/textures/build_textures.ps1`.

## Audio assets

### Freesound jackpot sound 213922
- Title: `onearmedbanditjackpot.wav`
- Author: vumseplutten1709
- Source: https://freesound.org/people/vumseplutten1709/sounds/213922/
- License: CC0 1.0
- Local file: `assets_sources/audio/original/freesound_213922_jackpot_cc0.ogg`
- Used for: jackpot event.

### Freesound arcade buttons 261074
- Title: `Asteroids Arcade Game Buttons.wav`
- Author: portwain
- Source: https://freesound.org/people/portwain/sounds/261074/
- License: CC0 1.0
- Local file: `assets_sources/audio/original/freesound_261074_arcade_buttons_cc0.ogg`
- Used for: interface button click.

### Freesound mechanical slot 316887
- Title: `Peliautomaatti, Yksikätinen rosvo / Old slot machine, game`
- Author: YleArkisto
- Source: https://freesound.org/people/YleArkisto/sounds/316887/
- License: CC-BY 4.0
- Local file: `assets_sources/audio/original/freesound_316887_mechanical_slot_cc-by-4.0.ogg`
- Used for: machine start.

### Freesound slot machine payout 361346
- Title: `slot machine payout.wav`
- Author: jack126guy
- Source: https://freesound.org/people/jack126guy/sounds/361346/
- License: CC0 1.0
- Local file: `assets_sources/audio/original/freesound_361346_slot_payout_cc0.ogg`
- Used for: small win.

### Freesound antique slot machine 808160
- Title: `GAMECas-Blue Snowball Microphone, CU_Slot Machine, Antique`
- Author: designerschoice
- Source: https://freesound.org/people/designerschoice/sounds/808160/
- License: CC-BY 4.0
- Local file: `assets_sources/audio/original/freesound_808160_antique_reels_cc-by-4.0.ogg`
- Used for: reel rotation sound.

### Kenney Casino Audio 1.1
- Author: Kenney Vleugels (Kenney.nl)
- Source: https://kenney.nl/assets/casino-audio
- License: CC0 1.0, see `assets_sources/audio/kenney_casino_audio_cc0/License.txt`
- Local source pack: `assets_sources/audio/original/kenney_casino_audio_cc0.zip`
- Used: `chips-collide-1.ogg` for resource deposit; `chip-lay-1.ogg` for mechanical reel stop.

CC-BY attribution: "Old slot machine game recording by YleArkisto" and "Antique slot machine recording by designerschoice", both via Freesound, CC BY 4.0.
