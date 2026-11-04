package com.lucky7.mod.blockentity;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.game.WinCategory;
import com.lucky7.mod.game.probability.ProbabilityCalculator;
import com.lucky7.mod.game.probability.ProbabilityProfile;
import com.lucky7.mod.game.reels.ReelLayout;
import com.lucky7.mod.game.reels.ReelSymbol;
import com.lucky7.mod.screen.Lucky7ScreenHandler;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lucky7BlockEntity extends BlockEntity implements Inventory, ExtendedScreenHandlerFactory<BlockPos> {
    public static final int STAKE_SLOTS = 9;
    public static final int PRIZE_SLOTS = 27;
    public static final int TOTAL_SLOTS = STAKE_SLOTS + PRIZE_SLOTS;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final int[] reels = new int[]{ReelSymbol.CHERRY.ordinal(), ReelSymbol.BELL.ordinal(), ReelSymbol.SEVEN.ordinal()};
    private final ArrayPropertyDelegate props = new ArrayPropertyDelegate(20);

    private UUID owner;
    private UUID activePlayer;
    private boolean spinning;
    private long spinStart;
    private int spinTicks = 60;
    private long lastSpinAt = 0;

    private WinCategory pendingCategory = WinCategory.NO_WIN;
    private ItemStack pendingPrize = ItemStack.EMPTY;
    private int pendingPayoutValue = 0;

    private int spins;
    private int wins;
    private int jackpots;
    private int bestWin;
    private int lastWin;
    private int lastTier = -1;
    private int jackpotPool = 100;
    private int totalWagered;
    private int totalPaid;

    private final List<String> history = new ArrayList<>();
    private final List<UUID> viewers = new ArrayList<>();
    private long clientSpinStart = -1;

    private int redstoneOutput = 0;
    private int redstonePulseTicks = 0;

    public Lucky7BlockEntity(BlockPos pos, BlockState state) {
        super(Lucky7BlockEntities.SLOT_MACHINE, pos, state);
        ensureCanonicalPrizes();
    }

    public void ensureCanonicalPrizes() {
        ItemStack s0 = items.get(STAKE_SLOTS);
        ItemStack s1 = items.get(STAKE_SLOTS + 1);
        ItemStack s2 = items.get(STAKE_SLOTS + 2);
        if (!s0.isOf(Items.NETHERITE_BLOCK) || s0.getCount() != 16 ||
            !s1.isOf(Items.NETHERITE_INGOT) || s1.getCount() != 32 ||
            !s2.isOf(Items.DIAMOND_BLOCK) || s2.getCount() != 64) {
            applyNewPrizePool();
            markDirty();
        }
    }

    private void applyNewPrizePool() {
        // Row 0: ДЖЕКПОТ (Jackpot - slots 0..8)
        setPrizeStack(0, new ItemStack(Items.NETHERITE_BLOCK, 16));
        setPrizeStack(1, new ItemStack(Items.NETHERITE_INGOT, 32));
        setPrizeStack(2, new ItemStack(Items.DIAMOND_BLOCK, 64));
        setPrizeStack(3, new ItemStack(Items.ANCIENT_DEBRIS, 16));
        setPrizeStack(4, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
        setPrizeStack(5, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 8));
        setPrizeStack(6, new ItemStack(Items.EMERALD_BLOCK, 64));
        setPrizeStack(7, new ItemStack(Items.GOLD_BLOCK, 64));
        setPrizeStack(8, new ItemStack(Items.NETHERITE_BLOCK, 8));

        // Row 1: КРУПНЫЙ ВЫИГРЫШ (Big Win - slots 9..17)
        setPrizeStack(9, new ItemStack(Items.DIAMOND, 64));
        setPrizeStack(10, new ItemStack(Items.EMERALD, 64));
        setPrizeStack(11, new ItemStack(Items.GOLD_INGOT, 64));
        setPrizeStack(12, new ItemStack(Items.IRON_BLOCK, 64));
        setPrizeStack(13, new ItemStack(Items.GOLDEN_APPLE, 16));
        setPrizeStack(14, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
        setPrizeStack(15, createPotionStack(Potions.LONG_NIGHT_VISION, 3));
        setPrizeStack(16, createPotionStack(Potions.LONG_FIRE_RESISTANCE, 3));
        setPrizeStack(17, createPotionStack(Potions.LONG_WATER_BREATHING, 3));

        // Row 2: СРЕДНИЙ / МАЛЫЙ (Medium / Utility - slots 18..26)
        setPrizeStack(18, new ItemStack(Items.IRON_INGOT, 64));
        setPrizeStack(19, new ItemStack(Items.COPPER_BLOCK, 64));
        setPrizeStack(20, new ItemStack(Items.LAPIS_LAZULI, 64));
        setPrizeStack(21, new ItemStack(Items.REDSTONE, 64));
        setPrizeStack(22, new ItemStack(Items.QUARTZ, 64));
        setPrizeStack(23, new ItemStack(Items.AMETHYST_SHARD, 32));
        setPrizeStack(24, createPotionStack(Potions.LONG_NIGHT_VISION, 1));
        setPrizeStack(25, createPotionStack(Potions.LONG_FIRE_RESISTANCE, 1));
        setPrizeStack(26, createPotionStack(Potions.LONG_WATER_BREATHING, 1));
    }

    private static ItemStack createPotionStack(RegistryEntry<Potion> potion, int count) {
        ItemStack stack = PotionContentsComponent.createStack(Items.POTION, potion);
        stack.setCount(count);
        return stack;
    }

    private void setPrizeStack(int index, ItemStack stack) {
        int slot = STAKE_SLOTS + index;
        if (slot < TOTAL_SLOTS) {
            items.set(slot, stack);
        }
    }

    // ---------------------------------------------------------------- ExtendedScreenHandlerFactory
    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.lucky7.slot_machine");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        ensureCanonicalPrizes();
        addViewer(player);
        return new Lucky7ScreenHandler(syncId, playerInventory, this, pos);
    }

    // ---------------------------------------------------------------- Ownership & Viewers
    public UUID ownerId() { return owner; }
    public boolean hasOwner() { return owner != null; }

    public void setOwner(PlayerEntity player) {
        if (owner == null && player != null) {
            owner = player.getUuid();
            markDirtyAndSync();
        }
    }

    public boolean isOwner(PlayerEntity player) {
        return owner == null || (player != null && owner.equals(player.getUuid()));
    }

    public void addViewer(PlayerEntity player) {
        if (!viewers.contains(player.getUuid())) {
            viewers.add(player.getUuid());
        }
        if (owner == null) {
            setOwner(player);
        }
    }

    public void removeViewer(PlayerEntity player) {
        viewers.remove(player.getUuid());
    }

    // ---------------------------------------------------------------- Stake Calculation
    public int stakeValue() {
        int total = 0;
        for (int i = 0; i < STAKE_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                total += Lucky7Config.value(stack.getItem()) * stack.getCount();
            }
        }
        return total;
    }

    public int stakeItems() {
        int total = 0;
        for (int i = 0; i < STAKE_SLOTS; i++) {
            total += items.get(i).getCount();
        }
        return total;
    }

    // ---------------------------------------------------------------- Start Spin
    public boolean startSpin(PlayerEntity player) {
        if (spinning) {
            Lucky7Mod.LOGGER.info("Spin rejected: machine is already spinning at {}", pos);
            return false;
        }
        if (world == null || world.isClient) return false;
        if (!Lucky7Config.INSTANCE.enabled) {
            Lucky7Mod.LOGGER.info("Spin rejected: machine disabled in config");
            message(player, "message.lucky7.disabled");
            return false;
        }

        int value = stakeValue();
        if (value <= 0) {
            Lucky7Mod.LOGGER.info("Spin rejected: stake value is 0 (stakeItems = {})", stakeItems());
            message(player, "message.lucky7.empty_stake");
            return false;
        }
        if (value > Lucky7Config.INSTANCE.maxStakeValue) {
            Lucky7Mod.LOGGER.info("Spin rejected: stake value {} exceeds max {}", value, Lucky7Config.INSTANCE.maxStakeValue);
            message(player, "message.lucky7.stake_too_big");
            return false;
        }
        if (stakeItems() > Lucky7Config.INSTANCE.maxStakeItems) {
            Lucky7Mod.LOGGER.info("Spin rejected: stake items count {} exceeds max {}", stakeItems(), Lucky7Config.INSTANCE.maxStakeItems);
            message(player, "message.lucky7.too_many_items");
            return false;
        }
        if (lastSpinAt > 0 && world.getTime() >= lastSpinAt && (world.getTime() - lastSpinAt) < 5) {
            Lucky7Mod.LOGGER.info("Spin rejected: cooldown active (< 5 ticks)");
            return false;
        }

        lastSpinAt = world.getTime();
        activePlayer = (player != null) ? player.getUuid() : null;

        // Clear stake items atomically on server
        for (int i = 0; i < STAKE_SLOTS; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        if (player instanceof ServerPlayerEntity sp && sp.currentScreenHandler instanceof com.lucky7.mod.screen.Lucky7ScreenHandler) {
            sp.currentScreenHandler.sendContentUpdates();
        }

        totalWagered += value;
        jackpotPool += Math.max(1, value / 10);
        spins++;

        // Roll outcome and map to exact reel positions
        rollOutcome(value);

        spinning = true;
        spinStart = world.getTime();
        spinTicks = Lucky7Config.INSTANCE.spinTicks;

        markDirtyAndSync();
        playSound(Lucky7Sounds.MACHINE_START, 1.0f);
        playSound(Lucky7Sounds.REEL_SPIN, 0.85f);

        String pName = (player != null) ? player.getName().getString() : "Redstone";
        Lucky7Mod.LOGGER.info("Spin STARTED by {}: wager = {}, category = {}, reel positions = [{}, {}, {}]",
            pName, value, pendingCategory, reels[0], reels[1], reels[2]);
        return true;
    }

    private void rollOutcome(int stakeVal) {
        ensureCanonicalPrizes();
        ProbabilityProfile profile = ProbabilityCalculator.calculate(stakeVal, Lucky7Config.INSTANCE.probabilitySettings());
        double roll = (world != null ? world.getRandom().nextDouble() : Math.random());
        WinCategory category = profile.choose(roll);
        pendingCategory = category;

        int finalReel0, finalReel1, finalReel2;
        var rng = world != null ? world.getRandom() : net.minecraft.util.math.random.Random.create();

        switch (category) {
            case JACKPOT -> {
                ReelSymbol jpSym = (rng.nextFloat() < 0.35f) ? ReelSymbol.GOLDEN_SEVEN : ReelSymbol.SEVEN;
                finalReel0 = ReelLayout.positionOf(0, jpSym);
                finalReel1 = ReelLayout.positionOf(1, jpSym);
                finalReel2 = ReelLayout.positionOf(2, jpSym);
                pendingPayoutValue = (int) Math.round(stakeVal * Lucky7Config.INSTANCE.jackpotMultiplier) + jackpotPool;
                jackpotPool = 100; // Reset pool base after jackpot hit

                // Pure uniform random across all 9 Jackpot prizes (Row 0: slots 0..8)
                int prizeIndex = rng.nextInt(9);
                ItemStack showcaseItem = items.get(STAKE_SLOTS + prizeIndex);
                pendingPrize = showcaseItem.isEmpty() ? new ItemStack(Items.NETHERITE_BLOCK, 16) : showcaseItem.copy();
            }
            case BIG_WIN -> {
                ReelSymbol[] bigSymbols = {ReelSymbol.BAR, ReelSymbol.DOUBLE_BAR, ReelSymbol.CROWN, ReelSymbol.DIAMOND};
                ReelSymbol sym = bigSymbols[rng.nextInt(bigSymbols.length)];
                finalReel0 = ReelLayout.positionOf(0, sym);
                finalReel1 = ReelLayout.positionOf(1, sym);
                finalReel2 = ReelLayout.positionOf(2, sym);
                pendingPayoutValue = (int) Math.round(stakeVal * Lucky7Config.INSTANCE.bigMultiplier);

                // Pure uniform random across all 9 Big Win prizes (Row 1: slots 9..17)
                int prizeIndex = 9 + rng.nextInt(9);
                ItemStack showcaseItem = items.get(STAKE_SLOTS + prizeIndex);
                pendingPrize = showcaseItem.isEmpty() ? new ItemStack(Items.DIAMOND, 64) : showcaseItem.copy();
            }
            case MEDIUM_WIN -> {
                ReelSymbol[] medSymbols = {ReelSymbol.BELL, ReelSymbol.STAR, ReelSymbol.HORSESHOE, ReelSymbol.CLOVER, ReelSymbol.COIN};
                ReelSymbol sym = medSymbols[rng.nextInt(medSymbols.length)];
                finalReel0 = ReelLayout.positionOf(0, sym);
                finalReel1 = ReelLayout.positionOf(1, sym);
                finalReel2 = ReelLayout.positionOf(2, sym);
                pendingPayoutValue = (int) Math.round(stakeVal * Lucky7Config.INSTANCE.mediumMultiplier);

                // Pure uniform random across all 9 Medium Win prizes (Row 2: slots 18..26)
                int prizeIndex = 18 + rng.nextInt(9);
                ItemStack showcaseItem = items.get(STAKE_SLOTS + prizeIndex);
                pendingPrize = showcaseItem.isEmpty() ? new ItemStack(Items.IRON_INGOT, 64) : showcaseItem.copy();
            }
            case SMALL_WIN -> {
                ReelSymbol[] fruitSymbols = {ReelSymbol.CHERRY, ReelSymbol.LEMON, ReelSymbol.ORANGE, ReelSymbol.GRAPES, ReelSymbol.WATERMELON};
                ReelSymbol sym = fruitSymbols[rng.nextInt(fruitSymbols.length)];
                finalReel0 = ReelLayout.positionOf(0, sym);
                finalReel1 = ReelLayout.positionOf(1, sym);
                finalReel2 = ReelLayout.positionOf(2, sym);
                pendingPayoutValue = (int) Math.round(stakeVal * Lucky7Config.INSTANCE.smallMultiplier);

                // Full stack directly from Row 2 (slots 18..26)
                int prizeIndex = 18 + rng.nextInt(9);
                ItemStack showcaseItem = items.get(STAKE_SLOTS + prizeIndex);
                pendingPrize = showcaseItem.isEmpty() ? new ItemStack(Items.IRON_INGOT, 64) : showcaseItem.copy();
            }
            case PAIR -> {
                ReelSymbol[] allSymbols = ReelSymbol.values();
                ReelSymbol pairSym = allSymbols[rng.nextInt(allSymbols.length)];
                ReelSymbol diffSym;
                do {
                    diffSym = allSymbols[rng.nextInt(allSymbols.length)];
                } while (diffSym == pairSym);
                finalReel0 = ReelLayout.positionOf(0, pairSym);
                finalReel1 = ReelLayout.positionOf(1, pairSym);
                finalReel2 = ReelLayout.positionOf(2, diffSym);
                pendingPayoutValue = (int) Math.round(stakeVal * Lucky7Config.INSTANCE.pairMultiplier);

                // Full stack directly from Row 2 (slots 18..26)
                int prizeIndex = 18 + rng.nextInt(9);
                ItemStack showcaseItem = items.get(STAKE_SLOTS + prizeIndex);
                pendingPrize = showcaseItem.isEmpty() ? new ItemStack(Items.IRON_INGOT, 64) : showcaseItem.copy();
            }
            default -> { // NO_WIN
                // Secret easter egg: 3 SKULLS curse! (deals 3 HP damage to the player)
                if (rng.nextFloat() < 0.06f) {
                    finalReel0 = ReelLayout.positionOf(0, ReelSymbol.SKULL);
                    finalReel1 = ReelLayout.positionOf(1, ReelSymbol.SKULL);
                    finalReel2 = ReelLayout.positionOf(2, ReelSymbol.SKULL);
                } else {
                    ReelSymbol[] allSymbols = ReelSymbol.values();
                    ReelSymbol s0 = allSymbols[rng.nextInt(allSymbols.length)];
                    ReelSymbol s1;
                    do {
                        s1 = allSymbols[rng.nextInt(allSymbols.length)];
                    } while (s1 == s0);
                    ReelSymbol s2;
                    do {
                        s2 = allSymbols[rng.nextInt(allSymbols.length)];
                    } while (s2 == s0 || s2 == s1);
                    finalReel0 = ReelLayout.positionOf(0, s0);
                    finalReel1 = ReelLayout.positionOf(1, s1);
                    finalReel2 = ReelLayout.positionOf(2, s2);
                }
                pendingPayoutValue = 0;
                pendingPrize = ItemStack.EMPTY;
            }
        }

        reels[0] = finalReel0;
        reels[1] = finalReel1;
        reels[2] = finalReel2;
        lastTier = category.ordinal();
    }

    // ---------------------------------------------------------------- Lifecycle Ticks
    public static void serverTick(net.minecraft.world.World world, BlockPos pos, BlockState state, Lucky7BlockEntity be) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (be.spinning && serverWorld.getTime() >= be.spinStart + be.spinTicks) {
            be.finishSpin(serverWorld);
        }
        if (be.redstonePulseTicks > 0) {
            be.redstonePulseTicks--;
            if (be.redstonePulseTicks == 0) {
                be.redstoneOutput = 0;
                serverWorld.updateComparators(pos, state.getBlock());
                serverWorld.updateComparators(pos.up(), state.getBlock());
            }
        }
        be.updateProperties();
    }

    public static void clientTick(net.minecraft.world.World world, BlockPos pos, BlockState state, Lucky7BlockEntity be) {
        if (be.spinning && be.clientSpinStart < 0) {
            be.clientSpinStart = System.currentTimeMillis();
        }
        if (!be.spinning) {
            be.clientSpinStart = -1;
        }
    }

    private void finishSpin(ServerWorld serverWorld) {
        spinning = false;
        lastWin = pendingPayoutValue;

        PlayerEntity player = activePlayer != null ? serverWorld.getServer().getPlayerManager().getPlayer(activePlayer) : null;

        if (pendingCategory.isWin() && !pendingPrize.isEmpty()) {
            wins++;
            if (pendingCategory == WinCategory.JACKPOT) {
                jackpots++;
                jackpotPool = 100;
                redstoneOutput = 15; // Max signal 15 on Jackpot!
            } else if (pendingCategory == WinCategory.BIG_WIN) {
                redstoneOutput = 10;
            } else if (pendingCategory == WinCategory.MEDIUM_WIN) {
                redstoneOutput = 6;
            } else {
                redstoneOutput = 3;
            }
            redstonePulseTicks = 80; // 4 seconds active pulse
            serverWorld.updateComparators(pos, getCachedState().getBlock());
            serverWorld.updateComparators(pos.up(), getCachedState().getBlock());

            bestWin = Math.max(bestWin, pendingPayoutValue);
            totalPaid += pendingPayoutValue;

            deliverPrize(player, pendingPrize.copy());
            history.add(0, pendingCategory.name() + ": " + pendingPrize.getCount() + "x " + Registries.ITEM.getId(pendingPrize.getItem()).getPath());

            if (pendingCategory == WinCategory.JACKPOT) {
                playSound(Lucky7Sounds.JACKPOT, 1.0f);
                spawnCelebrationParticles(serverWorld, true);
            } else {
                playSound(Lucky7Sounds.SMALL_WIN, 1.0f);
                spawnCelebrationParticles(serverWorld, false);
            }
        } else {
            // Secret Easter Egg: 3 Skulls curse! (deals 3 HP damage, player loses 1.5 hearts)
            if (reels[0] == ReelLayout.positionOf(0, ReelSymbol.SKULL) &&
                reels[1] == ReelLayout.positionOf(1, ReelSymbol.SKULL) &&
                reels[2] == ReelLayout.positionOf(2, ReelSymbol.SKULL)) {
                if (player != null) {
                    player.damage(serverWorld.getDamageSources().magic(), 3.0f);
                    serverWorld.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_WITHER_HURT, SoundCategory.BLOCKS, 1.0f, 0.8f);
                    serverWorld.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.25, 0.25, 0.25, 0.05);
                }
            }
            playSound(Lucky7Sounds.REEL_STOP, 1.0f);
            history.add(0, "БЕЗ ВЫИГРЫША");
        }

        while (history.size() > Lucky7Config.INSTANCE.historyLimit) {
            history.remove(history.size() - 1);
        }

        pendingPrize = ItemStack.EMPTY;
        pendingPayoutValue = 0;
        pendingCategory = WinCategory.NO_WIN;
        activePlayer = null;

        markDirtyAndSync();
    }

    private void deliverPrize(PlayerEntity player, ItemStack stack) {
        if (player instanceof ServerPlayerEntity sp) {
            String prizeName = stack.getCount() + "x " + stack.getName().getString();
            sp.sendMessage(Text.literal("★ ВЫИГРЫШ: " + prizeName).formatted(net.minecraft.util.Formatting.GREEN, net.minecraft.util.Formatting.BOLD), true);
        }
        if (player != null && player.getInventory().insertStack(stack)) {
            player.getInventory().markDirty();
            if (player instanceof ServerPlayerEntity sp) {
                sp.currentScreenHandler.sendContentUpdates();
            }
            return;
        }
        if (world != null) {
            ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            entity.setVelocity(0.0, 0.15, 0.0);
            world.spawnEntity(entity);
        }
    }

    private void spawnCelebrationParticles(ServerWorld world, boolean jackpot) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.2;
        double z = pos.getZ() + 0.5;
        if (jackpot) {
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 40, 0.4, 0.4, 0.4, 0.15);
            world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 30, 0.3, 0.5, 0.3, 0.1);
        } else {
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 15, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // ---------------------------------------------------------------- Return Stake
    public boolean returnStake(PlayerEntity player) {
        if (spinning) {
            message(player, "message.lucky7.busy");
            return false;
        }
        boolean moved = false;
        for (int i = 0; i < STAKE_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().insertStack(stack.copy())) {
                    player.dropItem(stack.copy(), false);
                }
                items.set(i, ItemStack.EMPTY);
                moved = true;
            }
        }
        if (moved) {
            markDirtyAndSync();
            if (player instanceof ServerPlayerEntity sp && sp.currentScreenHandler instanceof com.lucky7.mod.screen.Lucky7ScreenHandler) {
                sp.currentScreenHandler.sendContentUpdates();
            }
            playSound(Lucky7Sounds.BUTTON_CLICK, 1.0f);
        }
        return moved;
    }

    private void message(PlayerEntity player, String key) {
        if (player instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.translatable(key).formatted(net.minecraft.util.Formatting.RED), false);
        }
    }

    public void playSound(net.minecraft.sound.SoundEvent event, float volume) {
        if (world == null || !Lucky7Config.INSTANCE.soundsEnabled) return;
        world.playSound(null, pos, event, SoundCategory.BLOCKS, (float) (Lucky7Config.INSTANCE.masterVolume * volume * Lucky7Config.INSTANCE.effectsVolume), 1.0f);
    }

    // ---------------------------------------------------------------- Sync Properties
    private void updateProperties() {
        props.set(0, stakeValue());
        props.set(1, spinning ? 1 : 0);
        props.set(2, spins);
        props.set(3, wins);
        props.set(4, jackpots);
        props.set(5, bestWin);
        props.set(6, lastWin);
        props.set(7, lastTier);
        props.set(8, reels[0]);
        props.set(9, reels[1]);
        props.set(10, reels[2]);
        props.set(11, jackpotPool);
        props.set(12, hasOwner() ? 1 : 0);
        props.set(13, totalWagered);
        props.set(14, totalPaid);
        props.set(15, stakeItems());
    }

    public PropertyDelegate propertyDelegate() {
        updateProperties();
        return props;
    }

    public boolean isSpinning() { return spinning; }
    public int[] reels() { return reels; }
    public int jackpotPool() { return jackpotPool; }
    public List<String> historyLines() { return history; }
    public int getComparatorOutput() { return redstoneOutput; }

    public void startSpinClient() {
        this.spinning = true;
        this.spinTicks = 60;
        this.clientSpinStart = System.currentTimeMillis();
    }

    public float clientProgress(float tickDelta) {
        if (!spinning) return 1.0f;
        if (clientSpinStart <= 0) {
            clientSpinStart = System.currentTimeMillis();
        }
        float elapsedSeconds = (System.currentTimeMillis() - clientSpinStart) / 1000.0f;
        float totalDuration = Math.max(1.0f, spinTicks / 20.0f);
        return MathHelper.clamp(elapsedSeconds / totalDuration, 0.0f, 1.0f);
    }

    public void markDirtyAndSync() {
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.getChunkManager().markForUpdate(pos);
            }
        }
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt, registries);
        return nbt;
    }

    // ---------------------------------------------------------------- NBT
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, items, registries);
        if (owner != null) nbt.putString("owner", owner.toString());
        nbt.putBoolean("spinning", spinning);
        nbt.putLong("spinStart", spinStart);
        nbt.putInt("spinTicks", spinTicks);
        nbt.putLong("lastSpinAt", lastSpinAt);
        nbt.putIntArray("reels", reels);
        nbt.putInt("spins", spins);
        nbt.putInt("wins", wins);
        nbt.putInt("jackpots", jackpots);
        nbt.putInt("bestWin", bestWin);
        nbt.putInt("lastWin", lastWin);
        nbt.putInt("lastTier", lastTier);
        nbt.putInt("jackpotPool", jackpotPool);
        nbt.putInt("totalWagered", totalWagered);
        nbt.putInt("totalPaid", totalPaid);
        nbt.putInt("pendingValue", pendingPayoutValue);
        nbt.putInt("pendingCategory", pendingCategory.ordinal());

        NbtList list = new NbtList();
        for (String line : history) list.add(NbtString.of(line));
        nbt.put("history", list);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        items.clear();
        Inventories.readNbt(nbt, items, registries);
        ensureCanonicalPrizes();

        owner = nbt.contains("owner") ? uuid(nbt.getString("owner")) : null;
        boolean wasSpinning = spinning;
        spinning = nbt.getBoolean("spinning");
        spinStart = nbt.getLong("spinStart");
        spinTicks = nbt.getInt("spinTicks") > 0 ? nbt.getInt("spinTicks") : 60;
        lastSpinAt = nbt.contains("lastSpinAt") ? nbt.getLong("lastSpinAt") : 0;

        if (spinning && !wasSpinning) {
            clientSpinStart = System.currentTimeMillis();
        } else if (!spinning) {
            clientSpinStart = -1;
        }

        int[] saved = nbt.getIntArray("reels");
        if (saved.length == 3) System.arraycopy(saved, 0, reels, 0, 3);

        spins = nbt.getInt("spins");
        wins = nbt.getInt("wins");
        jackpots = nbt.getInt("jackpots");
        bestWin = nbt.getInt("bestWin");
        lastWin = nbt.getInt("lastWin");
        lastTier = nbt.getInt("lastTier");
        jackpotPool = nbt.getInt("jackpotPool");
        totalWagered = nbt.getInt("totalWagered");
        totalPaid = nbt.getInt("totalPaid");
        pendingPayoutValue = nbt.getInt("pendingValue");

        int catIdx = nbt.getInt("pendingCategory");
        pendingCategory = (catIdx >= 0 && catIdx < WinCategory.values().length) ? WinCategory.values()[catIdx] : WinCategory.NO_WIN;

        history.clear();
        NbtList list = nbt.getList("history", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) history.add(list.getString(i));
    }

    private static UUID uuid(String value) {
        try { return UUID.fromString(value); } catch (Exception e) { return null; }
    }

    // ---------------------------------------------------------------- Inventory
    @Override
    public int size() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return (slot >= 0 && slot < TOTAL_SLOTS) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        markDirtyAndSync();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        markDirtyAndSync();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            items.set(slot, stack);
            markDirtyAndSync();
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        items.clear();
        markDirtyAndSync();
    }
}
