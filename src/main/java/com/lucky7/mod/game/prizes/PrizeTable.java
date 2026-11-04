package com.lucky7.mod.game.prizes;

import com.lucky7.mod.config.Lucky7Config;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The machine is pre-stocked with ore prizes. The player never removes them;
 * a win is paid out as a fresh stack straight into the winner's inventory.
 */
public final class PrizeTable {
    public record Prize(Item item, int value, int weight) { }

    private PrizeTable() { }

    public static List<Prize> showcase() {
        List<Prize> prizes=new ArrayList<>();
        List<Map.Entry<String,Integer>> table=Lucky7Config.INSTANCE.lootTable();
        for(int i=table.size()-1;i>=0;i--) {
            Map.Entry<String,Integer> entry=table.get(i);
            Identifier id=Identifier.tryParse(entry.getKey());
            if(id==null||!Registries.ITEM.containsId(id)) continue;
            Item item=Registries.ITEM.get(id);
            if(item==Items.AIR) continue;
            prizes.add(new Prize(item,entry.getValue(),weightFor(entry.getValue())));
            if(prizes.size()>=9) break;
        }
        return prizes;
    }

    /** Cheaper ore pays out more often, the rare ore stays rare. */
    private static int weightFor(int value) {
        return Math.max(1,120/Math.max(1,value));
    }

    public static ItemStack payout(int payoutValue) {
        if(payoutValue<=0) return ItemStack.EMPTY;
        List<Prize> prizes=showcase();
        if(prizes.isEmpty()) return ItemStack.EMPTY;
        Prize best=prizes.get(0);
        for(Prize prize:prizes) {
            if(prize.value()<=payoutValue&&prize.value()>=best.value()) best=prize;
        }
        int count=Math.max(1,payoutValue/Math.max(1,best.value()));
        if(count>64) count=64;
        return new ItemStack(best.item(),count);
    }

    public static int valueOf(ItemStack stack) {
        if(stack.isEmpty()) return 0;
        return Lucky7Config.value(stack.getItem())*stack.getCount();
    }
}
