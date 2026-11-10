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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Exclusive Royal VIP Trophy item.
 * Can be used in hand to activate the "Casino Boss Luck Aura" or placed into the Lucky 7 Slot Machine
 * as a 777-point VIP stake that returns to the player and triggers VIP jackpot odds.
 */
public class VipTrophyItem extends Item {
    private static final String[] BOSS_QUOTES = {
        "👑 [VIP СИНДИКАТ] «Сегодня фортуна на моей стороне — пора срывать джекпот 777!»",
        "👑 [VIP СИНДИКАТ] «Золотой кубок сияет — ставки повышаются, шансы растут!»",
        "👑 [VIP СИНДИКАТ] «Так, стоп, почему за мной снова бегут коллекторы в золотой броне?!»",
        "👑 [VIP СИНДИКАТ] «Последний спин и иду в шахту... Ладно, ещё один бесплатный спин!»",
        "👑 [VIP СИНДИКАТ] «Дон Криперионе, старина, эти алмазы точно окупятся сполна!»"
    };

    public VipTrophyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sw) {
            sp.getCooldowns().addCooldown(player.getItemInHand(hand), 100);
            int idx = sw.getRandom().nextInt(BOSS_QUOTES.length);
            sp.sendSystemMessage(Component.literal(BOSS_QUOTES[idx]).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            sp.sendOverlayMessage(Component.literal("✨ [VIP Бонус] Удача III и Скорость II активированы! Положите кубок в автомат для VIP-спина!").withStyle(ChatFormatting.GREEN));

            sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 2400, 2));
            sp.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 1));
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));

            sw.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);
            sw.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.5f);
            sw.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 25, 0.4, 0.5, 0.4, 0.1);
            sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 20, 0.4, 0.5, 0.4, 0.12);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
