package com.hardcraft.economy;

import com.hardcraft.economy.api.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class DefaultEconomyService implements EconomyApi, AutoCloseable {
    private final EconomyRepository repository;
    private final ExecutorService databaseExecutor = Executors.newFixedThreadPool(4, Thread.ofPlatform().name("HardCraftEconomy-DB-", 0).factory());

    public DefaultEconomyService(EconomyRepository repository) { this.repository = repository; }
    @Override public CompletableFuture<Long> balance(UUID owner) { return submit(() -> repository.balance(owner)); }
    @Override public CompletableFuture<TransactionResult> earn(UUID owner,long amount,String key){return submit(()->repository.earn(owner,amount,key));}
    @Override public CompletableFuture<TransactionResult> spend(UUID owner,long amount,String key){return submit(()->repository.spend(owner,amount,key));}
    @Override public CompletableFuture<TransferResult> transfer(UUID source,UUID destination,long amount,String key){return submit(()->repository.transfer(source,destination,amount,key));}
    @Override public CompletableFuture<List<LedgerEntry>> ledger(UUID owner){return submit(()->repository.ledger(owner));}
    private <T> CompletableFuture<T> submit(Supplier<T> operation){return CompletableFuture.supplyAsync(operation,databaseExecutor);}
    @Override public void close(){databaseExecutor.shutdown();try{if(!databaseExecutor.awaitTermination(15,TimeUnit.SECONDS))databaseExecutor.shutdownNow();}catch(InterruptedException failure){databaseExecutor.shutdownNow();Thread.currentThread().interrupt();}}
}