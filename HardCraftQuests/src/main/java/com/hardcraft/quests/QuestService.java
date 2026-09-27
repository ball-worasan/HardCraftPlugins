package com.hardcraft.quests;

import com.hardcraft.economy.api.EconomyApi;import com.hardcraft.items.api.ItemsApi;
import java.util.UUID;import java.util.concurrent.*;import java.util.function.Supplier;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.plugin.java.JavaPlugin;

final class QuestService implements AutoCloseable {
    private enum ItemResult { DELIVERED, FULL, MISMATCH }
    private final JavaPlugin plugin;private final QuestRepository repository;private final QuestDefinition quest;private final EconomyApi economy;private final RewardDelivery delivery;
    private final ExecutorService database=Executors.newSingleThreadExecutor(Thread.ofPlatform().name("HardCraftQuests-DB").factory());
    QuestService(JavaPlugin plugin,QuestRepository repository,QuestDefinition quest,EconomyApi economy,ItemsApi items){this.plugin=plugin;this.repository=repository;this.quest=quest;this.economy=economy;delivery=new RewardDelivery(items);}
    CompletableFuture<QuestProgress> start(UUID owner){return db(()->repository.start(owner,quest.id()));}
    CompletableFuture<QuestProgress> progress(UUID owner){return db(()->repository.find(owner,quest.id()).orElse(null));}
    CompletableFuture<QuestProgress> advance(Player player){return db(()->repository.advance(player.getUniqueId(),quest.id(),quest.amount())).thenCompose(p->p.state()==QuestProgress.State.REWARDING?reward(player,p):CompletableFuture.completedFuture(p));}
    CompletableFuture<QuestProgress> resume(Player player){return progress(player.getUniqueId()).thenCompose(p->p!=null&&p.state()==QuestProgress.State.REWARDING?reward(player,p):CompletableFuture.completedFuture(p));}
    CompletableFuture<QuestProgress> inspect(UUID owner,String actor){return db(()->{QuestProgress p=repository.find(owner,quest.id()).orElseThrow(()->new IllegalArgumentException("quest progress not found"));repository.audit(owner,quest.id(),actor,"ADMIN_INSPECT",p.toString());return p;});}
    CompletableFuture<QuestProgress> resolve(UUID owner,String actor,boolean confirm){return db(()->repository.resolve(owner,quest.id(),actor,confirm));}
    private CompletableFuture<QuestProgress> reward(Player player,QuestProgress initial){
        UUID owner=player.getUniqueId();String rewardId=quest.id()+":"+owner+":item:v1";
        CompletableFuture<QuestProgress> money=initial.moneyPaid()?CompletableFuture.completedFuture(initial):economy.earn(owner,quest.money(),"quest:"+quest.id()+":"+owner+":money:v1").thenCompose(ignored->db(()->repository.markMoney(owner,quest.id())));
        return money.thenCompose(p->{if(p.itemDelivered())return CompletableFuture.completedFuture(p);return main(()->{
            if(delivery.contains(player.getInventory(),rewardId)){player.sendMessage("[Quest] Reward mismatch detected. Delivery locked for admin review.");return ItemResult.MISMATCH;}
            if(!delivery.deliver(player.getInventory(),owner,quest.id(),rewardId,quest.itemId())){player.sendMessage("[Quest] Reward item pending: free one inventory slot, then /quest progress");return ItemResult.FULL;}
            return ItemResult.DELIVERED;
        }).thenCompose(result->switch(result){case FULL->CompletableFuture.completedFuture(p);case MISMATCH->db(()->repository.requireReview(owner,quest.id(),"SYSTEM","DB says undelivered but reward_id exists in inventory"));case DELIVERED->db(()->repository.markItem(owner,quest.id()));});});
    }
    String describe(QuestProgress p){if(p==null)return "[Quest] "+quest.title()+": /quest start | Break "+quest.amount()+" oak logs | Rewards: "+quest.money()+" coins + Wayfinder";if(p.state()==QuestProgress.State.COMPLETED)return "[Quest] "+quest.title()+" completed | Rewards delivered | Next: "+quest.next();if(p.state()==QuestProgress.State.REVIEW_REQUIRED)return "[Quest] Reward locked: admin review required";return "[Quest] "+quest.title()+": break oak logs "+p.progress()+"/"+quest.amount()+" | Rewards: "+quest.money()+" coins + Wayfinder"+(p.state()==QuestProgress.State.REWARDING?" | Reward delivery pending":"");}
    private <T> CompletableFuture<T> db(Supplier<T> work){return CompletableFuture.supplyAsync(work,database);}
    private <T> CompletableFuture<T> main(Supplier<T> work){CompletableFuture<T> f=new CompletableFuture<>();Bukkit.getScheduler().runTask(plugin,()->{try{f.complete(work.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    @Override public void close(){database.shutdown();try{if(!database.awaitTermination(15,TimeUnit.SECONDS))database.shutdownNow();}catch(InterruptedException e){database.shutdownNow();Thread.currentThread().interrupt();}}
}