package com.lucky7.mod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class CasinoContractItem extends Item {
    public CasinoContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sw)) {
            return InteractionResult.PASS;
        }

        boolean hasDebt = player.entityTags().contains("lucky7_debtor");
        boolean isBoss = player.entityTags().contains("lucky7_vip_boss");

        if (!hasDebt && !isBoss) {
            // ACT I: Sign the Mafia Loan Contract with Don Creeperione
            player.addTag("lucky7_debtor");
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 32));
            giveOrDrop(player, new ItemStack(Lucky7Items.GOLDEN_CHIP, 8));

            sp.sendSystemMessage(Component.literal("═══════════════════════════════════════════").withStyle(ChatFormatting.GOLD));
            sp.sendSystemMessage(Component.literal("🎩 ДОН КРИПЕРИОНЕ (Крёстный отец «Lucky 7»):").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            sp.sendSystemMessage(Component.literal("«Слушай сюда, уважаемый гость! Я выдал тебе 32 Алмаза и 8 Золотых Фишек на раскрутку в моём казино.»").withStyle(ChatFormatting.YELLOW));
            sp.sendSystemMessage(Component.literal("«Крути слоты, испытай Колесо Риска или обыграй шулера в Напёрстки!»").withStyle(ChatFormatting.WHITE));
            sp.sendSystemMessage(Component.literal("⚠ УСЛОВИЕ СДЕЛКИ: Верни мне 48 Алмазов (кликни Контрактом с алмазами в инвентаре).").withStyle(ChatFormatting.GOLD));
            sp.sendSystemMessage(Component.literal("💀 Если решишь кинуть мафию и кликнешь без 48 алмазов — за тобой выедут мои коллекторы Боб и Джо!»").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            sp.sendSystemMessage(Component.literal("═══════════════════════════════════════════").withStyle(ChatFormatting.GOLD));

            sw.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0f, 0.85f);
            sw.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.15f);
            sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 45, 0.5, 0.5, 0.5, 0.15);
            return InteractionResult.SUCCESS;
        }

        if (hasDebt) {
            // Count diamonds in player inventory
            int diamondCount = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.is(Items.DIAMOND)) {
                    diamondCount += s.getCount();
                }
            }

            if (diamondCount >= 48) {
                // ACT II (GOOD ENDING): Pay off the debt & become VIP Casino Baron + get Royal VIP Trophy!
                int toRemove = 48;
                for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
                    ItemStack s = player.getInventory().getItem(i);
                    if (s.is(Items.DIAMOND)) {
                        int take = Math.min(toRemove, s.getCount());
                        s.shrink(take);
                        toRemove -= take;
                    }
                }
                player.removeTag("lucky7_debtor");
                player.addTag("lucky7_vip_boss");

                giveOrDrop(player, new ItemStack(Lucky7Items.VIP_TROPHY, 1));
                giveOrDrop(player, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 3));
                giveOrDrop(player, new ItemStack(Items.NETHERITE_INGOT, 4));
                giveOrDrop(player, new ItemStack(Lucky7Items.GOLDEN_CHIP, 16));

                sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 12000, 2));
                sp.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 12000, 1));

                sp.sendSystemMessage(Component.literal("═══════════════════════════════════════════").withStyle(ChatFormatting.GREEN));
                sp.sendSystemMessage(Component.literal("🎩 ДОН КРИПЕРИОНЕ:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                sp.sendSystemMessage(Component.literal("«Моё почтение! Ты человек слова! Долг в 48 алмазов закрыт сполна!»").withStyle(ChatFormatting.GREEN));
                sp.sendSystemMessage(Component.literal("«Отныне ты — Совладелец Синдиката Lucky 7! Держи эксклюзивный Королевский VIP Кубок Казино, незерит и 16 Золотых фишек!»").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                sp.sendSystemMessage(Component.literal("═══════════════════════════════════════════").withStyle(ChatFormatting.GREEN));

                sw.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 90, 0.6, 0.7, 0.6, 0.2);
            } else {
                // ACT II (CHAOS EVENT): Not enough diamonds -> Mafia Bouncers ambush!
                sp.sendSystemMessage(Component.literal("═══════════════════════════════════════════").withStyle(ChatFormatting.DARK_RED));
                sp.sendSystemMessage(Component.literal("🚨 ДОН КРИПЕРИОНЕ:").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                sp.sendSystemMessage(Component.literal("«У тебя всего " + diamondCount + " из 48 алмазов! Решил кинуть наше казино?!»").withStyle(ChatFormatting.YELLOW));
                sp.sendSystemMessage(Component.literal("«Парни! Вышибала Боб, Коллектор Джо — оформите этому лудоману горячий приём!»").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                sp.sendSystemMessage(Component.literal("═══════════════════════════════════════════").withStyle(ChatFormatting.DARK_RED));

                spawnBouncer(sw, player, "🕶 Вышибала Боб (Отдел Взысканий)", 2.2, 0.0);
                spawnBouncer(sw, player, "💼 Коллектор Джо (Налоговая Мафия)", -2.2, 0.0);

                sw.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5f, 1.0f);
                sw.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 1.3f);
            }
            return InteractionResult.SUCCESS;
        }

        // ACT III: Player is already a VIP Boss -> Collect daily syndicate dividend or reset story
        if (player.isShiftKeyDown()) {
            player.removeTag("lucky7_vip_boss");
            sp.sendSystemMessage(Component.literal("🔄 [Сюжет сброшен] Вы снова можете подписать новый Контракт с Доном Криперионе!").withStyle(ChatFormatting.YELLOW));
        } else {
            giveOrDrop(player, new ItemStack(Lucky7Items.LUCKY_COIN, 4));
            sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 2));
            sp.sendSystemMessage(Component.literal("👑 [Синдикат Lucky 7] Вы уже почётный партнёр Дона Криперионе! Выдано +4 Монеты Удачи (Shift+ПКМ — начать сюжет заново).").withStyle(ChatFormatting.GOLD));
            sw.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
        }
        return InteractionResult.SUCCESS;
    }

    private void spawnBouncer(ServerLevel sw, Player player, String name, double offsetX, double offsetZ) {
        Skeleton bouncer = EntityTypes.SKELETON.spawn(sw, player.blockPosition().offset((int) offsetX, 0, (int) offsetZ), EntitySpawnReason.EVENT);
        if (bouncer != null) {
            bouncer.setCustomName(Component.literal(name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            bouncer.setCustomNameVisible(true);
            bouncer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            bouncer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            bouncer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            bouncer.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
            bouncer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            bouncer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Lucky7Items.GOLDEN_CHIP, 4));
            bouncer.setDropChance(EquipmentSlot.OFFHAND, 1.0f);
            bouncer.addEffect(new MobEffectInstance(MobEffects.SPEED, 6000, 0));
            sw.sendParticles(ParticleTypes.LARGE_SMOKE, bouncer.getX(), bouncer.getY() + 1.0, bouncer.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
        }
    }
}
