# Configuration

Lucky 7 creates `config/lucky7.json` on first launch. Invalid values are clamped to safe ranges and the validated file is written back.

## Gameplay

- `enabled`: master switch for machine interaction.
- `stakeSlots`: active stake slots (1–9).
- `maxStakeItems`: item count limit per spin.
- `maxStakeValue`: maximum total configured resource value per spin.
- `historyLimit`: saved recent results (1–100).
- `attractionMode`: enables idle decorative effects where available.

## Probability curve

- `winBase`, `winPerValue`, `winMax`: total probability assigned to all winning categories.
- `jackpotBase`, `jackpotPerValue`, `jackpotMax`: jackpot curve; it is always clamped to the total win probability.
- `bigWeight`, `mediumWeight`, `smallWeight`, `pairWeight`: relative allocation of the non jackpot win probability. Weights are normalized by the probability calculator.

The current defaults yield these jackpot chances: value 4 = 0.8%, value 16 = 3.2%, and value 24 = 4.8%.

## Rewards and jackpot pool

- `pairMultiplier`, `smallMultiplier`, `mediumMultiplier`, `bigMultiplier`: requested reward value relative to the stake.
- `jackpotContribution`: fraction of each stake added to the jackpot pool.

Rewards are physical Minecraft items removed from the machine's 27 slot prize reserve. The stake is transferred into the reserve before the spin. A spin is rejected when the reserve cannot cover the minimum reward or has no room for the wager.

The reserve is administered in the right side of the machine screen by a player with operator permissions or in creative mode. Ordinary players can deposit and return their stake in the left slots and cannot extract reserve items.

## Resource values

`resourceValues` maps item registry IDs to positive integer values. Defaults:

- `minecraft:coal`: 1
- `minecraft:copper_ingot`: 2
- `minecraft:iron_ingot`: 4
- `minecraft:lapis_lazuli`, `minecraft:redstone`, `minecraft:quartz`: 5
- `minecraft:gold_ingot`: 8
- `minecraft:emerald`: 10
- `minecraft:diamond`: 16
- `minecraft:ancient_debris`: 24

Add an item from another mod by adding its registry ID, for example `"othermod:rare_ingot": 30`.

## Audio

- `soundsEnabled`: turns Lucky 7 sounds on or off.
- `masterVolume`: overall multiplier.
- `reelVolume`: start and looping reel volume multiplier.
- `effectsVolume`: insertion, stop, and win effect volume multiplier.

All volume values are clamped from 0 to 1. Audio assets and licenses are listed in `ASSET_CREDITS.md`.
