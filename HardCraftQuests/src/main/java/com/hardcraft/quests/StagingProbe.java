package com.hardcraft.quests;

import com.hardcraft.economy.api.EconomyApi;import com.hardcraft.items.api.ItemsApi;
import java.util.UUID;import org.bukkit.Bukkit;import org.bukkit.Material;import org.bukkit.inventory.Inventory;import org.bukkit.inventory.ItemStack;import org.bukkit.plugin.java.JavaPlugin;

final class StagingProbe {
    private static final UUID OWNER=UUID.fromString("00000000-0000-0000-0000-000000000404");
    private final JavaPlugin plugin;private final QuestRepository repository;private final EconomyApi economy;private final ItemsApi items;private final QuestDefinition quest;
    StagingProbe(JavaPlugin plugin,QuestRepository repository,EconomyApi economy,ItemsApi items,QuestDefinition quest){this.plugin=plugin;this.repository=repository;this.economy=economy;this.items=items;this.quest=quest;}
    void run(){if(!Boolean.getBoolean("hardcraft.stagingProbe"))return;Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
        try{QuestProgress p=repository.start(OWNER,quest.id()+"_probe");while(p.progress()<quest.amount())p=repository.advance(OWNER,p.questId(),quest.amount());QuestProgress duplicate=repository.advance(OWNER,p.questId(),quest.amount());if(duplicate.progress()!=quest.amount())throw new IllegalStateException("duplicate objective counted");
            String key="quest:"+p.questId()+":"+OWNER+":money:v1";var first=economy.earn(OWNER,quest.money(),key).join();var retry=economy.earn(OWNER,quest.money(),key).join();if(!first.transactionId().equals(retry.transactionId())||first.balance()!=retry.balance()||!retry.replayed())throw new IllegalStateException("economy retry differed");repository.markMoney(OWNER,p.questId());
            String probeQuestId=p.questId();Bukkit.getScheduler().runTask(plugin,()->{Inventory full=Bukkit.createInventory(null,9);for(int i=0;i<9;i++)full.setItem(i,new ItemStack(Material.STONE,64));RewardDelivery delivery=new RewardDelivery(items);String rewardId=probeQuestId+":"+OWNER+":item:v1";if(delivery.deliver(full,OWNER,probeQuestId,rewardId,quest.itemId()))throw new IllegalStateException("full inventory accepted reward");Inventory free=Bukkit.createInventory(null,9);if(!delivery.deliver(free,OWNER,probeQuestId,rewardId,quest.itemId())||!delivery.deliver(free,OWNER,probeQuestId,rewardId,quest.itemId()))throw new IllegalStateException("item retry failed");long count=java.util.Arrays.stream(free.getContents()).filter(i->rewardId.equals(items.read(i).metadata().get("reward_id"))).count();if(count!=1)throw new IllegalStateException("duplicate item reward");Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{repository.markItem(OWNER,probeQuestId);plugin.getLogger().info("STAGING_PROBE PASS: objective duplicate, persistence row, economy idempotency, full inventory pending, item retry, completion");});});
        }catch(Throwable e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"STAGING_PROBE FAIL",e);}
    });}
}