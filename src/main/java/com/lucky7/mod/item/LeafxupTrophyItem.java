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
 * Special streamer item & Easter Egg dedicated to leafxup (Лификсап).
 * Can be used in hand to activate the "Streamer Luck Boost" or placed into the Lucky 7 Slot Machine
 * as a 777-point VIP stake that guarantees a legendary Streamer Jackpot!
 */
public class LeafxupTrophyItem extends Item {
    private static final String[] LEAFXUP_QUOTES = {
        "🍃 [LEAFXUP EDITION] «Чат, ну сейчас точно выпадет 777, я чувствую этот занос!»",
        "🍃 [LEAFXUP EDITION] «Подождите, это что, реально сюжетное казино в Майнкрафте?! Десять из десяти!»",
        "🍃 [LEAFXUP EDITION] «Так, стоп, почему за мной бегут два скелета из налоговой в золотых трусах?!»",
        "🍃 [LEAFXUP EDITION] «Последний спин и точно идём копать шахту... Ладно, ещё один бесплатный спин!»",
        "🍃 [LEAFXUP EDITION] «Дон Криперионе, брат, я отдам алмазы в следующей серии, зуб даю!»"
    };

    public LeafxupTrophyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sw) {
            int idx = sw.getRandom().nextInt(LEAFXUP_QUOTES.length);
            sp.sendSystemMessage(Component.literal(LEAFXUP_QUOTES[idx]).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            sp.sendSystemMessage(Component.literal("✨ [Бонус Лификсапа] Вы получили эффект Удачи III и Скорости II! (Или положите этот трофей в слот ставки автомата Lucky 7 для Стримерского Джекпота!)").withStyle(ChatFormatting.GOLD), true);

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
        return true; // Permanent enchanted glint
    }
}
