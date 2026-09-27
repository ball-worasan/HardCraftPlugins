package com.hardcraft.quests;

import com.hardcraft.items.api.ItemsApi;
import java.util.Map;import java.util.UUID;
import org.bukkit.inventory.Inventory;import org.bukkit.inventory.ItemStack;

final class RewardDelivery {
    private final ItemsApi items;
    RewardDelivery(ItemsApi items){this.items=items;}
    boolean contains(Inventory inventory,String rewardId){for(ItemStack item:inventory.getContents())if(rewardId.equals(items.read(item).metadata().get("reward_id")))return true;return false;}
    boolean deliver(Inventory inventory,UUID owner,String questId,String rewardId,String itemId){
        if(contains(inventory,rewardId))return true;
        ItemStack reward=items.create(itemId,Map.of("source","quest","quest_id",questId,"reward_id",rewardId),owner);
        return inventory.addItem(reward).isEmpty();
    }
}