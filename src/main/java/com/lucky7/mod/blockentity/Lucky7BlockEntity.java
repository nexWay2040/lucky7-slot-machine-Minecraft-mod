package com.lucky7.mod.blockentity;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.Lucky7Block;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.game.WinCategory;
import com.lucky7.mod.game.probability.ProbabilityCalculator;
import com.lucky7.mod.game.probability.ProbabilityProfile;
import com.lucky7.mod.game.reels.ReelLayout;
import com.lucky7.mod.game.reels.ReelSymbol;
import com.lucky7.mod.item.Lucky7Items;
import com.lucky7.mod.screen.Lucky7ScreenHandler;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lucky7BlockEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos> {
    public static final int STAKE_SLOTS = 9;
    public static final int PRIZE_SLOTS = 27;
    public static final int TOTAL_SLOTS = STAKE_SLOTS + PRIZE_SLOTS;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final int[] reels = new int[]{ReelSymbol.CHERRY.ordinal(), ReelSymbol.BELL.ordinal(), ReelSymbol.SEVEN.ordinal()};
    private final SimpleContainerData props = new SimpleContainerData(20);

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

    private int feverSpins = 0;
    private int lastReelStoppedClient = -1;

    public int feverSpins() { return feverSpins; }
    public boolean isFeverMode() { return feverSpins > 0; }

    public Lucky7BlockEntity(BlockPos pos, BlockState state) {
        super(Lucky7BlockEntities.SLOT_MACHINE, pos, state);
        ensureCanonicalPrizes();
    }

    public void ensureCanonicalPrizes() {
        ItemStack s0 = items.get(STAKE_SLOTS);
        ItemStack s1 = items.get(STAKE_SLOTS + 1);
        ItemStack s2 = items.get(STAKE_SLOTS + 2);
        if (!s0.is(Items.NETHERITE_BLOCK) || s0.getCount() != 16 ||
            !s1.is(Items.NETHERITE_INGOT) || s1.getCount() != 32 ||
            !s2.is(Items.DIAMOND_BLOCK) || s2.getCount() != 64) {
            applyNewPrizePool();
            setChanged();
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
        setPrizeStack(19, new ItemStack(Items.RAW_COPPER_BLOCK, 64));
        setPrizeStack(20, new ItemStack(Items.LAPIS_LAZULI, 64));
        setPrizeStack(21, new ItemStack(Items.REDSTONE, 64));
        setPrizeStack(22, new ItemStack(Items.QUARTZ, 64));
        setPrizeStack(23, new ItemStack(Items.AMETHYST_SHARD, 32));
        setPrizeStack(24, createPotionStack(Potions.LONG_NIGHT_VISION, 1));
        setPrizeStack(25, createPotionStack(Potions.LONG_FIRE_RESISTANCE, 1));
        setPrizeStack(26, createPotionStack(Potions.LONG_WATER_BREATHING, 1));
    }

    private static ItemStack createPotionStack(Holder<Potion> potion, int count) {
        ItemStack stack = PotionContents.createItemStack(Items.POTION, potion);
        stack.setCount(count);
        return stack;
    }

    private void setPrizeStack(int index, ItemStack stack) {
        int slot = STAKE_SLOTS + index;
        if (slot < TOTAL_SLOTS) {
            items.set(slot, stack);
        }
    }

    // ---------------------------------------------------------------- ExtendedMenuProvider
    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.lucky7.slot_machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        ensureCanonicalPrizes();
        addViewer(player);
        return new Lucky7ScreenHandler(syncId, playerInventory, this, worldPosition);
    }

    // ---------------------------------------------------------------- Ownership & Viewers
    public UUID ownerId() { return owner; }
    public boolean hasOwner() { return owner != null; }

    public void setOwner(Player player) {
        if (owner == null && player != null) {
            owner = player.getUUID();
            markDirtyAndSync();
        }
    }

    public boolean isOwner(Player player) {
        return owner == null || (player != null && owner.equals(player.getUUID()));
    }

    public void addViewer(Player player) {
        if (!viewers.contains(player.getUUID())) {
            viewers.add(player.getUUID());
        }
        if (owner == null) {
            setOwner(player);
        }
    }

    public void removeViewer(Player player) {
        viewers.remove(player.getUUID());
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
    public boolean startSpin(Player player) {
        if (spinning) {
            Lucky7Mod.LOGGER.info("Spin rejected: machine is already spinning at {}", worldPosition);
            return false;
        }
        if (level == null || level.isClientSide()) return false;
        if (!Lucky7Config.INSTANCE.enabled) {
            Lucky7Mod.LOGGER.info("Spin rejected: machine disabled in config");
            message(player, "message.lucky7.disabled");
            return false;
        }

        boolean isFever = (feverSpins > 0);
        int value = stakeValue();
        if (value <= 0) {
            if (isFever) {
                value = 500; // Simulated high-roller stake during free fever spins
            } else {
                Lucky7Mod.LOGGER.info("Spin rejected: stake value is 0 (stakeItems = {})", stakeItems());
                message(player, "message.lucky7.empty_stake");
                return false;
            }
        }
        if (value > Lucky7Config.INSTANCE.maxStakeValue && !isFever) {
            Lucky7Mod.LOGGER.info("Spin rejected: stake value {} exceeds max {}", value, Lucky7Config.INSTANCE.maxStakeValue);
            message(player, "message.lucky7.stake_too_big");
            return false;
        }
        if (stakeItems() > Lucky7Config.INSTANCE.maxStakeItems && !isFever) {
            Lucky7Mod.LOGGER.info("Spin rejected: stake items count {} exceeds max {}", stakeItems(), Lucky7Config.INSTANCE.maxStakeItems);
            message(player, "message.lucky7.too_many_items");
            return false;
        }
        if (lastSpinAt > 0 && level.getGameTime() >= lastSpinAt && (level.getGameTime() - lastSpinAt) < 5) {
            Lucky7Mod.LOGGER.info("Spin rejected: cooldown active (< 5 ticks)");
            return false;
        }

        // Check for Divine VIP Stake or Royal Casino VIP Trophy!
        boolean isVip = false;
        boolean hasVipTrophy = false;
        for (int i = 0; i < STAKE_SLOTS; i++) {
            ItemStack st = items.get(i);
            if (st.is(Lucky7Items.VIP_TROPHY)) {
                hasVipTrophy = true;
                isVip = true;
            } else if (st.is(Items.ENCHANTED_GOLDEN_APPLE) || st.is(Items.NETHER_STAR) || st.is(Items.DRAGON_EGG)) {
                isVip = true;
            }
        }

        if (hasVipTrophy && player instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 2400, 2));
            sp.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 2400, 1));
            sp.sendSystemMessage(Component.literal("👑 [СИНДИКАТ LUCKY 7] Распознан Королевский VIP Кубок! Босс казино крутит с максимальным шансом — Кубок возвращён владельцу!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            if (level instanceof ServerLevel sw) {
                sw.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.15f);
            }
        } else if (isVip && player instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 1));
            sp.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0));
            sp.sendOverlayMessage(Component.literal("✨ БОЖЕСТВЕННАЯ СТАВКА! Госпожа Удача благоволит вам!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            if (level instanceof ServerLevel sw) {
                sw.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);
            }
        }

        if (isFever) {
            feverSpins--;
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.literal("★ FEVER SPIN! (Осталось бесплатных спинов: " + feverSpins + ")").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }

        lastSpinAt = level.getGameTime();
        activePlayer = (player != null) ? player.getUUID() : null;

        // Clear stake items atomically on server (return VIP Trophy to the Boss so it is never lost!)
        for (int i = 0; i < STAKE_SLOTS; i++) {
            ItemStack st = items.get(i);
            if (!st.isEmpty() && st.is(Lucky7Items.VIP_TROPHY) && player != null) {
                ItemStack copy = st.copy();
                if (!player.getInventory().add(copy)) {
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), copy));
                }
            }
            items.set(i, ItemStack.EMPTY);
        }
        if (player instanceof ServerPlayer sp && sp.containerMenu instanceof Lucky7ScreenHandler) {
            sp.containerMenu.broadcastChanges();
        }

        totalWagered += value;
        jackpotPool += Math.max(1, value / 10);
        spins++;

        // Roll outcome and map to exact reel positions
        rollOutcome(value, isFever, isVip);

        spinning = true;
        spinStart = level.getGameTime();
        spinTicks = Lucky7Config.INSTANCE.spinTicks;

        markDirtyAndSync();
        playSound(Lucky7Sounds.MACHINE_START, 1.0f);
        playSound(Lucky7Sounds.REEL_SPIN, 0.85f);

        String pName = (player != null) ? player.getName().getString() : "Redstone";
        Lucky7Mod.LOGGER.info("Spin STARTED by {}: wager = {}, category = {}, reel positions = [{}, {}, {}]",
            pName, value, pendingCategory, reels[0], reels[1], reels[2]);
        return true;
    }

    private boolean feverTriggeredThisSpin = false;
    private boolean coinShowerTriggeredThisSpin = false;

    private void rollOutcome(int stakeVal, boolean isFever, boolean isVip) {
        ensureCanonicalPrizes();
        feverTriggeredThisSpin = false;
        coinShowerTriggeredThisSpin = false;

        RandomSource rng = level != null ? level.getRandom() : RandomSource.create();
        WinCategory category;

        if (isVip) {
            category = rng.nextFloat() < 0.65f ? WinCategory.JACKPOT : WinCategory.BIG_WIN;
        } else if (isFever) {
            float f = rng.nextFloat();
            category = (f < 0.35f) ? WinCategory.JACKPOT : (f < 0.70f ? WinCategory.BIG_WIN : WinCategory.MEDIUM_WIN);
        } else {
            ProbabilityProfile profile = ProbabilityCalculator.calculate(stakeVal, Lucky7Config.INSTANCE.probabilitySettings());
            double roll = rng.nextDouble();
            category = profile.choose(roll);
        }
        pendingCategory = category;

        int finalReel0, finalReel1, finalReel2;

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
                // Rare chance to activate FEVER MODE on 3 Clovers or 3 Stars!
                if (!isFever && rng.nextFloat() < 0.25f) {
                    ReelSymbol sym = (rng.nextBoolean()) ? ReelSymbol.CLOVER : ReelSymbol.STAR;
                    finalReel0 = ReelLayout.positionOf(0, sym);
                    finalReel1 = ReelLayout.positionOf(1, sym);
                    finalReel2 = ReelLayout.positionOf(2, sym);
                    feverTriggeredThisSpin = true;
                } else {
                    ReelSymbol[] bigSymbols = {ReelSymbol.BAR, ReelSymbol.DOUBLE_BAR, ReelSymbol.CROWN, ReelSymbol.DIAMOND};
                    ReelSymbol sym = bigSymbols[rng.nextInt(bigSymbols.length)];
                    finalReel0 = ReelLayout.positionOf(0, sym);
                    finalReel1 = ReelLayout.positionOf(1, sym);
                    finalReel2 = ReelLayout.positionOf(2, sym);
                }
                pendingPayoutValue = (int) Math.round(stakeVal * Lucky7Config.INSTANCE.bigMultiplier);

                // Pure uniform random across all 9 Big Win prizes (Row 1: slots 9..17)
                int prizeIndex = 9 + rng.nextInt(9);
                ItemStack showcaseItem = items.get(STAKE_SLOTS + prizeIndex);
                pendingPrize = showcaseItem.isEmpty() ? new ItemStack(Items.DIAMOND, 64) : showcaseItem.copy();
            }
            case MEDIUM_WIN -> {
                // 3 Coins triggers Coin Shower Easter Egg!
                if (rng.nextFloat() < 0.20f) {
                    finalReel0 = ReelLayout.positionOf(0, ReelSymbol.COIN);
                    finalReel1 = ReelLayout.positionOf(1, ReelSymbol.COIN);
                    finalReel2 = ReelLayout.positionOf(2, ReelSymbol.COIN);
                    coinShowerTriggeredThisSpin = true;
                } else {
                    ReelSymbol[] medSymbols = {ReelSymbol.BELL, ReelSymbol.HORSESHOE, ReelSymbol.CLOVER, ReelSymbol.STAR};
                    ReelSymbol sym = medSymbols[rng.nextInt(medSymbols.length)];
                    finalReel0 = ReelLayout.positionOf(0, sym);
                    finalReel1 = ReelLayout.positionOf(1, sym);
                    finalReel2 = ReelLayout.positionOf(2, sym);
                }
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
    public static void serverTick(Level world, BlockPos pos, BlockState state, Lucky7BlockEntity be) {
        if (!(world instanceof ServerLevel serverWorld)) return;
        if (be.spinning && serverWorld.getGameTime() >= be.spinStart + be.spinTicks) {
            be.finishSpin(serverWorld);
        }
        if (be.redstonePulseTicks > 0) {
            be.redstonePulseTicks--;
            if (be.redstonePulseTicks == 0) {
                be.redstoneOutput = 0;
                serverWorld.updateNeighbourForOutputSignal(pos, state.getBlock());
                serverWorld.updateNeighbourForOutputSignal(pos.above(), state.getBlock());
            }
        }
        be.updateProperties();
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, Lucky7BlockEntity be) {
        if (be.spinning) {
            if (be.clientSpinStart < 0) {
                be.clientSpinStart = System.currentTimeMillis();
                be.lastReelStoppedClient = -1;
            }
        } else {
            be.clientSpinStart = -1;
            be.lastReelStoppedClient = -1;
        }

        // Ambient mystical flame particles during FEVER MODE
        if (be.feverSpins > 0 && world.getRandom().nextFloat() < 0.35f) {
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.2 + world.getRandom().nextDouble() * 0.6,
                pos.getY() + 1.8 + world.getRandom().nextDouble() * 0.3,
                pos.getZ() + 0.2 + world.getRandom().nextDouble() * 0.6,
                0.0, 0.02, 0.0);
        }
    }

    private void finishSpin(ServerLevel serverWorld) {
        spinning = false;
        lastWin = pendingPayoutValue;

        Player player = activePlayer != null ? serverWorld.getServer().getPlayerList().getPlayer(activePlayer) : null;

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
            serverWorld.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            serverWorld.updateNeighbourForOutputSignal(worldPosition.above(), getBlockState().getBlock());

            bestWin = Math.max(bestWin, pendingPayoutValue);
            totalPaid += pendingPayoutValue;

            deliverPrize(serverWorld, player, pendingPrize.copy());
            history.add(0, pendingCategory.name() + ": " + pendingPrize.getCount() + "x " + BuiltInRegistries.ITEM.getKey(pendingPrize.getItem()).getPath());

            if (pendingCategory == WinCategory.JACKPOT) {
                playSound(Lucky7Sounds.JACKPOT, 1.0f);
                spawnCelebrationParticles(serverWorld, true);
            } else {
                playSound(Lucky7Sounds.SMALL_WIN, 1.0f);
                spawnCelebrationParticles(serverWorld, false);
            }

            // Trigger FEVER MODE Activation event!
            if (feverTriggeredThisSpin) {
                feverSpins = 3;
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("🔥 FEVER MODE АКТИВИРОВАН! 3 БЕСПЛАТНЫХ СУПЕР-СПИНА! 🔥").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }
                serverWorld.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);
                serverWorld.playSound(null, worldPosition, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0f, 1.2f);
                serverWorld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.8, worldPosition.getZ() + 0.5, 60, 0.4, 0.4, 0.4, 0.2);
            }

            // Coin Shower Easter Egg!
            if (coinShowerTriggeredThisSpin) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.literal("🪙 МОНЕТНЫЙ ДОЖДЬ! +64 золотых самородка из лотка!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                }
                ejectFromHopper(serverWorld, new ItemStack(Items.GOLD_NUGGET, 64), 0.22);
            }
        } else {
            // Secret Easter Egg: 3 Skulls curse! (deals 3 HP damage, player loses 1.5 hearts)
            if (reels[0] == ReelLayout.positionOf(0, ReelSymbol.SKULL) &&
                reels[1] == ReelLayout.positionOf(1, ReelSymbol.SKULL) &&
                reels[2] == ReelLayout.positionOf(2, ReelSymbol.SKULL)) {
                if (player != null) {
                    player.hurtServer(serverWorld, serverWorld.damageSources().magic(), 3.0f);
                    serverWorld.playSound(null, worldPosition, SoundEvents.WITHER_HURT, SoundSource.BLOCKS, 1.0f, 0.8f);
                    serverWorld.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 25, 0.25, 0.25, 0.25, 0.05);
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

    private void deliverPrize(ServerLevel serverWorld, Player player, ItemStack stack) {
        if (player instanceof ServerPlayer sp) {
            String prizeName = stack.getCount() + "x " + stack.getHoverName().getString();
            sp.sendOverlayMessage(Component.literal("★ ВЫИГРЫШ: " + prizeName).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        // Eject item directly out of the bottom physical hopper payout tray!
        ejectFromHopper(serverWorld, stack, 0.18);
        serverWorld.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f);
        serverWorld.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.5f);
    }

    private void ejectFromHopper(ServerLevel serverWorld, ItemStack stack, double speed) {
        Direction facing = Direction.NORTH;
        try {
            facing = getBlockState().getValue(Lucky7Block.FACING);
        } catch (Exception ignored) {}

        double fx = worldPosition.getX() + 0.5 + facing.getStepX() * 0.52;
        double fy = worldPosition.getY() + 0.32;
        double fz = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.52;

        ItemEntity entity = new ItemEntity(serverWorld, fx, fy, fz, stack);
        double spreadX = (serverWorld.getRandom().nextDouble() - 0.5) * 0.06;
        double spreadZ = (serverWorld.getRandom().nextDouble() - 0.5) * 0.06;
        entity.setDeltaMovement(facing.getStepX() * speed + spreadX, 0.18, facing.getStepZ() * speed + spreadZ);
        serverWorld.addFreshEntity(entity);
    }

    private void spawnCelebrationParticles(ServerLevel world, boolean jackpot) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.2;
        double z = worldPosition.getZ() + 0.5;
        if (jackpot) {
            world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 50, 0.4, 0.4, 0.4, 0.15);
            world.sendParticles(ParticleTypes.FIREWORK, x, y + 0.8, z, 50, 0.4, 0.6, 0.4, 0.15);
            world.playSound(null, worldPosition, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 1.0f, 1.0f);
            world.playSound(null, worldPosition, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1.0f, 1.0f);
            world.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);

            // Bonus shower of 16 gold nuggets cascading into the tray
            ejectFromHopper(world, new ItemStack(Items.GOLD_NUGGET, 16), 0.22);
        } else {
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 20, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // ---------------------------------------------------------------- Return Stake
    public boolean returnStake(Player player) {
        if (spinning) {
            message(player, "message.lucky7.busy");
            return false;
        }
        boolean moved = false;
        for (int i = 0; i < STAKE_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                if (!player.getInventory().add(copy)) {
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), copy));
                }
                items.set(i, ItemStack.EMPTY);
                moved = true;
            }
        }
        if (moved) {
            markDirtyAndSync();
            if (player instanceof ServerPlayer sp && sp.containerMenu instanceof Lucky7ScreenHandler) {
                sp.containerMenu.broadcastChanges();
            }
            playSound(Lucky7Sounds.BUTTON_CLICK, 1.0f);
        }
        return moved;
    }

    private void message(Player player, String key) {
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
        }
    }

    public void playSound(SoundEvent event, float volume) {
        if (level == null || !Lucky7Config.INSTANCE.soundsEnabled) return;
        level.playSound(null, worldPosition, event, SoundSource.BLOCKS, (float) (Lucky7Config.INSTANCE.masterVolume * volume * Lucky7Config.INSTANCE.effectsVolume), 1.0f);
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
        props.set(16, feverSpins);
    }

    public ContainerData propertyDelegate() {
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
        return Mth.clamp(elapsedSeconds / totalDuration, 0.0f, 1.0f);
    }

    public void markDirtyAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            if (level instanceof ServerLevel serverWorld) {
                serverWorld.getChunkSource().blockChanged(worldPosition);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    // ---------------------------------------------------------------- Serialization (26.3 ValueInput / ValueOutput)
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        if (owner != null) output.putString("owner", owner.toString());
        output.putBoolean("spinning", spinning);
        output.putLong("spinStart", spinStart);
        output.putInt("spinTicks", spinTicks);
        output.putLong("lastSpinAt", lastSpinAt);
        output.putIntArray("reels", reels);
        output.putInt("spins", spins);
        output.putInt("wins", wins);
        output.putInt("jackpots", jackpots);
        output.putInt("bestWin", bestWin);
        output.putInt("lastWin", lastWin);
        output.putInt("lastTier", lastTier);
        output.putInt("jackpotPool", jackpotPool);
        output.putInt("totalWagered", totalWagered);
        output.putInt("totalPaid", totalPaid);
        output.putInt("pendingValue", pendingPayoutValue);
        output.putInt("pendingCategory", pendingCategory.ordinal());
        output.putInt("feverSpins", feverSpins);
        output.putString("historyText", String.join("\n", history));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        ensureCanonicalPrizes();

        String ownerStr = input.getStringOr("owner", "");
        owner = !ownerStr.isEmpty() ? uuid(ownerStr) : null;

        boolean wasSpinning = spinning;
        spinning = input.getBooleanOr("spinning", false);
        spinStart = input.getLongOr("spinStart", 0L);
        int st = input.getIntOr("spinTicks", 60);
        spinTicks = st > 0 ? st : 60;
        lastSpinAt = input.getLongOr("lastSpinAt", 0L);

        if (spinning && !wasSpinning) {
            clientSpinStart = System.currentTimeMillis();
        } else if (!spinning) {
            clientSpinStart = -1;
        }

        input.getIntArray("reels").ifPresent(saved -> {
            if (saved.length == 3) System.arraycopy(saved, 0, reels, 0, 3);
        });

        spins = input.getIntOr("spins", 0);
        wins = input.getIntOr("wins", 0);
        jackpots = input.getIntOr("jackpots", 0);
        bestWin = input.getIntOr("bestWin", 0);
        lastWin = input.getIntOr("lastWin", 0);
        lastTier = input.getIntOr("lastTier", -1);
        jackpotPool = input.getIntOr("jackpotPool", 100);
        totalWagered = input.getIntOr("totalWagered", 0);
        totalPaid = input.getIntOr("totalPaid", 0);
        pendingPayoutValue = input.getIntOr("pendingValue", 0);

        int catIdx = input.getIntOr("pendingCategory", 0);
        pendingCategory = (catIdx >= 0 && catIdx < WinCategory.values().length) ? WinCategory.values()[catIdx] : WinCategory.NO_WIN;
        feverSpins = input.getIntOr("feverSpins", 0);

        history.clear();
        String histText = input.getStringOr("historyText", "");
        if (!histText.isEmpty()) {
            for (String line : histText.split("\n")) {
                if (!line.isBlank()) history.add(line);
            }
        }
    }

    private static UUID uuid(String value) {
        try { return UUID.fromString(value); } catch (Exception e) { return null; }
    }

    // ---------------------------------------------------------------- Container Implementation
    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return (slot >= 0 && slot < TOTAL_SLOTS) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        markDirtyAndSync();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        markDirtyAndSync();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            items.set(slot, stack);
            markDirtyAndSync();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this &&
            player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Clear all 27 read-only prize showcase slots so breaking the machine NEVER drops showcase prizes!
        for (int i = STAKE_SLOTS; i < TOTAL_SLOTS; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void clearContent() {
        items.clear();
        markDirtyAndSync();
    }
}
