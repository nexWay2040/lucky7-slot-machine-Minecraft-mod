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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Interactive Lucky Dice item (2d6 roll on right-click with cooldown + casino stake support).
 */
public class LuckyDiceItem extends Item {
    public LuckyDiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sw) {
            sp.getCooldowns().addCooldown(player.getItemInHand(hand), 40);
            int d1 = 1 + sw.getRandom().nextInt(6);
            int d2 = 1 + sw.getRandom().nextInt(6);
            int sum = d1 + d2;

            sw.playSound(null, player.blockPosition(), SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1.0f, 1.4f);

            if (sum == 12) {
                sp.sendSystemMessage(Component.literal("🎲 [Счастливые Кости] Дубль 6+6 = 12! ДЖЕКПОТ КУБИКОВ: Удача IV + 4 Изумруда!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 1800, 3));
                sp.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1));
                sw.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 25, 0.3, 0.4, 0.3, 0.1);
                ItemStack prize = new ItemStack(Items.EMERALD, 4);
                if (!player.getInventory().add(prize)) {
                    sw.addFreshEntity(new ItemEntity(sw, player.getX(), player.getY() + 0.5, player.getZ(), prize));
                }
            } else if (sum == 7) {
                sp.sendSystemMessage(Component.literal("🎲 [Счастливые Кости] Выпало " + d1 + " + " + d2 + " = 7! Счастливая семёрка — Удача II!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 1));
                sw.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
            } else if (sum == 2) {
                sp.sendSystemMessage(Component.literal("🎲 [Счастливые Кости] «Змеиные глазки» 1+1 = 2! Мафия смеётся над вашим везением!").withStyle(ChatFormatting.RED));
                sp.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
                sw.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.2, player.getZ(), 15, 0.25, 0.3, 0.25, 0.04);
            } else {
                sp.sendOverlayMessage(Component.literal("🎲 Бросок костей: [" + d1 + "] + [" + d2 + "] = " + sum).withStyle(ChatFormatting.YELLOW));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
